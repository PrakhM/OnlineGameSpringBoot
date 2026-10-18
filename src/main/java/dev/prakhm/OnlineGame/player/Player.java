package dev.prakhm.OnlineGame.player;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.prakhm.OnlineGame.Battle.BattleState;
import dev.prakhm.OnlineGame.Items.BattleItem;
import dev.prakhm.OnlineGame.Items.Beast;
import dev.prakhm.OnlineGame.Items.NonBattleItem;

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
    private List<NonBattleItem> nonBattleInventory = new ArrayList<>();
    private List<BattleItem> battleInventory = new ArrayList<>();
    private BattleState battle = new BattleState();

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

    @JsonProperty("beasts")
    @JsonAlias("battleInventory")
    public List<BattleItem> getBattleInventory()
    {
        return battleInventory;
    }

    public List<NonBattleItem> getNonBattleInventory()
    {
        return nonBattleInventory;
    }

    @JsonUnwrapped
    public BattleState getBattle()
    {
        return battle;
    }

    @JsonIgnore
    public Beast getActiveBeast()
    {
        return getUsableBeast(battle.getActiveBeastIndex());
    }

    @JsonIgnore
    public Beast getUsableBeast(int index)
    {
        if (index < 0 || index >= battleInventory.size()) return null;
        return Beast.usable(battleInventory.get(index));
    }

    @JsonIgnore
    public boolean hasUsableBeast()
    {
        return battleInventory.stream().anyMatch(item -> Beast.usable(item) != null);
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

    @JsonProperty("beasts")
    @JsonAlias("battleInventory")
    public void setBattleInventory(List<BattleItem> battleInventory)
    {
        this.battleInventory = battleInventory == null ? new ArrayList<>() : battleInventory;
    }

    public void setNonBattleInventory(List<NonBattleItem> nonBattleInventory)
    {
        this.nonBattleInventory = nonBattleInventory == null ? new ArrayList<>() : nonBattleInventory;
    }

    public void setBattle(BattleState battle)
    {
        this.battle = battle == null ? new BattleState() : battle;
    }
}