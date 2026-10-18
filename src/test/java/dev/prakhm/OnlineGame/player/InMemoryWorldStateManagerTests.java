package dev.prakhm.OnlineGame.player;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registration and teardown of sessions under concurrency.
 *
 * <p>Worth testing hard because websocket handlers run on many threads at once: joins, drops and a
 * disconnect racing its own arena opponent all hit the manager simultaneously. The invariant is
 * that the registry never resolves a session through one index but not another, and that a
 * username is only ever claimed once.
 */
class InMemoryWorldStateManagerTests
{
    private static final int THREADS = 16;
    private static final int SHIRE = 0;
    private static final int BREE = 1;
    private static final long TIMEOUT_SECONDS = 30;

    private final InMemoryWorldStateManager worldState = new InMemoryWorldStateManager();
    private ExecutorService pool;

    @BeforeEach
    void setUp()
    {
        pool = Executors.newFixedThreadPool(THREADS);
    }

    @AfterEach
    void tearDown()
    {
        pool.shutdownNow();
    }

    @Test
    void aRegisteredSessionIsReachableThroughEveryIndex()
    {
        PlayerSession session = worldState.add(session("Alpha", "ws-1", SHIRE)).orElseThrow();

        assertEquals(1, worldState.size());
        assertSame(session, worldState.findByPlayerId(session.getPlayerId()).orElseThrow());
        assertSame(session, worldState.findByUsername("Alpha").orElseThrow());
        assertSame(session, worldState.findByWebsocketSessionId("ws-1").orElseThrow());
        assertEquals(List.of(session), List.copyOf(worldState.findByMap(SHIRE)));
    }

    @Test
    void removingASessionClearsEveryIndex()
    {
        PlayerSession session = worldState.add(session("Alpha", "ws-1", SHIRE)).orElseThrow();

        assertSame(session, worldState.remove(session.getPlayerId()).orElseThrow());

        assertEquals(0, worldState.size());
        assertTrue(worldState.findByPlayerId(session.getPlayerId()).isEmpty(), "player id index should be empty");
        assertTrue(worldState.findByUsername("Alpha").isEmpty(), "username index should be empty");
        assertTrue(worldState.findByWebsocketSessionId("ws-1").isEmpty(), "websocket index should be empty");
        assertTrue(worldState.findByMap(SHIRE).isEmpty(), "map index should be empty");
    }

    @Test
    void aUsernameCanOnlyBeClaimedOnce()
    {
        assertTrue(worldState.add(session("Alpha", "ws-1", SHIRE)).isPresent());
        assertTrue(worldState.add(session("Alpha", "ws-2", SHIRE)).isEmpty(), "the second Alpha is rejected");
        assertEquals(1, worldState.size(), "the rejected session is not stored");
        assertEquals("ws-1", worldState.findByUsername("Alpha").orElseThrow().getWebsocketSessionId());
    }

    @Test
    void reindexingFollowsAPlayerThroughAPortal()
    {
        PlayerSession session = worldState.add(session("Alpha", "ws-1", SHIRE)).orElseThrow();

        session.getPlayer().setLocation(BREE);
        assertTrue(worldState.reindex(session));

        assertTrue(worldState.findByMap(SHIRE).isEmpty(), "Alpha should no longer be filed under the Shire");
        assertEquals(List.of(session), List.copyOf(worldState.findByMap(BREE)));

        worldState.remove(session.getPlayerId());
        assertTrue(worldState.findByMap(BREE).isEmpty(), "the Bree bucket should be dropped once empty");
    }

    @Test
    void reindexingTwiceIsHarmless()
    {
        PlayerSession session = worldState.add(session("Alpha", "ws-1", SHIRE)).orElseThrow();

        assertTrue(worldState.reindex(session));
        assertTrue(worldState.reindex(session));

        assertEquals(List.of(session), List.copyOf(worldState.findByMap(SHIRE)));
    }

