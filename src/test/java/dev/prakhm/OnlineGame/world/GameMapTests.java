package dev.prakhm.OnlineGame.world;

import dev.prakhm.OnlineGame.beast.Beast;
import dev.prakhm.OnlineGame.config.GameMapProperties;
import dev.prakhm.OnlineGame.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of the two maps, pinned to what the single class GameMap used to do.
 *
 * <p>Scenarios are set up through legal moves only, so they exercise the same path a player takes.
 */
class GameMapTests
{
    private GameMapProperties properties;
    private Shire shire;
    private Bree bree;
    private MapRegistry registry;

    @BeforeEach
    void setUp()
    {
        properties = new GameMapProperties();
        rebuild();
    }

    private void rebuild()
    {
        shire = new Shire(properties);
        bree = new Bree(properties);
        registry = new MapRegistry(List.of(shire, bree));
    }

    private Player joining(GameMap map)
    {
        Player player = new Player();
        player.setTag("Tester");
        player.getBeasts().add(new Beast("FIRE"));
        assertTrue(map.addPlayer(player), "player should find a free spawn tile");
        return player;
    }

    private Player arrivingInBree()
    {
        Player player = joining(shire);
        shire.movePlayer(player, new int[] {19, 11});
        assertEquals(1, player.getLocation(), "setup: player should have reached bree");
        return player;
    }

    @Test
    void mapsCarryTheirIdentityAndDimensions()
    {
        assertEquals(0, shire.getMapId());
        assertEquals("shire", shire.getName());
        assertEquals(1, bree.getMapId());
        assertEquals("bree", bree.getName());

        for (GameMap map : List.of(shire, bree))
        {
            assertEquals(20, map.getWidth());
            assertEquals(15, map.getHeight());
        }
    }

    @Test
    void gridsLoadedFromResourcesMatchTheOriginalTiles()
    {
        assertEquals(TileType.TREE, shire.tileAt(0, 0));
        assertEquals(TileType.GRASS, shire.tileAt(1, 1));
        assertEquals(TileType.WATER, shire.tileAt(17, 5));
        assertEquals(TileType.ARENA, shire.tileAt(11, 4));
        assertEquals(TileType.PORTAL_TO_BREE, shire.tileAt(19, 11));

        assertEquals(TileType.BREE_TREE, bree.tileAt(0, 0));
        assertEquals(TileType.BREE_GRASS, bree.tileAt(8, 5));
        assertEquals(TileType.PORTAL_TO_SHIRE, bree.tileAt(0, 11));
        assertEquals(TileType.PORTAL_SEALED, bree.tileAt(19, 4));
    }

    @Test
    void walkabilityIsPerMap()
    {
        assertTrue(shire.isWalkable(1, 1), "shire grass");
        assertFalse(shire.isWalkable(0, 0), "shire tree");
        assertFalse(shire.isWalkable(17, 5), "shire water");
        assertTrue(shire.isWalkable(11, 4), "arena tile is walkable");

        assertTrue(bree.isWalkable(8, 5), "bree grass");
        assertFalse(bree.isWalkable(0, 0), "bree tree");
        assertFalse(bree.isWalkable(19, 4), "portal tiles are never walkable");

        assertFalse(shire.isWalkable(-1, 0), "out of bounds");
        assertFalse(shire.isWalkable(0, 15), "out of bounds");
    }

    @Test
    void joiningPlacesPlayersOnConsecutiveSpawnTiles()
    {
        Player first = joining(shire);
        assertEquals(1, first.getX());
        assertEquals(1, first.getY());

        Player second = new Player();
        second.setTag("Second");
        second.getBeasts().add(new Beast("WATER"));
        assertTrue(shire.addPlayer(second));
        assertEquals(2, second.getX(), "the next joiner takes the next free tile");
        assertEquals(1, second.getY());
    }

    @Test
    void walkingOntoABlockedTileDoesNothing()
    {
        Player player = joining(shire);
        shire.movePlayer(player, new int[] {1, 0});

        assertEquals(1, player.getX());
        assertEquals(1, player.getY());
    }

    @Test
    void aValidMoveUpdatesPositionAndClearsActivity()
    {
        Player player = joining(shire);
        shire.movePlayer(player, new int[] {1, 2});

        assertEquals(1, player.getX());
        assertEquals(2, player.getY());
        assertEquals(0, player.getMode());
        assertEquals(0, player.getActivity());
        assertTrue(shire.isOccupied(1, 2));
        assertFalse(shire.isOccupied(1, 1), "the tile behind the player is freed");
    }

    @Test
    void occupiedTilesCannotBeWalkedOn()
    {
        Player first = joining(shire);
        Player second = new Player();
        second.setTag("Second");
        second.getBeasts().add(new Beast("WATER"));
        assertTrue(shire.addPlayer(second));

        shire.movePlayer(first, new int[] {2, 1});
        assertEquals(1, first.getX(), "cannot stand on the tile another player occupies");
        assertEquals(1, first.getY());
    }

