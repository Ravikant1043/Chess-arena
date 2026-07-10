package com.chessarena.engine.pieces;

import com.chessarena.engine.core.Board;
import com.chessarena.engine.core.Color;
import com.chessarena.engine.core.PieceType;
import com.chessarena.engine.core.Position;

import java.util.List;

/** The rook: slides any distance along ranks and files. Its {@code hasMoved} flag also governs castling rights. */
public final class Rook extends Piece {

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public Rook(Color color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.ROOK;
    }

    @Override
    public List<Position> attacks(Board board, Position from) {
        return slide(board, from, DIRECTIONS);
    }

    @Override
    public Piece copy() {
        Rook c = new Rook(color);
        c.hasMoved = this.hasMoved;
        return c;
    }
}