    @Test
    void concurrentAddsOfDistinctPlayersAllSurvive() throws Exception
    {
        int perThread = 40;

        awaitAll(inParallel(thread -> () -> {
            for (int i = 0; i < perThread; i++)
            {
                String username = "P" + thread + "-" + i;
                worldState.add(session(username, "ws-" + username, thread % 2)).orElseThrow();
            }
            return null;
        }));

        int expected = THREADS * perThread;
        assertEquals(expected, worldState.size());
        assertEquals(expected, worldState.findByMap(SHIRE).size() + worldState.findByMap(BREE).size(),
                "every session should be filed under exactly one map");
        assertIndexesAgree();
    }

    @Test
    void concurrentAddsOfTheSameUsernameElectExactlyOneWinner() throws Exception
    {
        List<Future<PlayerSession>> attempts = inParallel(thread ->
                () -> worldState.add(session("Alpha", "ws-" + thread, SHIRE)).orElse(null));

        int winners = 0;
        for (Future<PlayerSession> attempt : attempts)
            if (await(attempt) != null) winners++;

        assertEquals(1, winners, "exactly one Alpha should be registered");
        assertEquals(1, worldState.size());
        assertEquals(1, worldState.findByMap(SHIRE).size(), "the losing sessions left no map entry");

        PlayerSession registered = worldState.findByUsername("Alpha").orElseThrow();
        assertSame(registered, worldState.findByWebsocketSessionId(registered.getWebsocketSessionId()).orElseThrow(),
                "only the winning session's websocket id should resolve");
        assertIndexesAgree();
    }

    @Test
    void concurrentAddThenRemoveOfTheSamePlayersLeavesNothingBehind() throws Exception
    {
        int perThread = 60;

        awaitAll(inParallel(thread -> () -> {
            for (int i = 0; i < perThread; i++)
            {
                String username = "P" + thread + "-" + i;
                PlayerSession session = session(username, "ws-" + username, thread % 2);
                worldState.add(session).orElseThrow();
                worldState.remove(session.getPlayerId()).orElseThrow();
            }
            return null;
        }));

        assertEquals(0, worldState.size());
        assertTrue(worldState.findByMap(SHIRE).isEmpty(), "no Shire bucket should survive");
        assertTrue(worldState.findByMap(BREE).isEmpty(), "no Bree bucket should survive");
        assertIndexesAgree();
    }

    @Test
    void addingAndRemovingOneSharedSessionConcurrentlyStaysConsistent() throws Exception
    {
        PlayerSession shared = session("Alpha", "ws-1", SHIRE);

        awaitAll(inParallel(thread -> () -> {
            for (int i = 0; i < 60; i++)
            {
                worldState.add(shared);
                worldState.remove(shared.getPlayerId());
            }
            return null;
        }));

        // Who ends up holding the session is a race. What must hold is that the indexes agree.
        worldState.remove(shared.getPlayerId());
        assertEquals(0, worldState.size());
        assertIndexesAgree();
    }

    @Test
    void concurrentDisconnectsOfDistinctSessionsLeaveAGoneWorld() throws Exception
    {
        int perThread = 40;
        List<PlayerSession> registered = new ArrayList<>();
        for (int thread = 0; thread < THREADS; thread++)
            for (int i = 0; i < perThread; i++)
                registered.add(worldState.add(session("P" + thread + "-" + i, "ws-" + thread + "-" + i,
                        thread % 2)).orElseThrow());
        assertEquals(THREADS * perThread, worldState.size());

        awaitAll(inParallel(thread -> () -> {
            for (int i = thread; i < registered.size(); i += THREADS)
                worldState.removeByWebsocketSessionId(registered.get(i).getWebsocketSessionId());
            return null;
        }));

        assertEquals(0, worldState.size());
        assertIndexesAgree();
    }

    @Test
    void removingByWebsocketSessionIdDropsTheWholeRegistration()
    {
        PlayerSession session = worldState.add(session("Alpha", "ws-1", SHIRE)).orElseThrow();

        assertSame(session, worldState.removeByWebsocketSessionId("ws-1").orElseThrow());

        assertEquals(0, worldState.size());
        assertTrue(worldState.findByUsername("Alpha").isEmpty());
        assertTrue(worldState.findByMap(SHIRE).isEmpty());
        assertTrue(worldState.removeByWebsocketSessionId("ws-1").isEmpty(), "a second removal finds nothing");
    }

