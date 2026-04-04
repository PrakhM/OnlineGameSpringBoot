package dev.prakhm.OnlineGame.Map;

import dev.prakhm.OnlineGame.player.Player;
import org.springframework.stereotype.Component;

@Component
public class GameMap
{
    int width = 20;
    int height = 15;

    private int[][] map = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,1,1,0,0,0,0,1,1,0,0,0,1,0,0,0,0,1},
            {1,0,0,1,1,0,0,0,0,1,1,0,0,0,1,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,1},
            {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2,2,0,1},
            {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2,2,0,1},
            {1,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,1,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
    };

    private boolean[][] occupied = new boolean[15][20];

    public void addPlayer(Player player)
    {

    }

    public void movePlayer(Player player, int[] coordinates)
    {
        if(isWalkable(coordinates) && !isOccupied(coordinates))
        {
            int[] curr = player.getCoordinate();
            occupied[curr[0]][curr[1]] = false;
            player.setCoordinates(coordinates);
            occupied[coordinates[0]][coordinates[1]] = true;
        }
    }

    boolean isWalkable(int[] coordinates)
    {
        int x = coordinates[0];
        int y = coordinates[1];
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        return map[y][x] == 0;
    }

    boolean isOccupied(int[] coordinates)
    {
        return occupied[coordinates[0]][coordinates[1]];
    }
}
