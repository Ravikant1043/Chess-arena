package com.chessarena.engine.pieces;

import com.chessarena.engine.core.Board;
import com.chessarena.engine.core.Color;
import com.chessarena.engine.core.Move;
import com.chessarena.engine.core.PieceType;
import com.chessarena.engine.core.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * The king: one step in any direction, plus castling. Castling legality (the king and
 * rook have not moved, the squares between are empty, and the king neither starts in,
 * passes through, nor lands on an attacked square) is checked here using the board's
 * own attack detection.
 */
public final class King extends Piece {

    private static final int[][] OFFSETS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public King(Color color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.KING;
    }

    @Override
    public List<Position> attacks(Board board, Position from) {
        List<Position> result = new ArrayList<>();
        for (int[] o : OFFSETS) {
            Position p = from.offset(o[0], o[1]);
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public List<Move> pseudoLegalMoves(Board board, Position from) {
        List<Move> moves = super.pseudoLegalMoves(board, from); // ordinary one-step moves
        addCastlingMoves(board, from, moves);
        return moves;
    }

    private void addCastlingMoves(Board board, Position from, List<Move> moves) {
        if (hasMoved) {
            return;
        }
        Color enemy = color.opposite();
        int rank = (color == Color.WHITE) ? 0 : 7;

        // The king must not currently be in check to castle.
        if (board.isSquareAttacked(from, enemy)) {
            return;
        }

        // --- King-side (O-O): squares f,g empty; rook on h unmoved; f,g not attacked. ---
        if (isRookReadyToCastle(board, new Position(7, rank))
                && board.pieceAt(new Position(5, rank)) == null
                && board.pieceAt(new Position(6, rank)) == null
                && !board.isSquareAttacked(new Position(5, rank), enemy)
                && !board.isSquareAttacked(new Position(6, rank), enemy)) {
            moves.add(new Move(from, new Position(6, rank)));
        }

        // --- Queen-side (O-O-O): squares b,c,d empty; rook on a unmoved; c,d not attacked. ---
        if (isRookReadyToCastle(board, new Position(0, rank))
                && board.pieceAt(new Position(1, rank)) == null
                && board.pieceAt(new Position(2, rank)) == null
                && board.pieceAt(new Position(3, rank)) == null
                && !board.isSquareAttacked(new Position(3, rank), enemy)
                && !board.isSquareAttacked(new Position(2, rank), enemy)) {
            moves.add(new Move(from, new Position(2, rank)));
        }
    }

    private boolean isRookReadyToCastle(Board board, Position rookSquare) {
        Piece p = board.pieceAt(rookSquare);
        return p instanceof Rook && p.color() == color && !p.hasMoved();
    }

    @Override
    public Piece copy() {
        King c = new King(color);
        c.hasMoved = this.hasMoved;
        return c;
    }
}
