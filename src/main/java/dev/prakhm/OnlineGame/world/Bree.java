package dev.prakhm.OnlineGame.world;

import dev.prakhm.OnlineGame.config.GameMapProperties;
import dev.prakhm.OnlineGame.config.GameMapProperties.MapSettings;
import org.springframework.stereotype.Component;

@Component
public class Bree extends GameMap
{
    public Bree(GameMapProperties properties)
    {
        super(GameMap.BREE_ID, "bree", properties.getBree());
    }
}