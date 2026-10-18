package dev.prakhm.OnlineGame.Battle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.prakhm.OnlineGame.Items.Beast;

public class BattleState
{
    private int activeBeastIndex;
    private Beast encounterBeast;
    private String opponent;
    private boolean turn;
    private boolean choosingBeast;
    private String battleStatus;

    public int getActiveBeastIndex()
    {
        return activeBeastIndex;
    }

    public Beast getEncounterBeast()
    {
        return encounterBeast;
    }

    public String getOpponent()
    {
        return opponent;
    }

    public boolean isTurn()
    {
        return turn;
    }

    public boolean isChoosingBeast()
    {
        return choosingBeast;
    }

    public String getBattleStatus()
    {
        return battleStatus;
    }

    @JsonIgnore
    public boolean isWildEncounter()
    {
        return encounterBeast != null;
    }

    @JsonIgnore
    public boolean isArenaBattle()
    {
        return opponent != null;
    }

    @JsonIgnore
    public boolean isInBattle()
    {
        return isWildEncounter() || isArenaBattle();
    }

    public void setActiveBeastIndex(int activeBeastIndex)
    {
        this.activeBeastIndex = activeBeastIndex;
    }

    public void setEncounterBeast(Beast encounterBeast)
    {
        this.encounterBeast = encounterBeast;
    }

    public void setOpponent(String opponent)
    {
        this.opponent = opponent;
    }

    public void setTurn(boolean turn)
    {
        this.turn = turn;
    }

    public void setChoosingBeast(boolean choosingBeast)
    {
        this.choosingBeast = choosingBeast;
    }

    public void setBattleStatus(String battleStatus)
    {
        this.battleStatus = battleStatus;
    }

    public void clear()
    {
        encounterBeast = null;
        opponent = null;
        turn = false;
        choosingBeast = false;
        battleStatus = null;
    }
}