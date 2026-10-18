package dev.prakhm.OnlineGame.world;

import dev.prakhm.OnlineGame.beast.Beast;
import dev.prakhm.OnlineGame.config.GameMapProperties.MapSettings;
import dev.prakhm.OnlineGame.player.Player;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A single playable map. Subclasses supply their identity and their tile grid; everything that
 * depends on how the numbers were tuned (encounter rate, spawn table, level range) comes from
 * {@link MapSettings}.
 */
public abstract class GameMap
{
    public static final int SHIRE_ID = 0;
    public static final int BREE_ID = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GRID_RESOURCE = "/maps/%s.json";

    private final int mapId;
    private final String name;
    private final int width;
    private final int height;
    private final TileType[][] grid;
    private final boolean[][] occupancy;
    private final int encounterRate;
    private final Map<String, Double> beastProbabilities;
    private final int minLevel;
    private final int maxLevel;
    private final Random random = new Random();

    private MapRegistry registry;

    protected GameMap(int mapId, String name, MapSettings settings)
    {
        this.mapId = mapId;
        this.name = name;
        this.grid = loadGrid(String.format(GRID_RESOURCE, name));
        this.height = grid.length;
        this.width = grid[0].length;
        this.occupancy = new boolean[height][width];
        this.encounterRate = settings.getEncounterRate();
        this.beastProbabilities = Collections.unmodifiableMap(new LinkedHashMap<>(settings.getBeastProbabilities()));
        this.minLevel = settings.getLevelRange().getMin();
        this.maxLevel = settings.getLevelRange().getMax();
    }

    /**
     * Supplies the registry used to resolve portal destinations. {@link MapRegistry} calls this
     * while registering its maps, which keeps map construction free of any registry dependency.
     */
    void setMapRegistry(MapRegistry registry)
    {
        this.registry = registry;
    }

    public int getMapId()
    {
        return mapId;
    }

