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

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The websocket endpoint for the world: join, move and fight.
 *
 * <p>No state lives here. Profiles belong to {@link Player}, live connections to
 * {@link PlayerSession}, and the set of connected players to {@link WorldStateManager}. This class
 * translates STOMP messages into operations on those objects.
 *
 * <p>Handlers are not synchronized. Each one takes the lock of the session it is acting for, and
 * the handlers that touch an opponent take both locks in a fixed order so two players fighting each
 * other cannot deadlock. Maps guard their own tile occupancy. Arena matchmaking is the one place
 * that touches an arbitrary third player, so it is serialised behind {@link #arenaMatchmaking} and
 * is always entered <em>before</em> any session lock, keeping the lock order one-way.
 */
@Controller
public class PlayerMovementController
{
    private static final String TAG_ATTRIBUTE = "Tag";
    private static final String PLAYERS_TOPIC = "/topic/players";

    private final String[] colours = {
            "#e74c3c","#3498db","#2ecc71","#f39c12",
            "#9b59b6","#1abc9c","#e67e22","#e91e63"
    };
    private final AtomicInteger colourIndex = new AtomicInteger();
    private final Random random = new Random();
    /** Serialises arena pairing, which is the only operation reaching an arbitrary third player. */
    private final Object arenaMatchmaking = new Object();

    private final SimpMessagingTemplate messagingTemplate;
    private final MapRegistry mapRegistry;
    private final WorldStateManager worldState;

    public PlayerMovementController(SimpMessagingTemplate messagingTemplate, MapRegistry mapRegistry,
                                    WorldStateManager worldState)
    {
        this.messagingTemplate = messagingTemplate;
        this.mapRegistry = mapRegistry;
        this.worldState = worldState;
    }

    @MessageMapping("/player.addPlayer")
    public void addPlayer(@Payload Player player, SimpMessageHeaderAccessor headerAccessor)
    {
        if (player == null || player.getTag() == null || player.getTag().isBlank() || player.getTag().length() > 12 ||
                headerAccessor.getSessionAttributes() == null || headerAccessor.getSessionAttributes().containsKey(TAG_ATTRIBUTE) ||
                worldState.findByUsername(player.getTag()).isPresent() ||
                player.getBeasts() == null || player.getBeasts().size() != 1 ||
                player.getBeasts().get(0) == null || !isBeastType(player.getBeasts().get(0).getType())) return;

        GameMap spawnMap = mapRegistry.defaultMap();
        player.assignId();
        player.setLocation(spawnMap.getMapId());
        player.setActiveBeastIndex(0);
        player.setColour(colours[colourIndex.getAndIncrement() % colours.length]);
        player.setBattleStatus("Exploring");
        if (!spawnMap.addPlayer(player)) return;

        PlayerSession session = new PlayerSession(player, headerAccessor.getSessionId());
        if (worldState.add(session).isEmpty())
        {
            // Lost the race for this username between the check above and registration.
            spawnMap.removePlayer(player);
            return;
        }

        headerAccessor.getSessionAttributes().put(TAG_ATTRIBUTE, player.getTag());
        broadcastPlayers();
    }

    @MessageMapping("/player.movePlayer")
    public void movePlayer(@Payload int dir, SimpMessageHeaderAccessor headerAccessor)
    {
        PlayerSession session = getSession(headerAccessor);
        if (session == null) return;

        boolean wantsArenaBattle;
        synchronized (session)
        {
            Player player = session.getPlayer();
            if (dir < 0 || dir > 3 || player.getEncounterBeast() != null || player.getOpponent() != null) return;
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
            if (moved)
            {
                session.markMoved();
                worldState.reindex(session);
                restoreBeasts(player);
            }
            if (moved && player.getActivity() != TileType.ARENA.code() && map.rollEncounter(player))
            {
                player.setEncounterBeast(map.spawnBeast());
                player.setBattleStatus("A wild beast appeared");
            }
            wantsArenaBattle = player.getActivity() == TileType.ARENA.code() && player.hasUsableBeast();
        }

        // Matchmaking reaches other players' locks, so it runs with this session unlocked.
        if (wantsArenaBattle) startArenaBattle(session);
        broadcastPlayers();
    }

    @MessageMapping("/player.wildAttack")
    public void wildAttack(@Payload int attack, SimpMessageHeaderAccessor headerAccessor)
    {
        PlayerSession session = getSession(headerAccessor);
        if (session == null) return;
        synchronized (session)
        {
            Player player = session.getPlayer();
            Beast activeBeast = player.getActiveBeast();
            Beast encounterBeast = player.getEncounterBeast();
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
    }

    @MessageMapping("/player.skipEncounter")
    public void skipEncounter(SimpMessageHeaderAccessor headerAccessor)
    {
        PlayerSession session = getSession(headerAccessor);
        if (session == null) return;
        synchronized (session)
        {
            Player player = session.getPlayer();
            if (player.getEncounterBeast() == null) return;
            player.setEncounterBeast(null);
            player.setChoosingBeast(false);
            player.setBattleStatus("Wild encounter skipped");
            broadcastPlayers();
        }
    }

    @MessageMapping("/player.arenaAttack")
    public void arenaAttack(@Payload int attack, SimpMessageHeaderAccessor headerAccessor)
    {
        PlayerSession session = getSession(headerAccessor);
        if (session == null) return;
        PlayerSession opponentSession = findOpponent(session);
        if (opponentSession == null) return;

        boolean[] acted = {false};
        PlayerSession.lockInOrder(session, opponentSession, () -> {
            Player player = session.getPlayer();
            Player opponent = opponentSession.getPlayer();
            if (!opponent.getTag().equals(player.getOpponent())) return;
            if (!player.isTurn() || player.isChoosingBeast() || attack < 0 || attack > 2) return;
            Beast activeBeast = player.getActiveBeast();
            Beast opponentBeast = opponent.getActiveBeast();
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
            acted[0] = true;
        });

        if (acted[0]) broadcastPlayers();
    }

    @MessageMapping("/player.chooseBeast")
    public void chooseBeast(@Payload int index, SimpMessageHeaderAccessor headerAccessor)
    {
        PlayerSession session = getSession(headerAccessor);
        if (session == null) return;

        PlayerSession opponentSession = findOpponent(session);
        boolean[] acted = {false};

        if (opponentSession == null)
        {
            synchronized (session)
            {
                Player player = session.getPlayer();
                if (!isValidBeastChoice(player, index)) return;
                player.setActiveBeastIndex(index);
                player.setChoosingBeast(false);
                player.setBattleStatus("Wild battle continues");
                acted[0] = true;
            }
        }
        else PlayerSession.lockInOrder(session, opponentSession, () -> {
            Player player = session.getPlayer();
            Player opponent = opponentSession.getPlayer();
            boolean stillPaired = opponent.getTag().equals(player.getOpponent());
            if (!isValidBeastChoice(player, index)) return;
            player.setActiveBeastIndex(index);
            player.setChoosingBeast(false);
            if (stillPaired)
            {
                player.setTurn(true);
                opponent.setTurn(false);
                player.setBattleStatus("Your turn");
                opponent.setBattleStatus(player.getTag() + " is choosing an attack");
            }
            else player.setBattleStatus("Wild battle continues");
            acted[0] = true;
        });

        if (acted[0]) broadcastPlayers();
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event)
    {
        PlayerSession session = getSession(SimpMessageHeaderAccessor.wrap(event.getMessage()));
        if (session == null) return;

        PlayerSession opponentSession = findOpponent(session);
        if (opponentSession == null)
        {
            removeSession(session);
            return;
        }

        boolean[] acted = {false};
        PlayerSession.lockInOrder(session, opponentSession, () -> {
            Player player = session.getPlayer();
            Player opponent = opponentSession.getPlayer();
            boolean stillPaired = opponent.getTag().equals(player.getOpponent());
            if (worldState.remove(session.getPlayerId()).isEmpty()) return;
            GameMap map = mapRegistry.byId(player.getLocation());
            if (map != null) map.removePlayer(player);
            if (stillPaired) endArenaBattle(opponent, player, "Arena opponent disconnected");
            acted[0] = true;
        });

        if (acted[0]) broadcastPlayers();
    }

    /**
     * The session behind an inbound message.
     *
     * <p>Prefers the websocket session id, falling back to the username attribute that chat and the
     * client-side roster are keyed by.
     */
    private PlayerSession getSession(SimpMessageHeaderAccessor headerAccessor)
    {
        if (headerAccessor == null) return null;
        return worldState.findByWebsocketSessionId(headerAccessor.getSessionId())
                .or(() -> getSessionByTag(headerAccessor))
                .orElse(null);
    }

    private Optional<PlayerSession> getSessionByTag(SimpMessageHeaderAccessor headerAccessor)
    {
        if (headerAccessor.getSessionAttributes() == null) return Optional.empty();
        Object tag = headerAccessor.getSessionAttributes().get(TAG_ATTRIBUTE);
        return tag == null ? Optional.empty() : worldState.findByUsername((String) tag);
    }

    /**
     * The session of this player's current arena opponent, if any.
     *
     * <p>Read without holding a lock so that both locks can then be taken in a fixed order. Callers
     * must re-check the pairing once they hold both.
     */
    private PlayerSession findOpponent(PlayerSession session)
    {
        String opponentTag = session.getPlayer().getOpponent();
        return opponentTag == null ? null : worldState.findByUsername(opponentTag).orElse(null);
    }

    private void removeSession(PlayerSession session)
    {
        synchronized (session)
        {
            if (worldState.remove(session.getPlayerId()).isEmpty()) return;
            Player player = session.getPlayer();
            GameMap map = mapRegistry.byId(player.getLocation());
            if (map != null) map.removePlayer(player);
            broadcastPlayers();
        }
    }

    private boolean isBeastType(String type)
    {
        return type != null && mapRegistry.defaultMap().getBeastProbabilities().containsKey(type);
    }

    private boolean isValidBeastChoice(Player player, int index)
    {
        return player.isChoosingBeast() && index >= 0 && index < player.getBeasts().size() &&
                player.getBeasts().get(index).getHp() > 0;
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

    /**
     * Pairs a player who just stepped onto an arena tile with a waiting opponent, or tells them it is
     * still waiting.
     *
     * <p>Held behind {@link #arenaMatchmaking} so two players stepping onto the tile together cannot
     * both choose each other, and so this is the only path that reaches a third player's lock.
     */
    private void startArenaBattle(PlayerSession session)
    {
        synchronized (arenaMatchmaking)
        {
            for (PlayerSession candidate : worldState.findAll())
            {
                if (candidate == session) continue;
                if (tryPairForArena(session, candidate)) return;
            }
            synchronized (session)
            {
                Player player = session.getPlayer();
                if (player.getOpponent() == null) player.setBattleStatus("Waiting for an arena opponent");
            }
        }
    }

    /**
     * Pairs two players if both are free and standing on an arena tile. Both locks are held and the
     * conditions are re-checked inside them.
     *
     * @return whether the pairing was made
     */
    private boolean tryPairForArena(PlayerSession seeker, PlayerSession candidate)
    {
        boolean[] paired = {false};
        PlayerSession.lockInOrder(seeker, candidate, () -> {
            Player player = seeker.getPlayer();
            Player opponent = candidate.getPlayer();
            if (player.getOpponent() != null || opponent.getOpponent() != null) return;
            if (opponent.getActivity() != TileType.ARENA.code()) return;
            if (opponent.getEncounterBeast() != null || !opponent.hasUsableBeast()) return;

            player.setOpponent(opponent.getTag());
            opponent.setOpponent(player.getTag());
            player.setTurn(true);
            opponent.setTurn(false);
            player.setBattleStatus("Your turn");
            opponent.setBattleStatus(player.getTag() + " is choosing an attack");
            paired[0] = true;
        });
        return paired[0];
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
        List<Player> players = worldState.findAll().stream().map(PlayerSession::getPlayer).toList();
        messagingTemplate.convertAndSend(PLAYERS_TOPIC, players);
    }
}
