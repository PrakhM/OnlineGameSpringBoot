package dev.prakhm.OnlineGame.player;

import dev.prakhm.OnlineGame.beast.Beast;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The persistent profile of a player: identity, where they are and what they carry.
 *
 * <p>This object outlives any single websocket connection. Everything here is what the browser
 * client renders, so the field names below are part of the {@code /topic/players} wire contract.
 * Connection-scoped concerns (the websocket session id, last move time) live in
 * {@link PlayerSession} instead, and the registry of connected players lives in
 * {@link WorldStateManager}.
 *
 * <p>The scalars are volatile and the beast roster is a copy-on-write list because a profile is
 * written by the session that owns it but read by everyone: the roster broadcast serialises every
 * connected profile while holding only the sender's lock, and an arena battle writes both
 * participants from whichever session moved first.
 */
public class Player
{
    private static final AtomicLong NEXT_ID = new AtomicLong();

    private long id;
    private volatile String tag;
    private volatile int x;
    private volatile int y;
    private volatile String colour;
    private volatile int location;
    private volatile int mode;
    private volatile int activity;
    private final List<Beast> beasts = new CopyOnWriteArrayList<>();
    private volatile int activeBeastIndex;
    private volatile Beast encounterBeast;
    private volatile String opponent;
    private volatile boolean turn;
    private volatile boolean choosingBeast;
    private volatile String battleStatus;

    /**
     * Issues the next profile id and assigns it to this player.
     *
     * <p>Called on join so that a client cannot choose its own id by putting one in the payload.
     *
     * @return this player, for chaining
     */
    public Player assignId()
    {
        this.id = NEXT_ID.incrementAndGet();
        return this;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getTag()
    {
        return tag;
    }

    public int[] getCoordinate()
    {
        return new int[] {x, y};
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public String getColour()
    {
        return this.colour;
    }

    public int getLocation()
    {
        return this.location;
    }

    public int getMode()
    {
        return this.mode;
    }

    public int getActivity()
    {
        return this.activity;
    }

    public List<Beast> getBeasts()
    {
        return beasts;
    }

    /**
     * Replaces the roster, keeping the copy-on-write list itself so callers holding
     * {@link #getBeasts()} see the new contents. Needed for deserialization: without a setter the
     * roster is read-only and a join arrives with no beasts.
     */
    public void setBeasts(List<Beast> beasts)
    {
        this.beasts.clear();
        if (beasts != null) this.beasts.addAll(beasts);
    }

    public int getActiveBeastIndex()
    {
        return activeBeastIndex;
    }

    public Beast getActiveBeast()
    {
        if (activeBeastIndex < 0 || activeBeastIndex >= beasts.size()) return null;
        return beasts.get(activeBeastIndex);
    }

    public Beast getEncounterBeast()
    {
        return encounterBeast;
    }

    public String getOpponent()
    {
        return opponent;
    }

    public boolean isTurn()
    {
        return turn;
    }

    public boolean isChoosingBeast()
    {
        return choosingBeast;
    }

    public String getBattleStatus()
    {
        return battleStatus;
    }

    public boolean hasUsableBeast()
    {
        return beasts.stream().anyMatch(beast -> beast.getHp() > 0);
    }

    public void setActivity(int activity)
    {
        this.activity = activity;
    }

    public void setMode(int mode)
    {
        this.mode = mode;
    }

    public void setLocation(int location)
    {
        this.location = location;
    }

    public void setColour(String colour)
    {
        this.colour = colour;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public void setCoordinates(int[] coordinate)
    {
        this.x = coordinate[0];
        this.y = coordinate[1];
    }

    public void setActiveBeastIndex(int activeBeastIndex)
    {
        this.activeBeastIndex = activeBeastIndex;
    }

    public void setEncounterBeast(Beast encounterBeast)
    {
        this.encounterBeast = encounterBeast;
    }

    public void setOpponent(String opponent)
    {
        this.opponent = opponent;
    }

    public void setTurn(boolean turn)
    {
        this.turn = turn;
    }

    public void setChoosingBeast(boolean choosingBeast)
    {
        this.choosingBeast = choosingBeast;
    }

    public void setBattleStatus(String battleStatus)
    {
        this.battleStatus = battleStatus;
    }
}
