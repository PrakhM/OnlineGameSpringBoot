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

    private boolean[][] occupied = new boolean[height][width];

    public void addPlayer(Player player)
    {
        int x = 1;
        while (occupied[1][x] || !isWalkable(new int[]{x, 1}))
        {
            x++;
        }
        occupied[1][x] = true;
        player.setCoordinates(new int[]{x, 1});
    }

    public void movePlayer(Player player, int[] coordinates)
    {
        if(isWalkable(coordinates) && !isOccupied(coordinates))
        {
            int[] curr = player.getCoordinate();
            occupied[curr[1]][curr[0]] = false;
            player.setCoordinates(coordinates);
            occupied[coordinates[1]][coordinates[0]] = true;
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
        return occupied[coordinates[1]][coordinates[0]];
    }
}
