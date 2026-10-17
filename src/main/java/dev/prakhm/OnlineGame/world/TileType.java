package dev.prakhm.OnlineGame.world;

/**
 * Every tile kind that can appear on a map grid.
 *
 * <p>The {@link #code()} is the value sent to clients and is what the browser tile renderer keys
 * off, so codes must stay stable. Map grids on disk store the enum {@link #name()} instead, which
 * keeps the JSON readable.
 */
public enum TileType
{
    GRASS(0, true, false),
    TREE(1, false, false),
    WATER(2, false, false),
    BREE_GRASS(3, true, false),
    BREE_TREE(4, false, false),
    PORTAL_TO_SHIRE(10, false, true),
    PORTAL_TO_BREE(20, false, true),
    /** Portal tile with no destination map; blocks movement instead of teleporting. */
    PORTAL_SEALED(30, false, true),
    ARENA(120, true, false);

    private final int code;
    private final boolean walkable;
    private final boolean portal;

    TileType(int code, boolean walkable, boolean portal)
    {
        this.code = code;
        this.walkable = walkable;
        this.portal = portal;
    }

    public int code()
    {
        return code;
    }

    /** Whether a player may stand on this tile. Portals are never walkable. */
    public boolean walkable()
    {
        return walkable;
    }

    /** Whether stepping on this tile is a portal rather than plain movement. */
    public boolean portal()
    {
        return portal;
    }

    /** Tiles a player can be dropped onto when arriving from another map, i.e. code >= 10. */
    public boolean arrivalPoint()
    {
        return code >= 10;
    }

    /** Tiles the player can interact with, i.e. code >= 100. */
    public boolean interactable()
    {
        return code >= 100;
    }

    /** Destination map id, or {@code null} when this portal leads nowhere. */
    public Integer portalTargetMapId()
    {
        if (!portal) return null;
        if (this == PORTAL_TO_SHIRE) return GameMap.SHIRE_ID;
        if (this == PORTAL_TO_BREE) return GameMap.BREE_ID;
        return null;
    }

    public static TileType fromName(String name)
    {
        return TileType.valueOf(name.trim().toUpperCase());
    }

    public static TileType fromCode(int code)
    {
        for (TileType tile : values())
        {
            if (tile.code == code) return tile;
        }
        throw new IllegalArgumentException("Unknown tile code: " + code);
    }
}