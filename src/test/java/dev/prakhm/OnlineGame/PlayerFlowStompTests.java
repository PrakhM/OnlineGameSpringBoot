package dev.prakhm.OnlineGame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end safety net for the STOMP contract the browser client relies on.
 *
 * <p>Two real STOMP clients connect over SockJS, join the world and move. The test asserts that
 * every state change comes back as a broadcast on {@code /topic/players}, which is the single
 * channel the frontend uses to render players. Keeping this green guarantees later refactors do
 * not silently break the join/move path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlayerFlowStompTests
{
    private static final String TOPIC_PLAYERS = "/topic/players";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    private final List<StompSession> sessions = new ArrayList<>();
    private final List<WebSocketStompClient> clients = new ArrayList<>();

    @AfterEach
    void tearDown()
    {
        for (StompSession session : sessions)
        {
            try
            {
                session.disconnect();
            }
            catch (Exception ignored)
            {
                // best effort cleanup
            }
        }
        for (WebSocketStompClient client : clients)
        {
            try
            {
                client.stop();
            }
            catch (Exception ignored)
            {
                // best effort cleanup
            }
        }
    }

    @Test
    void joiningAndMovingBroadcastsUpdatedPlayersToEverySubscriber() throws Exception
    {
        BlockingQueue<String> alphaBroadcasts = new LinkedBlockingQueue<>();
        BlockingQueue<String> bravoBroadcasts = new LinkedBlockingQueue<>();

        StompSession alpha = connect(alphaBroadcasts);
        send(alpha, "/app/player.addPlayer", joinPayload("Alpha", "FIRE"));

        Map<String, JsonNode> afterAlphaJoin = awaitPlayers(alphaBroadcasts,
                players -> players.containsKey("Alpha"));
        assertEquals(1, afterAlphaJoin.size(), "only Alpha should be in the world: " + afterAlphaJoin);
        assertEquals(1, afterAlphaJoin.get("Alpha").path("x").asInt());
        assertEquals(1, afterAlphaJoin.get("Alpha").path("y").asInt());

        StompSession bravo = connect(bravoBroadcasts);
        send(bravo, "/app/player.addPlayer", joinPayload("Bravo", "WATER"));

        Map<String, JsonNode> afterBravoJoin = awaitPlayers(alphaBroadcasts,
                players -> players.containsKey("Alpha") && players.containsKey("Bravo"));
        assertEquals(2, afterBravoJoin.size(), "both players should be broadcast: " + afterBravoJoin);
        assertEquals(2, afterBravoJoin.get("Bravo").path("x").asInt(), "Bravo takes the free tile next to Alpha");

        // dir 2 is "down" in PlayerMovementController#movePlayer
        send(alpha, "/app/player.movePlayer", 2);

        Map<String, JsonNode> afterMove = awaitPlayers(alphaBroadcasts, players -> {
            JsonNode alphaPlayer = players.get("Alpha");
            return alphaPlayer != null && alphaPlayer.path("y").asInt() == 2;
        });
        assertEquals(1, afterMove.get("Alpha").path("x").asInt(), "moving down must not change x");
        assertEquals(2, afterMove.get("Alpha").path("y").asInt(), "moving down must change y");
        assertEquals(2, afterMove.get("Bravo").path("x").asInt(), "Bravo must not be moved by Alpha");
        assertEquals(1, afterMove.get("Bravo").path("y").asInt(), "Bravo must not be moved by Alpha");

        awaitPlayers(bravoBroadcasts, players -> {
            JsonNode alphaPlayer = players.get("Alpha");
            return alphaPlayer != null && alphaPlayer.path("y").asInt() == 2;
        });
    }

    private Map<String, Object> joinPayload(String tag, String beastType)
    {
        return Map.of("tag", tag, "beasts", List.of(Map.of("type", beastType)));
    }

    private void send(StompSession session, String destination, Object payload)
    {
        StompHeaders headers = new StompHeaders();
        headers.setDestination(destination);
        session.send(headers, payload);
    }

    private StompSession connect(BlockingQueue<String> broadcasts) throws Exception
    {
        SockJsClient sockJsClient = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient())));
        WebSocketStompClient client = new WebSocketStompClient(sockJsClient);
        client.setMessageConverter(new JacksonJsonMessageConverter());
        client.start();
        clients.add(client);

        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws",
                new StompSessionHandlerAdapter()
                {
                    @Override
                    public void afterConnected(StompSession connected, StompHeaders headers)
                    {
                        connected.subscribe(TOPIC_PLAYERS, new StompFrameHandler()
                        {
                            @Override
                            public Type getPayloadType(StompHeaders headers)
                            {
                                return byte[].class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload)
                            {
                                broadcasts.add(new String((byte[]) payload, StandardCharsets.UTF_8));
                            }
                        });
                    }
                }).get(15, TimeUnit.SECONDS);

        sessions.add(session);
        return session;
    }

    private Map<String, JsonNode> awaitPlayers(BlockingQueue<String> broadcasts,
            Predicate<Map<String, JsonNode>> condition) throws InterruptedException
    {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        Map<String, JsonNode> lastSeen = Map.of();
        String lastRaw = null;

        while (System.nanoTime() < deadline)
        {
            String raw = broadcasts.poll(250, TimeUnit.MILLISECONDS);
            if (raw == null) continue;
            lastRaw = raw;
            lastSeen = byTag(raw);
            if (condition.test(lastSeen)) return lastSeen;
        }

        fail("Timed out waiting for a matching /topic/players broadcast. Last payload: " + lastRaw);
        return lastSeen;
    }

    private Map<String, JsonNode> byTag(String rawBroadcast)
    {
        Map<String, JsonNode> players = new HashMap<>();
        for (JsonNode player : MAPPER.readTree(rawBroadcast))
        {
            players.put(player.path("tag").asString(), player);
        }
        return players;
    }
}