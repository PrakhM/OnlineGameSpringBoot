package dev.prakhm.OnlineGame.player;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class PlayerMovementController
{
    @MessageMapping("/player.addPlayer")
    public void addPlayer(@Payload Player player, SimpMessageHeaderAccessor headerAccessor)
    {
        headerAccessor.getSessionAttributes().put("Tag", player.getTag());
    }
}
