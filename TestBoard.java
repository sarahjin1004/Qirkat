package qirkat;

import static qirkat.PieceColor.EMPTY;
/** Testboard. */
public class TestBoard extends Board {
    /**Testboard. */
    TestBoard() {
        super();
    }
    /**Testboard B. */
    TestBoard(Board b) {
        super(b);
    }
    @Override
    void makeMove(Move mov) {
        set(mov.toIndex(), super.whoseMove());
        set(mov.fromIndex(), EMPTY);
        set(mov.jumpedCol(), mov.jumpedRow(), EMPTY);
        setChanged();
        notifyObservers();
    }
    /** return M.*/
    Move returnNextJump(Move m) {
        makeMove(m);
        if (canJump(super.whoseMove())) {
            return findlegalmoves(this, super.whoseMove()).get(0);
        }
        return null;
    }

}
