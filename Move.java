package qirkat;

import static java.lang.Math.abs;

import java.util.Formatter;
import java.util.HashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Represents a Qirkat move. There is one Move object created for
 * each distinct Move.  A "vestigial" move represents a single board
 * position, as opposed to a move (its starting and ending rows are
 * equal, likewise columns).
 *
 * @author Sarah Jin
 */
class Move {

    /**
     * Size of a side of the board.
     */
    static final int SIDE = 5;

    /**
     * Maximum linearized index.
     */
    static final int MAX_INDEX = SIDE * SIDE - 1;

    /**
     * Constants used to compute linearized indices.
     */
    private static final int
            STEP_C = 1,
            STEP_R = 5,
            INDEX_ORIGIN = -('a' * STEP_C + '1' * STEP_R);

    /**
     * Pattern for valid move input.
     */
    private static final Pattern MOVE_PATTERN =
            Pattern.compile("(?:.*-)?([a-e])([1-5])-([a-e])([1-5])$");

    /* Moves get generated profligately during the calculations of an AI,
     * so it's a good idea to make that operation efficient.  Instead of
     * relying on a Move constructor, which does a memory allocation with
     * each use of 'new', we use a "Move factory": Move.move,
     * a static method that returns a Move, but not necessarily a new
     * one. Moves themselves are immutable, and for any possible move,
     * there is exactly one object of type Move. */

    /* To avoid creating Move objects that are not needed, we maintain a
     * a static variable, _staged containing a Move object.  The move
     * factory methods then sets the fields of this object before looking to
     * to see if there is already a Move object with the same parameters.
     * If there is, the move methods will simply return it, thus allowing
     * _staged to be reused on the next call without having to create a
     * new Move object.  Otherwise, we use the _staged object itself as the
     * new Move, and set the variable to null so that we create a new
     * Move on the next call to move. There is a drawback: since there
     * is at most one _staged object at any time, at most one call to move
     * may execute simultaneously.  Otherwise, two the methods may attempt
     * to use the same Move object for two different Moves, which clearly
     * will not work.  Therefore, we indicate in the comments that a number
     * of the operations are not "thread safe". Fortunately, you'll not run
     * into this problem as long as you don't do anything fancy (such as
     * trying to make your AI a parallel program).
     */

    /**
     * The move constructor, made private to prevent its use except in
     * this class.
     */
    private Move() {
    }

    /**
     * A factory method that returns a Move from COL0 ROW0 to COL1 ROW1,
     * followed by NEXTJUMP, if this move is a jump. Assumes the column
     * and row designations are valid and that NEXTJUMP is null for a
     * non-capturing move.  Not thread-safe.
     */
    static Move move(char col0, char row0, char col1, char row1,
                     Move nextJump) {
        if (_staged == null) {
            _staged = new Move();
        }
        _staged.set(col0, row0, col1, row1, nextJump);
        Move result = _internedMoves.computeIfAbsent(_staged, IDENTITY);
        if (result == _staged) {
            _staged = null;
        }
        return result;
    }

    /**
     * Return a single move or jump from (COL0, ROW0) to (COL1, ROW1).
     * Not thread-safe.
     */
    static Move move(char col0, char row0, char col1, char row1) {
        return move(col0, row0, col1, row1, null);
    }

    /**
     * Return a vestigial Move consisting only of starting square
     * COL0 ROW0.
     */
    static Move move(char col0, char row0) {
        return move(col0, row0, col0, row0);
    }
    /** return MOVE. FROMINDEX. TOINDEX. */
    static Move move(int fromIndex, int toIndex) {
        char col0 = indexToCol(fromIndex);
        char row0 = indexToRow(fromIndex);
        char col1 = indexToCol(toIndex);
        char row1 = indexToRow(toIndex);
        return move(col0, row0, col1, row1);
    }
    /** return char INDEX. RETURN I. */
    static char indexToCol(int i) {
        String index = Integer.toString(i);
        if (index.equals("0") || index.equals("5") || index.equals("10")
                || index.equals("15") || index.equals("20")) {
            return 'a';
        } else if (index.equals("1") || index.equals("6") || index.equals("11")
                || index.equals("16") || index.equals("21")) {
            return 'b';
        } else if (index.equals("2") || index.equals("7") || index.equals("12")
                || index.equals("17") || index.equals("22")) {
            return 'c';
        } else if (index.equals("3") || index.equals("8") || index.equals("13")
                || index.equals("18") || index.equals("23")) {
            return 'd';
        } else {
            return 'e';
        }
    }
    /** return char INDEX. */
    static char indexToRow(int index) {
        if (index >= Integer.parseInt("0")
                && index < Integer.parseInt("5")) {
            return '1';
        } else if (index >= Integer.parseInt("5")
                && index < Integer.parseInt("10")) {
            return '2';
        } else if (index >= Integer.parseInt("10")
                && index < Integer.parseInt("15")) {
            return '3';
        } else if (index >= Integer.parseInt("15")
                && index < Integer.parseInt("20")) {
            return '4';
        } else {
            return '5';
        }
    }

