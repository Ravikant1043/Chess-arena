package com.chessarena.engine.core;

import com.chessarena.engine.pieces.Bishop;
import com.chessarena.engine.pieces.King;
import com.chessarena.engine.pieces.Knight;
import com.chessarena.engine.pieces.Pawn;
import com.chessarena.engine.pieces.Piece;
import com.chessarena.engine.pieces.Queen;
import com.chessarena.engine.pieces.Rook;

/**
 * The complete state of a chess position: piece placement plus side-to-move, the
 * en-passant target square, and the half/full-move clocks (everything a FEN string
 * would capture).
 *
 * <p><b>Encapsulation:</b> the backing {@code Piece[file][rank]} grid is private and is
 * never handed out. All reads go through {@link #pieceAt}, and the only mutation entry
 * point is {@link #applyMove}, which enforces the special-move bookkeeping (castling
 * rook relocation, en-passant capture, promotion, clock updates). This guarantees the
 * board can never drift into an inconsistent state from outside the engine.
 */
public class Board {

    private final Piece[][] squares = new Piece[8][8]; // [file][rank]
    private Color sideToMove;
    private Position enPassantTarget; // the square a pawn skipped over last move, or null
    private int halfmoveClock;        // plies since last pawn move or capture (fifty-move rule)
    private int fullmoveNumber;

    private Board() {
        // Use the static factory methods.
    }

    /** Builds a board in the standard chess starting position. */
    public static Board standard() {
        Board b = new Board();
        b.sideToMove = Color.WHITE;
        b.fullmoveNumber = 1;

        // Pawns
        for (int file = 0; file < 8; file++) {
            b.squares[file][1] = new Pawn(Color.WHITE);
            b.squares[file][6] = new Pawn(Color.BLACK);
        }
        // Back ranks
        placeBackRank(b, Color.WHITE, 0);
        placeBackRank(b, Color.BLACK, 7);
        return b;
    }

    private static void placeBackRank(Board b, Color color, int rank) {
        b.squares[0][rank] = new Rook(color);
        b.squares[1][rank] = new Knight(color);
        b.squares[2][rank] = new Bishop(color);
        b.squares[3][rank] = new Queen(color);
        b.squares[4][rank] = new King(color);
        b.squares[5][rank] = new Bishop(color);
        b.squares[6][rank] = new Knight(color);
        b.squares[7][rank] = new Rook(color);
    }

    // ----------------------------------------------------------------- reads

    public Piece pieceAt(Position p) {
        return squares[p.file()][p.rank()];
    }

    public Piece pieceAt(int file, int rank) {
        return squares[file][rank];
    }

    public Color sideToMove() {
        return sideToMove;
    }

    public Position enPassantTarget() {
        return enPassantTarget;
    }

    public int halfmoveClock() {
        return halfmoveClock;
    }

    public int fullmoveNumber() {
        return fullmoveNumber;
    }

    /** Locates the king of the given color. Throws if it is missing (which should never happen in a legal game). */
    public Position findKing(Color color) {
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece p = squares[f][r];
                if (p instanceof King && p.color() == color) {
                    return new Position(f, r);
                }
            }
        }
        throw new IllegalStateException("No " + color + " king on the board");
    }

    /**
     * @return true if any piece of {@code byColor} attacks {@code target}. Central to
     *         check, checkmate, and castling-safety detection.
     */
    public boolean isSquareAttacked(Position target, Color byColor) {
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece p = squares[f][r];
                if (p != null && p.color() == byColor) {
                    if (p.attacks(this, new Position(f, r)).contains(target)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** @return true if the given color's king is currently in check. */
    public boolean isInCheck(Color color) {
        return isSquareAttacked(findKing(color), color.opposite());
    }

    // -------------------------------------------------------------- mutation

    /**
     * Applies a move that has already been vetted as legal, performing all special-move
     * side effects and flipping the side to move. Called both on the real board and on
     * throw-away copies during legality checking.
     */
    public void applyMove(Move move) {
        Piece piece = pieceAt(move.from());
        if (piece == null) {
            throw new IllegalStateException("No piece on " + move.from());
        }

        boolean isPawn = piece.type() == PieceType.PAWN;
        boolean isCapture = pieceAt(move.to()) != null;

        // --- En passant capture: remove the pawn that sits beside the destination ---
        if (isPawn && move.to().equals(enPassantTarget) && pieceAt(move.to()) == null) {
            int capturedRank = move.from().rank(); // the captured pawn is on the mover's rank
            squares[move.to().file()][capturedRank] = null;
            isCapture = true;
        }

        // --- Move the piece ---
        squares[move.from().file()][move.from().rank()] = null;
        squares[move.to().file()][move.to().rank()] = piece;
        piece.markMoved();

        // --- Castling: also relocate the rook ---
        if (piece.type() == PieceType.KING && Math.abs(move.to().file() - move.from().file()) == 2) {
            int rank = move.from().rank();
            if (move.to().file() == 6) { // king-side
                relocateRook(new Position(7, rank), new Position(5, rank));
            } else if (move.to().file() == 2) { // queen-side
                relocateRook(new Position(0, rank), new Position(3, rank));
            }
        }

        // --- Promotion: replace the pawn with the chosen piece ---
        if (isPawn && move.to().rank() == piece.color().promotionRank()) {
            PieceType promo = move.promotion() != null ? move.promotion() : PieceType.QUEEN;
            Piece promoted = createPiece(promo, piece.color());
            promoted.markMoved();
            squares[move.to().file()][move.to().rank()] = promoted;
        }

        // --- Update the en-passant target (only a double pawn push sets it) ---
        if (isPawn && Math.abs(move.to().rank() - move.from().rank()) == 2) {
            int passedRank = (move.from().rank() + move.to().rank()) / 2;
            enPassantTarget = new Position(move.from().file(), passedRank);
        } else {
            enPassantTarget = null;
        }

        // --- Clocks ---
        halfmoveClock = (isPawn || isCapture) ? 0 : halfmoveClock + 1;
        if (sideToMove == Color.BLACK) {
            fullmoveNumber++;
        }
        sideToMove = sideToMove.opposite();
    }

    private void relocateRook(Position from, Position to) {
        Piece rook = pieceAt(from);
        squares[from.file()][from.rank()] = null;
        squares[to.file()][to.rank()] = rook;
        if (rook != null) {
            rook.markMoved();
        }
    }

    private static Piece createPiece(PieceType type, Color color) {
        return switch (type) {
            case QUEEN -> new Queen(color);
            case ROOK -> new Rook(color);
            case BISHOP -> new Bishop(color);
            case KNIGHT -> new Knight(color);
            default -> throw new IllegalArgumentException("Cannot promote to " + type);
        };
    }

    /** Deep copy: used to test a candidate move without disturbing the live board. */
    public Board copy() {
        Board b = new Board();
        b.sideToMove = this.sideToMove;
        b.enPassantTarget = this.enPassantTarget;
        b.halfmoveClock = this.halfmoveClock;
        b.fullmoveNumber = this.fullmoveNumber;
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                b.squares[f][r] = this.squares[f][r] == null ? null : this.squares[f][r].copy();
            }
        }
        return b;
    }

    /**
     * A plain 8-rank × 8-file view of piece codes ({@code "wP"}, {@code "bK"}, or null)
     * for JSON serialization to the browser. Index [0] is rank 8 (top of the board as
     * White sees it) down to index [7] = rank 1.
     */
    public String[][] toGrid() {
        String[][] grid = new String[8][8];
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                Piece p = squares[file][rank];
                grid[7 - rank][file] = (p == null) ? null : p.code();
            }
        }
        return grid;
    }
}
