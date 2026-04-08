package dev.prakhm.OnlineGame.player;

public class Player
{
    private String tag;
    private int x;
    private int y;
    private String colour;
    private int location;
    private int mode;
    private int activity;

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
}