    @Test
    void nullLookupsAreEmptyRatherThanAnError()
    {
        assertTrue(worldState.findByUsername(null).isEmpty());
        assertTrue(worldState.findByWebsocketSessionId(null).isEmpty());
        assertTrue(worldState.removeByWebsocketSessionId(null).isEmpty());
        assertTrue(worldState.add(null).isEmpty());
    }

    @Test
    void aDroppedSessionFreesItsUsernameForReuse()
    {
        PlayerSession first = worldState.add(session("Alpha", "ws-1", SHIRE)).orElseThrow();
        worldState.remove(first.getPlayerId());

        PlayerSession second = worldState.add(session("Alpha", "ws-2", SHIRE)).orElseThrow();

        assertEquals(1, worldState.size());
        assertSame(second, worldState.findByUsername("Alpha").orElseThrow());
        assertTrue(worldState.findByWebsocketSessionId("ws-1").isEmpty(), "the old socket must not resolve");
        assertTrue(worldState.findByWebsocketSessionId("ws-2").isPresent());
    }

    @Test
    void clearEmptiesEveryIndex()
    {
        worldState.add(session("Alpha", "ws-1", SHIRE)).orElseThrow();
        worldState.add(session("Bravo", "ws-2", BREE)).orElseThrow();

        worldState.clear();

        assertEquals(0, worldState.size());
        assertTrue(worldState.findByUsername("Alpha").isEmpty());
        assertTrue(worldState.findByUsername("Bravo").isEmpty());
        assertTrue(worldState.findByMap(SHIRE).isEmpty());
        assertTrue(worldState.findByMap(BREE).isEmpty());
        assertTrue(worldState.add(session("Alpha", "ws-3", SHIRE)).isPresent(), "the username is free again");
    }

    /**
     * Every session reachable from the main map must also resolve through the username, websocket
     * and per-map indexes, and no per-map bucket may hold a session that is no longer registered.
     */
    private void assertIndexesAgree()
    {
        Set<Long> registered = new HashSet<>();
        Set<Long> onAMap = new HashSet<>();

        for (PlayerSession session : worldState.findAll())
        {
            registered.add(session.getPlayerId());
            assertSame(session, worldState.findByPlayerId(session.getPlayerId()).orElse(null),
                    session + " should resolve by player id");
            assertSame(session, worldState.findByUsername(session.getUsername()).orElse(null),
                    session + " should resolve by username");
            if (session.getWebsocketSessionId() != null)
                assertSame(session, worldState.findByWebsocketSessionId(session.getWebsocketSessionId()).orElse(null),
                        session + " should resolve by websocket session id");
        }

        for (int mapId : List.of(SHIRE, BREE))
            for (PlayerSession session : worldState.findByMap(mapId))
            {
                onAMap.add(session.getPlayerId());
                assertSame(session, worldState.findByPlayerId(session.getPlayerId()).orElse(null),
                        "a map bucket must not outlive its session");
                assertEquals(mapId, session.getCurrentMap(), "a map bucket must hold sessions on that map");
            }

        assertEquals(registered.size(), worldState.size(), "no duplicate registrations");
        assertEquals(registered, onAMap, "the per-map index should cover exactly the registered sessions");
    }

    private PlayerSession session(String username, String websocketSessionId, int mapId)
    {
        Player player = new Player();
        player.setTag(username);
        player.assignId();
        player.setLocation(mapId);
        return new PlayerSession(player, websocketSessionId);
    }

    /** Runs one task per thread, all released from a barrier so they genuinely overlap. */
    private <T> List<Future<T>> inParallel(IntFunction<Callable<T>> taskForThread) throws Exception
    {
        CyclicBarrier startTogether = new CyclicBarrier(THREADS);
        List<Future<T>> futures = new ArrayList<>();
        for (int thread = 0; thread < THREADS; thread++)
        {
            Callable<T> task = taskForThread.apply(thread);
            futures.add(pool.submit(() ->
            {
                startTogether.await(10, TimeUnit.SECONDS);
                return task.call();
            }));
        }
        return futures;
    }

    private void awaitAll(List<? extends Future<?>> futures) throws Exception
    {
        for (Future<?> future : futures) await(future);
    }

    private <T> T await(Future<T> future) throws Exception
    {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
