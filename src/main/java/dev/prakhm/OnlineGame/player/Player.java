package dev.prakhm.OnlineGame.player;

public class Player
{
    private String tag;
    private int x;
    private int y;

    public String getTag()
    {
        return tag;
    }

    public int[] getCoordinate()
    {
        return new int[] {x, y};
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
