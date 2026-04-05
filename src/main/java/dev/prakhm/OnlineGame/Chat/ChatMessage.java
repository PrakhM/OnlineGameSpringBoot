package dev.prakhm.OnlineGame.Chat;

public class ChatMessage
{
    private String sender;
    private String content;
    private String type; // "CHAT", "JOIN", "LEAVE"

    public ChatMessage() {}

    public ChatMessage(String sender, String content, String type)
    {
        this.sender = sender;
        this.content = content;
        this.type = type;
    }

    public String getSender()
    {
        return sender;
    }
    public String getContent()
    {
        return content;
    }
    public String getType()
    {
        return type;
    }

    public void setSender(String sender)
    {
        this.sender = sender;
    }
    public void setContent(String content)
    {
        this.content = content;
    }
    public void setType(String type)
    {
        this.type = type;
    }
}

