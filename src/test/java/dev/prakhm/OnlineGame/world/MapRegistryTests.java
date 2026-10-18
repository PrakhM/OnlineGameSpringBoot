package dev.prakhm.OnlineGame.world;

import dev.prakhm.OnlineGame.config.GameMapProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiring level checks: the registry is a real bean, the maps get it injected even though they are
 * built before it exists, and the numbers come from application.yml.
 */
@SpringBootTest
class MapRegistryTests
{
    @Autowired
    private MapRegistry registry;

    @Autowired
    private GameMapProperties properties;

    @Test
    void mapsAreFoundById()
    {
        assertEquals(GameMap.SHIRE_ID, registry.byId(0).getMapId());
        assertEquals(GameMap.BREE_ID, registry.byId(1).getMapId());
        assertNull(registry.byId(99), "unknown ids resolve to nothing");
        assertTrue(registry.byId(0) instanceof Shire);
        assertTrue(registry.byId(1) instanceof Bree);
    }

    @Test
    void mapsAreFoundByNameCaseInsensitively()
    {
        assertSame(registry.byId(0), registry.byName("shire"));
        assertSame(registry.byId(1), registry.byName("bree"));
        assertSame(registry.byId(0), registry.byName("Shire"));
        assertSame(registry.byId(1), registry.byName("BREE"));
        assertNull(registry.byName("mordor"));
        assertNull(registry.byName(null));
    }

    @Test
    void theDefaultMapIsShireAndBothMapsAreRegistered()
    {
        assertSame(registry.byId(0), registry.defaultMap());
        assertEquals(2, registry.all().size());
    }

    @Test
    void portalTilesCanResolveAnotherMapThroughTheInjectedRegistry()
    {
        // proves the late bound registry reached the map beans, which is what teleports depend on
        assertEquals(GameMap.BREE_ID, registry.byId(0).tileAt(19, 11).portalTargetMapId());
        assertEquals(GameMap.SHIRE_ID, registry.byId(1).tileAt(0, 11).portalTargetMapId());
        assertNull(registry.byId(1).tileAt(19, 4).portalTargetMapId());
    }

    @Test
    void numericSettingsAreBoundFromApplicationYml()
    {
        for (GameMapProperties.MapSettings settings : Set.of(properties.getShire(), properties.getBree()))
        {
            assertEquals(20, settings.getEncounterRate(), "shipped encounter rate was 20%");
            assertEquals(1, settings.getLevelRange().getMin());
            assertEquals(3, settings.getLevelRange().getMax());
            assertEquals(Set.of("FIRE", "WATER", "EARTH"), settings.getBeastProbabilities().keySet());
        }

        assertEquals(20, registry.byId(0).getEncounterRate());
        assertEquals(20, registry.byId(1).getEncounterRate());
    }

    @Test
    void everyMapLoadedItsGridFromResources()
    {
        for (GameMap map : registry.all())
        {
            assertNotNull(map.tileAt(0, 0), map.getName() + " should have a loaded grid");
            assertEquals(20, map.getWidth());
            assertEquals(15, map.getHeight());
        }
    }
}