package qirkat;
/** MVOEVOBJECT CLASS.
 * @author Sarah Jin */

public class MoveObject {
    /** return MOVEOBJECT. COL0, ROW0, COL1, ROW1. */
    public MoveObject(char col0, char row0, char col1, char row1) {
        _col0 = col0;
        _row0 = row0;
        _col1 = col1;
        _row1 = row1;
    }
    /** return CHAR. */
    public char getCol0() {
        return _col0;
    }
    /** return CHAR. */
    public char getRow0() {
        return _row0;
    }
    /** return CHAR. */
    public char getCol1() {
        return _col1;
    }
    /** return CHAR. */
    public char getRow1() {
        return _row1;
    }
    /** return CHAR. */
    private char _col0, _row0, _col1, _row1;


}
