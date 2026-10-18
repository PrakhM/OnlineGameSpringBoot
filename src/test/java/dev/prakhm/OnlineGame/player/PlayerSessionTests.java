package dev.prakhm.OnlineGame.player;

import dev.prakhm.OnlineGame.beast.Beast;
import dev.prakhm.OnlineGame.world.TileType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a session exposes about the player it is driving: position is read through to the profile,
 * and the state enum is derived from the profile rather than stored beside it.
 */
class PlayerSessionTests
{
    @Test
    void aSessionNeedsAnIdentifiedPlayer()
    {
        assertThrows(IllegalArgumentException.class, () -> new PlayerSession(null, "ws-1"));
        assertThrows(IllegalArgumentException.class, () -> new PlayerSession(new Player(), "ws-1"),
                "a player without an assigned id cannot be tracked");
    }

    @Test
    void positionAndMapAreReadThroughToTheProfile()
    {
        Player player = player("Alpha");
        player.setLocation(3);
        player.setCoordinates(new int[] {7, 9});
        PlayerSession session = new PlayerSession(player, "ws-1");

        assertEquals(player.getId(), session.getPlayerId());
        assertEquals("Alpha", session.getUsername());
        assertEquals(3, session.getCurrentMap());
        assertEquals(7, session.getX());
        assertEquals(9, session.getY());
        assertArrayEquals(new int[] {7, 9}, session.getPosition());

        player.setCoordinates(new int[] {8, 9});

        assertEquals(8, session.getX(), "the session must not cache a stale coordinate");
        assertEquals(3, session.getCurrentMap());
    }

    @Test
    void stateIsFreeUntilSomethingHappens()
    {
        PlayerSession session = new PlayerSession(player("Alpha"), "ws-1");

        assertEquals(PlayerState.FREE, session.getState());
    }

    @Test
    void standingOnTheArenaTileIsAQueueNotABattle()
    {
        Player player = player("Alpha");
        player.setActivity(TileType.ARENA.code());
        PlayerSession session = new PlayerSession(player, "ws-1");

        assertEquals(PlayerState.IN_ARENA_QUEUE, session.getState());
        assertTrue(!session.isInBattle());
        assertTrue(!session.isInWildEncounter());
    }

    @Test
    void aWildBeastPutsThePlayerInAnEncounter()
    {
        Player player = player("Alpha");
        PlayerSession session = new PlayerSession(player, "ws-1");

        player.setEncounterBeast(new Beast("FIRE"));

        assertEquals(PlayerState.IN_WILD_ENCOUNTER, session.getState());
        assertTrue(session.isInWildEncounter());
    }

    @Test
    void anOpponentMeansAnArenaBattle()
    {
        Player player = player("Alpha");
        player.setActivity(TileType.ARENA.code());
        PlayerSession session = new PlayerSession(player, "ws-1");

        player.setOpponent("Bravo");

        assertEquals(PlayerState.IN_BATTLE, session.getState());
        assertTrue(session.isInBattle());
    }

    @Test
    void anArenaBattleOutranksAWildEncounter()
    {
        Player player = player("Alpha");
        player.setEncounterBeast(new Beast("FIRE"));
        PlayerSession session = new PlayerSession(player, "ws-1");

        player.setOpponent("Bravo");

        assertEquals(PlayerState.IN_BATTLE, session.getState(),
                "being paired should win over a wild encounter that has not been cleared");
    }

    @Test
    void movingStampsTheSession()
    {
        PlayerSession session = new PlayerSession(player("Alpha"), "ws-1");

        assertEquals(session.getConnectedAt(), session.getLastMove(), "a session starts out never having moved");

        session.markMoved();

        assertTrue(session.getLastMove().isAfter(session.getConnectedAt()) ||
                        session.getLastMove().equals(session.getConnectedAt()),
                "the last move should be at or after the connection");
    }

    @Test
    void lockingTwoSessionsRunsTheBodyExactlyOnce()
    {
        PlayerSession alpha = new PlayerSession(player("Alpha"), "ws-1");
        PlayerSession bravo = new PlayerSession(player("Bravo"), "ws-2");
        AtomicInteger runs = new AtomicInteger();

        PlayerSession.lockInOrder(alpha, bravo, runs::incrementAndGet);
        PlayerSession.lockInOrder(bravo, alpha, runs::incrementAndGet);
        PlayerSession.lockInOrder(alpha, alpha, runs::incrementAndGet);

        assertEquals(3, runs.get());
    }

    @Test
    void lockingTwoSessionsInEitherOrderNeverDeadlocks() throws Exception
    {
        PlayerSession alpha = new PlayerSession(player("Alpha"), "ws-1");
        PlayerSession bravo = new PlayerSession(player("Bravo"), "ws-2");
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);

        Thread forward = new Thread(() -> race(alpha, bravo, start, ran));
        Thread backward = new Thread(() -> race(bravo, alpha, start, ran));
        forward.start();
        backward.start();
        start.countDown();
        forward.join(TimeUnit.SECONDS.toMillis(10));
        backward.join(TimeUnit.SECONDS.toMillis(10));

        assertTrue(!forward.isAlive() && !backward.isAlive(),
                "both threads should finish; opposing lock order must not deadlock");
        assertTrue(ran.get(), "the body should have run");
    }

    @Test
    void idsAreUniqueAcrossPlayers()
    {
        Player first = player("Alpha");
        Player second = player("Bravo");

        assertNotEquals(first.getId(), second.getId());
        assertTrue(first.getId() > 0 && second.getId() > 0);
    }

    private void race(PlayerSession first, PlayerSession second, CountDownLatch start, AtomicBoolean ran)
    {
        try
        {
            start.await(5, TimeUnit.SECONDS);
            for (int i = 0; i < 2000; i++) PlayerSession.lockInOrder(first, second, () -> ran.set(true));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private Player player(String username)
    {
        Player player = new Player();
        player.setTag(username);
        return player.assignId();
    }
}
