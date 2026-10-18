package dev.prakhm.OnlineGame.player;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;

@Component
public class PlayerRegistry
{
    private final HashMap<String, Player> players = new HashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public PlayerRegistry(SimpMessagingTemplate messagingTemplate)
    {
        this.messagingTemplate = messagingTemplate;
    }

    public synchronized Player getPlayer(SimpMessageHeaderAccessor headerAccessor)
    {
        if (headerAccessor == null || headerAccessor.getSessionAttributes() == null) return null;
        return find((String) headerAccessor.getSessionAttributes().get("Tag"));
    }

    public synchronized Player find(String tag)
    {
        return tag == null ? null : players.get(tag);
    }

    public synchronized boolean contains(String tag)
    {
        return tag != null && players.containsKey(tag);
    }

    public synchronized void add(Player player)
    {
        players.put(player.getTag(), player);
    }

    public synchronized Player remove(String tag)
    {
        return tag == null ? null : players.remove(tag);
    }

    public synchronized Collection<Player> getPlayers()
    {
        return players.values();
    }

    public synchronized void broadcastPlayers()
    {
        messagingTemplate.convertAndSend("/topic/players", players.values());
    }
}