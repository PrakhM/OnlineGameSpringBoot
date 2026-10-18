package dev.prakhm.OnlineGame.Items;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class Beast extends BattleItem
{
    public static final List<String> ELEMENTS = List.of("FIRE", "WATER", "EARTH");
    public static final int MAX_HP = 100;

    private int hp = MAX_HP;

    public Beast()
    {
        setAloneUsable(true);
    }

    public int getHp()
    {
        return hp;
    }

    public void setHp(int hp)
    {
        this.hp = Math.max(0, Math.min(MAX_HP, hp));
    }

    @JsonIgnore
    public boolean isConscious()
    {
        return hp > 0;
    }

    public void heal(int amount)
    {
        setHp(hp + amount);
    }

    public void takeDamage(int damage)
    {
        setHp(hp - damage);
    }

    public boolean isElement(String element)
    {
        return getType() != null && getType().equals(element);
    }

    public static Beast of(String element)
    {
        Beast beast = new Beast();
        beast.setType(element);
        return beast;
    }

    public static boolean isKnownElement(String type)
    {
        return type != null && ELEMENTS.contains(type);
    }

    public static Beast usable(BattleItem item)
    {
        if (item == null || !item.getAloneUsable() || !(item instanceof Beast beast) || !beast.isConscious()) return null;
        return beast;
    }
}