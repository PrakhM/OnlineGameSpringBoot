package dev.prakhm.OnlineGame.player;

import java.util.ArrayList;
import java.util.List;

public class Player
{
    private String tag;
    private int x;
    private int y;
    private String colour;
    private int location;
    private int mode;
    private int activity;
    private List<Beast> beasts = new ArrayList<>();
    private int activeBeastIndex;
    private Beast encounterBeast;
    private String opponent;
    private boolean turn;
    private boolean choosingBeast;
    private String battleStatus;

    public String getTag()
    {
        return tag;
    }

    public int[] getCoordinate()
    {
        return new int[] {x, y};
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public String getColour()
    {
        return this.colour;
    }

    public int getLocation()
    {
        return this.location;
    }

    public int getMode()
    {
        return this.mode;
    }

    public int getActivity()
    {
        return this.activity;
    }

    public List<Beast> getBeasts()
    {
        return beasts;
    }

    public int getActiveBeastIndex()
    {
        return activeBeastIndex;
    }

    public Beast getActiveBeast()
    {
        if (activeBeastIndex < 0 || activeBeastIndex >= beasts.size()) return null;
        return beasts.get(activeBeastIndex);
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

    public boolean hasUsableBeast()
    {
        return beasts.stream().anyMatch(beast -> beast.getHp() > 0);
    }

    public void setActivity(int activity)
    {
        this.activity = activity;
    }

    public void setMode(int mode)
    {
        this.mode = mode;
    }

    public void setLocation(int location)
    {
        this.location = location;
    }

    public void setColour(String colour)
    {
        this.colour = colour;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public void setCoordinates(int[] coordinate)
    {
        this.x = coordinate[0];
        this.y = coordinate[1];
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
}
