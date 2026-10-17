package dev.prakhm.OnlineGame.beast;

public class Beast
{
    private String type;
    private int hp = 100;
    private int level = 1;

    public Beast() {}

    public Beast(String type)
    {
        this(type, 1);
    }

    public Beast(String type, int level)
    {
        this.type = type;
        this.level = level;
    }

    public String getType()
    {
        return type;
    }

    public int getHp()
    {
        return hp;
    }

    public int getLevel()
    {
        return level;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setHp(int hp)
    {
        this.hp = hp;
    }

    public void setLevel(int level)
    {
        this.level = level;
    }
}