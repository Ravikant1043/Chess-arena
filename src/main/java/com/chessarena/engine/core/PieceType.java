package com.chessarena.engine.core;

/**
 * The six kinds of chess piece, each carrying its conventional material value
 * and the letter used in FEN / algebraic notation (uppercase; case is applied
 * per-color elsewhere).
 */
public enum PieceType {
    PAWN(1, 'P'),
    KNIGHT(3, 'N'),
    BISHOP(3, 'B'),
    ROOK(5, 'R'),
    QUEEN(9, 'Q'),
    KING(0, 'K');

    private final int value;
    private final char letter;

    PieceType(int value, char letter) {
        this.value = value;
        this.letter = letter;
    }

    public int value() {
        return value;
    }

    public char letter() {
        return letter;
    }
}