    public String getName()
    {
        return name;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public int getEncounterRate()
    {
        return encounterRate;
    }

    public Map<String, Double> getBeastProbabilities()
    {
        return beastProbabilities;
    }

    public TileType tileAt(int x, int y)
    {
        return grid[y][x];
    }

    public boolean isWalkable(int x, int y)
    {
        return isInBounds(x, y) && grid[y][x].walkable();
    }

    public boolean isInteractable(int x, int y)
    {
        return isInBounds(x, y) && grid[y][x].interactable();
    }

    public boolean isTeleport(int x, int y)
    {
        return isInBounds(x, y) && grid[y][x].portal();
    }

    public synchronized boolean isOccupied(int x, int y)
    {
        return !isInBounds(x, y) || occupancy[y][x];
    }

    public boolean isInBounds(int x, int y)
    {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Places a player on the first free walkable tile of the spawn row.
     *
     * @return whether a tile was found and the player was placed
     */
    public synchronized boolean addPlayer(Player player)
    {
        int x = 1;
        while (x < width - 1 && (isOccupied(x, 1) || !isWalkable(x, 1))) x++;
        if (isOccupied(x, 1) || !isWalkable(x, 1)) return false;

        occupancy[1][x] = true;
        player.setCoordinates(new int[] {x, 1});
        onPlayerEnter(player);
        return true;
    }

    public synchronized void removePlayer(Player player)
    {
        int[] coordinates = player.getCoordinate();
        if (isInBounds(coordinates[0], coordinates[1])) occupancy[coordinates[1]][coordinates[0]] = false;
        onPlayerLeave(player);
    }

    /**
     * Moves a player by intent: the coordinates are the tile being stepped onto, already resolved
     * from a direction by the caller. Illegal targets are silently ignored.
     */
    public synchronized void movePlayer(Player player, int[] coordinates)
    {
        int x = coordinates[0];
        int y = coordinates[1];
        if (!isInBounds(x, y)) return;

        if (isTeleport(x, y))
        {
            moveThroughPortal(player, x, y);
            return;
        }

        if (!isWalkable(x, y) || isOccupied(x, y)) return;

        int[] current = player.getCoordinate();
        occupancy[current[1]][current[0]] = false;
        player.setCoordinates(new int[] {x, y});
        occupancy[y][x] = true;

        if (isInteractable(x, y))
        {
            player.setMode(1);
            player.setActivity(tileAt(x, y).code());
        }
        else
        {
            player.setMode(0);
            player.setActivity(0);
        }
    }

    /**
     * Every valid move is one encounter roll against this map's rate. A player with no usable beast
     * never draws one.
     */
    public boolean rollEncounter(Player player)
    {
        if (player == null || !player.hasUsableBeast()) return false;
        return random.nextInt(100) < encounterRate;
    }

    /** Builds a wild beast from this map's spawn table and level range. */
    public Beast spawnBeast()
    {
        return new Beast(rollBeastType(), rollLevel());
    }

    /** Hook run after a player is placed on, or teleports onto, this map. */
    protected void onPlayerEnter(Player player)
    {
    }

    /** Hook run after a player is removed from, or teleports off, this map. */
    protected void onPlayerLeave(Player player)
    {
    }

    private void moveThroughPortal(Player player, int x, int y)
    {
        Integer targetId = tileAt(x, y).portalTargetMapId();
        if (targetId == null || registry == null) return;

        GameMap destination = registry.byId(targetId);
        if (destination == null) return;

        int[] arrival = destination.arrivalPointFrom(mapId);
        if (arrival == null || destination.isOccupied(arrival[0], arrival[1])) return;

        int[] current = player.getCoordinate();
        occupancy[current[1]][current[0]] = false;
        player.setCoordinates(arrival);
        player.setLocation(destination.getMapId());
        destination.occupancy[arrival[1]][arrival[0]] = true;
        player.setMode(0);
        player.setActivity(0);

        onPlayerLeave(player);
        destination.onPlayerEnter(player);
    }

    /**
     * Where a player arriving from {@code fromMapId} lands: on the near edge if they came from a
     * lower map id, otherwise on the far edge.
     */
    private int[] arrivalPointFrom(int fromMapId)
    {
        boolean fromLowerMap = fromMapId < mapId;
        for (int row = 0; row < height; row++)
        {
            int column = fromLowerMap ? 0 : width - 1;
            if (grid[row][column].arrivalPoint()) return new int[] {column, row};
        }
        return new int[] {10, 10};
    }

    private String rollBeastType()
    {
        double total = 0;
        for (Double weight : beastProbabilities.values()) total += weight;
        if (total <= 0) return null;

        double target = random.nextDouble() * total;
        double running = 0;
        for (Map.Entry<String, Double> entry : beastProbabilities.entrySet())
        {
            running += entry.getValue();
            if (target < running) return entry.getKey();
        }
        List<String> types = new ArrayList<>(beastProbabilities.keySet());
        return types.get(types.size() - 1);
    }

    private int rollLevel()
    {
        if (maxLevel <= minLevel) return minLevel;
        return minLevel + random.nextInt(maxLevel - minLevel + 1);
    }

    private static TileType[][] loadGrid(String resource)
    {
        try (InputStream stream = GameMap.class.getResourceAsStream(resource))
        {
            if (stream == null) throw new IllegalStateException("Missing map resource: " + resource);

            JsonNode rows = MAPPER.readTree(stream).path("grid");
            if (!rows.isArray() || rows.isEmpty()) throw new IllegalStateException("No grid in " + resource);

            TileType[][] grid = new TileType[rows.size()][];
            for (int y = 0; y < rows.size(); y++)
            {
                JsonNode row = rows.get(y);
                if (row.size() != rows.get(0).size()) throw new IllegalStateException("Ragged grid in " + resource);
                grid[y] = new TileType[row.size()];
                for (int x = 0; x < row.size(); x++) grid[y][x] = TileType.fromName(row.get(x).asString());
            }
            return grid;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot read map resource: " + resource, e);
        }
    }
}