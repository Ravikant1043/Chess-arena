package com.chessarena.game;

/**
 * The result of an attempted game action (a move, a resignation) — everything the
 * WebSocket layer needs to broadcast the new state to both players, including a
 * spoken-style description of the move ("Rook to a4") for the game chat.
 */
public record GameResult(
        boolean accepted,
        String rejectionReason,
        GameSession session,
        boolean gameOver,
        String winnerUsername,
        String endReason,
        String moveDescription) {

    public static GameResult rejected(GameSession session, String reason) {
        return new GameResult(false, reason, session, false, null, null, null);
    }

    public static GameResult ongoing(GameSession session, String moveDescription) {
        return new GameResult(true, null, session, false, null, null, moveDescription);
    }

    public static GameResult finished(GameSession session, String winnerUsername,
                                      String endReason, String moveDescription) {
        return new GameResult(true, null, session, true, winnerUsername, endReason, moveDescription);
    }
}
