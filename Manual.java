package qirkat;

import static qirkat.PieceColor.*;
import static qirkat.Command.Type.*;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Sarah Jin
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
        _prompt = myColor + ": ";
    }

    @Override
    Move myMove() {
        Command cmd;
        cmd = super.game().getMoveCmnd(String.format(_prompt));
        if (cmd == null) {
            return null;
        }
        String[] arr = cmd.operands();
        Move x =  Move.parseMove(arr[0]);
        return x;
    }

    /** Identifies the player serving as a source of input commands. */
    private String _prompt;
}

