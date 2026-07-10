package com.chessarena.engine.pieces;

import com.chessarena.engine.core.Board;
import com.chessarena.engine.core.Color;
import com.chessarena.engine.core.Move;
import com.chessarena.engine.core.PieceType;
import com.chessarena.engine.core.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * The abstract base of every chess piece — the centrepiece of this project's
 * object-oriented design.
 *
 * <ul>
 *   <li><b>Abstraction:</b> callers work with the {@code Piece} type and never care
 *       whether they hold a {@code Knight} or a {@code Queen}.</li>
 *   <li><b>Inheritance:</b> the six concrete pieces extend this class, reusing the
 *       common {@link #pseudoLegalMoves} template and the {@link #slide} ray helper.</li>
 *   <li><b>Polymorphism:</b> {@link #attacks(Board, Position)} is abstract; the board's
 *       move generation and check detection call it uniformly on any piece, and the
 *       correct piece-specific logic runs.</li>
 *   <li><b>Encapsulation:</b> {@link #color} is immutable and {@link #hasMoved} can
 *       only be flipped through {@link #markMoved()} — the flag that governs castling
 *       and pawn double-steps cannot be corrupted from outside.</li>
 * </ul>
 */
public abstract class Piece {

    protected final Color color;
    protected boolean hasMoved;

    protected Piece(Color color) {
        this.color = color;
    }

    public Color color() {
        return color;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void markMoved() {
        this.hasMoved = true;
    }

    /** The concrete kind of this piece. Implemented by each subclass. */
    public abstract PieceType type();

    /**
     * The set of squares this piece <i>attacks</i> from {@code from}, ignoring whether
     * the target holds a friendly piece and ignoring king-safety. Used both for move
     * generation and for {@link Board#isSquareAttacked} check detection.
     *
     * <p>Note: a pawn's attacked squares (diagonals) differ from the squares it can
     * move to (forward), which is why attack generation is kept separate from move
     * generation.
     */
    public abstract List<Position> attacks(Board board, Position from);

    /** Deep copy, used when the engine simulates a move on a throw-away board. */
    public abstract Piece copy();

    /**
     * Pseudo-legal moves: real moves this piece could make ignoring only whether they
     * leave the mover's own king in check (that final filter is applied by the move
     * generator). The default derives moves from {@link #attacks}; {@link Pawn} and
     * {@link King} override to add their special rules.
     */
    public List<Move> pseudoLegalMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        for (Position target : attacks(board, from)) {
            Piece occupant = board.pieceAt(target);
            if (occupant == null || occupant.color() != color) {
                moves.add(new Move(from, target));
            }
        }
        return moves;
    }

    /**
     * Shared ray-casting helper for the sliding pieces (bishop, rook, queen): walk each
     * direction until the board edge or the first occupied square (that square is
     * included, because a piece attacks the square of whatever blocks it).
     */
    protected List<Position> slide(Board board, Position from, int[][] directions) {
        List<Position> result = new ArrayList<>();
        for (int[] d : directions) {
            Position p = from.offset(d[0], d[1]);
            while (p != null) {
                result.add(p);
                if (board.pieceAt(p) != null) {
                    break; // blocked; include the blocker, then stop
                }
                p = p.offset(d[0], d[1]);
            }
        }
        return result;
    }

    /** Single-character code such as {@code "wP"} / {@code "bK"} for serialization to the UI. */
    public String code() {
        return (color == Color.WHITE ? "w" : "b") + type().letter();
    }
}
