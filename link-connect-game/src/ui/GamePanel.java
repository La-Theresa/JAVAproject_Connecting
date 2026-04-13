package ui;

import model.Constants;
import model.GameBoard;
import model.GameSession;
import model.Path;
import model.Position;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 游戏棋盘绘制与鼠标交互面板。
 */
public class GamePanel extends JPanel {
    private final GameFrame frame;
    private GameSession session;
    private Position first;
    private Position second;
    private Position hintA;
    private Position hintB;

    public GamePanel(GameFrame frame) {
        this.frame = frame;
        setPreferredSize(new Dimension(Constants.FRAME_WIDTH, Constants.FRAME_HEIGHT - Constants.HUD_HEIGHT));
        setBackground(new Color(245, 245, 220));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onBoardClick(e.getX(), e.getY());
            }
        });
    }

    /**
     * 绑定会话并重置选择状态。
     */
    public void bindSession(GameSession session) {
        this.session = session;
        this.first = null;
        this.second = null;
        this.hintA = null;
        this.hintB = null;
        repaint();
    }

    /**
     * 高亮提示方块。
     */
    public void showHint(Position[] pair) {
        if (pair == null) {
            hintA = null;
            hintB = null;
        } else {
            hintA = pair[0];
            hintB = pair[1];
        }
        repaint();
    }

    private void onBoardClick(int x, int y) {
        if (session == null || session.hasWon() || session.hasLost()) {
            return;
        }
        Position p = pixelToPos(x, y);
        if (p == null) {
            return;
        }
        GameBoard board = session.board();
        if (board.isEmpty(p)) {
            return;
        }

        if (first == null) {
            first = p;
            hintA = null;
            hintB = null;
            repaint();
            return;
        }

        if (first.equals(p)) {
            first = null;
            second = null;
            repaint();
            return;
        }

        second = p;
        if (session.eliminate(first, second)) {
            Timer t = new Timer(220, e -> {
                session.clearLastPath();
                repaint();
            });
            t.setRepeats(false);
            t.start();
        }
        first = null;
        second = null;
        frame.refreshHud();
        frame.checkGameStateAndPrompt();
        repaint();
    }

    private Position pixelToPos(int x, int y) {
        GameBoard board = session.board();
        int cellW = getWidth() / board.cols();
        int cellH = getHeight() / board.rows();
        int col = x / Math.max(1, cellW);
        int row = y / Math.max(1, cellH);
        Position p = new Position(row, col);
        if (!board.isValid(p)) {
            return null;
        }
        return p;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (session == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBoard(g2);
        drawPath(g2);
    }

    private void drawBoard(Graphics2D g2) {
        GameBoard board = session.board();
        int cellW = getWidth() / board.cols();
        int cellH = getHeight() / board.rows();

        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.cols(); c++) {
                Position p = new Position(r, c);
                int x = c * cellW;
                int y = r * cellH;
                g2.setColor(new Color(250, 248, 239));
                g2.fillRect(x, y, cellW, cellH);
                g2.setColor(new Color(187, 173, 160));
                g2.drawRect(x, y, cellW, cellH);

                if (!board.isEmpty(p)) {
                    int pattern = board.tileAt(p).patternId();
                    g2.setColor(patternColor(pattern));
                    g2.fillRoundRect(x + 6, y + 6, Math.max(10, cellW - 12), Math.max(10, cellH - 12), 16, 16);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, Math.min(cellW, cellH) / 3)));
                    g2.drawString(String.valueOf(pattern), x + cellW / 2 - 4, y + cellH / 2 + 5);
                }

                if ((first != null && first.equals(p)) || (second != null && second.equals(p))) {
                    g2.setColor(Constants.SELECTED_COLOR);
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawRect(x + 2, y + 2, cellW - 4, cellH - 4);
                }
                if ((hintA != null && hintA.equals(p)) || (hintB != null && hintB.equals(p))) {
                    g2.setColor(new Color(46, 204, 113));
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawRect(x + 4, y + 4, cellW - 8, cellH - 8);
                }
            }
        }
    }

    private void drawPath(Graphics2D g2) {
        Path path = session.lastPath();
        if (path == null) {
            return;
        }
        List<Position> points = path.points();
        GameBoard board = session.board();
        int cellW = getWidth() / board.cols();
        int cellH = getHeight() / board.rows();

        g2.setColor(Constants.PATH_COLOR);
        g2.setStroke(new BasicStroke(4f));
        for (int i = 0; i < points.size() - 1; i++) {
            Position a = points.get(i);
            Position b = points.get(i + 1);
            int ax = a.col() * cellW + cellW / 2;
            int ay = a.row() * cellH + cellH / 2;
            int bx = b.col() * cellW + cellW / 2;
            int by = b.row() * cellH + cellH / 2;
            g2.drawLine(ax, ay, bx, by);
        }
    }

    private Color patternColor(int pattern) {
        Color[] palette = {
                new Color(231, 76, 60),
                new Color(230, 126, 34),
                new Color(241, 196, 15),
                new Color(46, 204, 113),
                new Color(26, 188, 156),
                new Color(52, 152, 219),
                new Color(41, 128, 185),
                new Color(142, 68, 173),
                new Color(243, 156, 18),
                new Color(22, 160, 133),
                new Color(127, 140, 141),
                new Color(149, 165, 166)
        };
        return palette[(pattern - 1 + palette.length) % palette.length];
    }
}
