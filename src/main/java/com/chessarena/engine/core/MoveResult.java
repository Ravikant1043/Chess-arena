package com.chessarena.engine.core;

/**
 * The outcome of attempting to apply a {@link Move} to a game.
 *
 * <p>A rejected move carries {@code legal = false} and a human-readable {@code message};
 * a legal move carries the resulting {@link GameStatus}, whose turn it now is, and a
 * spoken-style {@code description} such as "Rook to a4" or "Bishop takes c4 — check!"
 * for display in the game chat.
 */
public record MoveResult(
        boolean legal,
        String message,
        GameStatus status,
        Color sideToMove,
        Color winner,
        String description) {

    public static MoveResult illegal(String reason) {
        return new MoveResult(false, reason, GameStatus.IN_PROGRESS, null, null, null);
    }

    public static MoveResult ok(GameStatus status, Color sideToMove, Color winner, String description) {
        return new MoveResult(true, "ok", status, sideToMove, winner, description);
    }
}
