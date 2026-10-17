package dev.prakhm.OnlineGame.world;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lookup for every map in the world. Maps are keyed by their stable numeric id, which is what
 * {@code Player.location} stores, and by their lowercase name.
 */
@Component
public class MapRegistry
{
    private final Map<Integer, GameMap> byId = new LinkedHashMap<>();
    private final Map<String, GameMap> byName = new LinkedHashMap<>();

    public MapRegistry(List<GameMap> maps)
    {
        // Spring does not guarantee the order of injected GameMap beans, so order by id to keep
        // defaultMap() and all() deterministic.
        maps.stream()
                .sorted(Comparator.comparingInt(GameMap::getMapId))
                .forEach(map ->
                {
                    byId.put(map.getMapId(), map);
                    byName.put(map.getName().toLowerCase(Locale.ROOT), map);
                    map.setMapRegistry(this);
                });
    }

    /** @return the map with that id, or {@code null} when no such map exists */
    public GameMap byId(int mapId)
    {
        return byId.get(mapId);
    }

    /** @return the map with that name, case insensitive, or {@code null} when no such map exists */
    public GameMap byName(String name)
    {
        return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
    }

    public GameMap defaultMap()
    {
        return byId.get(GameMap.SHIRE_ID) != null ? byId.get(GameMap.SHIRE_ID) : byId.values().iterator().next();
    }

    public List<GameMap> all()
    {
        return List.copyOf(byId.values());
    }
}