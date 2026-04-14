package ui;

import controller.GameController;
import model.Constants;
import model.GameBoard;
import model.GameSession;
import model.Path;
import model.Position;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 游戏棋盘绘制与鼠标交互面板。
 */
public class GamePanel extends JPanel {
    private final GameController controller;
    private GameSession session;
    private Position hintA;
    private Position hintB;

    /**
     * 构造游戏棋盘面板。
     * @param controller 游戏控制器
     */
    public GamePanel(GameController controller) {
        this.controller = controller;
        setPreferredSize(new Dimension(Constants.FRAME_WIDTH, Constants.FRAME_HEIGHT - Constants.HUD_HEIGHT));
        setBackground(AuthUiKit.APP_BG);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onBoardClick(e.getX(), e.getY());
            }
        });
    }

    /**
     * 绑定会话并重置选择状态。
     */
    public void bindSession(GameSession session) {
        this.session = session;
        repaint();
    }

    /**
     * 高亮提示方块。
     */
    public void setHintPair(Position[] pair) {
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
        Position p = pixelToPos(x, y);
        if (p == null) {
            return;
        }
        controller.handleBoardClick(p);
    }

    /**
     * 像素坐标转换为棋盘位置。
     * @param x 像素X坐标
     * @param y 像素Y坐标
     * @return 棋盘位置，无效返回null
     */
    private Position pixelToPos(int x, int y) {
        GameSession currentSession = controller.getSession();
        if (currentSession == null) {
            return null;
        }
        GameBoard board = currentSession.board();
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
        GameSession currentSession = controller.getSession();
        if (currentSession == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBoard(g2);
        drawPath(g2);
    }

    /**
     * 绘制棋盘方块、选中高亮和提示标记。
     * @param g2 图形上下文
     */
    private void drawBoard(Graphics2D g2) {
        GameSession currentSession = controller.getSession();
        if (currentSession == null) {
            return;
        }
        GameBoard board = currentSession.board();
        int cellW = getWidth() / board.cols();
        int cellH = getHeight() / board.rows();
        Position first = controller.getSelectedFirst();
        Position second = controller.getSelectedSecond();
        Position[] hints = controller.getHintPair();

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
                    int iconSize = Math.max(18, Math.min(cellW, cellH) - 14);
                    String iconKey = resolveIconKey(currentSession.difficulty(), pattern);
                    BufferedImage image = ThemePngIconLoader.loadTileImage(iconKey, iconSize);
                    int ix = x + (cellW - iconSize) / 2;
                    int iy = y + (cellH - iconSize) / 2;
                    g2.drawImage(image, ix, iy, null);
                }

                if ((first != null && first.equals(p)) || (second != null && second.equals(p))) {
                    g2.setColor(Constants.SELECTED_COLOR);
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawRect(x + 2, y + 2, cellW - 4, cellH - 4);
                }
                if ((hints != null && hints.length > 0 && hints[0] != null && hints[0].equals(p))
                        || (hints != null && hints.length > 1 && hints[1] != null && hints[1].equals(p))
                        || (hintA != null && hintA.equals(p))
                        || (hintB != null && hintB.equals(p))) {
                    g2.setColor(new Color(46, 204, 113));
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawRect(x + 4, y + 4, cellW - 8, cellH - 8);
                }
            }
        }
    }

    /**
     * 绘制消除路径连线。
     * @param g2 图形上下文
     */
    private void drawPath(Graphics2D g2) {
        GameSession currentSession = controller.getSession();
        if (currentSession == null) {
            return;
        }
        Path path = currentSession.lastPath();
        if (path == null) {
            return;
        }
        List<Position> points = path.points();
        GameBoard board = currentSession.board();
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

    private String resolveIconKey(Constants.Difficulty difficulty, int patternId) {
        if (difficulty == Constants.Difficulty.EASY) {
            int idx = ((patternId - 1) % 4) + 1;
            return "icon" + idx;
        }
        int idx = ((patternId - 1) % 12) + 1;
        if (idx <= 6) {
            return "icon" + idx;
        }
        return "icon" + (idx - 6) + "_2";
    }
}
