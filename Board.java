package qirkat;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Observable;
import java.util.Observer;
import java.util.Stack;
import static qirkat.PieceColor.*;
import static qirkat.Move.*;

/**
 * A Qirkat board.   The squares are labeled by column (a char value between
 * 'a' and 'e') and row (a char value between '1' and '5'.
 * <p>
 * For some purposes, it is useful to refer to squares using a single
 * integer, which we call its "linearized index".  This is simply the
 * number of the square in row-major order (with row 0 being the bottom row)
 * counting from 0).
 * <p>
 * Moves on this board are denoted by Moves.
 *
 * @author Sarah Jin
 */
class Board extends Observable {
    /**
     * Current board.
     */
    private PieceColor[] _board;

    /** boolean variables. */
    private static boolean isAIWHITE, isAIBLACK;

    /**
     * A new, cleared board at the start of the game.
     */
    Board() {
        _board = new PieceColor[linearSize];
        clear();
    }


    /**
     * A copy of B.
     */
    Board(Board b) {
        internalCopy(b);
    }

    /**
     * Return a constant view of me (allows any access method, but no
     * method that modifies it).
     */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /**
     * Clear me to my starting state, with pieces in their initial
     * positions.
     */
    void clear() {
        isAIWHITE = false;
        isAIBLACK = false;
        _prevBoards = new Stack<Board>();
        stackOfPrevWhite = new Stack<String>();
        stackOfPrevBlack = new Stack<String>();
        _whoseMove = WHITE;
        if (Main.getuseGUI() && Main.getINIT()) {
            Main.getDisplay().setLabel("reporter",
                    whoseMove().toString() + " to move");
        }
        _gameOver = false;

        for (char x = 'a'; x <= 'e'; x++) {
            for (char y = '1'; y <= '2'; y++) {
                set(x, y, WHITE);
            }
        }
        for (char x = 'a'; x <= 'e'; x++) {
            for (char y = '4'; y <= '5'; y++) {
                set(x, y, BLACK);
            }
        }
        for (char x = 'a'; x <= 'b'; x++) {
            char y = '3';
            set(x, y, BLACK);
        }
        for (char x = 'd'; x <= 'e'; x++) {
            char y = '3';
            set(x, y, WHITE);
        }
        set('c', '3', EMPTY);
        setChanged();
        notifyObservers();
    }

    /**
     * Copy B into me.
     */
    void copy(Board b) {
        internalCopy(b);
    }

    /**
     * Copy B into me.
     */
    private void internalCopy(Board b) {
        _board = b._board.clone();
        _whoseMove = b._whoseMove;
        _prevBoards = b._prevBoards;
        _gameOver = b._gameOver;
        stackOfPrevBlack = b.stackOfPrevBlack;
        stackOfPrevWhite = b.stackOfPrevWhite;
    }

    /**
     * Set my contents as defined by STR.  STR consists of 25 characters,
     * each of which is b, w, or -, optionally interspersed with whitespace.
     * These give the contents of the Board in row-major order, starting
     * with the bottom row (row 1) and left column (column a). All squares
     * are initialized to allow horizontal movement in either direction.
     * NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }

        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k, EMPTY);
                break;
            case 'b':
            case 'B':
                set(k, BLACK);
                break;
            case 'w':
            case 'W':
                set(k, WHITE);
                break;
            default:
                break;
            }
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Return true iff the game is over: i.e., if the current player has
     * no moves. X.
     */
    void setgameover(boolean x) {
        _gameOver = x;
    }

    /** boolean that returns true if game is over. */
    boolean gameOver() {
        ArrayList<PieceColor> checker = new ArrayList<>();
        for (int x = 0; x < SIDE * SIDE; x++) {
            PieceColor z = get(x);
            checker.add(z);
        }
        if (!checker.contains(WHITE)) {
            _gameOver = true;
            return _gameOver;
        }
        if (!checker.contains(BLACK)) {
            _gameOver = true;
            return _gameOver;
        }
        if (findlegalmoves(this, whoseMove()).isEmpty()) {
            _gameOver = true;
            return _gameOver;
        }
        return _gameOver;
    }


    /**
     * Return the current contents of square C R, where 'a' <= C <= 'e',
     * and '1' <= R <= '5'.
     */
    PieceColor get(char c, char r) {
        return get(index(c, r));
    }

