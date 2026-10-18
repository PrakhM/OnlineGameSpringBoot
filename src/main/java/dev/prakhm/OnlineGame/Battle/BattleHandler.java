package dev.prakhm.OnlineGame.Battle;

import dev.prakhm.OnlineGame.Items.BattleItem;
import dev.prakhm.OnlineGame.Items.Beast;
import dev.prakhm.OnlineGame.player.Player;
import dev.prakhm.OnlineGame.player.PlayerRegistry;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Controller
public class BattleHandler
{
    public static final int ARENA_ACTIVITY = 120;
    private static final int MAX_ATTACK = 2;
    private static final int ENCOUNTER_CHANCE = 20;
    private static final int MOVE_HEAL = 5;

    private final PlayerRegistry registry;
    private final Random random = new Random();

    public BattleHandler(PlayerRegistry registry)
    {
        this.registry = registry;
    }

    @MessageMapping("/player.wildAttack")
    public synchronized void wildAttack(@Payload int attack, SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = registry.getPlayer(headerAccessor);
        BattleState state = player == null ? null : player.getBattle();
        Beast encounterBeast = state == null ? null : state.getEncounterBeast();
        Beast activeBeast = player == null ? null : player.getActiveBeast();
        if (activeBeast == null || encounterBeast == null || state.isChoosingBeast() || attack < 0 || attack > MAX_ATTACK) return;

        encounterBeast.takeDamage(getDamage(activeBeast, encounterBeast, attack));
        if (!encounterBeast.isConscious())
        {
            encounterBeast.setHp(Beast.MAX_HP);
            player.getBattleInventory().add(encounterBeast);
            state.setEncounterBeast(null);
            state.setBattleStatus("Wild beast captured");
        }
        else
        {
            activeBeast.takeDamage(getDamage(encounterBeast, activeBeast, random.nextInt(MAX_ATTACK + 1)));
            if (!activeBeast.isConscious()) prepareNextBeast(player, "Choose your next beast");
        }
        registry.broadcastPlayers();
    }

    @MessageMapping("/player.skipEncounter")
    public synchronized void skipEncounter(SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = registry.getPlayer(headerAccessor);
        BattleState state = player == null ? null : player.getBattle();
        if (state == null || !state.isWildEncounter()) return;
        state.setEncounterBeast(null);
        state.setChoosingBeast(false);
        state.setBattleStatus("Wild encounter skipped");
        registry.broadcastPlayers();
    }

    @MessageMapping("/player.arenaAttack")
    public synchronized void arenaAttack(@Payload int attack, SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = registry.getPlayer(headerAccessor);
        BattleState state = player == null ? null : player.getBattle();
        Player opponent = state == null ? null : registry.find(state.getOpponent());
        Beast activeBeast = player == null ? null : player.getActiveBeast();
        Beast opponentBeast = opponent == null ? null : opponent.getActiveBeast();
        if (activeBeast == null || opponentBeast == null || !state.isTurn() || state.isChoosingBeast() ||
                attack < 0 || attack > MAX_ATTACK) return;

        opponentBeast.takeDamage(getDamage(activeBeast, opponentBeast, attack));
        if (!opponentBeast.isConscious())
        {
            if (opponent.hasUsableBeast())
            {
                state.setTurn(false);
                opponent.getBattle().setTurn(false);
                prepareNextBeast(opponent, "Choose your next beast");
                state.setBattleStatus(opponent.getTag() + " is choosing a beast");
            }
            else defeatArenaPlayer(player, opponent);
        }
        else
        {
            state.setTurn(false);
            opponent.getBattle().setTurn(true);
            state.setBattleStatus(opponent.getTag() + " is choosing an attack");
            opponent.getBattle().setBattleStatus("Your turn");
        }
        registry.broadcastPlayers();
    }

    @MessageMapping("/player.chooseBeast")
    public synchronized void chooseBeast(@Payload int index, SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = registry.getPlayer(headerAccessor);
        BattleState state = player == null ? null : player.getBattle();
        Beast chosenBeast = player == null ? null : player.getUsableBeast(index);
        if (chosenBeast == null || state == null || !state.isChoosingBeast()) return;

        state.setActiveBeastIndex(index);
        state.setChoosingBeast(false);
        if (state.isArenaBattle())
        {
            Player opponent = registry.find(state.getOpponent());
            if (opponent != null)
            {
                state.setTurn(true);
                opponent.getBattle().setTurn(false);
                state.setBattleStatus("Your turn");
                opponent.getBattle().setBattleStatus(player.getTag() + " is choosing an attack");
            }
        }
        else state.setBattleStatus("Wild battle continues");
        registry.broadcastPlayers();
    }

