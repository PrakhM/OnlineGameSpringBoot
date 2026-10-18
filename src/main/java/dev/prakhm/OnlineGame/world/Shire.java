package dev.prakhm.OnlineGame.world;

import dev.prakhm.OnlineGame.config.GameMapProperties;
import dev.prakhm.OnlineGame.config.GameMapProperties.MapSettings;
import org.springframework.stereotype.Component;

@Component
public class Shire extends GameMap
{
    public Shire(GameMapProperties properties)
    {
        super(GameMap.SHIRE_ID, "shire", properties.getShire());
    }
}