    /**
     * Return the current contents of the square at linearized index K.
     */
    PieceColor get(int k) {
        try {
            return _board[k];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Set get(C, R) to V, where 'a' <= C <= 'e', and
     * '1' <= R <= '5'.
     */
    public void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /**
     * Set get(K) to V, where K is the linearized index of a square.
     */
    public void set(int k, PieceColor v) {
        try {
            _board[k] = v;
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }
    }

    /**
     * Return true iff MOV is legal on the current board.
     */
    boolean legalMove(Move mov) {
        PieceColor a = get(mov.fromIndex());
        if (a == EMPTY) {
            return false;
        }
        if (a != _whoseMove) {
            return false;
        }
        if (!validMove(mov)) {
            return false;
        }
        if (!canDiagnoalMove(mov)) {
            return false;
        }
        if (!mov.isJump()) {
            if (canJump(_whoseMove)) {
                return false;
            }
            if (isOnOpponentsBase(mov)) {
                return false;
            }
            if (mov.row0() == mov.row1()) {
                if (_whoseMove == WHITE && !stackOfPrevWhite.empty()) {
                    String b = (String) stackOfPrevWhite.peek();
                    String c = mov.col1() + "" + mov.row1()
                            + "" + mov.col0() + "" + mov.row0();
                    if (b.equals(c)) {
                        return false;
                    }
                }
                if (_whoseMove == BLACK && !stackOfPrevBlack.empty()) {
                    String b = (String) stackOfPrevBlack.peek();
                    String c = mov.col1() + "" + mov.row1()
                            + "" + mov.col0() + "" + mov.row0();
                    if (b.equals(c)) {
                        return false;
                    }
                }

                if (_whoseMove == BLACK) {
                    return blackBack(mov);
                }
                return whiteBack(mov);
            }
            if (mov.isJump()) {
                if (mov.getNextJump() == null) {
                    return jumpValid(mov);
                } else if (mov.getNextJump() != null) {
                    if (!jumpValid(mov)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** returns true if the move can be diagonal. M. */
    boolean canDiagnoalMove(Move m) {
        int a = m.fromIndex();
        String a1 = Integer.toString(a);
        int b = m.toIndex();
        String b1 = Integer.toString(b);
        if (a1.equals("1")) {
            if (b1.equals("5") || b1.equals("7") || b1.equals("13")) {
                return false;
            }
        } else if (a1.equals("3")) {
            if (b1.equals("7") || b1.equals("9") || b1.equals("11")) {
                return false;
            }
        } else if (a1.equals("5")) {
            if (b1.equals("1") || b1.equals("11") || b1.equals("17")) {
                return false;
            }
        } else if (a1.equals("7")) {
            if (b1.equals("1") || b1.equals("3") || b1.equals("11")
                    || b1.equals("13") || b1.equals("15") || b1.equals("19")) {
                return false;
            }
        } else if (a1.equals("9")) {
            if (b1.equals("3") || b1.equals("13") || b1.equals("17")) {
                return false;
            }
        } else if (a1.equals("11")) {
            if (b1.equals("5") || b1.equals("7") || b1.equals("15")
                    || b1.equals("17") || b1.equals("23") || b1.equals("3")) {
                return false;
            }
        } else if (a1.equals("13")) {
            if (b1.equals("7") || b1.equals("9") || b1.equals("17")
                    || b1.equals("19") ||  b1.equals("21") || b1.equals("1")) {
                return false;
            }
        } else if (a1.equals("15")) {
            if (b1.equals("11") || b1.equals("21") || b1.equals("7")) {
                return false;
            }
        } else if (a1.equals("17")) {
            if (b1.equals("11") || b1.equals("13") || b1.equals("21")
                    || b1.equals("23") || b1.equals("5") || b1.equals("9")) {
                return false;
            }
        } else if (a1.equals("19")) {
            if (b1.equals("13") || b1.equals("23") || b1.equals("7")) {
                return false;
            }
        } else if (a1.equals("21")) {
            if (b1.equals("15") || b1.equals("17") || b1.equals("13")) {
                return false;
            }
        } else if (a1.equals("23")) {
            if (b1.equals("17") || b1.equals("19") || b1.equals("11")) {
                return false;
            }
        }
        return true;
    }
    /** returns whether or not it's valid move. return M. */
    boolean validMove(Move m) {
        if (get(m.toIndex()) == EMPTY) {
            return true;
        }
        return false;
    }

    /** returns whether or not it's on oppoennt's base. return M. */
    boolean isOnOpponentsBase(Move m) {
        if (_whoseMove == BLACK && m.row0() == '1') {
            return true;
        } else if (_whoseMove == WHITE && m.row0() == '5') {
            return true;
        }
        return false;
    }
    /** returns true if piece moved back. return M. */
    boolean blackBack(Move m) {
        if (m.row0() == '1') {
            if (m.row1() == '2') {
                return false;
            }
        }
        if (m.row0() == '2') {
            if (m.row1() == '3') {
                return false;
            }
        }
        if (m.row0() == '3') {
            if (m.row1() == '4') {
                return false;
            }
        }
        if (m.row0() == '4') {
            if (m.row1() == '5') {
                return false;
            }
        }
        return true;
    }

    /** returns true if white moved back. return M. */
    boolean whiteBack(Move m) {
        if (m.row0() == '5') {
            if (m.row1() == '4') {
                return false;
            }
        }
        if (m.row0() == '4') {
            if (m.row1() == '3') {
                return false;
            }
        }
        if (m.row0() == '3') {
            if (m.row1() == '2') {
                return false;
            }
        }
        if (m.row0() == '2') {
            if (m.row1() == '1') {
                return false;
            }
        }
        return true;
    }

    /** get east. return I. */
    PieceColor getEast(int i) {
        String index = Integer.toString(i);
        if (index.equals("4") || index.equals("9")
                || index.equals("14") || index.equals("19")) {
            return null;
        }
        return get(i + 1);
    }
    /** get west. return I.  GETWEST. */
    PieceColor getWest(int i) {
        String index = Integer.toString(i);
        if (index.equals("0") || index.equals("5") || index.equals("10")
                || index.equals("15") || index.equals("20")) {
            return null;
        }
        return get(i - 1);
    }

    /**
     * Return a list of all legal moves from the current position.
     */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getMoves(result);
        return result;
    }

    /**
     * Add all legal moves from the current position to MOVES.
     */
    void getMoves(ArrayList<Move> moves) {
        if (gameOver()) {
            return;
        }
        if (jumpPossible()) {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getJumps(moves, k);
            }
        } else {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getMoves(moves, k);
            }
        }
    }
    /** set whosemove. W. */
    public void setWhoseMove(PieceColor w) {
        this._whoseMove = w;
    }

    /**
     * Add all legal non-capturing moves from the position
     * with linearized index K to MOVES.
     */
    private void getMoves(ArrayList<Move> moves, int k) {
    }

    /**
     * Add all legal captures from the position with linearized index K
     * to MOVES.
     */
    private void getJumps(ArrayList<Move> moves, int k) {
    }

    /**
     * Return true iff MOV is a valid jump sequence on the current board.
     * MOV must be a jump or null.  If ALLOWPARTIAL, allow jumps that
     * could be continued and are valid as far as they go.
     */
    boolean checkJump(Move mov, boolean allowPartial) {
        if (mov == null) {
            return true;
        }
        return false;
    }
    /** movevalid. return MOV. */
    boolean moveValid(Move mov) {
        char c0 = mov.col0();
        char c1 = mov.col1();
        char r0 = mov.row0();
        char r1 = mov.row1();
        PieceColor a = get(mov.fromIndex());
        if (a != _whoseMove) {
            return false;
        }
        if (!validMove(mov)) {
            return false;
        }
        if (!canDiagnoalMove(mov)) {
            return false;
        }
        if (Math.abs(c0 - c1) >= 2
                || Math.abs(r0 - r1) >= 2) {
            return false;
        }
        if (mov.row0() == mov.row1()) {
            if (_whoseMove == WHITE && !stackOfPrevWhite.empty()) {
                String b = (String) stackOfPrevWhite.peek();
                String c = mov.col1() + "" + mov.row1() + ""
                        + mov.col0() + "" + mov.row0();
                if (b.equals(c)) {
                    return false;
                }
            }
            if (_whoseMove == BLACK && !stackOfPrevBlack.empty()) {
                String b = (String) stackOfPrevBlack.peek();
                String c = mov.col1() + "" + mov.row1() + ""
                        + mov.col0() + "" + mov.row0();
                if (b.equals(c)) {
                    return false;
                }
            }
        }
        if (_whoseMove == BLACK) {
            return blackBack(mov);
        } else if (_whoseMove == WHITE) {
            return whiteBack(mov);
        }
        return true;
    }
    /** jumpvalid. MOV. return VALID.  */
    boolean jumpValid(Move mov) {
        char c0 = mov.col0();
        char c1 = mov.col1();
        char r0 = mov.row0();
        char r1 = mov.row1();
        if (get(c0, r0) == EMPTY) {
            return false;
        } else if (get(c1, r1) != EMPTY) {
            return false;
        } else if (get(mov.jumpedCol(), mov.jumpedRow()) == _whoseMove
                || get(mov.jumpedCol(), mov.jumpedRow()) == EMPTY) {
            return false;
        } else if (!jumpSpaceValid(mov)) {
            return false;
        }
        return true;
    }
    /**
     * returns jumpSpaceValid. return M.
     */
    boolean jumpSpaceValid(Move m) {
        String x = Integer.toString(m.fromIndex());
        String y = Integer.toString(m.toIndex());
        if (x.equals("0")) {
            if (y.equals("10") || y.equals("2") || y.equals("12")) {
                return true;
            }
        } else if (x.equals("1")) {
            if (y.equals("11") || y.equals("3")) {
                return true;
            }
        } else if (x.equals("2")) {
            if (y.equals("12") || y.equals("4") || y.equals("0")
                    || y.equals("10") || y.equals("14")) {
                return true;
            }
        } else if (x.equals("3")) {
            if (y.equals("13") || y.equals("1")) {
                return true;
            }
        } else if (x.equals("4")) {
            if (y.equals("2") || y.equals("14") || y.equals("12")) {
                return true;
            }
        } else if (x.equals("5")) {
            if (y.equals("15") || y.equals("7")) {
                return true;
            }
        } else if (x.equals("6")) {
            if (y.equals("16") || y.equals("8") || y.equals("18")) {
                return true;
            }
        } else if (x.equals("7")) {
            if (y.equals("17") || y.equals("5") || y.equals("9")) {
                return true;
            }
        } else if (x.equals("8")) {
            if (y.equals("18") || y.equals("6") || y.equals("16")) {
                return true;
            }
        }
        return jumpSpaceValid2(m) || jumpSpaceValid3(m);
    }
    /** return MOVE M.*/
    boolean jumpSpaceValid2(Move m) {
        String x = Integer.toString(m.fromIndex());
        String y = Integer.toString(m.toIndex());
        if (x.equals("9")) {
            if (y.equals("7") || y.equals("19")) {
                return true;
            }
        } else if (x.equals("10")) {
            if (y.equals("0") || y.equals("20") || y.equals("12")
                    || y.equals("2") || y.equals("22")) {
                return true;
            }
        } else if (x.equals("11")) {
            if (y.equals("21") || y.equals("1") || y.equals("13")) {
                return true;
            }
        } else if (x.equals("12")) {
            if (y.equals("2") || y.equals("22") || y.equals("10")
                    || y.equals("14") || y.equals("0")
                    || y.equals("4") || y.equals("20") || y.equals("24")) {
                return true;
            }
        } else if (x.equals("13")) {
            if (y.equals("3") || y.equals("23") || y.equals("11")) {
                return true;
            }
        } else if (x.equals("14")) {
            if (y.equals("4") || y.equals("24") || y.equals("12")
                    || y.equals("2") || y.equals("22")) {
                return true;
            }
        } else if (x.equals("15")) {
            if (y.equals("5") || y.equals("17")) {
                return true;
            }
        }
        return false;
    }
    /** return MOVE M. */
    boolean jumpSpaceValid3(Move m) {
        String x = Integer.toString(m.fromIndex());
        String y = Integer.toString(m.toIndex());
        if (x.equals("16")) {
            if (y.equals("6") || y.equals("8") || y.equals("18")) {
                return true;
            }
        } else if (x.equals("17")) {
            if (y.equals("7") || y.equals("15") || y.equals("19")) {
                return true;
            }
        } else if (x.equals("18")) {
            if (y.equals("16") || y.equals("6") || y.equals("8")) {
                return true;
            }
        } else if (x.equals("19")) {
            if (y.equals("17") || y.equals("9")) {
                return true;
            }
        } else if (x.equals("20")) {
            if (y.equals("10") || y.equals("22") || y.equals("12")) {
                return true;
            }
        } else if (x.equals("21")) {
            if (y.equals("11") || y.equals("23")) {
                return true;
            }
        } else if (x.equals("22")) {
            if (y.equals("20") || y.equals("24") || y.equals("12")
                    || y.equals("10") || y.equals("14")) {
                return true;
            }
        } else if (x.equals("23")) {
            if (y.equals("21") || y.equals("13")) {
                return true;
            }
        } else if (x.equals("24")) {
            if (y.equals("22") || y.equals("14") || y.equals("12")) {
                return true;
            }
        }
        return false;
    }



    /**
     * Return true iff a jump is possible for a piece at position C R.
     */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /**
     * Return true iff a jump is possible for a piece at position with
     * linearized index K.
     */
    boolean jumpPossible(int k) {
        return false;
    }

    /**
     * Return true iff a jump is possible from the current board.
     */
    boolean jumpPossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the color of the player who has the next move.  The
     * value is arbitrary if gameOver().
     */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /**
     * Perform the move C0R0-C1R1. Assumes that legalMove(C0, R0, C1, R1).
     */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(Move.move(c0, r0, c1, r1, null));
    }

    /**
     * Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     * Assumes the result is legal.
     */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(Move.move(c0, r0, c1, r1, next));
    }

    /**
     * Make the Move MOV on this Board, assuming it is legal.
     */
    void makeMove(Move mov) {
        if (!legalMove(mov) && (!isAIWHITE || !isAIBLACK)) {
            System.out.println("Invalid Move.");
            return;
        } else if (!legalMove(mov) && (isAIWHITE || isAIBLACK)) {
            return;
        }
        boolean didTurn = false;
        if (mov.getNextJump() == null) {
            _prevBoards.push(new Board(this));
        }
        PieceColor opponent = _whoseMove.opposite();
        if (mov.isJump()) {
            if (_whoseMove == WHITE && !stackOfPrevWhite.empty()) {
                stackOfPrevWhite.pop();
            } else if (_whoseMove == BLACK && !stackOfPrevBlack.empty()) {
                stackOfPrevBlack.pop();
            }
            set(mov.toIndex(), _whoseMove);
            set(mov.fromIndex(), EMPTY);
            set(mov.jumpedCol(), mov.jumpedRow(), EMPTY);
            if (mov.getNextJump() != null) {
                makeMove(mov.getNextJump());
            }
            didTurn = true;
        }
        if (didTurn) {
            _whoseMove = opponent;
        } else if (!didTurn) {
            if (mov.row0() == mov.row1()) {
                String a = mov.col0() + "" + mov.row0() + ""
                        + mov.col1() + "" + mov.row1();
                if (_whoseMove == WHITE) {
                    stackOfPrevWhite.push(a);
                } else if (_whoseMove == BLACK) {
                    stackOfPrevBlack.push(a);
                }
            }
            set(mov.toIndex(), _whoseMove);
            set(mov.fromIndex(), EMPTY);
            _whoseMove = opponent;
        }
        if (Main.getuseGUI()) {
            Main.getDisplay().setLabel("reporter",
                    whoseMove().toString() + " to move");
        }
        setChanged();
        notifyObservers();
    }
    /** north. return INDEX. */
    int north(int index) {
        return index + 5;
    }
    /** south. return INDEX. */
    int south(int index) {
        return index - 5;
    }
    /** east. return INDEX. */
    int east(int index) {
        return index + 1;
    }
    /** west. return INDEX. */
    int west(int index) {
        return index - 1;
    }
    /** northwest. return INDEX. */
    int northwest(int index) {
        return index - 1 + 5;
    }
    /** northeast. return INDEX. */
    int northeast(int index) {
        return index + 1 + 5;
    }
    /** southwest. return INDEX. */
    int southwest(int index) {
        return index - 1 - 5;
    }
    /** southeast. return INDEX. */
    int southeast(int index) {
        return index + 1 - 5;
    }
    /** twoNorth. return INDEX. */
    int twoNorth(int index) {
        return index + 10;
    }
    /** twoSouth. return INDEX. */
    int twoSouth(int index) {
        return index - 10;
    }
    /** twowest. return INDEX. */
    int twoWest(int index) {
        return index - 2;
    }
    /** twoeast. return INDEX. */
    int twoEast(int index) {
        return index + 2;
    }
    /** twoNorthwest. return INDEX. */
    int twoNorthWest(int index) {
        return index - 2 + 10;
    }
    /** twoNorthEast. return INDEX. */
    int twoNorthEast(int index) {
        return index + 2 + 10;
    }
    /**twoSouthWest. return INDEX. */
    int twoSouthWest(int index) {
        return index - 2 - 10;
    }
    /** twoSouthEast. return INDEX. */
    int twoSouthEast(int index) {
        return index + 2 - 10;
    }
    /** canJump. return JUMP. WHO. */
    boolean canJump(PieceColor who) {
        for (int i = 0; i < SIDE * SIDE; i++) {
            if (get(i) == who) {
                if (twoNorth(i) >= 0 && twoNorth(i) < SIDE * SIDE) {
                    if (get(twoNorth(i)) == EMPTY) {
                        if (jumpValid(Move.move(i, twoNorth(i)))) {
                            return true;
                        }
                    }
                }
                if (twoSouth(i) >= 0 && twoSouth(i) < SIDE * SIDE) {
                    if (get(twoSouth(i)) == EMPTY) {
                        if (jumpValid(Move.move(i, twoSouth(i)))) {
                            return true;
                        }
                    }
                }
                if (twoEast(i) >= 0 && twoEast(i) < SIDE * SIDE) {
                    if (get(twoEast(i)) == EMPTY) {
                        if (jumpValid(Move.move(i, twoEast(i)))) {
                            return true;
                        }
                    }
                }
                if (twoWest(i) >= 0 && twoWest(i) < SIDE * SIDE) {
                    PieceColor d1 = get(twoWest(i));
                    if (d1 == EMPTY) {
                        if (jumpValid(Move.move(i, twoWest(i)))) {
                            return true;
                        }
                    }
                }
                if (twoNorthEast(i) >= 0 && twoNorthEast(i) < SIDE * SIDE) {
                    PieceColor e1 = get(twoNorthEast(i));
                    if (jumpValid(Move.move(i, twoNorthEast(i)))) {
                        return true;
                    }
                }
                if (twoNorthWest(i) >= 0 && twoNorthWest(i) < SIDE * SIDE) {
                    PieceColor f1 = get(twoNorthWest(i));
                    if (jumpValid(Move.move(i, twoNorthWest(i)))) {
                        return true;
                    }
                }
                if (twoSouthEast(i) >= 0 && twoSouthEast(i) < SIDE * SIDE) {
                    PieceColor g1 = get(twoSouthEast(i));
                    if (jumpValid(Move.move(i, twoSouthEast(i)))) {
                        return true;
                    }
                }
                if (twoSouthWest(i) >= 0 && twoSouthWest(i) < SIDE * SIDE) {
                    PieceColor h1 = get(twoSouthWest(i));
                    if (jumpValid(Move.move(i, twoSouthWest(i)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Undo the last move, if any. return WHO.
     */
    void undo() {
        if (_prevBoards.empty()) {
            return;
        }
        Board a = _prevBoards.pop();
        for (int i = 0; i < SIDE * SIDE; i++) {
            this.set(i, a.get(i));
        }
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    /**
     * Return an ArrayList FINDLEGALMOVES with BOARD and WHO.
     */
    /**
     * Return an ArrayList FINDLEGALMOVES with BOARD and WHO.
     */
    public ArrayList<Move> findlegalmoves(Board board, PieceColor who) {
        ArrayList<Move> legalmoves = new ArrayList<Move>();
        for (int i = 0; i < SIDE * SIDE; i++) {
            if (get(i) == _whoseMove) {
                int north = north(i);
                int south = south(i);
                int west = west(i);
                int east = east(i);
                if (validIndex(north)) {
                    Move x = Move.move(i, north);
                    if (legalMove(x) && moveValid(x)) {
                        legalmoves.add(x);
                    }
                }
                if (validIndex(south)) {
                    Move x = Move.move(i, south);
                    if (legalMove(x) && moveValid(x)) {
                        legalmoves.add(x);
                    }
                }
                if (validIndex(east)) {
                    Move x = Move.move(i, east);
                    if (legalMove(x) && moveValid(x)) {
                        legalmoves.add(x);
                    }
                }
                if (validIndex(west)) {
                    Move x = Move.move(i, west);
                    if (legalMove(x) && moveValid(x)) {
                        legalmoves.add(x);
                    }
                }
                for (Move s : checkotherindexes()) {
                    legalmoves.add(s);
                }
                for (Move p : checkotherindexes2()) {
                    legalmoves.add(p);
                }
                for (Move r : checkotherindexes22()) {
                    legalmoves.add(r);
                }
                for (Move r : checkotherindexes3()) {
                    legalmoves.add(r);
                }
                for (Move r : checkotherindexes4()) {
                    legalmoves.add(r);
                }
            }
        }
        return legalmoves;
    }

    /**
     * checker. returns MOVE.
     */
    public ArrayList<Move> checkotherindexes2() {
        ArrayList<Move> legalmoves = new ArrayList<Move>();
        for (int i = 0; i < SIDE * SIDE; i++) {
            if (get(i) == _whoseMove) {
                int twoNorth = twoNorth(i);
                int twoSouth = twoSouth(i);

                if (validIndex(twoNorth)) {
                    Move x = Move.move(i, twoNorth);
                    if (legalMove(x) && jumpValid(x)) {
                        TestBoard hello = new TestBoard(this);
                        if (!isAIBLACK) {
                            hello.makeMove(x);
                            Move y = hello.returnNextJump(x);
                            Move superMove =
                                    Move.move(x.col0(), x.row0(),
                                            x.col1(), x.row1(), y);
                            legalmoves.add(superMove);
                        } else {
                            legalmoves.add(x);
                        }
                    }
                }
                if (validIndex(twoSouth)) {
                    Move x = Move.move(i, twoSouth);
                    if (legalMove(x) && jumpValid(x)) {
                        TestBoard hello = new TestBoard(this);
                        if (!isAIBLACK) {
                            hello.makeMove(x);
                            Move y = hello.returnNextJump(x);
                            Move superMove =
                                    Move.move(x.col0(), x.row0(),
                                            x.col1(), x.row1(), y);
                            legalmoves.add(superMove);
                        } else {
                            legalmoves.add(x);
                        }
                    }
                }
            }
        }
        return legalmoves;
    }

    /**
     * checker.returns MOVE.
     */
    public ArrayList<Move> checkotherindexes22() {
        ArrayList<Move> legalmoves = new ArrayList<Move>();
        for (int i = 0; i < SIDE * SIDE; i++) {
            if (get(i) == _whoseMove) {
                int twoEast = twoEast(i);
                int twoWest = twoWest(i);
                if (validIndex(twoEast)) {
                    Move x = Move.move(i, twoEast);
                    if (legalMove(x) && jumpValid(x)) {
                        TestBoard hello = new TestBoard(this);
                        if (!isAIBLACK) {
                            hello.makeMove(x);
                            Move y = hello.returnNextJump(x);
                            Move superMove =
                                    Move.move(x.col0(), x.row0(),
                                            x.col1(), x.row1(), y);
                            legalmoves.add(superMove);
                        } else {
                            legalmoves.add(x);
                        }
                    }
                }

                if (validIndex(twoWest)) {
                    Move x = Move.move(i, twoWest);
                    if (legalMove(x) && jumpValid(x)) {
                        TestBoard hello = new TestBoard(this);
                        if (!isAIBLACK) {
                            hello.makeMove(x);
                            Move y = hello.returnNextJump(x);
                            Move superMove =
                                    Move.move(x.col0(), x.row0(),
                                            x.col1(), x.row1(), y);
                            legalmoves.add(superMove);
                        } else {
                            legalmoves.add(x);
                        }
                    }
                }
            }
        }
        return legalmoves;
    }

    /**
     * checker. returns MOVE.
     */
    public ArrayList<Move> checkotherindexes3() {
        ArrayList<Move> legalmoves = new ArrayList<Move>();
        for (int i = 0; i < SIDE * SIDE; i++) {
            if (get(i) == _whoseMove) {
                int twonortheast = twoNorthEast(i);
                int twonorthwest = twoNorthWest(i);
                if (validIndex(twonorthwest)) {
                    Move x = Move.move(i, twonorthwest);
                    if (legalMove(x) && jumpValid(x)) {
                        TestBoard hello = new TestBoard(this);
                        if (!isAIBLACK) {
                            hello.makeMove(x);
                            Move y = hello.returnNextJump(x);
                            Move superMove =
                                    Move.move(x.col0(), x.row0(),
                                            x.col1(), x.row1(), y);
                            legalmoves.add(superMove);
                        } else {
                            legalmoves.add(x);
                        }
                    }
                }
                if (validIndex(twonortheast)) {
                    Move x = Move.move(i, twonortheast);
                    if (legalMove(x) && jumpValid(x)) {
                        TestBoard hello = new TestBoard(this);
                        if (!isAIBLACK) {
                            hello.makeMove(x);
                            Move y = hello.returnNextJump(x);
                            Move superMove =
                                    Move.move(x.col0(), x.row0(),
                                            x.col1(), x.row1(), y);
                            legalmoves.add(superMove);
                        } else {
                            legalmoves.add(x);
                        }
                    }
                }

            }
        }
        return legalmoves;
    }

    /**
     * checker. return MOVE.
     */
    public ArrayList<Move> checkotherindexes4() {
        ArrayList<Move> legalmoves = new ArrayList<Move>();
        for (int i = 0; i < SIDE * SIDE; i++) {
            if (get(i) == _whoseMove) {
                int twosoutheast = twoSouthEast(i);
                int twosouthwest = twoSouthWest(i);
                if (validIndex(twosoutheast)) {
                    Move x = Move.move(i, twosoutheast);
                    if (legalMove(x) && jumpValid(x)) {
                        TestBoard hello = new TestBoard(this);
                        if (!isAIBLACK) {
                            hello.makeMove(x);
                            Move y = hello.returnNextJump(x);
                            Move superMove =
                                    Move.move(x.col0(), x.row0(),
                                            x.col1(), x.row1(), y);
                            legalmoves.add(superMove);
                        } else {
                            legalmoves.add(x);
                        }
                    }
                }
                if (validIndex(twosouthwest)) {
                    Move x = Move.move(i, twosouthwest);
                    if (legalMove(x) && jumpValid(x)) {
                        TestBoard hello = new TestBoard(this);
                        if (!isAIBLACK) {
                            hello.makeMove(x);
                            Move y = hello.returnNextJump(x);
                            Move superMove =
                                    Move.move(x.col0(), x.row0(),
                                            x.col1(), x.row1(), y);
                            legalmoves.add(superMove);
                        } else {
                            legalmoves.add(x);
                        }
                    }
                }
            }
        }
        return legalmoves;
    }

    /**
     * checker. return MOVE.
     */
    public ArrayList<Move> checkotherindexes() {
        ArrayList<Move> legalmoves = new ArrayList<Move>();
        for (int i = 0; i < SIDE * SIDE; i++) {
            if (get(i) == _whoseMove) {
                int northwest = northwest(i);
                int northeast = northeast(i);
                int southeast = southeast(i);
                int southwest = southwest(i);
                if (validIndex(northeast)) {
                    Move x = Move.move(i, northeast);
                    if (legalMove(x) && moveValid(x)) {
                        legalmoves.add(x);
                    }
                }
                if (validIndex(northwest)) {
                    Move x = Move.move(i, northwest);
                    if (legalMove(x) && moveValid(x)) {
                        legalmoves.add(x);
                    }
                }
                if (validIndex(southeast)) {
                    Move x = Move.move(i, southeast);
                    if (legalMove(x) && moveValid(x)) {
                        legalmoves.add(x);
                    }
                }
                if (validIndex(southwest)) {
                    Move x = Move.move(i, southwest);
                    if (legalMove(x) && moveValid(x)) {
                        legalmoves.add(x);
                    }
                }
            }
        }
        return legalmoves;
    }
    /** returns VALIDINDEX. INDEX. */
    boolean validIndex(int index) {
        return index >= 0 && index < SIDE * SIDE;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Return a text depiction of the board.  If LEGEND, supply row and
     * column numbers around the edges.
     */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        if (legend) {
            out.format("    " + "a b c d e");
        }
        boolean firstLine = true;
        for (char i = '5'; i >= '1'; i--) {
            if (i != '5') {
                out.format("\n" + " ");
            }
            if (i == '5') {
                out.format(" ");
            }
            if (legend) {
                out.format(Character.toString(i) + " ");
            } else {
                for (char m = 'a'; m <= 'e'; m++) {
                    if ((_board[index(m, i)]) == BLACK) {
                        out.format(" b");
                    } else if (_board[index(m, i)] == WHITE) {
                        out.format(" w");
                    } else {
                        out.format(" -");
                    }
                }
            }
        }
        return out.toString();
    }
    /** numPieces. COLOR. return NUMPIECES. */
    int numPieces(PieceColor color) {
        int counter = 0;
        for (PieceColor piece : _board) {
            if (color == piece) {
                counter += 1;
            }
        }
        return counter;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Board) {
            Board b = (Board) o;
            return (b.toString().equals(toString()));
        } else {
            return false;
        }
    }
    /** countnumPiece. return B. P. */
    public int countNumPiece(Board b, PieceColor p) {
        int count = 0;
        for (int i = 0; i < SIDE * SIDE; i++) {
            if (b.get(i) == p) {
                count++;
            }
        }
        return count;
    }

    /** sets isAIWHITE. B. */
    public void setisAIWhite(boolean b) {
        isAIWHITE = b;
    }

    /** Sets setisAIBLACK. Takes in boolean B. */
    public void setisAIBlack(boolean b) {
        isAIBLACK = b;
    }

    /** Return true iff there is a move for the current player. */

    /**
     * Saved previous board for Undo.
     */
    private Stack<Board> _prevBoards;

    /**
     * Player that is on move.
     */
    private PieceColor _whoseMove;

    /**
     * Set true when game ends.
     */
    private boolean _gameOver;

    /**
     * Convenience value giving values of pieces at each ordinal position.
     */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();

    /**
     * Linear size of the current board.
     */
    private int linearSize = Move.SIDE * Move.SIDE;
    /** stack. */
    private Stack<String> stackOfPrevWhite;
    /** stack. */
    private Stack<String> stackOfPrevBlack;

    /**
     * One cannot create arrays of ArrayList<Move>, so we introduce
     * a specialized private list type for this purpose.
     */
    private static class MoveList extends ArrayList<Move> {
    }

    /**
     * A read-only view of a Board.
     */
    private class ConstantBoard extends Board implements Observer {
        /**
         * A constant view of this Board. return BOARD.
         */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
        }

        @Override
        void clear() {
        }

        /**
         * Undo the last move.
         */
        @Override
        void undo() {
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }

    }


}
