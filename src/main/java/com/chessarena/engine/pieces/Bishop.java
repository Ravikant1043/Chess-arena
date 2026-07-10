package com.chessarena.engine.pieces;

import com.chessarena.engine.core.Board;
import com.chessarena.engine.core.Color;
import com.chessarena.engine.core.PieceType;
import com.chessarena.engine.core.Position;

import java.util.List;

/** The bishop: slides any distance along the four diagonals. */
public final class Bishop extends Piece {

    private static final int[][] DIRECTIONS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    public Bishop(Color color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.BISHOP;
    }

    @Override
    public List<Position> attacks(Board board, Position from) {
        return slide(board, from, DIRECTIONS);
    }

    @Override
    public Piece copy() {
        Bishop c = new Bishop(color);
        c.hasMoved = this.hasMoved;
        return c;
    }
}
