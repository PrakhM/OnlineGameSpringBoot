package dev.prakhm.OnlineGame.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileTypeTests
{
    @Test
    void codesMatchTheValuesTheClientRenders()
    {
        assertEquals(0, TileType.GRASS.code());
        assertEquals(1, TileType.TREE.code());
        assertEquals(2, TileType.WATER.code());
        assertEquals(3, TileType.BREE_GRASS.code());
        assertEquals(4, TileType.BREE_TREE.code());
        assertEquals(10, TileType.PORTAL_TO_SHIRE.code());
        assertEquals(20, TileType.PORTAL_TO_BREE.code());
        assertEquals(30, TileType.PORTAL_SEALED.code());
        assertEquals(120, TileType.ARENA.code());
    }

    @Test
    void codesAndNamesRoundTrip()
    {
        for (TileType tile : TileType.values())
        {
            assertEquals(tile, TileType.fromCode(tile.code()));
            assertEquals(tile, TileType.fromName(tile.name()));
        }
    }

    @Test
    void unknownCodesAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> TileType.fromCode(77));
        assertThrows(IllegalArgumentException.class, () -> TileType.fromName("LAVA"));
    }

    @Test
    void walkabilityMatchesTheOriginalTileArithmetic()
    {
        // the old code allowed tiles where code % 3 == 0, or code >= 100
        assertTrue(TileType.GRASS.walkable());
        assertTrue(TileType.BREE_GRASS.walkable());
        assertTrue(TileType.ARENA.walkable());

        assertFalse(TileType.TREE.walkable());
        assertFalse(TileType.WATER.walkable());
        assertFalse(TileType.BREE_TREE.walkable());
        assertFalse(TileType.PORTAL_TO_SHIRE.walkable());
        assertFalse(TileType.PORTAL_TO_BREE.walkable());
        assertFalse(TileType.PORTAL_SEALED.walkable());
    }

    @Test
    void onlyTheArenaTileIsInteractable()
    {
        assertTrue(TileType.ARENA.interactable());
        for (TileType tile : TileType.values())
        {
            if (tile != TileType.ARENA) assertFalse(tile.interactable(), tile + " must not be interactable");
        }
    }

    @Test
    void portalsPointAtKnownMapsAndTheSealedOnePointsNowhere()
    {
        assertEquals(GameMap.SHIRE_ID, TileType.PORTAL_TO_SHIRE.portalTargetMapId());
        assertEquals(GameMap.BREE_ID, TileType.PORTAL_TO_BREE.portalTargetMapId());
        assertNull(TileType.PORTAL_SEALED.portalTargetMapId());

        for (TileType tile : TileType.values())
        {
            boolean isPortal = tile.portal();
            assertEquals(isPortal, tile.portalTargetMapId() != null || tile == TileType.PORTAL_SEALED,
                    tile + " portal flag disagrees with its target");
        }
    }
}