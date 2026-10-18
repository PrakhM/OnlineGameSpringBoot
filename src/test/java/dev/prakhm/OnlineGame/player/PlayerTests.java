package dev.prakhm.OnlineGame.player;

import dev.prakhm.OnlineGame.beast.Beast;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The profile's identity and its JSON contract.
 *
 * <p>{@link Player} is both the STOMP payload for a join and the roster broadcast to every client,
 * so a field that stops being deserializable fails silently: the join is simply rejected and nothing
 * is logged. These tests pin both directions.
 */
class PlayerTests
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void aJoinPayloadPopulatesTheUsernameAndRoster()
    {
        Player player = MAPPER.readValue("{\"tag\":\"Alpha\",\"beasts\":[{\"type\":\"FIRE\"}]}", Player.class);

        assertEquals("Alpha", player.getTag());
        assertEquals(1, player.getBeasts().size());
        assertEquals("FIRE", player.getBeasts().get(0).getType());
    }

    @Test
    void settingTheRosterReplacesWhatCallersAlreadyHold()
    {
        Player player = new Player();
        List<Beast> roster = player.getBeasts();
        roster.add(new Beast("WATER"));

        player.setBeasts(List.of(new Beast("EARTH")));

        assertEquals(1, roster.size(), "the list handed out earlier should see the replacement");
        assertEquals("EARTH", player.getBeasts().get(0).getType());
    }

    @Test
    void aNullRosterLeavesThePlayerWithNoBeasts()
    {
        Player player = new Player();
        player.getBeasts().add(new Beast("FIRE"));

        player.setBeasts(null);

        assertTrue(player.getBeasts().isEmpty());
    }

    @Test
    void assignIdOverridesAnyIdTheClientSent()
    {
        Player player = MAPPER.readValue("{\"tag\":\"Alpha\",\"id\":99999,\"beasts\":[]}", Player.class);
        player.assignId();

        assertNotEquals(99999L, player.getId(), "a client must not be able to choose its own id");
    }

    @Test
    void idsAreUniquePerAssignment()
    {
        long first = new Player().assignId().getId();
        long second = new Player().assignId().getId();

        assertTrue(first > 0);
        assertTrue(second > first);
    }

    @Test
    void theRosterBroadcastStillCarriesEveryFieldTheClientRenders()
    {
        Player player = new Player();
        player.setTag("Alpha");
        player.assignId();
        player.setLocation(0);
        player.setCoordinates(new int[] {3, 4});
        player.setActivity(120);
        player.setOpponent("Bravo");
        player.setTurn(true);
        player.setBattleStatus("Your turn");
        player.getBeasts().add(new Beast("FIRE"));

        ObjectNode json = (ObjectNode) MAPPER.valueToTree(player);

        assertEquals("Alpha", json.path("tag").asString());
        assertEquals(3, json.path("x").asInt());
        assertEquals(4, json.path("y").asInt());
        assertEquals(0, json.path("location").asInt());
        assertEquals(120, json.path("activity").asInt());
        assertEquals("Bravo", json.path("opponent").asString());
        assertTrue(json.path("turn").asBoolean());
        assertEquals("Your turn", json.path("battleStatus").asString());
        assertEquals("FIRE", json.path("beasts").get(0).path("type").asString());
        assertTrue(json.has("id"), "the profile id rides along on the roster");
    }
}
