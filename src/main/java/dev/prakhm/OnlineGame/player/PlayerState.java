package dev.prakhm.OnlineGame.player;

/**
 * What a connected player is currently doing.
 *
 * <p>This is the session-level summary of a player's situation. The individual booleans it replaces
 * ({@code encounterBeast != null}, {@code opponent != null}, standing on an arena tile) still live
 * on {@link Player} because the browser client renders them; the enum exists so server code can
 * branch on a single value instead of re-deriving the same three conditions.
 */
public enum PlayerState
{
    /** Roaming, no encounter and no arena battle. */
    FREE,
    /** A wild beast is active and must be defeated, captured or skipped. */
    IN_WILD_ENCOUNTER,
    /** Standing on an arena tile waiting to be paired with an opponent. */
    IN_ARENA_QUEUE,
    /** Paired in an arena battle, attacking in turn order. */
    IN_BATTLE
}
