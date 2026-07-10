package com.chessarena.engine.pieces;

import com.chessarena.engine.core.Board;
import com.chessarena.engine.core.Color;
import com.chessarena.engine.core.PieceType;
import com.chessarena.engine.core.Position;

import java.util.List;

/** The queen: the union of rook and bishop movement — slides along ranks, files, and diagonals. */
public final class Queen extends Piece {

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public Queen(Color color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.QUEEN;
    }

    @Override
    public List<Position> attacks(Board board, Position from) {
        return slide(board, from, DIRECTIONS);
    }

    @Override
    public Piece copy() {
        Queen c = new Queen(color);
        c.hasMoved = this.hasMoved;
        return c;
    }
}
