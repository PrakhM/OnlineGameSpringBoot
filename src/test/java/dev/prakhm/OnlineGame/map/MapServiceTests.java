package dev.prakhm.OnlineGame.map;

import dev.prakhm.OnlineGame.beast.Beast;
import dev.prakhm.OnlineGame.config.GameMapProperties;
import dev.prakhm.OnlineGame.player.InMemoryWorldStateManager;
import dev.prakhm.OnlineGame.player.Player;
import dev.prakhm.OnlineGame.player.PlayerSession;
import dev.prakhm.OnlineGame.player.WorldStateManager;
import dev.prakhm.OnlineGame.world.Bree;
import dev.prakhm.OnlineGame.world.GameMap;
import dev.prakhm.OnlineGame.world.MapRegistry;
import dev.prakhm.OnlineGame.world.Shire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ExecutorSubscribableChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What one direction from the browser does and does not do to the world.
 *
 * <p>The maps are the real ones from {@code /maps}, since most of what is pinned down here is what
 * the shipped geometry actually does. Walking to a portal is thirty-odd steps, done here directly
 * rather than over a socket.
 */
class MapServiceTests
{
    private static final int SHIRE = GameMap.SHIRE_ID;
    private static final int BREE = GameMap.BREE_ID;
    private static final String SHIRE_TOPIC = "/topic/map/shire";
    private static final String BREE_TOPIC = "/topic/map/bree";
    private static final String ALPHA_COLOUR = "#ff0000";
    private static final String BRAVO_COLOUR = "#00ff00";

    private MapRegistry registry;
    private WorldStateManager worldState;
    private RecordingTemplate template;
    private MapBroadcaster broadcaster;
    private MapService service;

    @BeforeEach
    void setUp()
    {
        GameMapProperties mapProperties = new GameMapProperties();
        registry = new MapRegistry(List.of(new Bree(mapProperties), new Shire(mapProperties)));
        worldState = new InMemoryWorldStateManager();
        template = new RecordingTemplate();
        broadcaster = new MapBroadcaster(template, registry, worldState);
        // No post-move listener: encounters belong to the controller, and a random encounter here
        // would freeze the player in place and make every later assertion meaningless.
        service = serviceWith(List.of());
    }

    // ─── publishing ───────────────────────────────────────────────────────────

    @Test
    void aStepOnAnOpenTileIsTakenAndPublishedToThatMapOnly()
    {
        PlayerSession alpha = join("Alpha");

        move(alpha, MoveInput.DOWN, 1);

        assertPosition(alpha, 1, 2, SHIRE);
        assertEquals(List.of(new PlayerPosition("Alpha", ALPHA_COLOUR, 1, 2, SHIRE)), template.sentTo(SHIRE_TOPIC));
        assertTrue(template.sentTo(BREE_TOPIC).isEmpty(), "the other map must hear nothing");
        assertEquals(PlayerEvent.MOVE, template.lastPrivate().type());
        assertEquals(1, template.lastPrivate().sequence(), "the reply names the input it answers");
        assertEquals("shire", template.lastPrivate().mapName());
    }

