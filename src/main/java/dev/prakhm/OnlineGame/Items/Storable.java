package dev.prakhm.OnlineGame.Items;

public abstract class Storable
{
    private String type;
    private boolean aloneUsable;

    public String getType()
    {
        return type;
    }

    public boolean getAloneUsable()
    {
        return aloneUsable;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setAloneUsable(boolean aloneUsable)
    {
        this.aloneUsable = aloneUsable;
    }
}