    /**
     * Return the concatenation MOVE0 followed by MOVE1.  Either may be
     * null, in which case the result is the other.  A vestigial move
     * is equivalent to a position and extends a move on either end by
     * one square.
     */
    static Move move(Move move0, Move move1) {
        if (move0 == null) {
            return move1;
        }
        if (move1 == null) {
            return move0;
        }
        if (move0.isVestigial()) {
            return null;
        }
        if (move0.jumpTail() == null) {
            return null;
        } else {
            return null;
        }

    }

    /**
     * Return true iff (C, R) is a valid square designation.
     */
    static boolean validSquare(char c, char r) {
        return 'a' <= c && c <= 'e' && '1' <= r && r <= '5';
    }

    /**
     * Return true iff K is a valid linearized index.
     */
    static boolean validSquare(int k) {
        return 0 <= k && k <= MAX_INDEX;
    }

    /**
     * Return the linearized index of square C R.
     */
    static int index(char c, char r) {
        int k = c * STEP_C + r * STEP_R + INDEX_ORIGIN;
        return k;
    }

    /**
     * Return the column letter of linearized index K.
     */
    static char col(int k) {
        return (char) (k % STEP_R + 'a');
    }

    /**
     * Return the row digit of linearized index K.
     */
    static char row(int k) {
        return (char) (k / STEP_R + '1');
    }

    /**
     * Return true iff this is a capturing move (a jump).
     */
    boolean isJump() {
        return _isJump;
    }

    /**
     * Return true iff this is a vestigial Move consisting only of a single
     * position.
     */
    boolean isVestigial() {
        return _col0 == _col1 && _row0 == _row1 && _nextJump == null;
    }

    /**
     * Return true iff this is a horizontal, non-capturing move to
     * the left.
     */
    boolean isLeftMove() {
        return false;
    }

    /**
     * Return true iff this is a horizontal, non-capturing move
     * to the right.
     */
    boolean isRightMove() {
        return false;
    }

    /**
     * Returns the source column.
     */
    char col0() {
        return _col0;
    }

    /**
     * Returns the source row.
     */
    char row0() {
        return _row0;
    }

    /**
     * Returns the destination column.
     */
    char col1() {
        return _col1;
    }

    /**
     * Returns the destination row.
     */
    char row1() {
        return _row1;
    }

    /**
     * For a jump, returns the row of the jumped-over square for the
     * first leg of the jump.  For a non-capturing move, same as row1().
     */
    char jumpedRow() {
        int row0 = (int) _row0;
        int row1 = (int) _row1;
        return (char) ((row1 + row0) / 2);
    }

    /**
     * For a jump, returns the column of the jumped-over square for the
     * first leg of the jump.  For a non-capturing move, same as col1().
     */
    char jumpedCol() {
        if (_col0 == 'a' && _col1 == 'a') {
            return 'a';
        } else if (_col0 == 'a' && _col1 == 'c' || _col0 == 'c' && _col1 == 'a'
                || _col0 == 'b' && _col1 == 'b') {
            return 'b';
        } else if (_col0 == 'b' && _col1 == 'd' || _col0 == 'd' && _col1 == 'b'
                || _col0 == 'c' && _col1 == 'c') {
            return 'c';
        } else if (_col0 == 'c' && _col1 == 'e' || _col0 == 'e' && _col1 == 'c'
                || _col0 == 'd' && _col1 == 'd') {
            return 'd';
        }
        return 'e';
    }

