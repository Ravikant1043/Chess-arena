package com.chessarena.engine.pieces;

import com.chessarena.engine.core.Board;
import com.chessarena.engine.core.Color;
import com.chessarena.engine.core.Move;
import com.chessarena.engine.core.PieceType;
import com.chessarena.engine.core.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * The pawn — the piece with the most special rules, so it fully overrides
 * {@link #pseudoLegalMoves}: single and double forward steps, diagonal captures,
 * en passant, and promotion (expanded into one move per promotion choice).
 */
public final class Pawn extends Piece {

    private static final PieceType[] PROMOTIONS = {
            PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
    };

    public Pawn(Color color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.PAWN;
    }

    /** A pawn attacks only the two forward diagonals — regardless of what (if anything) sits there. */
    @Override
    public List<Position> attacks(Board board, Position from) {
        int dir = color.pawnDirection();
        List<Position> result = new ArrayList<>();
        for (int df : new int[]{-1, 1}) {
            Position p = from.offset(df, dir);
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public List<Move> pseudoLegalMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        int dir = color.pawnDirection();

        // --- Forward one square (only onto an empty square) ---
        Position oneForward = from.offset(0, dir);
        if (oneForward != null && board.pieceAt(oneForward) == null) {
            addForwardOrPromotion(moves, from, oneForward);

            // --- Forward two squares from the starting rank ---
            if (from.rank() == color.pawnStartRank()) {
                Position twoForward = from.offset(0, 2 * dir);
                if (twoForward != null && board.pieceAt(twoForward) == null) {
                    moves.add(new Move(from, twoForward));
                }
            }
        }

        // --- Diagonal captures (incl. en passant) ---
        for (int df : new int[]{-1, 1}) {
            Position diag = from.offset(df, dir);
            if (diag == null) {
                continue;
            }
            Piece occupant = board.pieceAt(diag);
            if (occupant != null && occupant.color() != color) {
                addForwardOrPromotion(moves, from, diag);
            } else if (occupant == null && diag.equals(board.enPassantTarget())) {
                // En passant: the capture square is empty; the captured pawn sits beside us.
                moves.add(new Move(from, diag));
            }
        }
        return moves;
    }

    /** If the destination is the promotion rank, expand into four promotion moves; otherwise a plain move. */
    private void addForwardOrPromotion(List<Move> moves, Position from, Position to) {
        if (to.rank() == color.promotionRank()) {
            for (PieceType promo : PROMOTIONS) {
                moves.add(new Move(from, to, promo));
            }
        } else {
            moves.add(new Move(from, to));
        }
    }

    @Override
    public Piece copy() {
        Pawn c = new Pawn(color);
        c.hasMoved = this.hasMoved;
        return c;
    }
}
