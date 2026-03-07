package dev.prakhm.OnlineGame.player;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.HashMap;

@Controller
public class PlayerMovementController
{
    HashMap<String, Player> players = new HashMap<>();

    @MessageMapping("/player.addPlayer")
    public void addPlayer(@Payload Player player, SimpMessageHeaderAccessor headerAccessor)
    {
        headerAccessor.getSessionAttributes().put("Tag", player.getTag());
        players.put(player.getTag(), player);
    }

    @MessageMapping("/player.movePlayer")
    public void movePlayer(@Payload int dir, SimpMessageHeaderAccessor headerAccessor)
    {
        Player player = players.get((String)headerAccessor.getSessionAttributes().get("Tag"));
        int[] coordinate = new int[2];
        int[] oc = player.getCoordinate();
        if(dir == 0)
        {
            coordinate[1] = oc[1] + 1;
        }
        else if(dir == 1)
        {
            coordinate[0] = oc[0] - 1;
        }
        else if(dir == 2)
        {
            coordinate[1] = oc[1] - 1;
        }
        else if(dir == 3)
        {
            coordinate[0] = oc[0] + 1;
        }
        player.setCoordinates(coordinate);
    }
}
