package com.chessarena.engine.core;

/**
 * An immutable description of a single move request: from-square, to-square, and
 * (optionally) the piece type a pawn promotes to.
 *
 * <p>Flags such as "this was a castling move" or "this captured en passant" are not
 * stored here — they are <i>derived</i> by the engine when the move is applied, so a
 * {@code Move} stays a pure intent object that the client can send verbatim.
 */
public record Move(Position from, Position to, PieceType promotion) {

    public Move(Position from, Position to) {
        this(from, to, null);
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    /** Build a move from UCI-style text such as {@code "e2e4"} or {@code "e7e8q"}. */
    public static Move fromUci(String uci) {
        if (uci == null || (uci.length() != 4 && uci.length() != 5)) {
            throw new IllegalArgumentException("Invalid UCI move: " + uci);
        }
        Position from = Position.fromAlgebraic(uci.substring(0, 2));
        Position to = Position.fromAlgebraic(uci.substring(2, 4));
        PieceType promo = null;
        if (uci.length() == 5) {
            promo = switch (Character.toLowerCase(uci.charAt(4))) {
                case 'q' -> PieceType.QUEEN;
                case 'r' -> PieceType.ROOK;
                case 'b' -> PieceType.BISHOP;
                case 'n' -> PieceType.KNIGHT;
                default -> throw new IllegalArgumentException("Bad promotion piece: " + uci);
            };
        }
        return new Move(from, to, promo);
    }

    public String toUci() {
        String base = from.toAlgebraic() + to.toAlgebraic();
        return isPromotion() ? base + Character.toLowerCase(promotion.letter()) : base;
    }

    @Override
    public String toString() {
        return toUci();
    }
}
