package com.chessarena.engine.game;

import com.chessarena.engine.core.Board;
import com.chessarena.engine.core.Color;
import com.chessarena.engine.core.GameStatus;
import com.chessarena.engine.core.Move;
import com.chessarena.engine.core.MoveGenerator;
import com.chessarena.engine.core.MoveResult;
import com.chessarena.engine.core.PieceType;
import com.chessarena.engine.core.Position;
import com.chessarena.engine.pieces.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * The public <b>facade</b> over the chess engine — the only class the rest of the
 * application talks to. It hides the interplay of {@link Board}, {@link MoveGenerator},
 * and the pieces behind three simple operations: submit a move, ask for the board
 * state, and ask whether the game is over.
 *
 * <p>This is deliberate application of the Facade pattern and Dependency Inversion: the
 * web/matchmaking layers depend on this small, stable surface rather than on the
 * engine's moving parts.
 */
public class ChessGame {

    private final Board board;
    private final List<String> moveHistory = new ArrayList<>();
    private GameStatus status;

    public ChessGame() {
        this.board = Board.standard();
        this.status = GameStatus.IN_PROGRESS;
    }

    /**
     * Validate and apply a move submitted by the side to move.
     *
     * @return a {@link MoveResult}; if illegal, {@code legal() == false} and the board
     *         is left untouched.
     */
    public synchronized MoveResult submitMove(Move requested) {
        if (status.isGameOver()) {
            return MoveResult.illegal("The game is already over");
        }

        Move matched = matchLegalMove(requested);
        if (matched == null) {
            return MoveResult.illegal("Illegal move: " + requested);
        }

        String description = describeBeforeApply(matched);
        board.applyMove(matched);
        moveHistory.add(matched.toUci());

        status = computeStatus();
        Color winner = (status == GameStatus.CHECKMATE) ? board.sideToMove().opposite() : null;
        description = appendStatusSuffix(description, status);
        return MoveResult.ok(status, board.sideToMove(), winner, description);
    }

    /**
     * Builds a spoken-style description of a legal move ("Rook to a4", "Bishop takes c4",
     * "King castles king-side", "Pawn to e8, promoted to Queen"). Must run <i>before</i>
     * the move is applied, while the moving piece and any capture target are still on
     * their squares.
     */
    private String describeBeforeApply(Move move) {
        Piece piece = board.pieceAt(move.from());
        String pieceName = prettyName(piece.type());

        // Castling: the king moves two files.
        if (piece.type() == PieceType.KING
                && Math.abs(move.to().file() - move.from().file()) == 2) {
            return move.to().file() == 6 ? "King castles king-side" : "King castles queen-side";
        }

        boolean capture = board.pieceAt(move.to()) != null
                || (piece.type() == PieceType.PAWN && move.to().equals(board.enPassantTarget()));
        String verb = capture ? " takes " : " to ";
        String base = pieceName + verb + move.to().toAlgebraic();

        if (move.isPromotion()) {
            base += ", promoted to " + prettyName(move.promotion());
        }
        return base;
    }

    private static String appendStatusSuffix(String description, GameStatus status) {
        return switch (status) {
            case CHECK -> description + " — check!";
            case CHECKMATE -> description + " — checkmate!";
            case STALEMATE -> description + " — stalemate";
            case DRAW -> description + " — draw";
            default -> description;
        };
    }

    private static String prettyName(PieceType type) {
        String n = type.name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }

    /**
     * Find the legal move that matches the client's request. Promotion is normalised:
     * if the client omits the promotion piece on a promoting move, it defaults to queen.
     */
    private Move matchLegalMove(Move requested) {
        List<Move> legal = MoveGenerator.legalMoves(board);
        for (Move legalMove : legal) {
            if (!legalMove.from().equals(requested.from()) || !legalMove.to().equals(requested.to())) {
                continue;
            }
            if (legalMove.promotion() == requested.promotion()) {
                return legalMove;
            }
            // Client omitted a promotion choice on a promoting move -> default to queen.
            if (requested.promotion() == null && legalMove.promotion() == PieceType.QUEEN) {
                return legalMove;
            }
        }
        return null;
    }

    private GameStatus computeStatus() {
        Color toMove = board.sideToMove();
        boolean hasMoves = !MoveGenerator.legalMoves(board).isEmpty();
        boolean inCheck = board.isInCheck(toMove);

        if (!hasMoves) {
            return inCheck ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        }
        if (isInsufficientMaterial()) {
            return GameStatus.DRAW;
        }
        if (board.halfmoveClock() >= 100) { // fifty-move rule = 100 plies
            return GameStatus.DRAW;
        }
        return inCheck ? GameStatus.CHECK : GameStatus.IN_PROGRESS;
    }

    /** Detects the simplest dead positions: K vs K, K+minor vs K. */
    private boolean isInsufficientMaterial() {
        int minorPieces = 0;
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece p = board.pieceAt(f, r);
                if (p == null) {
                    continue;
                }
                switch (p.type()) {
                    case PAWN, ROOK, QUEEN -> {
                        return false; // enough material to mate
                    }
                    case BISHOP, KNIGHT -> minorPieces++;
                    default -> { /* king */ }
                }
            }
        }
        return minorPieces <= 1;
    }

    /** The legal moves available right now, as UCI strings — handy for the UI to highlight. */
    public List<String> legalMovesUci() {
        List<String> out = new ArrayList<>();
        for (Move m : MoveGenerator.legalMoves(board)) {
            out.add(m.toUci());
        }
        return out;
    }

    /** Legal destination squares for a piece on {@code from} (for click-to-move highlighting). */
    public List<String> legalTargets(Position from) {
        List<String> out = new ArrayList<>();
        for (Move m : MoveGenerator.legalMoves(board)) {
            if (m.from().equals(from)) {
                out.add(m.to().toAlgebraic());
            }
        }
        return out;
    }

    public GameStatus status() {
        return status;
    }

    public Color sideToMove() {
        return board.sideToMove();
    }

    public String[][] boardGrid() {
        return board.toGrid();
    }

    public List<String> moveHistory() {
        return List.copyOf(moveHistory);
    }

    public boolean isOver() {
        return status.isGameOver();
    }
}