    @Test
    void thePositionPublishedCarriesNothingButWhatIsNeededToDrawSomeone()
    {
        PlayerSession alpha = join("Alpha");
        alpha.getPlayer().getBeasts().add(new Beast("FIRE"));

        move(alpha, MoveInput.DOWN, 1);

        PlayerPosition published = template.sentTo(SHIRE_TOPIC).get(0);
        List<String> fields = Arrays.stream(PlayerPosition.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();
        assertEquals(List.of("tag", "colour", "x", "y", "map"), fields,
                "the map topic must not start carrying profiles");
        assertEquals(ALPHA_COLOUR, published.colour());
    }

    @Test
    void aStepIsAnnouncedOnlyToTheMoverPrivately()
    {
        PlayerSession alpha = join("Alpha");

        move(alpha, MoveInput.DOWN, 1);

        assertEquals(1, template.privates.size(), "nobody else is told anything about it");
        assertEquals("Alpha", template.lastPrivate().player().getTag());
        assertEquals(SHIRE, template.lastPrivate().player().getLocation());
    }

    // ─── what a refused step may not do ───────────────────────────────────────

    @Test
    void aStepIntoAWallIsRefusedAndChangesNothing()
    {
        PlayerSession alpha = join("Alpha");
        walkDown(alpha, 13);
        int mode = alpha.getPlayer().getMode();
        template.clear();

        move(alpha, MoveInput.DOWN, 99);

        assertPosition(alpha, 1, 13, SHIRE);
        assertEquals(mode, alpha.getPlayer().getMode(), "a refused step must not touch the profile");
        assertTrue(template.sentTo(SHIRE_TOPIC).isEmpty());
        assertEquals(PlayerEvent.REJECTED, template.lastPrivate().type());
    }

    @Test
    void aStepOffTheEdgeOfTheMapIsRefusedAndChangesNothing()
    {
        PlayerSession alpha = join("Alpha");
        template.clear();

        move(alpha, MoveInput.LEFT, 1);

        assertPosition(alpha, 1, 1, SHIRE);
        assertTrue(template.sentTo(SHIRE_TOPIC).isEmpty());
        assertEquals(PlayerEvent.REJECTED, template.lastPrivate().type());
    }

    @Test
    void aTileSomebodyElseIsStandingOnIsRefusedAndChangesNothing()
    {
        PlayerSession alpha = join("Alpha");
        PlayerSession bravo = join("Bravo");
        template.clear();

        move(alpha, MoveInput.RIGHT, 1);

        assertPosition(alpha, 1, 1, SHIRE);
        assertPosition(bravo, 2, 1, SHIRE, "two players cannot share a tile");
        assertTrue(template.sentTo(SHIRE_TOPIC).isEmpty());
        assertEquals(PlayerEvent.REJECTED, template.lastPrivate().type());
    }

    @Test
    void aDirectionTheServerDoesNotKnowIsIgnored()
    {
        PlayerSession alpha = join("Alpha");
        template.clear();

        move(alpha, 7, 1);
        move(alpha, -1, 2);

        assertPosition(alpha, 1, 1, SHIRE);
        assertTrue(template.sentTo(SHIRE_TOPIC).isEmpty());
        assertEquals(PlayerEvent.REJECTED, template.lastPrivate().type());
        assertEquals("shire", template.lastPrivate().mapName());
    }

    @Test
    void aPlayerWhoNeverJoinedCannotMove()
    {
        SimpMessageHeaderAccessor stranger = SimpMessageHeaderAccessor.create();
        stranger.setSessionId("ws-somebody-else");

        service.input(new MoveInput(MoveInput.DOWN, 1), stranger);
        service.input(null, stranger);

        assertTrue(template.sentTo(SHIRE_TOPIC).isEmpty());
        assertTrue(template.privates.isEmpty(), "there is nobody to reply to");
    }

    @Test
    void aPlayerMidBattleCannotWalkAwayFromIt()
    {
        PlayerSession alpha = join("Alpha");
        alpha.getPlayer().setEncounterBeast(new Beast("FIRE"));
        template.clear();

        move(alpha, MoveInput.DOWN, 1);

        assertPosition(alpha, 1, 1, SHIRE);
        assertTrue(template.sentTo(SHIRE_TOPIC).isEmpty());
        assertEquals(PlayerEvent.REJECTED, template.lastPrivate().type());
    }

    @Test
    void aRefusedStepLeavesNoOccupancyBehind()
    {
        PlayerSession alpha = join("Alpha");
        template.clear();

        move(alpha, MoveInput.LEFT, 1);

        assertPosition(alpha, 1, 1, SHIRE);
        assertFalse(registry.byId(SHIRE).isOccupied(0, 1),
                "the tile the refused step aimed at must be exactly as it was");
        assertTrue(registry.byId(SHIRE).isOccupied(1, 1), "and the player is still standing where they were");
    }

    // ─── portals ──────────────────────────────────────────────────────────────

    @Test
    void walkingIntoAPortalCarriesThePlayerToTheOtherMap()
    {
        PlayerSession alpha = join("Alpha");
        walkToPortal(alpha);
        template.clear();

        move(alpha, MoveInput.RIGHT, 42);

        assertPosition(alpha, 0, 11, BREE, "arriving from a lower map id lands on the near edge");

        assertEquals(List.of(new PlayerPosition("Alpha", ALPHA_COLOUR, 0, 11, BREE)), template.sentTo(SHIRE_TOPIC),
                "the map being left has to hear about it, naming the map they are now on");
        assertEquals(List.of(new PlayerPosition("Alpha", ALPHA_COLOUR, 0, 11, BREE)), template.sentTo(BREE_TOPIC));

        PlayerEvent event = template.lastPrivate();
        assertEquals(PlayerEvent.MAP_CHANGED, event.type());
        assertEquals("bree", event.mapName(), "the traveller is told which topic to move to");
        assertEquals(42, event.sequence());
        assertTrue(event.occupants().stream().anyMatch(occupant -> occupant.tag().equals("Alpha")),
                "the new map's roster comes with the event: a topic has no replay");
    }

    @Test
    void theWorldIndexFollowsAPlayerThroughAPortal()
    {
        PlayerSession alpha = join("Alpha");
        walkToPortal(alpha);

        move(alpha, MoveInput.RIGHT, 1);

        assertTrue(worldState.findByMap(BREE).stream().anyMatch(session -> session == alpha));
        assertFalse(worldState.findByMap(SHIRE).stream().anyMatch(session -> session == alpha),
                "the map they left must stop listing them");
    }

    @Test
    void aPortalWithItsArrivalAlreadyTakenIsRefusedAndChangesNothing()
    {
        PlayerSession alpha = join("Alpha");
        PlayerSession bravo = join("Bravo");
        walkToPortal(alpha);
        move(alpha, MoveInput.RIGHT, 1);
        assertEquals(BREE, alpha.getPlayer().getLocation());

        walkToPortal(bravo);
        template.clear();

        move(bravo, MoveInput.RIGHT, 2);

        assertPosition(bravo, 18, 11, SHIRE, "nobody may arrive on top of somebody else");
        assertTrue(template.sentTo(SHIRE_TOPIC).isEmpty());
        assertTrue(template.sentTo(BREE_TOPIC).isEmpty());
        assertEquals(PlayerEvent.REJECTED, template.lastPrivate().type());
    }

    @Test
    void aRefusedPortalDoesNotDisturbTheArrivalTileForTheNextArrival()
    {
        PlayerSession alpha = join("Alpha");
        PlayerSession bravo = join("Bravo");
        walkToPortal(alpha);
        move(alpha, MoveInput.RIGHT, 1);

        walkToPortal(bravo);
        move(bravo, MoveInput.RIGHT, 2);
        assertEquals(SHIRE, bravo.getPlayer().getLocation());

        move(alpha, MoveInput.LEFT, 3);

        assertEquals(BREE, alpha.getPlayer().getLocation(), "Alpha still has the arrival tile to themselves");
    }

    @Test
    void walkingBackThroughAPortalReturnsToTheMapTheyCameFrom()
    {
        PlayerSession alpha = join("Alpha");
        walkToPortal(alpha);
        move(alpha, MoveInput.RIGHT, 1);
        assertPosition(alpha, 0, 11, BREE);
        template.clear();

        // Arriving on a portal leaves you standing on it, so the way back is to step off and on
        // again rather than off the western edge.
        move(alpha, MoveInput.RIGHT, 2);
        assertPosition(alpha, 1, 11, BREE);

        move(alpha, MoveInput.LEFT, 3);

        assertPosition(alpha, 19, 11, SHIRE, "coming from a higher map id lands on the far edge");
        assertEquals(PlayerEvent.MAP_CHANGED, template.lastPrivate().type());
        assertEquals("shire", template.lastPrivate().mapName());
        assertEquals(BREE, template.sentTo(BREE_TOPIC).get(0).map(),
                "the departure names the map the player has gone back to");
        assertEquals(SHIRE, template.sentTo(SHIRE_TOPIC).get(0).map());
    }

    @Test
    void aPortalToNowhereIsJustAWall()
    {
        PlayerSession alpha = join("Alpha");
        // Bree's east edge is sealed portals: walkable never, so the same as a wall.
        assertEquals(30, registry.byId(BREE).tileAt(19, 4).code());
    }

    // ─── post-move listeners ──────────────────────────────────────────────────

    @Test
    void listenersHearAboutAStepButNotAboutAPortalArrival()
    {
        List<Integer> reported = new ArrayList<>();
        MapService withListener = serviceWith(List.of((session, map) -> reported.add(map.getMapId())));
        PlayerSession alpha = join("Alpha");

        withListener.input(new MoveInput(MoveInput.DOWN, 1), headersFor(alpha));
        assertEquals(List.of(SHIRE), reported, "walking onto the map is a step");

        reported.clear();
        walkToPortal(alpha);
        withListener.input(new MoveInput(MoveInput.RIGHT, 2), headersFor(alpha));
        assertTrue(reported.isEmpty(), "arriving somewhere is not walking into anything on the map left behind");
    }

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private MapService serviceWith(List<PostMoveListener> listeners)
    {
        return new MapService(registry, worldState, new PlayerSessionResolver(worldState),
                broadcaster, listeners);
    }

    /** Puts a player in the world the way a join does: on the default map's first free spawn tile. */
    private PlayerSession join(String tag)
    {
        Player player = new Player();
        player.setTag(tag);
        player.setColour(tag.equals("Alpha") ? ALPHA_COLOUR : BRAVO_COLOUR);
        player.assignId();
        GameMap spawn = registry.defaultMap();
        player.setLocation(spawn.getMapId());
        assertTrue(spawn.addPlayer(player), "the spawn row should have a free tile");

        PlayerSession session = new PlayerSession(player, "ws-" + tag);
        assertTrue(worldState.add(session).isPresent());
        return session;
    }

    private void move(PlayerSession session, int direction, long sequence)
    {
        service.input(new MoveInput(direction, sequence), headersFor(session));
    }

    private SimpMessageHeaderAccessor headersFor(PlayerSession session)
    {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(session.getWebsocketSessionId());
        return accessor;
    }

    private void assertPosition(PlayerSession session, int x, int y, int map)
    {
        assertPosition(session, x, y, map, "");
    }

    private void assertPosition(PlayerSession session, int x, int y, int map, String because)
    {
        Player player = session.getPlayer();
        assertEquals(x, player.getX(), "x: " + because);
        assertEquals(y, player.getY(), "y: " + because);
        assertEquals(map, player.getLocation(), "map: " + because);
    }

    /** Walks down the spawn column, which is clear all the way to row 13. */
    private void walkDown(PlayerSession session, int row)
    {
        long sequence = 1;
        while (session.getY() < row)
        {
            int before = session.getY();
            move(session, MoveInput.DOWN, sequence++);
            assertEquals(before + 1, session.getY(),
                    "column " + session.getX() + " should be clear down to row " + row);
        }
    }

    private void walkRight(PlayerSession session, int column)
    {
        long sequence = 1;
        while (session.getX() < column)
        {
            int before = session.getX();
            move(session, MoveInput.RIGHT, sequence++);
            assertEquals(before + 1, session.getX(),
                    "row " + session.getY() + " should be clear across to column " + column);
        }
    }

    /**
     * Walks a player to the tile in front of the Shire's east portal, at {@code (18, 11)}. The only
     * approach goes down the clear row 12 and across underneath the trees on row 11.
     */
    private void walkToPortal(PlayerSession session)
    {
        walkDown(session, 12);
        walkRight(session, 18);
        move(session, MoveInput.UP, 500);
        assertPosition(session, 18, 11, SHIRE, "should be standing in front of the portal");
    }

    private record Sent(String destination, PlayerPosition position) {}

    /**
     * A messaging template that writes down what it was asked to send instead of sending it. A
     * subclass rather than a mock because these tests are about payloads, not about call counts.
     */
    private static final class RecordingTemplate extends SimpMessagingTemplate
    {
        private final List<Sent> broadcasts = new ArrayList<>();
        private final List<PlayerEvent> privates = new ArrayList<>();

        RecordingTemplate()
        {
            super(new ExecutorSubscribableChannel());
        }

        @Override
        public void convertAndSend(String destination, Object payload)
        {
            if (payload instanceof PlayerPosition position) broadcasts.add(new Sent(destination, position));
        }

        @Override
        public void convertAndSendToUser(String user, String destination, Object payload)
        {
            if (payload instanceof PlayerEvent event) privates.add(event);
        }

        List<PlayerPosition> sentTo(String destination)
        {
            return broadcasts.stream()
                    .filter(sent -> sent.destination().equals(destination))
                    .map(Sent::position)
                    .toList();
        }

        PlayerEvent lastPrivate()
        {
            assertFalse(privates.isEmpty(), "expected a private event but none was sent");
            return privates.get(privates.size() - 1);
        }

        void clear()
        {
            broadcasts.clear();
            privates.clear();
        }
    }
}