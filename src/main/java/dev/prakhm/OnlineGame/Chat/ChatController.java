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
    public ChatMessage sendMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor)
    {
        if (message == null || headerAccessor.getSessionAttributes() == null) return null;
        String sender = (String) headerAccessor.getSessionAttributes().get("Tag");
        if (sender == null || message.getContent() == null || message.getContent().isBlank() || message.getContent().length() > 80) return null;
        return new ChatMessage(sender, message.getContent(), "CHAT");
    }

    @MessageMapping("/chat.join")
    @SendTo("/topic/chat")
    public ChatMessage join(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor)
    {
        if (headerAccessor.getSessionAttributes() == null) return null;
        String sender = (String) headerAccessor.getSessionAttributes().get("Tag");
        if (sender == null) return null;
        return new ChatMessage(sender, sender + " joined the world!", "JOIN");
    }

}
