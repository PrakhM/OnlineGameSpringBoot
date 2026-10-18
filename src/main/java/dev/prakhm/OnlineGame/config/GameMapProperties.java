package dev.prakhm.OnlineGame.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per map tuning, bound from {@code game.maps.<name>} in application.yml.
 *
 * <p>Defaults match the values the game shipped with before this became configurable, so a missing
 * or partial property block behaves exactly as before.
 */
@ConfigurationProperties(prefix = "game.maps")
public class GameMapProperties
{
    private final MapSettings shire = new MapSettings();
    private final MapSettings bree = new MapSettings();

    public MapSettings getShire()
    {
        return shire;
    }

    public MapSettings getBree()
    {
        return bree;
    }

    public MapSettings forName(String name)
    {
        return switch (name.toLowerCase())
        {
            case "shire" -> shire;
            case "bree" -> bree;
            default -> throw new IllegalArgumentException("No configuration for map: " + name);
        };
    }

    public static class MapSettings
    {
        /** Percentage chance, 0-100, that a valid move starts a wild encounter. */
        private int encounterRate = 20;

        private final LevelRange levelRange = new LevelRange();

        private final Map<String, Double> beastProbabilities = new LinkedHashMap<>(Map.of(
                "FIRE", 1.0,
                "WATER", 1.0,
                "EARTH", 1.0));

        public int getEncounterRate()
        {
            return encounterRate;
        }

        public void setEncounterRate(int encounterRate)
        {
            this.encounterRate = encounterRate;
        }

        public LevelRange getLevelRange()
        {
            return levelRange;
        }

        public Map<String, Double> getBeastProbabilities()
        {
            return beastProbabilities;
        }
    }

    public static class LevelRange
    {
        private int min = 1;
        private int max = 3;

        public int getMin()
        {
            return min;
        }

        public void setMin(int min)
        {
            this.min = min;
        }

        public int getMax()
        {
            return max;
        }

        public void setMax(int max)
        {
            this.max = max;
        }
    }
}