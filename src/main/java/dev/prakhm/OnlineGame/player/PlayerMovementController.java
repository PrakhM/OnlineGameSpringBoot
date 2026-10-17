package dev.prakhm.OnlineGame.player;

import dev.prakhm.OnlineGame.beast.Beast;
import dev.prakhm.OnlineGame.world.GameMap;
import dev.prakhm.OnlineGame.world.MapRegistry;
import dev.prakhm.OnlineGame.world.TileType;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Controller
public class PlayerMovementController
{
    HashMap<String, Player> players = new HashMap<>();
    private String[] colours = {
            "#e74c3c","#3498db","#2ecc71","#f39c12",
            "#9b59b6","#1abc9c","#e67e22","#e91e63"
    };
    private int colourIndex = 0;
    private final Random random = new Random();
    private SimpMessagingTemplate messagingTemplate;
    private MapRegistry mapRegistry;

    public PlayerMovementController(SimpMessagingTemplate messagingTemplate, MapRegistry mapRegistry)
    {
        this.messagingTemplate = messagingTemplate;
        this.mapRegistry = mapRegistry;
    }

    @MessageMapping("/player.addPlayer")
    public synchronized void addPlayer(@Payload Player player, SimpMessageHeaderAccessor headerAccessor)
    {
        if (player == null || player.getTag() == null || player.getTag().isBlank() || player.getTag().length() > 12 ||
                headerAccessor.getSessionAttributes() == null || headerAccessor.getSessionAttributes().containsKey("Tag") ||
                players.containsKey(player.getTag()) || player.getBeasts() == null || player.getBeasts().size() != 1 ||
                player.getBeasts().get(0) == null || !isBeastType(player.getBeasts().get(0).getType())) return;
        GameMap spawnMap = mapRegistry.defaultMap();
        player.setLocation(spawnMap.getMapId());
        player.setActiveBeastIndex(0);
        player.setColour(colours[colourIndex % colours.length]);
        player.setBattleStatus("Exploring");
        if (!spawnMap.addPlayer(player)) return;

        colourIndex++;
        headerAccessor.getSessionAttributes().put("Tag", player.getTag());
        players.put(player.getTag(), player);
        broadcastPlayers();
    }

    @MessageMapping("/player.movePlayer")
    public synchronized void movePlayer(@Payload int dir, SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = getPlayer(headerAccessor);
        if (player == null || dir < 0 || dir > 3 || player.getEncounterBeast() != null || player.getOpponent() != null) return;
        int[] coordinate = player.getCoordinate();
        if (dir == 0) coordinate[1]--;
        else if (dir == 1) coordinate[0]--;
        else if (dir == 2) coordinate[1]++;
        else coordinate[0]++;

        int[] before = player.getCoordinate();
        GameMap map = mapRegistry.byId(player.getLocation());
        if (map == null) return;
        map.movePlayer(player, coordinate);
        boolean moved = before[0] != player.getX() || before[1] != player.getY();
        if (moved) restoreBeasts(player);
        if (moved && player.getActivity() != TileType.ARENA.code() && map.rollEncounter(player))
        {
            player.setEncounterBeast(map.spawnBeast());
            player.setBattleStatus("A wild beast appeared");
        }
        if (player.getActivity() == TileType.ARENA.code() && player.hasUsableBeast()) startArenaBattle(player);
        broadcastPlayers();
    }

    @MessageMapping("/player.wildAttack")
    public synchronized void wildAttack(@Payload int attack, SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = getPlayer(headerAccessor);
        Beast activeBeast = player == null ? null : player.getActiveBeast();
        Beast encounterBeast = player == null ? null : player.getEncounterBeast();
        if (activeBeast == null || encounterBeast == null || player.isChoosingBeast() || attack < 0 || attack > 2) return;

        encounterBeast.setHp(Math.max(0, encounterBeast.getHp() - getDamage(activeBeast, encounterBeast, attack)));
        if (encounterBeast.getHp() == 0)
        {
            encounterBeast.setHp(100);
            player.getBeasts().add(encounterBeast);
            player.setEncounterBeast(null);
            player.setBattleStatus("Wild beast captured");
        }
        else
        {
            activeBeast.setHp(Math.max(0, activeBeast.getHp() - getDamage(encounterBeast, activeBeast, random.nextInt(3))));
            if (activeBeast.getHp() == 0) prepareNextBeast(player, "Choose your next beast");
        }
        broadcastPlayers();
    }

    @MessageMapping("/player.skipEncounter")
    public synchronized void skipEncounter(SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = getPlayer(headerAccessor);
        if (player == null || player.getEncounterBeast() == null) return;
        player.setEncounterBeast(null);
        player.setChoosingBeast(false);
        player.setBattleStatus("Wild encounter skipped");
        broadcastPlayers();
    }

