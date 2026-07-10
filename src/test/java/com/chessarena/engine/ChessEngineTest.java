package com.chessarena.engine;

import com.chessarena.engine.core.GameStatus;
import com.chessarena.engine.core.Move;
import com.chessarena.engine.core.MoveResult;
import com.chessarena.engine.game.ChessGame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rule-coverage tests for the pure-Java chess engine. These run with no Spring context,
 * proving the engine is a self-contained, independently testable domain model.
 */
class ChessEngineTest {

    private static MoveResult play(ChessGame game, String uci) {
        return game.submitMove(Move.fromUci(uci));
    }

    @Test
    void initialPositionHasTwentyLegalMoves() {
        ChessGame game = new ChessGame();
        assertEquals(20, game.legalMovesUci().size(),
                "Standard opening position must have exactly 20 legal moves");
    }

    @Test
    void rejectsIllegalMove() {
        ChessGame game = new ChessGame();
        // e2 to e5 is not a legal pawn move.
        MoveResult result = play(game, "e2e5");
        assertFalse(result.legal());
    }

    @Test
    void foolsMateIsCheckmate() {
        ChessGame game = new ChessGame();
        assertTrue(play(game, "f2f3").legal());  // 1. f3
        assertTrue(play(game, "e7e5").legal());  // 1... e5
        assertTrue(play(game, "g2g4").legal());  // 2. g4
        MoveResult mate = play(game, "d8h4");    // 2... Qh4#
        assertTrue(mate.legal());
        assertEquals(GameStatus.CHECKMATE, mate.status());
        assertTrue(game.isOver());
    }

    @Test
    void whiteCanCastleKingside() {
        ChessGame game = new ChessGame();
        play(game, "e2e4"); play(game, "e7e5");
        play(game, "g1f3"); play(game, "b8c6");
        play(game, "f1c4"); play(game, "f8c5");
        MoveResult castle = play(game, "e1g1"); // O-O
        assertTrue(castle.legal(), "White should be able to castle kingside");
        assertEquals("wK", game.boardGrid()[7][6], "King should be on g1");
        assertEquals("wR", game.boardGrid()[7][5], "Rook should have jumped to f1");
    }

    @Test
    void pawnPromotionProducesQueen() {
        ChessGame game = new ChessGame();
        // March the a-pawn up, then promote by capturing the a8 rook diagonally
        // (a pawn cannot promote by pushing straight into an occupied square).
        play(game, "a2a4"); play(game, "g8f6");
        play(game, "a4a5"); play(game, "f6g8");
        play(game, "a5a6"); play(game, "g8f6");
        play(game, "a6b7"); play(game, "f6g8"); // a6 pawn captures the b7 pawn
        MoveResult promo = play(game, "b7a8q"); // b7 captures the a8 rook, promoting
        assertTrue(promo.legal(), "Pawn should promote on the 8th rank");
        assertEquals("wQ", game.boardGrid()[0][0], "a8 should now hold a white queen");
    }
}
