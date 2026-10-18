package dev.prakhm.OnlineGame.player;

import dev.prakhm.OnlineGame.Battle.BattleHandler;
import dev.prakhm.OnlineGame.Items.BattleItem;
import dev.prakhm.OnlineGame.Items.Beast;
import dev.prakhm.OnlineGame.Map.GameMap;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class PlayerMovementController
{
    private static final int MAX_TAG_LENGTH = 12;

    private final String[] colours = {
            "#e74c3c","#3498db","#2ecc71","#f39c12",
            "#9b59b6","#1abc9c","#e67e22","#e91e63"
    };
    private int colourIndex = 0;
    private final PlayerRegistry registry;
    private final BattleHandler battleHandler;
    private final GameMap gameMap;

    public PlayerMovementController(PlayerRegistry registry, BattleHandler battleHandler, GameMap gameMap)
    {
        this.registry = registry;
        this.battleHandler = battleHandler;
        this.gameMap = gameMap;
    }

    @MessageMapping("/player.addPlayer")
    public synchronized void addPlayer(@Payload Player player, SimpMessageHeaderAccessor headerAccessor)
    {
        if (player == null || player.getTag() == null || player.getTag().isBlank() || player.getTag().length() > MAX_TAG_LENGTH ||
                headerAccessor.getSessionAttributes() == null || headerAccessor.getSessionAttributes().containsKey("Tag") ||
                registry.contains(player.getTag()) || player.getBattleInventory().size() != 1 ||
                !hasValidStarterBeast(player)) return;
        player.setLocation(0);
        player.getBattle().setActiveBeastIndex(0);
        player.getBattle().setBattleStatus("Exploring");
        player.setColour(colours[colourIndex % colours.length]);
        player.setBattleInventory(new ArrayList<>(List.of(Beast.of(player.getBattleInventory().get(0).getType()))));
        player.setNonBattleInventory(new ArrayList<>());
        if (!gameMap.addPlayer(player)) return;

        colourIndex++;
        headerAccessor.getSessionAttributes().put("Tag", player.getTag());
        registry.add(player);
        registry.broadcastPlayers();
    }

    @MessageMapping("/player.movePlayer")
    public synchronized void movePlayer(@Payload int dir, SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = registry.getPlayer(headerAccessor);
        if (player == null || dir < 0 || dir > 3 || player.getBattle().isInBattle()) return;
        int[] coordinate = player.getCoordinate();
        if (dir == 0) coordinate[1]--;
        else if (dir == 1) coordinate[0]--;
        else if (dir == 2) coordinate[1]++;
        else coordinate[0]++;

        int[] before = player.getCoordinate();
        gameMap.movePlayer(player, coordinate);
        if (before[0] != player.getX() || before[1] != player.getY()) battleHandler.onPlayerMoved(player);
        if (player.getActivity() == BattleHandler.ARENA_ACTIVITY) battleHandler.tryStartArenaBattle(player);
        registry.broadcastPlayers();
    }

    @EventListener
    public synchronized void handleDisconnect(SessionDisconnectEvent event)
    {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return;
        String tag = (String) attrs.get("Tag");
        Player player = registry.remove(tag);
        if (player == null) return;
        gameMap.removePlayer(player);
        battleHandler.onPlayerLeft(player);
        registry.broadcastPlayers();
    }

    private boolean hasValidStarterBeast(Player player)
    {
        BattleItem starter = player.getBattleInventory().get(0);
        return starter != null && Beast.isKnownElement(starter.getType());
    }
}