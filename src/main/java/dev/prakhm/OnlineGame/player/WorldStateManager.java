package dev.prakhm.OnlineGame.player;

import java.util.Collection;
import java.util.Optional;

/**
 * The registry of players currently connected to the world.
 *
 * <p>Exists as an interface so the in-memory implementation can be swapped for a shared store
 * (Redis, say) without touching the controllers. Implementations must be safe for concurrent use:
 * websocket handlers run on many threads at once and registration happens as often as joins and
 * drops.
 *
 * <p>Usernames are unique among connected players. That is the one invariant registration has to
 * enforce atomically, because chat, opponent lookup and the client-side roster are all keyed by
 * username.
 */
public interface WorldStateManager
{
    /**
     * Registers a session.
     *
     * @return the registered session, or empty if a player with the same username is already
     *         connected, in which case nothing is stored
     */
    Optional<PlayerSession> add(PlayerSession session);

    /**
     * Removes a session by player id.
     *
     * @return the removed session, or empty if it was not registered
     */
    Optional<PlayerSession> remove(long playerId);

    /**
     * Removes a session by the websocket session it is attached to.
     *
     * @return the removed session, or empty if no session is attached to that websocket, or the id
     *         was {@code null}
     */
    Optional<PlayerSession> removeByWebsocketSessionId(String websocketSessionId);

    Optional<PlayerSession> findByPlayerId(long playerId);

    Optional<PlayerSession> findByWebsocketSessionId(String websocketSessionId);

    Optional<PlayerSession> findByUsername(String username);

    /**
     * Moves a session's per-map index entry to the map it is actually on.
     *
     * <p>A session's map lives on its profile and changes when it steps through a portal, which the
     * registry cannot observe on its own. Call this after anything that may have moved the player;
     * calling it when nothing moved is harmless.
     *
     * @return whether the session was registered and therefore indexed
     */
    boolean reindex(PlayerSession session);

    /** The connected players on one map. */
    Collection<PlayerSession> findByMap(int mapId);

    /** Every connected player. */
    Collection<PlayerSession> findAll();

    int size();

    void clear();
}
