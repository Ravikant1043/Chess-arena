package com.chessarena.ws.messages;

import java.util.List;

/**
 * Server → client messages published on the lobby channels. Each carries a {@code type}
 * discriminator (a real field, so it serializes to JSON) that the browser switches on.
 * Grouped as nested records because they form one cohesive protocol.
 */
public final class LobbyMessages {

    private LobbyMessages() {
    }

    public record PlayerInfo(String username, int score, boolean inGame) {
    }

    /** The full list of who is online (sent on join/leave and on connect). */
    public record OnlinePlayers(String type, List<PlayerInfo> players) {
        public OnlinePlayers(List<PlayerInfo> players) {
            this("ONLINE_PLAYERS", players);
        }
    }

    /** A new open challenge broadcast to everyone in the lobby. */
    public record ChallengeBroadcast(String type, String challengeId, String challenger) {
        public ChallengeBroadcast(String challengeId, String challenger) {
            this("CHALLENGE", challengeId, challenger);
        }
    }

    /** A challenge is no longer joinable (claimed by someone else, or cancelled). */
    public record ChallengeClosed(String type, String challengeId, String reason) {
        public ChallengeClosed(String challengeId, String reason) {
            this("CHALLENGE_CLOSED", challengeId, reason);
        }
    }
}