    public synchronized boolean onPlayerMoved(Player player)
    {
        restoreBeasts(player);
        return maybeStartWildEncounter(player);
    }

    public synchronized void tryStartArenaBattle(Player player)
    {
        if (!player.hasUsableBeast() || player.getActivity() != ARENA_ACTIVITY) return;
        BattleState state = player.getBattle();
        if (state.isInBattle()) return;

        for (Player opponent : registry.getPlayers())
        {
            BattleState opponentState = opponent.getBattle();
            if (opponent.getTag().equals(player.getTag()) || opponent.getActivity() != ARENA_ACTIVITY ||
                    opponentState.isInBattle() || !opponent.hasUsableBeast()) continue;

            state.setOpponent(opponent.getTag());
            opponentState.setOpponent(player.getTag());
            state.setTurn(true);
            opponentState.setTurn(false);
            state.setBattleStatus("Your turn");
            opponentState.setBattleStatus(player.getTag() + " is choosing an attack");
            return;
        }
        state.setBattleStatus("Waiting for an arena opponent");
    }

    public synchronized void onPlayerLeft(Player player)
    {
        BattleState state = player.getBattle();
        Player opponent = registry.find(state.getOpponent());
        if (opponent != null) endArenaBattle(opponent, player, "Arena opponent disconnected");
    }

    private boolean maybeStartWildEncounter(Player player)
    {
        BattleState state = player.getBattle();
        if (state.isInBattle() || player.getActivity() == ARENA_ACTIVITY || !player.hasUsableBeast()) return false;
        if (random.nextInt(100) >= ENCOUNTER_CHANCE) return false;

        state.setEncounterBeast(Beast.of(Beast.ELEMENTS.get(random.nextInt(Beast.ELEMENTS.size()))));
        state.setBattleStatus("A wild beast appeared");
        return true;
    }

    private void restoreBeasts(Player player)
    {
        for (BattleItem item : player.getBattleInventory())
        {
            if (item instanceof Beast beast && beast.getHp() < Beast.MAX_HP) beast.heal(MOVE_HEAL);
        }
    }

    private void prepareNextBeast(Player player, String status)
    {
        BattleState state = player.getBattle();
        if (player.hasUsableBeast())
        {
            state.setChoosingBeast(true);
            state.setBattleStatus(status);
            return;
        }
        if (state.isArenaBattle())
        {
            state.setChoosingBeast(false);
            state.setBattleStatus("No usable beasts remain");
            return;
        }
        for (BattleItem item : player.getBattleInventory())
        {
            if (item instanceof Beast beast) beast.setHp(Beast.MAX_HP);
        }
        state.setEncounterBeast(null);
        state.setChoosingBeast(false);
        state.setBattleStatus("Wild beast escaped and your beast recovered");
    }

    private void defeatArenaPlayer(Player winner, Player loser)
    {
        List<BattleItem> wonBeasts = new ArrayList<>();
        for (BattleItem item : loser.getBattleInventory())
        {
            if (item.getAloneUsable()) wonBeasts.add(item);
        }
        loser.getBattleInventory().removeAll(wonBeasts);
        winner.getBattleInventory().addAll(wonBeasts);
        endArenaBattle(winner, loser, "You won and received all opposing beasts");
        loser.getBattle().setBattleStatus("You lost all your beasts");
    }

    private void endArenaBattle(Player winner, Player loser, String winnerStatus)
    {
        winner.getBattle().setOpponent(null);
        loser.getBattle().setOpponent(null);
        winner.getBattle().setTurn(false);
        loser.getBattle().setTurn(false);
        winner.getBattle().setChoosingBeast(false);
        loser.getBattle().setChoosingBeast(false);
        winner.getBattle().setBattleStatus(winnerStatus);
    }

    private int getDamage(Beast attacker, Beast defender, int attack)
    {
        int damage = 15 + attack * 10;
        if (beats(attacker, defender)) return damage + 10;
        if (!attacker.getType().equals(defender.getType())) return damage - 5;
        return damage;
    }

    private boolean beats(Beast attacker, Beast defender)
    {
        return (attacker.isElement("FIRE") && defender.isElement("EARTH")) ||
                (attacker.isElement("WATER") && defender.isElement("FIRE")) ||
                (attacker.isElement("EARTH") && defender.isElement("WATER"));
    }
}