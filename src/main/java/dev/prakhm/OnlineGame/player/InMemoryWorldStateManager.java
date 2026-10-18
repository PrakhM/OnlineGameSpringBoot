package dev.prakhm.OnlineGame.player;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This loses everything on restart and cannot span more than one node. That is deliberate for
 * now — the interface is the seam a Redis-backed implementation would slot into.
 */
@Component
public class InMemoryWorldStateManager implements WorldStateManager
{
    private final ConcurrentHashMap<Long, PlayerSession> sessionsByPlayerId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> playerIdsByUsername = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> playerIdsByWebsocketSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Set<Long>> playerIdsByMap = new ConcurrentHashMap<>();
    /** Which map bucket each session is filed under, so a portal move can evict the old one. Guarded by {@link #indexLock}. */
    private final Map<Long, Integer> indexedMaps = new HashMap<>();

    /** Guards index bookkeeping only. Never held while gameplay code runs. */
    private final Object indexLock = new Object();

    @Override
    public Optional<PlayerSession> add(PlayerSession session)
    {
        if (session == null) return Optional.empty();
        synchronized (indexLock)
        {
            if (playerIdsByUsername.containsKey(session.getUsername())) return Optional.empty();
            if (sessionsByPlayerId.containsKey(session.getPlayerId())) return Optional.empty();

            sessionsByPlayerId.put(session.getPlayerId(), session);
            playerIdsByUsername.put(session.getUsername(), session.getPlayerId());
            if (session.getWebsocketSessionId() != null)
                playerIdsByWebsocketSessionId.put(session.getWebsocketSessionId(), session.getPlayerId());
            indexUnder(session.getPlayerId(), session.getCurrentMap());
            indexedMaps.put(session.getPlayerId(), session.getCurrentMap());
            return Optional.of(session);
        }
    }

    @Override
    public Optional<PlayerSession> remove(long playerId)
    {
        synchronized (indexLock)
        {
            PlayerSession removed = sessionsByPlayerId.remove(playerId);
            if (removed == null) return Optional.empty();
            unindex(removed);
            return Optional.of(removed);
        }
    }

    @Override
    public Optional<PlayerSession> removeByWebsocketSessionId(String websocketSessionId)
    {
        if (websocketSessionId == null) return Optional.empty();
        synchronized (indexLock)
        {
            Long playerId = playerIdsByWebsocketSessionId.get(websocketSessionId);
            if (playerId == null) return Optional.empty();
            PlayerSession removed = sessionsByPlayerId.remove(playerId);
            if (removed == null) return Optional.empty();
            unindex(removed);
            return Optional.of(removed);
        }
    }

    @Override
    public Optional<PlayerSession> findByPlayerId(long playerId)
    {
        return Optional.ofNullable(sessionsByPlayerId.get(playerId));
    }

    @Override
    public Optional<PlayerSession> findByWebsocketSessionId(String websocketSessionId)
    {
        if (websocketSessionId == null) return Optional.empty();
        Long playerId = playerIdsByWebsocketSessionId.get(websocketSessionId);
        return playerId == null ? Optional.empty() : findByPlayerId(playerId);
    }

    @Override
    public Optional<PlayerSession> findByUsername(String username)
    {
        if (username == null) return Optional.empty();
        Long playerId = playerIdsByUsername.get(username);
        return playerId == null ? Optional.empty() : findByPlayerId(playerId);
    }

    @Override
    public boolean reindex(PlayerSession session)
    {
        if (session == null) return false;
        synchronized (indexLock)
        {
            if (!sessionsByPlayerId.containsKey(session.getPlayerId())) return false;
            long playerId = session.getPlayerId();
            int currentMap = session.getCurrentMap();
            Integer previous = indexedMaps.get(playerId);
            if (previous != null && previous.intValue() == currentMap) return true;
            if (previous != null) dropFromMap(previous, playerId);
            indexUnder(playerId, currentMap);
            indexedMaps.put(playerId, currentMap);
            return true;
        }
    }

    @Override
    public Collection<PlayerSession> findByMap(int mapId)
    {
        Set<Long> playerIds = playerIdsByMap.get(mapId);
        if (playerIds == null || playerIds.isEmpty()) return List.of();
        return playerIds.stream()
                .map(sessionsByPlayerId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Collection<PlayerSession> findAll()
    {
        return Collections.unmodifiableCollection(sessionsByPlayerId.values());
    }

    @Override
    public int size()
    {
        return sessionsByPlayerId.size();
    }

    @Override
    public void clear()
    {
        synchronized (indexLock)
        {
            sessionsByPlayerId.clear();
            playerIdsByUsername.clear();
            playerIdsByWebsocketSessionId.clear();
            playerIdsByMap.clear();
            indexedMaps.clear();
        }
    }

    /** Drops every index entry for a session that has just been taken out of the main map. */
    private void unindex(PlayerSession removed)
    {
        playerIdsByUsername.remove(removed.getUsername(), removed.getPlayerId());
        if (removed.getWebsocketSessionId() != null)
            playerIdsByWebsocketSessionId.remove(removed.getWebsocketSessionId(), removed.getPlayerId());
        Integer indexed = indexedMaps.remove(removed.getPlayerId());
        dropFromMap(indexed == null ? removed.getCurrentMap() : indexed, removed.getPlayerId());
    }

    private void indexUnder(long playerId, int mapId)
    {
        playerIdsByMap.computeIfAbsent(mapId, key -> ConcurrentHashMap.newKeySet()).add(playerId);
    }

    private void dropFromMap(int mapId, long playerId)
    {
        Set<Long> playerIds = playerIdsByMap.get(mapId);
        if (playerIds == null) return;
        playerIds.remove(playerId);
        if (playerIds.isEmpty()) playerIdsByMap.remove(mapId, playerIds);
    }
}
