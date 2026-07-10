package com.chessarena.engine.core;

import com.chessarena.engine.pieces.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the pseudo-legal moves offered by individual pieces into fully <b>legal</b>
 * moves by discarding any that would leave the mover's own king in check.
 *
 * <p>Kept as its own class (rather than piling the logic into {@link Board}) to honour
 * the Single Responsibility Principle: {@code Board} knows how to hold and mutate a
 * position; {@code MoveGenerator} knows the rule "you may not leave your king in check".
 */
public final class MoveGenerator {

    private MoveGenerator() {
    }

    /** All legal moves for whichever side is to move on {@code board}. */
    public static List<Move> legalMoves(Board board) {
        Color mover = board.sideToMove();
        List<Move> legal = new ArrayList<>();

        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece piece = board.pieceAt(f, r);
                if (piece == null || piece.color() != mover) {
                    continue;
                }
                Position from = new Position(f, r);
                for (Move candidate : piece.pseudoLegalMoves(board, from)) {
                    if (isKingSafeAfter(board, candidate, mover)) {
                        legal.add(candidate);
                    }
                }
            }
        }
        return legal;
    }

    /** Simulate {@code move} on a copy and confirm {@code mover}'s king is not left in check. */
    private static boolean isKingSafeAfter(Board board, Move move, Color mover) {
        Board copy = board.copy();
        copy.applyMove(move);
        return !copy.isInCheck(mover);
    }
}
