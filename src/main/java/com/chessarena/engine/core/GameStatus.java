package com.chessarena.engine.core;

/**
 * The lifecycle state of a game after any given move.
 */
public enum GameStatus {
    IN_PROGRESS,
    CHECK,
    CHECKMATE,
    STALEMATE,
    /** Draw by insufficient material, fifty-move rule, or agreement. */
    DRAW;

    public boolean isGameOver() {
        return this == CHECKMATE || this == STALEMATE || this == DRAW;
    }
}
