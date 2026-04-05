package dev.prakhm.OnlineGame.player;

public class Player
{
    private String tag;
    private int x;
    private int y;
    private String colour;

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