    @MessageMapping("/player.arenaAttack")
    public synchronized void arenaAttack(@Payload int attack, SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = getPlayer(headerAccessor);
        if (player == null || player.getOpponent() == null || !player.isTurn() || player.isChoosingBeast() || attack < 0 || attack > 2) return;
        Player opponent = players.get(player.getOpponent());
        Beast activeBeast = player.getActiveBeast();
        Beast opponentBeast = opponent == null ? null : opponent.getActiveBeast();
        if (activeBeast == null || opponentBeast == null) return;

        opponentBeast.setHp(Math.max(0, opponentBeast.getHp() - getDamage(activeBeast, opponentBeast, attack)));
        if (opponentBeast.getHp() == 0)
        {
            if (opponent.hasUsableBeast())
            {
                player.setTurn(false);
                opponent.setTurn(false);
                prepareNextBeast(opponent, "Choose your next beast");
                player.setBattleStatus(opponent.getTag() + " is choosing a beast");
            }
            else defeatArenaPlayer(player, opponent);
        }
        else
        {
            player.setTurn(false);
            opponent.setTurn(true);
            player.setBattleStatus(opponent.getTag() + " is choosing an attack");
            opponent.setBattleStatus("Your turn");
        }
        broadcastPlayers();
    }

    @MessageMapping("/player.chooseBeast")
    public synchronized void chooseBeast(@Payload int index, SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = getPlayer(headerAccessor);
        if (player == null || !player.isChoosingBeast() || index < 0 || index >= player.getBeasts().size() ||
                player.getBeasts().get(index).getHp() <= 0) return;
        player.setActiveBeastIndex(index);
        player.setChoosingBeast(false);
        if (player.getOpponent() != null)
        {
            Player opponent = players.get(player.getOpponent());
            if (opponent != null)
            {
                player.setTurn(true);
                opponent.setTurn(false);
                player.setBattleStatus("Your turn");
                opponent.setBattleStatus(player.getTag() + " is choosing an attack");
            }
        }
        else player.setBattleStatus("Wild battle continues");
        broadcastPlayers();
    }

    @EventListener
    public synchronized void handleDisconnect(SessionDisconnectEvent event)
    {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return;
        String tag = (String) attrs.get("Tag");
        Player player = tag == null ? null : players.remove(tag);
        if (player == null) return;
        GameMap map = mapRegistry.byId(player.getLocation());
        if (map != null) map.removePlayer(player);
        if (player.getOpponent() != null)
        {
            Player opponent = players.get(player.getOpponent());
            if (opponent != null) endArenaBattle(opponent, player, "Arena opponent disconnected");
        }
        broadcastPlayers();
    }

    private Player getPlayer(SimpMessageHeaderAccessor headerAccessor)
    {
        if (headerAccessor.getSessionAttributes() == null) return null;
        return players.get((String) headerAccessor.getSessionAttributes().get("Tag"));
    }

    private boolean isBeastType(String type)
    {
        return type != null && mapRegistry.defaultMap().getBeastProbabilities().containsKey(type);
    }

    private void restoreBeasts(Player player)
    {
        for (Beast beast : player.getBeasts())
        {
            if (beast.getHp() < 100) beast.setHp(Math.min(100, beast.getHp() + 5));
        }
    }

    private int getDamage(Beast attacker, Beast defender, int attack)
    {
        int damage = 15 + attack * 10;
        if ((attacker.getType().equals("FIRE") && defender.getType().equals("EARTH")) ||
                (attacker.getType().equals("WATER") && defender.getType().equals("FIRE")) ||
                (attacker.getType().equals("EARTH") && defender.getType().equals("WATER"))) return damage + 10;
        if (!attacker.getType().equals(defender.getType())) return damage - 5;
        return damage;
    }

    private void prepareNextBeast(Player player, String status)
    {
        if (player.hasUsableBeast())
        {
            player.setChoosingBeast(true);
            player.setBattleStatus(status);
        }
        else
        {
            if (player.getOpponent() == null)
            {
                Beast activeBeast = player.getActiveBeast();
                if (activeBeast != null) activeBeast.setHp(100);
                player.setEncounterBeast(null);
                player.setChoosingBeast(false);
                player.setBattleStatus("Wild beast escaped and your beast recovered");
            }
            else
            {
                player.setEncounterBeast(null);
                player.setChoosingBeast(false);
                player.setBattleStatus("No usable beasts remain");
            }
        }
    }

    private void startArenaBattle(Player player)
    {
        if (player.getOpponent() != null) return;
        for (Player opponent : players.values())
        {
            if (!opponent.getTag().equals(player.getTag()) && opponent.getActivity() == TileType.ARENA.code() && opponent.getOpponent() == null &&
                    opponent.getEncounterBeast() == null && opponent.hasUsableBeast())
            {
                player.setOpponent(opponent.getTag());
                opponent.setOpponent(player.getTag());
                player.setTurn(true);
                opponent.setTurn(false);
                player.setBattleStatus("Your turn");
                opponent.setBattleStatus(player.getTag() + " is choosing an attack");
                return;
            }
        }
        player.setBattleStatus("Waiting for an arena opponent");
    }

    private void defeatArenaPlayer(Player winner, Player loser)
    {
        winner.getBeasts().addAll(loser.getBeasts());
        loser.getBeasts().clear();
        winner.setBattleStatus("You won and received all opposing beasts");
        loser.setBattleStatus("You lost all your beasts");
        endArenaBattle(winner, loser, winner.getBattleStatus());
    }

    private void endArenaBattle(Player winner, Player loser, String winnerStatus)
    {
        winner.setOpponent(null);
        loser.setOpponent(null);
        winner.setTurn(false);
        loser.setTurn(false);
        winner.setChoosingBeast(false);
        loser.setChoosingBeast(false);
        winner.setBattleStatus(winnerStatus);
    }

    private void broadcastPlayers()
    {
        messagingTemplate.convertAndSend("/topic/players", players.values());
    }
}
