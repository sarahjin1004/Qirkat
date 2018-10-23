package qirkat;

import ucb.gui2.TopLevel;
import ucb.gui2.LayoutSpec;
import java.util.Observable;
import java.util.Observer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.File;
import java.io.InputStream;
import java.util.Scanner;

/** The GUI for the Qirkat game.
 *  @author Sarah Jin
 */
class GUI extends TopLevel implements Observer, Reporter {

    /* The implementation strategy applied here is to make it as
     * unnecessary as possible for the rest of the program to know that it
     * is interacting with a GUI as opposed to a terminal.
     *
     * To this end, we first have made Board observable, so that the
     * GUI gets notified of changes to a Game's board and can interrogate
     * it as needed, while the Game and Board themselves need not be aware
     * that it is being watched.
     *
     * Second, instead of creating a new API by which the GUI communicates
     * with a Game, we instead simply arrange to make the GUI's input look
     * like that from a terminal, so that we can reuse all the machinery
     * in the rest of the program to interpret and execute commands.  The
     * GUI simply composes commands (such as "start" or "clear") and
     * writes them to a Writer that (using the Java library's PipedReader
     * and PipedWriter classes) provides input to the Game using exactly the
     * same API as would be used to read from a terminal. Thus, a simple
     * Manual player can handle all commands and moves from the GUI.
     *
     * See also Main.java for how this might get set up.
     */

    /** Minimum size of board in pixels. */
    private static final int MIN_SIZE = 300;

    /** A new display observing MODEL, with TITLE as its window title.
     *  It uses OUTCOMMANDS to send commands to a game instance, using the
     *  same commands as the text format for Qirkat. */
    GUI(String title, Board model, Writer outCommands) {
        super(title, true);
        frame.setUndecorated(false);
        addMenuButton("Game->New", this::newGame);
        addMenuButton("Game->Start", this::startGame);
        addMenuButton("Game->Restart", this::restartGame);
        addMenuButton("Game->Quit", this::quit);

        addMenuRadioButton("Options->Players->White AI",
                "whites", false, this::setWhiteAI);
        addMenuRadioButton("Options->Players->White Manual",
                "whites", true, this::setWhiteManual);
        addMenuRadioButton("Options->Players->Black AI",
                "blacks", true, this::setBlackAI);
        addMenuRadioButton("Options->Players->Black Manual",
                "blacks", false, this::setBlackManual);
        addMenuButton("Info->Help", this::help);
        _model = model;
        _widget = new BoardWidget(model);
        addLabel(_model.whoseMove().toString() + " to move", "reporter",
                new LayoutSpec("y", 5, "anchor", "west"));
        _out = new PrintWriter(outCommands, true);
        add(_widget,
                new LayoutSpec("height", "1",
                        "width", "REMAINDER",
                        "ileft", 5, "itop", 5, "iright", 5,
                        "ibottom", 5));
        setMinimumSize(MIN_SIZE, MIN_SIZE);
        _widget.addObserver(this);
        _model.addObserver(this);
    }
    /** create. */
    private synchronized void newGame(String unused) {
        _out.printf("clear%n");
        setChanged();
        notifyObservers();
    }
    /** start. */
    private synchronized void startGame(String unused) {
        _out.printf("start%n");
        reportwinner();
        setChanged();
        notifyObservers();
    }
    /** win. */
    public void reportwinner() {
        if (Game.getReportWinner() != null) {
            showMessage(Game.getReportWinner() + " wins.", "Outcome", "foo");
        }
        Game.setReportWinner(null);
    }

    @Override
    protected void setLabel(String id, String text) {
        super.setLabel(id, text);
    }
    /** Move pieces on the board. */
    private synchronized void movePieces(String unused) {
        setChanged();
        notifyObservers("Piece Move");
    }

    /** Set White to AI. */
    private synchronized void setWhiteAI(String unused) {
        _out.printf("auto white%n");
        setChanged();
        notifyObservers();
    }
    /** Set White to Manual. */
    private synchronized void setWhiteManual(String unused) {
        _out.printf("manual white%n");
        setChanged();
        notifyObservers();
    }
    /** Set Black to AI. */
    private synchronized void setBlackAI(String unused) {
        _out.printf("auto black%n");
        setChanged();
        notifyObservers();
    }
    /** Set Black to Manual. */
    private synchronized void setBlackManual(String unused) {
        _out.printf("manual black%n");
        setChanged();
        notifyObservers();
    }
    /** Display the help message. */
    private synchronized void help(String unused) {
        String text = "";
        try {
            text = new Scanner(new File("qirkat/help.txt")).
                    useDelimiter("\\A").next();
        } catch (FileNotFoundException e) {
            _out.printf("help%n");
        }
        showMessage(text, "Help", "foo");
        setChanged();
        notifyObservers();
    }

    /** Execute the "Quit" button function. */
    private synchronized void quit(String unused) {
        _out.printf("quit%n");
    }

    /** UNUSED. */
    private synchronized void restartGame(String unused) {
        _out.printf("clear%n");
        _out.printf("start%n");
        setChanged();
        notifyObservers();
    }
    /** Display text in file NAME in a box titled TITLE. */
    private void displayText(String name, String title) {
        InputStream input =
                Game.class.getClassLoader().getResourceAsStream(name);
        if (input != null) {
            try {
                BufferedReader r
                        = new BufferedReader(new InputStreamReader(input));
                char[] buffer = new char[1 << 15];
                int len = r.read(buffer);
                showMessage("BOBA", title, "plain");
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    @Override
    public void errMsg(String format, Object... args) {
    }

    @Override
    public void outcomeMsg(String format, Object... args) {
    }

    @Override
    public void moveMsg(String format, Object... args) {
    }

    @Override
    public void update(Observable obs, Object arg) {
        if (obs == _model) {
            setChanged();
            notifyObservers("click");
        } else if (obs == _widget) {
            setChanged();
            notifyObservers("click");
        }
    }

    /** Respond to a click on SQ. */
    private void movePiece(String sq) {
        setChanged();
        notifyObservers("Piece Move");
    }

    /** Make MOV the user-selected move (no move if null). */
    private void selectMove(Move mov) {
        _selectedMove = mov;
        _widget.indicateMove(mov);
    }

    /** Contains the drawing logic for the Qirkat model. */
    private BoardWidget _widget;
    /** The model of the game. */
    private Board _model;
    /** Output sink for sending commands to a game. */
    private static PrintWriter _out;

    /** return OUT. */
    public static PrintWriter getOut() {
        return _out;
    }
    /** Move selected by clicking. */
    private Move _selectedMove;
}
