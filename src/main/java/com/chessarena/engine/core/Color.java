package com.chessarena.engine.core;

/**
 * The two sides in a game of chess.
 *
 * <p>Demonstrates <b>encapsulation of behaviour in an enum</b>: instead of scattering
 * {@code if (color == WHITE)} checks around the codebase, each constant knows its own
 * forward direction and how to find its opponent.
 */
public enum Color {
    WHITE(1),
    BLACK(-1);

    /** The direction pawns of this color advance along the rank axis (+1 for white, -1 for black). */
    private final int pawnDirection;

    Color(int pawnDirection) {
        this.pawnDirection = pawnDirection;
    }

    public int pawnDirection() {
        return pawnDirection;
    }

    /** @return the opposing color. */
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** The rank (0-based) on which this color's pawns start. */
    public int pawnStartRank() {
        return this == WHITE ? 1 : 6;
    }

    /** The rank (0-based) a pawn must reach to promote. */
    public int promotionRank() {
        return this == WHITE ? 7 : 0;
    }
}
