package com.chessarena.ws.messages;

import java.util.List;

/**
 * The game-channel protocol: inbound client requests and outbound state broadcasts.
 * Each outbound record carries a {@code type} discriminator as a real field so it
 * serializes to JSON.
 */
public final class GameMessages {

    private GameMessages() {
    }

    // ------------------------------------------------- inbound (client → server)

    public record MoveRequest(String gameId, String uci) {
    }

    public record ResignRequest(String gameId) {
    }

    public record AcceptRequest(String challengeId) {
    }

    public record CancelRequest(String challengeId) {
    }

    /** A chat message typed by a player. */
    public record ChatRequest(String gameId, String text) {
    }

    /** "Which squares can the piece on {@code square} move to?" (for click highlighting). */
    public record TargetsRequest(String gameId, String square) {
    }

    // ------------------------------------------------- outbound (server → client)

    /**
     * Sent privately to each matched player when a game starts. Carries the initial
     * board so the client can render immediately — it must not depend on catching a
     * {@code GAME_STATE} broadcast that may fire before its game-channel subscription
     * is established.
     */
    public record GameStart(String type, String gameId, String white, String black,
                            String yourColor, String opponent,
                            String[][] board, String sideToMove) {
        public GameStart(String gameId, String white, String black, String yourColor,
                         String opponent, String[][] board, String sideToMove) {
            this("GAME_START", gameId, white, black, yourColor, opponent, board, sideToMove);
        }
    }

    /** Broadcast to both players after every accepted move. */
    public record GameState(String type, String gameId, String[][] board, String sideToMove,
                            String status, String lastMove, String lastMoveText,
                            List<String> moveHistory) {
        public GameState(String gameId, String[][] board, String sideToMove, String status,
                         String lastMove, String lastMoveText, List<String> moveHistory) {
            this("GAME_STATE", gameId, board, sideToMove, status, lastMove, lastMoveText, moveHistory);
        }
    }

    /** Broadcast when the game ends. */
    public record GameOver(String type, String gameId, String winner, String endReason,
                           String[][] board) {
        public GameOver(String gameId, String winner, String endReason, String[][] board) {
            this("GAME_OVER", gameId, winner, endReason, board);
        }
    }

    /** A player's chat message, broadcast to both players. */
    public record ChatMessage(String type, String gameId, String sender, String text) {
        public ChatMessage(String gameId, String sender, String text) {
            this("CHAT", gameId, sender, text);
        }
    }

    /** Private reply listing the legal destination squares for a clicked piece. */
    public record TargetsResponse(String type, String square, List<String> targets) {
        public TargetsResponse(String square, List<String> targets) {
            this("TARGETS", square, targets);
        }
    }

    /** Sent privately to a player when their action is rejected (illegal move, not your turn, ...). */
    public record GameError(String type, String message) {
        public GameError(String message) {
            this("ERROR", message);
        }
    }
}
