package dev.prakhm.OnlineGame.player;

import dev.prakhm.OnlineGame.world.TileType;

import java.time.Instant;

/**
 * One live connection by one player.
 *
 * <p>Created when a websocket connects and discarded when it drops. The profile itself is
 * {@link Player} and is the authority for current map and position, so this class reads those
 * through rather than keeping a second copy that could drift. What it owns is the connection:
 * which websocket it is attached to, when the player last moved, and the derived
 * {@link PlayerState}.
 *
 * <p>Instances are confined to a single websocket thread at a time by the handlers that mutate
 * them; arena handlers additionally lock both participants in a fixed order.
 */
public class PlayerSession
{
    private final long playerId;
    private final String websocketSessionId;
    private final Player player;
    private final Instant connectedAt;
    private volatile Instant lastMove;

    public PlayerSession(Player player, String websocketSessionId)
    {
        if (player == null) throw new IllegalArgumentException("player is required");
        if (player.getId() <= 0) throw new IllegalArgumentException("player must have an assigned id");
        this.player = player;
        this.playerId = player.getId();
        this.websocketSessionId = websocketSessionId;
        this.connectedAt = Instant.now();
        this.lastMove = this.connectedAt;
    }

    public long getPlayerId()
    {
        return playerId;
    }

    /**
     * The id of the underlying websocket session, or {@code null} when the transport did not
     * supply one. Never used as a lookup key when {@code null}.
     */
    public String getWebsocketSessionId()
    {
        return websocketSessionId;
    }

    public String getUsername()
    {
        return player.getTag();
    }

    /** The profile this session is driving. */
    public Player getPlayer()
    {
        return player;
    }

    /** The map the player is currently on. */
    public int getCurrentMap()
    {
        return player.getLocation();
    }

    public int getX()
    {
        return player.getX();
    }

    public int getY()
    {
        return player.getY();
    }

    /** A fresh copy of the player's tile coordinates. */
    public int[] getPosition()
    {
        return player.getCoordinate();
    }

    public Instant getConnectedAt()
    {
        return connectedAt;
    }

    public Instant getLastMove()
    {
        return lastMove;
    }

    /** Stamps the session as having just moved. Called after a move that changed tile. */
    public void markMoved()
    {
        this.lastMove = Instant.now();
    }

    /**
     * What the player is doing right now, derived from the profile.
     *
     * <p>Derived rather than stored so it cannot fall out of step with the fields it summarises: an
     * arena pairing wins over a wild encounter, which wins over merely standing on an arena tile.
     */
    public PlayerState getState()
    {
        if (player.getOpponent() != null) return PlayerState.IN_BATTLE;
        if (player.getEncounterBeast() != null) return PlayerState.IN_WILD_ENCOUNTER;
        if (player.getActivity() == TileType.ARENA.code()) return PlayerState.IN_ARENA_QUEUE;
        return PlayerState.FREE;
    }

    public boolean isInBattle()
    {
        return getState() == PlayerState.IN_BATTLE;
    }

    public boolean isInWildEncounter()
    {
        return getState() == PlayerState.IN_WILD_ENCOUNTER;
    }

    /**
     * Takes the lock for two sessions in a stable order, so that mutually attacking players cannot
     * deadlock by grabbing each other in opposite orders.
     */
    static void lockInOrder(PlayerSession first, PlayerSession second, Runnable body)
    {
        if (first == second)
        {
            synchronized (first)
            {
                body.run();
            }
            return;
        }
        if (first.getPlayerId() < second.getPlayerId())
        {
            synchronized (first)
            {
                synchronized (second)
                {
                    body.run();
                }
            }
            return;
        }
        synchronized (second)
        {
            synchronized (first)
            {
                body.run();
            }
        }
    }

    @Override
    public String toString()
    {
        return "PlayerSession[playerId=" + playerId + ", username=" + getUsername() +
                ", map=" + getCurrentMap() + ", state=" + getState() + "]";
    }
}
