package qirkat;

import ucb.gui2.Pad;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import static qirkat.PieceColor.BLACK;
import static qirkat.PieceColor.WHITE;

/**
 * Widget for displaying a Qirkat board.
 *
 * @author Sarah Jin
 */
class BoardWidget extends Pad implements Observer {
    /** ArrayList to keep track of mouse movements. */
    private ArrayList<String> prevmousemove = new ArrayList<>();
    /** The color of my board. */
    private static final Color BOARDCOLOR = new Color(235, 198, 196);
    /**
     * Length of side of one square,in pixels.
     */
    static final int SQDIM = 50;
    /**
     * Number of squares on a side.
     */
    static final int SIDE = Move.SIDE;
    /**
     * Radius of circle representing a piece.
     */
    static final int PIECE_RADIUS = 15;

    /**
     * Color of white pieces.
     */
    private static final Color WHITE_COLOR = Color.WHITE;
    /** Color of "phantom" white pieces. */
    /**
     * Color of black pieces.
     */
    private static final Color BLACK_COLOR = Color.BLACK;
    /**
     * Color of painted lines.
     */
    private static final Color LINE_COLOR = Color.BLACK;
    /**
     * Color of blank squares.
     */
    private static final Color BLANK_COLOR = new Color(100, 100, 100);

