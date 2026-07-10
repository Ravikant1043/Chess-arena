package com.chessarena.matchmaking;

import java.time.Instant;

/**
 * An open invitation broadcast to the lobby when a player clicks "Find Opponent".
 * The first online player to accept it wins the match; the challenge then closes.
 *
 * <p>State transitions are guarded: {@link #tryClaim(String)} is synchronized so that
 * under a race between two accepters, exactly one succeeds ("first accept wins").
 */
public class Challenge {

    public enum Status {OPEN, CLAIMED, CANCELLED}

    private final String id;
    private final String challenger;
    private final Instant createdAt;
    private volatile Status status = Status.OPEN;
    private volatile String claimedBy;

    public Challenge(String id, String challenger, Instant createdAt) {
        this.id = id;
        this.challenger = challenger;
        this.createdAt = createdAt;
    }

    /**
     * Atomically attempt to claim this challenge for {@code accepter}.
     *
     * @return true if this call won the race (challenge was OPEN and is now CLAIMED);
     *         false if it was already claimed, cancelled, or self-accepted.
     */
    public synchronized boolean tryClaim(String accepter) {
        if (status != Status.OPEN || accepter.equals(challenger)) {
            return false;
        }
        this.status = Status.CLAIMED;
        this.claimedBy = accepter;
        return true;
    }

    public synchronized boolean cancel() {
        if (status != Status.OPEN) {
            return false;
        }
        this.status = Status.CANCELLED;
        return true;
    }

    public String getId() {
        return id;
    }

    public String getChallenger() {
        return challenger;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Status getStatus() {
        return status;
    }

    public String getClaimedBy() {
        return claimedBy;
    }
}
