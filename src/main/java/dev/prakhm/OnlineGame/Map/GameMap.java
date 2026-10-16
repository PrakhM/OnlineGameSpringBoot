package dev.prakhm.OnlineGame.Map;

import dev.prakhm.OnlineGame.player.Player;
import org.springframework.stereotype.Component;

@Component
public class GameMap
{
    int width = 20;
    int height = 15;

    private final int[][] shire = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,1,1,0,0,0,0,1,1,0,0,0,1,0,0,0,0,1},
            {1,0,0,1,1,0,0,0,0,1,1,120,120,0,1,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,1},
            {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2,2,0,1},
            {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2,2,0,1},
            {1,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,1,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,0,0,20},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    private final int[][] bree = {
        {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,4,4,4,4,4,3,3,3,3,3,3,3,3,3,3,3,3,30},
        {4,4,4,4,4,4,4,3,3,3,3,3,3,3,3,3,3,3,3,30},
        {4,4,4,4,4,4,3,3,3,3,3,3,3,3,3,3,3,3,3,30},
        {4,4,4,4,4,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,4,4,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,4,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {10,3,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
        {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
    };

    private boolean[][] occupiedShire = new boolean[height][width];
    private boolean[][] occupiedBree = new boolean[height][width];


    class Maps
    {
        int[][] map;
        boolean[][] occupied;
        Maps(int[][] map, boolean[][] occupied)
        {
            this.map = map;
            this.occupied = occupied;
        }
    }

    Maps[] maps = new Maps[2];

    public GameMap()
    {
        maps[0] = new Maps(shire, occupiedShire);
        maps[1] = new Maps(bree, occupiedBree);
    }

    public synchronized boolean addPlayer(Player player)
    {
        int x = 1;
        while (x < width - 1 && (occupiedShire[1][x] || !isWalkable(new int[]{x, 1}, player.getLocation())))
        {
            x++;
        }
        if (occupiedShire[1][x] || !isWalkable(new int[]{x, 1}, player.getLocation())) return false;
        occupiedShire[1][x] = true;
        player.setCoordinates(new int[]{x, 1});
        return true;
    }

    public synchronized void removePlayer(Player player)
    {
        int[] coordinates = player.getCoordinate();
        int location = player.getLocation();
        if (isInBounds(coordinates) && maps[location].occupied[coordinates[1]][coordinates[0]])
        {
            maps[location].occupied[coordinates[1]][coordinates[0]] = false;
        }
    }

    public synchronized void movePlayer(Player player, int[] coordinates)
    {
        int location = player.getLocation();
        if (!isInBounds(coordinates)) return;

        if(isTeleport(coordinates, location))
        {
            int newLocation = maps[location].map[coordinates[1]][coordinates[0]] / 10 - 1;
            int[] start = getStart(location, coordinates[1], coordinates[0]);
            if (isOccupied(start, newLocation)) return;

            int[] curr = player.getCoordinate();
            maps[location].occupied[curr[1]][curr[0]] = false;
            player.setCoordinates(start);
            player.setLocation(newLocation);
            maps[newLocation].occupied[start[1]][start[0]] = true;
            player.setMode(0);
            player.setActivity(0);
        }
        else if(isWalkable(coordinates, location) && !isOccupied(coordinates, location))
        {
            int[] curr = player.getCoordinate();
            maps[location].occupied[curr[1]][curr[0]] = false;
            player.setCoordinates(coordinates);
            maps[location].occupied[coordinates[1]][coordinates[0]] = true;

            if(isInteractable(coordinates, location))
            {
                player.setMode(1);
                player.setActivity(maps[location].map[player.getY()][player.getX()]);
            }else
            {
                player.setMode(0);
                player.setActivity(0);
            }
        }
    }

    boolean isWalkable(int[] coordinates, int location)
    {
        int x = coordinates[0];
        int y = coordinates[1];
        if (!isInBounds(coordinates)) return false;
        return maps[location].map[y][x]%3 == 0 || maps[location].map[y][x] >= 100;
    }

    boolean isOccupied(int[] coordinates, int location)
    {
        if (!isInBounds(coordinates)) return true;
        return maps[location].occupied[coordinates[1]][coordinates[0]];
    }

    boolean isTeleport(int[] coordinates, int location)
    {
        if (!isInBounds(coordinates)) return false;
        if(maps[location].map[coordinates[1]][coordinates[0]] >= 10 &&
                maps[location].map[coordinates[1]][coordinates[0]] < 100) return true;
        else return false;
    }

    int[] getStart(int location, int y, int x)
    {
        int newLoc = maps[location].map[y][x]/10 - 1;
        int[][] map = maps[newLoc].map;
        if(location < newLoc)
        {
            for(int i = 0; i < height; i++)
            {
                if(map[i][0] >= 10)
                {
                    return new int[] {0, i};
                }
            }
        }
        else
        {
            for(int i = 0; i < height; i++)
            {
                if(map[i][width - 1] >= 10)
                {
                    return new int[] {width - 1, i};
                }
            }
        }

        return new int[] {10, 10};
    }

    boolean isInteractable(int[] coordinates, int location)
    {
        if(maps[location].map[coordinates[1]][coordinates[0]] >= 100)
        {
            return true;
        }
        return false;
    }

    boolean isInBounds(int[] coordinates)
    {
        int x = coordinates[0];
        int y = coordinates[1];
        return x >= 0 && x < width && y >= 0 && y < height;
    }
}