    /**
     * Checks if the move was jump.
     */
    void checkJump() {
        int row0 = (int) _row0;
        int row1 = (int) _row1;
        if (_col0 == 'a' && _col1 == 'c' || _col0 == 'c' && _col1 == 'a'
                || _col0 == 'b' && _col1 == 'd' || _col0 == 'd' && _col1 == 'b'
                || _col0 == 'c' && _col1 == 'e' || _col0 == 'e' && _col1 == 'c'
                || (Math.abs(row1 - row0)) == 2) {
            _isJump = true;
        }
    }

    /**
     * Return the linearized index of my source square.
     */
    int fromIndex() {
        return _fromIndex;
    }

    /**
     * Return The linearized index of my destination square.
     */
    int toIndex() {
        return _toIndex;
    }

    /**
     * Return the linearized index of (jumpedCol(), jumpedRow()).
     */
    int jumpedIndex() {
        return index(jumpedCol(), jumpedRow());
    }

    /**
     * Return the second and subsequent jumps comprising this jump, or null
     * for a single jump.
     */
    Move jumpTail() {
        return _nextJump;
    }

    @Override
    public int hashCode() {
        return (_fromIndex << 5) | _toIndex;
    }

    @Override
    public boolean equals(Object obj) {
        /* NOTE: Depends on there being no more than one Move object for
         * each distinct move, so that pointer equality of _nextJump
         * is valid. */
        Move m = (Move) obj;
        return _fromIndex == m._fromIndex && _nextJump == m._nextJump
                && _toIndex == m._toIndex;
    }

    /**
     * Return the non-vestigial Move denoted by STR.
     */
    static Move parseMove(String str) {
        Matcher mat = MOVE_PATTERN.matcher(str);
        int end;

        Move result;
        result = null;
        end = str.length();

        while (end > 2) {
            mat.region(0, end);
            if (!mat.matches()) {
                throw new IllegalArgumentException("bad move denotation");
            }

            result = move(mat.group(1).charAt(0), mat.group(2).charAt(0),
                    mat.group(3).charAt(0), mat.group(4).charAt(0),
                    result);
            end = mat.end(2);
        }
        if (result == null) {
            throw new IllegalArgumentException("bad move denotation");
        }
        return result;
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        toString(out);
        return out.toString();
    }

    /**
     * Write my string representation into OUT.
     */
    private void toString(Formatter out) {
        String a = _col0 + "" + _row0 + "-" + _col1 + "" + _row1;
        while (_nextJump != null) {
            a = a + "-" + _nextJump._col1 + "" + _nextJump._row1;
            _nextJump = _nextJump._nextJump;
        }
        out.format(a);
    }

    /**
     * Set me to COL0 ROW0 - COL1 ROW1 - NEXTJUMP.
     */
    private void set(char col0, char row0, char col1, char row1,
                     Move nextJump) {
        _col0 = col0;
        _row0 = row0;
        _col1 = col1;
        _row1 = row1;
        _fromIndex = (byte) index(col0, row0);
        _toIndex = (byte) index(col1, row1);
        _isJump = abs(col0 - col1) > 1 || abs(row0 - row1) > 1;
        _nextJump = nextJump;
    }

    /**
     * Linearized indices.
     */
    private byte _fromIndex, _toIndex;

    /**
     * True iff move is a jump.
     */
    private boolean _isJump;

    /**
     * From and to squares.
     */
    private char _col0, _row0, _col1, _row1;

    /**
     * For a jump, the Move representing the jumps following the
     * initial jump.
     */
    private Move _nextJump;
    /** return Move. */
    public Move getNextJump() {
        return _nextJump;
    }

    /* Used for the Move factory. */

    /**
     * Holds the next Move object to be added to _internedMoves.
     * The factory method move tentatively fills it in, and then returns
     * it if it is unique (resetting _staged to null).
     */
    private static Move _staged;

    /**
     * The set of all distinct moves generated so far.
     */
    private static HashMap<Move, Move> _internedMoves = new HashMap<>();

    /**
     * The identity function on Moves.
     */
    static final Function<Move, Move> IDENTITY = k -> k;

}
