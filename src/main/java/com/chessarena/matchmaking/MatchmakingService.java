package com.chessarena.matchmaking;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks open {@link Challenge}s and resolves the "first accept wins" race.
 *
 * <p>The registry is in-memory (a single-instance dev setup). The concurrency-critical
 * step — claiming a challenge — is delegated to {@link Challenge#tryClaim(String)}, whose
 * synchronized body guarantees that if two players accept the same challenge at once,
 * only one match is created.
 */
@Service
public class MatchmakingService {

    private final ConcurrentMap<String, Challenge> challenges = new ConcurrentHashMap<>();

    /** Creates and registers an OPEN challenge from {@code challenger}. */
    public Challenge createChallenge(String challenger) {
        String id = UUID.randomUUID().toString();
        Challenge challenge = new Challenge(id, challenger, Instant.now());
        challenges.put(id, challenge);
        return challenge;
    }

    /**
     * Attempt to accept a challenge.
     *
     * @return the successfully matched challenge, or empty if it no longer exists, was
     *         already taken, or the accepter is the challenger.
     */
    public Optional<Challenge> accept(String challengeId, String accepter) {
        Challenge challenge = challenges.get(challengeId);
        if (challenge == null) {
            return Optional.empty();
        }
        if (challenge.tryClaim(accepter)) {
            challenges.remove(challengeId); // resolved; drop from the open set
            return Optional.of(challenge);
        }
        return Optional.empty();
    }

    /** Cancels a specific open challenge (e.g. the challenger navigates away). */
    public void cancel(String challengeId) {
        Challenge challenge = challenges.get(challengeId);
        if (challenge != null && challenge.cancel()) {
            challenges.remove(challengeId);
        }
    }

    /** Cancels every open challenge created by {@code username} (used on disconnect). */
    public void cancelChallengesBy(String username) {
        challenges.values().removeIf(c ->
                c.getChallenger().equals(username) && c.cancel());
    }

    public Optional<Challenge> find(String challengeId) {
        return Optional.ofNullable(challenges.get(challengeId));
    }
}
