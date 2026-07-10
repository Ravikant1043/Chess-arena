package com.chessarena.engine.core;

/**
 * An immutable square on the board, addressed by {@code file} (column 0..7 = a..h)
 * and {@code rank} (row 0..7 = ranks 1..8).
 *
 * <p>Implemented as a {@code record} — a natural fit for an immutable value object:
 * it gives us value-based {@code equals}/{@code hashCode} for free, which matters
 * because positions are used as {@link java.util.Map} keys and {@link java.util.Set}
 * members throughout move generation.
 */
public record Position(int file, int rank) {

    public Position {
        if (!isValid(file, rank)) {
            throw new IllegalArgumentException("Off-board position: file=" + file + ", rank=" + rank);
        }
    }

    public static boolean isValid(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    /** Returns a new position offset by the given deltas, or {@code null} if it would fall off the board. */
    public Position offset(int df, int dr) {
        int nf = file + df, nr = rank + dr;
        return isValid(nf, nr) ? new Position(nf, nr) : null;
    }

    /** Parse algebraic coordinate notation such as {@code "e4"}. */
    public static Position fromAlgebraic(String s) {
        if (s == null || s.length() != 2) {
            throw new IllegalArgumentException("Invalid square: " + s);
        }
        int file = s.charAt(0) - 'a';
        int rank = s.charAt(1) - '1';
        return new Position(file, rank);
    }

    /** Render as algebraic coordinate notation, e.g. {@code "e4"}. */
    public String toAlgebraic() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