    @Test
    void steppingOnTheArenaTileMakesItInteractable()
    {
        Player player = joining(shire);
        shire.movePlayer(player, new int[] {11, 4});

        assertEquals(11, player.getX());
        assertEquals(4, player.getY());
        assertEquals(1, player.getMode());
        assertEquals(TileType.ARENA.code(), player.getActivity());
    }

    @Test
    void shirePortalTeleportsToTheNearEdgeOfBree()
    {
        Player player = joining(shire);
        shire.movePlayer(player, new int[] {19, 11});

        assertEquals(1, player.getLocation(), "player is now on bree");
        assertEquals(0, player.getX());
        assertEquals(11, player.getY());
        assertEquals(0, player.getMode());
        assertEquals(0, player.getActivity());
        assertTrue(bree.isOccupied(0, 11), "the arrival tile is taken on the destination map");
        assertFalse(shire.isOccupied(1, 1), "the tile the player left is freed");
    }

    @Test
    void breePortalTeleportsBackToTheFarEdgeOfShire()
    {
        Player player = arrivingInBree();
        bree.movePlayer(player, new int[] {0, 11});

        assertEquals(0, player.getLocation(), "player is back on shire");
        assertEquals(19, player.getX());
        assertEquals(11, player.getY());
        assertTrue(shire.isOccupied(19, 11));
        assertFalse(bree.isOccupied(0, 11));
    }

    @Test
    void aPortalCannotBeUsedWhileItsArrivalTileIsTaken()
    {
        arrivingInBree();

        Player latecomer = joining(shire);
        shire.movePlayer(latecomer, new int[] {19, 11});

        assertEquals(0, latecomer.getLocation(), "teleport refused while the arrival tile is occupied");
        assertEquals(1, latecomer.getX(), "latecomer stays on its spawn tile");
        assertEquals(1, latecomer.getY());
    }

    @Test
    void theSealedPortalBlocksMovementInsteadOfCrashing()
    {
        Player player = arrivingInBree();
        bree.movePlayer(player, new int[] {19, 4});

        assertEquals(0, player.getX(), "a portal with no destination is impassable");
        assertEquals(11, player.getY());
        assertEquals(1, player.getLocation());
    }

    @Test
    void removingAPlayerFreesTheirTile()
    {
        Player player = joining(shire);
        shire.removePlayer(player);
        assertFalse(shire.isOccupied(1, 1));
    }

    @Test
    void outOfBoundsTilesAreReportedAsOccupiedSoMovementStops()
    {
        assertTrue(shire.isOccupied(-1, 0));
        assertTrue(shire.isOccupied(20, 0));
        assertFalse(shire.isInBounds(-1, 0));
        assertFalse(shire.isInBounds(20, 0));
    }

    @Test
    void encounterRollFollowsTheConfiguredRate()
    {
        properties.getShire().setEncounterRate(0);
        rebuild();
        Player player = joining(shire);
        assertFalse(shire.rollEncounter(player));

        properties.getShire().setEncounterRate(100);
        rebuild();
        assertTrue(shire.rollEncounter(player));

        properties.getShire().setEncounterRate(20);
        rebuild();
        int rolls = 200;
        int hits = 0;
        for (int i = 0; i < rolls; i++) if (shire.rollEncounter(player)) hits++;
        assertTrue(hits > 0 && hits < rolls, "a 20% rate should neither always nor never fire, got " + hits);
    }

    @Test
    void aPlayerWithNoUsableBeastNeverRollsAnEncounter()
    {
        properties.getShire().setEncounterRate(100);
        rebuild();
        Player player = new Player();
        player.setTag("Fainted");
        player.getBeasts().add(new Beast("FIRE"));
        player.getBeasts().get(0).setHp(0);

        assertFalse(shire.rollEncounter(player));
        assertFalse(shire.rollEncounter(null));
    }

    @Test
    void spawnedBeastsUseTheConfiguredTableAndLevelRange()
    {
        assertEquals(Set.of("FIRE", "WATER", "EARTH"), shire.getBeastProbabilities().keySet());

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++)
        {
            Beast beast = shire.spawnBeast();
            seen.add(beast.getType());
            assertTrue(beast.getLevel() >= 1 && beast.getLevel() <= 3,
                    "level outside the configured range: " + beast.getLevel());
            assertEquals(100, beast.getHp());
        }
        assertEquals(Set.of("FIRE", "WATER", "EARTH"), seen, "every configured species should be reachable");
    }

    @Test
    void spawnProbabilitiesCanBeNarrowedFromConfiguration()
    {
        properties.getBree().getBeastProbabilities().clear();
        properties.getBree().getBeastProbabilities().put("WATER", 1.0);
        properties.getBree().getLevelRange().setMin(4);
        properties.getBree().getLevelRange().setMax(4);

        Bree tuned = new Bree(properties);
        for (int i = 0; i < 20; i++)
        {
            Beast beast = tuned.spawnBeast();
            assertEquals("WATER", beast.getType());
            assertEquals(4, beast.getLevel());
        }
    }
}