    /**
     * Stroke for lines..
     */
    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);

    /**
     * Stroke for outlining pieces.
     */
    private static final BasicStroke OUTLINE_STROKE = LINE_STROKE;

    /**
     * Model being displayed.
     */
    private static Board _model;

    /**
     * A new widget displaying MODEL.
     */
    BoardWidget(Board model) {
        _model = model;
        setMouseHandler("click", this::read);
        _model.addObserver(this);
        _dim = SQDIM * SIDE;
        setPreferredSize(_dim, _dim);
    }

    /**
     * Indicate that the squares indicated by MOV are the currently selected
     * squares for a pending move.
     */
    void indicateMove(Move mov) {
        _selectedMove = mov;
        repaint();
    }
    /** Takes in a graphics2d G. */
    @Override
    public synchronized void paintComponent(Graphics2D g) {
        g.setColor(BOARDCOLOR);
        g.fillRect(0, 0, _dim, _dim);
        int n = SIDE;
        int D = SQDIM;
        g.setColor(new Color(Integer.parseInt("248"),
                Integer.parseInt("97"), Integer.parseInt("151")));
        g.drawLine(Integer.parseInt("225"), Integer.parseInt("225"),
                Integer.parseInt("25"), Integer.parseInt("25"));
        g.drawLine(Integer.parseInt("25"), Integer.parseInt("225"),
                Integer.parseInt("225"), Integer.parseInt("25"));
        g.drawLine(Integer.parseInt("25"), Integer.parseInt("125"),
                Integer.parseInt("125"), Integer.parseInt("25"));
        g.drawLine(Integer.parseInt("125"), Integer.parseInt("225"),
                Integer.parseInt("225"), Integer.parseInt("125"));
        g.drawLine(Integer.parseInt("125"), Integer.parseInt("225"),
                Integer.parseInt("25"), Integer.parseInt("125"));
        g.drawLine(Integer.parseInt("125"), Integer.parseInt("25"),
                Integer.parseInt("225"), Integer.parseInt("125"));
        g.drawLine(Integer.parseInt("25"), Integer.parseInt("25"),
                Integer.parseInt("225"), Integer.parseInt("25"));
        g.drawLine(Integer.parseInt("25"), Integer.parseInt("75"),
                Integer.parseInt("225"), Integer.parseInt("75"));
        g.drawLine(Integer.parseInt("25"), Integer.parseInt("125"),
                Integer.parseInt("225"), Integer.parseInt("125"));
        g.drawLine(Integer.parseInt("25"), Integer.parseInt("175"),
                Integer.parseInt("225"), Integer.parseInt("175"));
        g.drawLine(Integer.parseInt("25"), Integer.parseInt("225"),
                Integer.parseInt("225"), Integer.parseInt("225"));
        g.drawLine(Integer.parseInt("25"), Integer.parseInt("25"),
                Integer.parseInt("25"), Integer.parseInt("225"));
        g.drawLine(Integer.parseInt("75"), Integer.parseInt("25"),
                Integer.parseInt("75"), Integer.parseInt("225"));
        g.drawLine(Integer.parseInt("125"), Integer.parseInt("25"),
                Integer.parseInt("125"), Integer.parseInt("225"));
        g.drawLine(Integer.parseInt("175"), Integer.parseInt("25"),
                Integer.parseInt("175"), Integer.parseInt("225"));
        g.drawLine(Integer.parseInt("225"), Integer.parseInt("25"),
                Integer.parseInt("225"), Integer.parseInt("225"));
        for (char c = 'a'; c < 'a' + n; c += 1) {
            for (char r = '1'; r < '1' + n; r += 1) {
                int x0 = (c - 'a') * SQDIM;
                int y0 = (n - (r - '1')) * SQDIM;
                if (_model.get(c, r) == WHITE) {
                    drawPiece(g, x0, y0, WHITE);
                } else if (_model.get(c, r) == BLACK) {
                    drawPiece(g, x0, y0, BLACK);
                } else {
                    g.setColor(BLANK_COLOR);
                }
            }
        }

    }
    /** A function to draw the pieces on the board. G. CX. CY. PLAYER. */
    void drawPiece(Graphics2D g, int cx, int cy, PieceColor player) {

        if (player == WHITE) {
            g.setColor(WHITE_COLOR);
        }
        if (player == BLACK) {
            g.setColor(new Color(Integer.parseInt("195"),
                    Integer.parseInt("240"), Integer.parseInt("247")));
        }
        g.drawRoundRect(cx + (SQDIM / Integer.parseInt("4")),
                cy + (SQDIM / Integer.parseInt("4")) - SQDIM,
                PIECE_RADIUS * Integer.parseInt("2"),
                PIECE_RADIUS * Integer.parseInt("2"),
                Integer.parseInt("20"), Integer.parseInt("20"));
        g.fillRoundRect(cx + (SQDIM / Integer.parseInt("4"))
                        + Integer.parseInt("5"),
                cy + (SQDIM / Integer.parseInt("4"))
                        - SQDIM + Integer.parseInt("5"),
                PIECE_RADIUS * Integer.parseInt("2") - Integer.parseInt("10"),
                PIECE_RADIUS * Integer.parseInt("2") - Integer.parseInt("10"),
                Integer.parseInt("20"), Integer.parseInt("20"));
    }
    /**
     * Notify UNUSED, WHERE.
     */
    private void read(String unused, MouseEvent where) {
        int x = where.getX(), y = where.getY();
        char mouseCol, mouseRow;
        if (where.getButton() == MouseEvent.BUTTON1) {
            mouseCol = (char) (x / SQDIM + 'a');
            mouseRow = (char) ((SQDIM * SIDE - y) / SQDIM + '1');
            String zz = Character.toString(mouseCol);
            String rot = Character.toString(mouseRow);
            String zzrot = zz + rot;
            prevmousemove.add(zzrot);
            if (mouseCol >= 'a' && mouseCol <= 'e'
                    && mouseRow >= '1' && mouseRow <= '5'
                    && prevmousemove.size() >= 2) {
                GUI.getOut().printf(prevmousemove.get
                        (prevmousemove.size() - 2) + "-" + zzrot + "%n");
                if (mouseCol >= 'a' && mouseCol <= 'e'
                        && mouseRow >= '1' && mouseRow <= '5') {
                    setChanged();
                    notifyObservers("" + mouseCol + mouseRow);
                }
            }
        }
    }

    @Override
    public synchronized void update(Observable model, Object arg) {
        repaint();
    }


    /**
     * Dimension of current drawing surface in pixels.
     */
    private int _dim;

    /**
     * A partial Move indicating selected squares.
     */
    private Move _selectedMove;
}
