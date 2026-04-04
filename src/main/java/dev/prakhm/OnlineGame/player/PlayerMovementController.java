package dev.prakhm.OnlineGame.player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;

@Controller
public class PlayerMovementController
{
    HashMap<String, Player> players = new HashMap<>();
    private String[] colors = {
            "#e74c3c","#3498db","#2ecc71","#f39c12",
            "#9b59b6","#1abc9c","#e67e22","#e91e63"
    };
    private int colourIndex = 0;

    private SimpMessagingTemplate messagingTemplate;

    public PlayerMovementController(SimpMessagingTemplate messagingTemplate)
    {
        this.messagingTemplate = messagingTemplate;
    }

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
