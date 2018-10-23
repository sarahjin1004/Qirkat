package qirkat;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;
import static qirkat.Command.Type.*;
import static qirkat.Game.State.PLAYING;
import static qirkat.Game.State.SETUP;
import static qirkat.GameException.error;
import static qirkat.PieceColor.*;

/**
 * Controls the play of the game.
 *
 * @author Sarah Jin
 */
class Game {

    /**
     * States of play.
     */
    static enum State {
        SETUP, PLAYING;
    }

    /**
     * A new Game, using BOARD to play on, reading initially from
     * BASESOURCE and using REPORTER for error and informational messages.
     */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _constBoard = _board.constantView();
        _reporter = reporter;
        _reportwinner = null;
        iSBLACK = new AI(this, BLACK);
        iSWHITE = new Manual(this, WHITE);
        whiteIsManual = true;
        blackIsManual = false;
    }

    /**
     * Run a session of Qirkat gaming.
     */

    void process() {
        doClear(null);

        while (true) {
            while (_state == SETUP) {
                try {
                    doCommand();
                } catch (GameException excp) {
                    _reporter.errMsg(excp.getMessage());
                }
            }

            _state = PLAYING;

            if (_board.findlegalmoves(_board, _board.whoseMove()).isEmpty()) {
                reportWinner();
                _state = SETUP;
            }
            if (_board.countNumPiece(_board, WHITE) == 0
                    || _board.countNumPiece(_board, BLACK) == 0) {
                reportWinner();
                _state = SETUP;
            }

            while (_state == PLAYING && !_board.gameOver()) {
                try {
                    Move move;
                    move = null;
                    PieceColor x = _board.whoseMove();
                    if (x == WHITE && _board.countNumPiece(_board, WHITE) != 0
                            && _board.countNumPiece(_board, BLACK) != 0) {
                        move = iSWHITE.myMove();
                    } else if (_board.countNumPiece(_board, WHITE) != 0
                            && _board.countNumPiece(_board, BLACK) != 0) {
                        move = iSBLACK.myMove();
                    }
                    if (_state == PLAYING
                            && _board.countNumPiece(_board, WHITE) != 0
                            && _board.countNumPiece(_board, BLACK) != 0) {
                        _board.makeMove(move);
                    }
                    if (_board.countNumPiece(_board, WHITE) == 0
                            || _board.countNumPiece(_board, BLACK) == 0) {
                        reportWinner();
                        _state = SETUP;
                        _board.setgameover(true);
                        doClear(new String[]{""});
                    }
                } catch (GameException exp) {
                    _reporter.errMsg(exp.getMessage());
                }
            }

            if (_state == PLAYING && _board.gameOver()) {
                reportWinner();
            }
            _state = SETUP;
        }

    }

    /**
     * Return the _STATE of the current program.
     */
    public static State getState() {
        return _state;
    }
    /** return REPORTWINNER. */
    public static String getReportWinner() {
        return _reportwinner;
    }

    /**
     * Return a read-only view of my game board.
     */
    Board board() {
        return _constBoard;
    }

    /**
     * Perform the next command from our input source.
     */
    void doCommand() {
        try {
            Command c = Command.parseCommand(_inputs.getLine("qirkat: "));
            _commands.get(c.commandType()).accept(c.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /**
     * Read and execute commands until encountering a move or until
     * the game leaves playing state due to one of the commands. Return
     * the terminating move command, or null if the game first drops out
     * of playing mode. If appropriate to the current input source, use
     * PROMPT to prompt for input.
     */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                switch (cmnd.commandType()) {
                case PIECEMOVE:
                    return cmnd;
                default:
                    _commands.get(cmnd.commandType()).
                                accept(cmnd.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /**
     * Return random integer between 0 (inclusive) and MAX>0 (exclusive).
     */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /**
     * Report a move, using a message formed from FORMAT and ARGS as
     * for String.format.
     */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /**
     * Report an error, using a message formed from FORMAT and ARGS as
     * for String.format.
     */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /**
     * Perform the command 'auto OPERANDS[0]'.
     */
    void doAuto(String[] operands) {
        _state = SETUP;
        if (operands[0].equalsIgnoreCase("white")) {
            iSWHITE = new AI(this, WHITE);
        } else if (operands[0].equalsIgnoreCase("black")) {
            iSBLACK = new AI(this, BLACK);
        } else {
            throw error("invalid player selection.");
        }
    }

    /**
     * Perform a 'help' command.
     */
    void doHelp(String[] unused) {
        InputStream helpIn =
                Game.class.getClassLoader().
                        getResourceAsStream("qirkat/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                        = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /**
     * Perform the command 'load OPERANDS[0]'.
     */
    void doLoad(String[] operands) {
        try {
            FileReader reader = new FileReader(operands[0]);
            ReaderSource parse = new ReaderSource(reader, true);
            _inputs.addSource(parse);
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /**
     * Perform the command 'manual OPERANDS[0]'.
     */
    void doManual(String[] operands) {
        _state = SETUP;
        if (operands[0].equalsIgnoreCase("WHITE")) {
            iSWHITE = new Manual(this, WHITE);
        } else if (operands[0].equalsIgnoreCase("BLACK")) {
            iSBLACK = new Manual(this, BLACK);
        } else {
            throw error("Not a valid player selection.");
        }
    }

    /**
     * Exit the program.
     */
    void doQuit(String[] unused) {
        Main.reportTotalTimes();
        System.exit(0);
    }

    /**
     * Perform the command 'start'.
     */
    void doStart(String[] unused) {
        _state = PLAYING;
    }

    /**
     * Perform the move OPERANDS[0].
     */
    void doMove(String[] operands) {
        _board.makeMove(Move.parseMove(operands[0]));
    }

    /**
     * Perform the command 'clear'.
     */
    void doClear(String[] unused) {
        _board.clear();
        _state = SETUP;
    }

    /**
     * Perform the command 'set OPERANDS[0] OPERANDS[1]'.
     */
    void doSet(String[] operands) {
        _state = SETUP;
        String who = operands[0];
        if (who.equalsIgnoreCase("white")) {
            _board.setWhoseMove(WHITE);
        } else if (who.equalsIgnoreCase("black")) {
            _board.setWhoseMove(BLACK);
        }
        char[] a = operands[1].toCharArray();
        int index = 0;
        int index1 = 0;
        while (index != a.length) {
            if (a[index] == 'w') {
                _board.set(index1, WHITE);
                index++;
                index1++;
            } else if (a[index] == 'b') {
                _board.set(index1, BLACK);
                index++;
                index1++;
            } else if (a[index] == '-') {
                _board.set(index1, EMPTY);
                index++;
                index1++;
            } else {
                index++;
            }
        }
    }

    /**
     * Perform the command 'dump'.
     */
    void doDump(String[] unused) {
        Formatter out = new Formatter();
        System.out.println("===");
        System.out.println(_board.toString());
        System.out.println("===");
    }

    /**
     * Execute 'seed OPERANDS[0]' command, where the operand is a string
     * of decimal digits. Silently substitutes another value if
     * too large.
     */
    void doSeed(String[] operands) {
        try {
            _randoms.setSeed(Long.parseLong(operands[0]));
        } catch (NumberFormatException e) {
            _randoms.setSeed(Long.MAX_VALUE);
        }
    }


    /**
     * Execute the artificial 'error' command.
     */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /**
     * Report the outcome of the current game.
     */
    void reportWinner() {
        System.out.println(_board.toString());
        String msg;
        PieceColor winner = _board.whoseMove().opposite();
        if (Main.getuseGUI()) {
            setReportWinner(winner.toString());
            Main.getDisplay().reportwinner();
        }
        msg = String.format("%s wins.", winner);
        _reporter.outcomeMsg(msg);

    }

    /**
     * Mapping of command types to methods that process them.
     */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
            new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(SETBOARD, this::doSet);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }

    /**
     * Input source.
     */
    private final CommandSources _inputs = new CommandSources();

    /**
     * My board and its read-only view.
     */
    private Board _board, _constBoard;

    /**
     * Indicate which players are manual players (as opposed to AIs).
     */
    private boolean blackIsManual, whiteIsManual;
    /**
     * Current game state.
     */
    private static State _state;
    /**
     * Used to send messages to the user.
     */
    private Reporter _reporter;
    /**
     * Source of pseudo-random numbers (used by AIs).
     */
    private Random _randoms = new Random();
    /**
     * True if player/AI is white.
     */
    private Player iSWHITE;
    /**
     * True if player/AI is black.
     */
    private Player iSBLACK;

    /** return ISWHITE. */
    public Player getiSWHITE() {
        return iSWHITE;
    }
    /** return ISBLACK. */
    public Player getiSBLACK() {
        return iSBLACK;
    }
    /**
     * Used to report the winner to GUI.
     */
    private static String _reportwinner;
    /** setREPORTWINNER. A. */
    public static void setReportWinner(String a) {
        _reportwinner = a;
    }
}
