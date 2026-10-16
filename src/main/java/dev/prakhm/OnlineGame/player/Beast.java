package dev.prakhm.OnlineGame.player;

public class Beast
{
    private String type;
    private int hp = 100;

    public Beast() {}

    public Beast(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public int getHp()
    {
        return hp;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setHp(int hp)
    {
        this.hp = hp;
    }
}
