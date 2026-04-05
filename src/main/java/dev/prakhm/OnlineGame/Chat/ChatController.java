package dev.prakhm.OnlineGame.Chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController
{
    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public ChatMessage sendMessage(@Payload ChatMessage message)
    {
        return message;
    }

    @MessageMapping("/chat.join")
    @SendTo("/topic/chat")
    public ChatMessage join(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor)
    {
        headerAccessor.getSessionAttributes().put("username", message.getSender());
        return new ChatMessage(message.getSender(), message.getSender() + " joined the world!", "JOIN");
    }

}
