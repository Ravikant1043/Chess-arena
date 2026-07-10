package com.chessarena.engine.pieces;

import com.chessarena.engine.core.Board;
import com.chessarena.engine.core.Color;
import com.chessarena.engine.core.PieceType;
import com.chessarena.engine.core.Position;

import java.util.ArrayList;
import java.util.List;

/** The knight: eight fixed L-shaped jumps, unobstructed by intervening pieces. */
public final class Knight extends Piece {

    private static final int[][] OFFSETS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };

    public Knight(Color color) {
        super(color);
    }

    @Override
    public PieceType type() {
        return PieceType.KNIGHT;
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
    public Piece copy() {
        Knight c = new Knight(color);
        c.hasMoved = this.hasMoved;
        return c;
    }
}
