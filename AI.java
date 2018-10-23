package qirkat;

import static qirkat.PieceColor.*;

import java.util.ArrayList;

/**
 * A Player that computes its own moves.
 *
 * @author Sarah Jin
 */
class AI extends Player {

    /**
     * Maximum minimax search depth before going to static evaluation.
     */
    private static final int MAX_DEPTH = 8;
    /**
     * A position magnitude indicating a win (for white if positive, black
     * if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;

    /**
     * A new AI for GAME that will play MYCOLOR.
     */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        if (myColor() == WHITE) {
            board().setisAIWhite(true);
        } else {
            board().setisAIBlack(true);
        }
        Main.startTiming();
        System.out.println(board().toString());
        Move move = findMove();
        Main.endTiming();
        String x = move.toString();

        System.out.printf("%s moves %s.\n", myColor(), x);
        if (x.length() > 5) {
            move = Move.parseMove(x);
        }
        return move;
    }

    /**
     * Return a move for me from the current position, assuming there
     * is a move.
     */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == WHITE) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /**
     * The move found by the last call to one of the ...FindMove methods
     * below.
     */
    private Move _lastFoundMove;

    /** finds the move for AI. Returns FINDMOVE. BOARD, DEPTH,
     *  SAVEMOVE, SENSE, ALPHA, BETA. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        int f = 0;
        int a = board.numPieces(board.whoseMove());
        int b = board.numPieces(board.whoseMove().opposite());
        if (board.gameOver() && (a > b)) {
            return WINNING_VALUE;
        } else if (board.gameOver() && (a < b)) {
            return -WINNING_VALUE;
        } else {
            if (depth == 0) {
                return staticScore(board);
            } else if (sense == 1) {
                f = -INFTY;
                ArrayList<Move> c =
                        board.findlegalmoves(board, board.whoseMove());
                for (Move move : c) {
                    TestBoard d = new TestBoard(board);
                    d.makeMove(move);
                    int e = findMove(d,
                            depth - 1, false, -1, alpha, beta);
                    if (saveMove && e > f) {
                        _lastFoundMove = move;
                    }
                    f = Math.max(f, e);
                    alpha = Math.max(alpha, f);
                    if (beta <= alpha) {
                        break;
                    }
                    return f;
                }
            } else {
                f = INFTY;
                ArrayList<Move> listOfMoves =
                        board.findlegalmoves(board, board.whoseMove());
                for (Move move : listOfMoves) {
                    TestBoard copyBoard = new TestBoard(board);
                    copyBoard.makeMove(move);
                    int e = findMove(copyBoard,
                            depth - 1, false, 1, alpha, beta);
                    if (saveMove && e < f) {
                        _lastFoundMove = move;
                    }
                    f = Math.min(f, e);
                    beta = Math.min(beta, f);
                    if (beta <= alpha) {
                        break;
                    }
                    return f;
                }
            }
            return 0;
        }
    }

    /**
     * begin store moves. return MOVES. BOARD, PLAYER. the other ai has it
     */
    private ArrayList<Move> beginStoreMoves(Board board, PieceColor player) {
        ArrayList<Move> a = new ArrayList<>();
        for (char row = '5'; row >= '1'; row--) {
            for (char col = 'a'; col <= 'e'; col++) {
                int s = Move.index(col, row);
                if (board.get(s) == player) {
                    ArrayList<Move> b = storedMoves(board, row, col);
                    a.addAll(b);
                }
            }
        }
        return a;
    }

    /**
     * Store moves into array. returns MOVES. BOARD, ROW, COL.
     */
    private ArrayList<Move> storedMoves(Board board, char row, char col) {
        ArrayList<Move> a = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                char row2 = (char) (row + j);
                char col2 = (char) (col + i);
                Move w = Move.move(col, row, col2, row2);
                if (board.legalMove(w)) {
                    a.add(w);
                }
            }
        }
        return a;
    }


    /**
     * Return a heuristic value for BOARD.
     */
    private int staticScore(Board board) {
        return 0;
    }

}
