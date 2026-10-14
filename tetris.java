import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class tetris extends JFrame {

    static final Color BG = new Color(0x1a0b2e);
    static final Color PANEL = new Color(0x2a1650);
    static final Color WELL = new Color(0x140824);
    static final Color GRID = new Color(0x24123f);
    static final Color FRAME = new Color(0x4c1d95);
    static final Color TEXT = new Color(0xf3e8ff);
    static final Color MUTED = new Color(0xa78bca);
    static final Color ACCENT = new Color(0xc084fc);
    static final Color BTN = new Color(0x7c3aed);
    static final Color BTN_HOVER = new Color(0x9b6bff);

    static final Color[] PIECE_COLORS = {
            new Color(0xa5b4fc),
            new Color(0xf5d0fe),
            new Color(0xa855f7),
            new Color(0xd946ef),
            new Color(0xf472b6),
            new Color(0x7c3aed),
            new Color(0xc084fc),
    };

    static final int[][][] SHAPES = {
            {{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}},
            {{1,1},{1,1}},
            {{0,1,0},{1,1,1},{0,0,0}},
            {{0,1,1},{1,1,0},{0,0,0}},
            {{1,1,0},{0,1,1},{0,0,0}},
            {{1,0,0},{1,1,1},{0,0,0}},
            {{0,0,1},{1,1,1},{0,0,0}},
    };

    static final int COLS = 10, ROWS = 20;
    static final int CELL = 24;
    static final int WELL_W = COLS * CELL, WELL_H = ROWS * CELL;
    static final int MARGIN = 12;
    static final int SIDE_W = 112;
    static final int W = MARGIN + WELL_W + MARGIN + SIDE_W + MARGIN;
    static final int H = MARGIN + WELL_H + MARGIN;
    static final String FONT = "SansSerif";

    public tetris() {
        super("Tetris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        Game game = new Game();
        setContentPane(game);
        pack();
        setLocationRelativeTo(null);
        addWindowFocusListener(new WindowAdapter() {
            public void windowLostFocus(WindowEvent e) { game.autoPause(); }
            public void windowGainedFocus(WindowEvent e) { game.requestFocusInWindow(); }
        });
    }

    static class Piece {
        final int type;
        int[][] m;
        int x, y;

        Piece(int type) {
            this.type = type;
            int[][] s = SHAPES[type];
            m = new int[s.length][];
            for (int i = 0; i < s.length; i++) m[i] = s[i].clone();
            x = (COLS - m.length) / 2;
            y = type == 0 ? -1 : 0;
        }

        int[][] rotated(boolean cw) {
            int n = m.length;
            int[][] r = new int[n][n];
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    if (cw) r[j][n - 1 - i] = m[i][j];
                    else r[n - 1 - j][i] = m[i][j];
            return r;
        }
    }

    static class Game extends JPanel {
        enum State { MENU, PLAYING, PAUSED, OVER }

        final int[][] board = new int[ROWS][COLS];
        final Random rng = new Random();
        final List<Integer> bag = new ArrayList<>();
        final Deque<Integer> queue = new ArrayDeque<>();
        Piece cur;
        int hold = -1;
        boolean holdUsed = false;
        int score = 0, lines = 0, level = 1, best = 0;
        boolean newBest = false;
        State state = State.MENU;

        long lastTick, dropAcc = 0, lockAcc = 0;
        int lockResets = 0;
        boolean softDrop = false;

        List<Integer> clearing = new ArrayList<>();
        long clearStart = 0;
        static final int CLEAR_MS = 180;

        final Rectangle button = new Rectangle();
        boolean hoverBtn = false;
        final Preferences prefs = tryPrefs();

        final javax.swing.Timer loop = new javax.swing.Timer(16, e -> step());

        Game() {
            setPreferredSize(new Dimension(W, H));
            setBackground(BG);
            setFocusable(true);
            best = prefs != null ? prefs.getInt("best", 0) : 0;

            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { onKey(e); }
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) softDrop = false;
                }
            });
            MouseAdapter mouse = new MouseAdapter() {
                public void mouseMoved(MouseEvent e) {
                    boolean h = state != State.PLAYING && button.contains(e.getPoint());
                    if (h != hoverBtn) { hoverBtn = h; repaint(); }
                    setCursor(Cursor.getPredefinedCursor(h ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
                public void mouseClicked(MouseEvent e) {
                    requestFocusInWindow();
                    if (state != State.PLAYING && button.contains(e.getPoint())) primaryAction();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            loop.start();
        }

        static Preferences tryPrefs() {
            try { return Preferences.userRoot().node("tetris-diva"); }
            catch (Exception e) { return null; }
        }

        void primaryAction() {
            if (state == State.PAUSED) resume();
            else startGame();
        }

        void startGame() {
            for (int[] row : board) Arrays.fill(row, 0);
            bag.clear();
            queue.clear();
            clearing.clear();
            score = 0; lines = 0; level = 1;
            hold = -1; holdUsed = false; newBest = false; softDrop = false;
            refillQueue();
            state = State.PLAYING;
            spawn();
            lastTick = System.currentTimeMillis();
            repaint();
        }

        void autoPause() {
            if (state == State.PLAYING) { state = State.PAUSED; softDrop = false; repaint(); }
        }

        void resume() {
            state = State.PLAYING;
            lastTick = System.currentTimeMillis();
            repaint();
        }

        int nextFromBag() {
            if (bag.isEmpty()) {
                for (int i = 0; i < 7; i++) bag.add(i);
                Collections.shuffle(bag, rng);
            }
            return bag.remove(bag.size() - 1);
        }

        void refillQueue() {
            while (queue.size() < 3) queue.addLast(nextFromBag());
        }

        void spawn() { spawn(queue.pollFirst()); }

        void spawn(int type) {
            refillQueue();
            cur = new Piece(type);
            dropAcc = 0; lockAcc = 0; lockResets = 0;
            if (!fits(cur.m, cur.x, cur.y)) gameOver();
        }

        void gameOver() {
            state = State.OVER;
            softDrop = false;
            if (score > best) {
                best = score;
                newBest = true;
                if (prefs != null) try { prefs.putInt("best", best); } catch (Exception ignored) {}
            }
            repaint();
        }

        boolean fits(int[][] m, int px, int py) {
            for (int i = 0; i < m.length; i++)
                for (int j = 0; j < m.length; j++) {
                    if (m[i][j] == 0) continue;
                    int x = px + j, y = py + i;
                    if (x < 0 || x >= COLS || y >= ROWS) return false;
                    if (y >= 0 && board[y][x] != 0) return false;
                }
            return true;
        }

        boolean onGround() { return !fits(cur.m, cur.x, cur.y + 1); }

        void afterMove() {
            if (onGround() && lockResets < 15) { lockAcc = 0; lockResets++; }
        }

        boolean move(int dx) {
            if (fits(cur.m, cur.x + dx, cur.y)) { cur.x += dx; afterMove(); return true; }
            return false;
        }

        void rotate(boolean cw) {
            if (cur.type == 1) return;
            int[][] r = cur.rotated(cw);
            int[][] kicks = {{0,0},{-1,0},{1,0},{0,-1},{-2,0},{2,0},{-1,-1},{1,-1}};
            for (int[] k : kicks) {
                if (fits(r, cur.x + k[0], cur.y + k[1])) {
                    cur.m = r; cur.x += k[0]; cur.y += k[1];
                    afterMove();
                    return;
                }
            }
        }

        void hardDrop() {
            int d = 0;
            while (fits(cur.m, cur.x, cur.y + 1)) { cur.y++; d++; }
            score += d * 2;
            lock();
        }

        void holdPiece() {
            if (holdUsed) return;
            int t = cur.type;
            if (hold < 0) { hold = t; spawn(); }
            else { int h = hold; hold = t; spawn(h); }
            holdUsed = true;
        }

        void lock() {
            boolean above = false;
            for (int i = 0; i < cur.m.length; i++)
                for (int j = 0; j < cur.m.length; j++)
                    if (cur.m[i][j] != 0) {
                        int y = cur.y + i;
                        if (y < 0) above = true;
                        else board[y][cur.x + j] = cur.type + 1;
                    }
            if (above) { gameOver(); return; }

            clearing.clear();
            for (int y = 0; y < ROWS; y++) {
                boolean full = true;
                for (int x = 0; x < COLS; x++) if (board[y][x] == 0) { full = false; break; }
                if (full) clearing.add(y);
            }
            holdUsed = false;
            if (!clearing.isEmpty()) {
                clearStart = System.currentTimeMillis();
                cur = null;
            } else {
                spawn();
            }
        }

        void finishClear() {
            int n = clearing.size();
            for (int y : clearing) {
                for (int r = y; r > 0; r--) board[r] = board[r - 1].clone();
                board[0] = new int[COLS];
            }
            clearing.clear();
            score += new int[]{0, 100, 300, 500, 800}[n] * level;
            lines += n;
            level = lines / 10 + 1;
            spawn();
        }

        long dropInterval() {
            long base = Math.max(60, 800 - (level - 1) * 75L);
            return softDrop ? Math.min(base, 45) : base;
        }

        void step() {
            if (state != State.PLAYING) return;
            long now = System.currentTimeMillis();
            long dt = Math.min(now - lastTick, 100);
            lastTick = now;

            if (!clearing.isEmpty()) {
                if (now - clearStart >= CLEAR_MS) finishClear();
                repaint();
                return;
            }
            if (cur == null) return;

            if (onGround()) {
                lockAcc += dt;
                if (lockAcc >= 450) lock();
            } else {
                dropAcc += dt;
                long iv = dropInterval();
                while (dropAcc >= iv && !onGround()) {
                    dropAcc -= iv;
                    cur.y++;
                    if (softDrop) score += 1;
                }
                if (onGround()) dropAcc = 0;
            }
            repaint();
        }

        void onKey(KeyEvent e) {
            int k = e.getKeyCode();
            if (state != State.PLAYING) {
                if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE
                        || (state == State.PAUSED && (k == KeyEvent.VK_P || k == KeyEvent.VK_ESCAPE)))
                    primaryAction();
                return;
            }
            if (k == KeyEvent.VK_P || k == KeyEvent.VK_ESCAPE) { autoPause(); return; }
            if (cur == null) return;
            switch (k) {
                case KeyEvent.VK_LEFT: move(-1); break;
                case KeyEvent.VK_RIGHT: move(1); break;
                case KeyEvent.VK_DOWN: softDrop = true; break;
                case KeyEvent.VK_UP: case KeyEvent.VK_X: rotate(true); break;
                case KeyEvent.VK_Z: rotate(false); break;
                case KeyEvent.VK_SPACE: hardDrop(); break;
                case KeyEvent.VK_C: case KeyEvent.VK_SHIFT: holdPiece(); break;
                default: return;
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int ox = MARGIN, oy = MARGIN;

            // poço
            g.setColor(FRAME);
            g.fill(new RoundRectangle2D.Float(ox - 4, oy - 4, WELL_W + 8, WELL_H + 8, 14, 14));
            g.setColor(WELL);
            g.fillRect(ox, oy, WELL_W, WELL_H);
            g.setColor(GRID);
            for (int x = 1; x < COLS; x++) g.drawLine(ox + x * CELL, oy, ox + x * CELL, oy + WELL_H - 1);
            for (int y = 1; y < ROWS; y++) g.drawLine(ox, oy + y * CELL, ox + WELL_W - 1, oy + y * CELL);

            float flash = clearing.isEmpty() ? 0 :
                    Math.min(1f, (System.currentTimeMillis() - clearStart) / (float) CLEAR_MS);
            for (int y = 0; y < ROWS; y++)
                for (int x = 0; x < COLS; x++)
                    if (board[y][x] != 0) {
                        if (clearing.contains(y)) {
                            g.setColor(new Color(1f, 1f, 1f, 1f - flash * 0.9f));
                            g.fillRoundRect(ox + x * CELL + 1, oy + y * CELL + 1, CELL - 2, CELL - 2, 6, 6);
                        } else {
                            block(g, ox + x * CELL, oy + y * CELL, CELL, PIECE_COLORS[board[y][x] - 1], 1f);
                        }
                    }

            if (cur != null && state != State.MENU) {
                int gy = cur.y;
                while (fits(cur.m, cur.x, gy + 1)) gy++;
                Color c = PIECE_COLORS[cur.type];
                for (int i = 0; i < cur.m.length; i++)
                    for (int j = 0; j < cur.m.length; j++)
                        if (cur.m[i][j] != 0) {
                            int px = ox + (cur.x + j) * CELL;
                            if (gy + i >= 0) ghost(g, px, oy + (gy + i) * CELL, c);
                        }
                for (int i = 0; i < cur.m.length; i++)
                    for (int j = 0; j < cur.m.length; j++)
                        if (cur.m[i][j] != 0 && cur.y + i >= 0)
                            block(g, ox + (cur.x + j) * CELL, oy + (cur.y + i) * CELL, CELL, c, 1f);
            }

            int sx = ox + WELL_W + MARGIN + 4, sy = oy - 4;
            g.setFont(new Font(FONT, Font.BOLD, 17));
            g.setColor(ACCENT);
            g.drawString("Tetris", sx + 2, sy + 18);
            g.setColor(TEXT);
            g.drawString("Diva", sx + 2 + g.getFontMetrics().stringWidth("Tetris "), sy + 18);

            sy += 30;
            sy = previewBox(g, "PRÓXIMAS", sx, sy, queue.isEmpty() ? null : new ArrayList<>(queue), 3);
            sy += 8;
            sy = previewBox(g, "GUARDADA (C)", sx, sy, hold >= 0 ? List.of(hold) : null, 1);
            sy += 12;

            sy = stat(g, "PONTOS", String.format("%,d", score).replace(',', '.'), sx, sy);
            sy = stat(g, "LINHAS", String.valueOf(lines), sx, sy);
            sy = stat(g, "NÍVEL", String.valueOf(level), sx, sy);
            stat(g, "RECORDE ♛", String.format("%,d", best).replace(',', '.'), sx, sy);

            if (state == State.MENU) overlay(g, "Bora jogar, diva?", null, "Começar");
            else if (state == State.PAUSED) overlay(g, "Pausado", "respira e volta ✨", "Continuar");
            else if (state == State.OVER)
                overlay(g, newBest ? "NOVO RECORDE! ♛" : "Game over",
                        "Pontos: " + String.format("%,d", score).replace(',', '.')
                                + "   Linhas: " + lines, "Jogar de novo");
            g.dispose();
        }

        int previewBox(Graphics2D g, String title, int x, int y, List<Integer> types, int slots) {
            g.setFont(new Font(FONT, Font.BOLD, 10));
            g.setColor(MUTED);
            g.drawString(title, x + 2, y + 10);
            y += 15;
            int mini = 14, slotH = mini * 2 + 10;
            int h = slotH * slots + 8;
            g.setColor(PANEL);
            g.fillRoundRect(x, y, SIDE_W - 4, h, 12, 12);
            if (types != null) {
                for (int s = 0; s < Math.min(slots, types.size()); s++) {
                    int t = types.get(s);
                    int[][] m = SHAPES[t];
                    int minR = 9, maxR = -1, minC = 9, maxC = -1;
                    for (int i = 0; i < m.length; i++)
                        for (int j = 0; j < m.length; j++)
                            if (m[i][j] != 0) {
                                minR = Math.min(minR, i); maxR = Math.max(maxR, i);
                                minC = Math.min(minC, j); maxC = Math.max(maxC, j);
                            }
                    int pw = (maxC - minC + 1) * mini, ph = (maxR - minR + 1) * mini;
                    int bx = x + (SIDE_W - 4 - pw) / 2, by = y + 4 + s * slotH + (slotH - ph) / 2;
                    float alpha = s == 0 ? 1f : 0.7f;
                    for (int i = minR; i <= maxR; i++)
                        for (int j = minC; j <= maxC; j++)
                            if (m[i][j] != 0)
                                block(g, bx + (j - minC) * mini, by + (i - minR) * mini, mini, PIECE_COLORS[t], alpha);
                }
            }
            return y + h;
        }

        int stat(Graphics2D g, String label, String value, int x, int y) {
            g.setFont(new Font(FONT, Font.BOLD, 10));
            g.setColor(MUTED);
            g.drawString(label, x + 2, y + 10);
            g.setFont(new Font(FONT, Font.BOLD, 17));
            g.setColor(TEXT);
            g.drawString(value, x + 2, y + 29);
            return y + 38;
        }

        static void block(Graphics2D g, int x, int y, int s, Color c, float alpha) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(c.darker());
            g.fillRoundRect(x + 1, y + 1, s - 2, s - 2, 6, 6);
            g.setColor(c);
            g.fillRoundRect(x + 1, y + 1, s - 2, s - 4, 6, 6);
            g.setColor(new Color(255, 255, 255, 70));
            g.fillRoundRect(x + 3, y + 3, s - 6, Math.max(2, s / 4), 4, 4);
            g.setComposite(old);
        }

        static void ghost(Graphics2D g, int x, int y, Color c) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
            g.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 6, 6);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 210));
            g.setStroke(new BasicStroke(1.8f));
            g.drawRoundRect(x + 2, y + 2, CELL - 4, CELL - 4, 6, 6);
        }

        void overlay(Graphics2D g, String title, String sub, String btn) {
            int ox = MARGIN, oy = MARGIN;
            g.setColor(new Color(20, 8, 36, 205));
            g.fillRect(ox, oy, WELL_W, WELL_H);

            int cw = WELL_W - 30, ch = sub == null ? 120 : 140;
            int cx = ox + 15, cy = oy + (WELL_H - ch) / 2;
            g.setColor(PANEL);
            g.fillRoundRect(cx, cy, cw, ch, 18, 18);
            g.setColor(FRAME);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(cx, cy, cw, ch, 18, 18);

            g.setFont(new Font(FONT, Font.BOLD, 18));
            g.setColor(ACCENT);
            centered(g, title, cx, cw, cy + 34);
            int by = cy + 52;
            if (sub != null) {
                g.setFont(new Font(FONT, Font.PLAIN, 12));
                g.setColor(TEXT);
                centered(g, sub, cx, cw, cy + 58);
                by = cy + 74;
            }
            int bw = 150, bh = 38;
            button.setBounds(cx + (cw - bw) / 2, by, bw, bh);
            g.setColor(hoverBtn ? BTN_HOVER : BTN);
            g.fillRoundRect(button.x, button.y, bw, bh, 14, 14);
            g.setFont(new Font(FONT, Font.BOLD, 14));
            g.setColor(TEXT);
            centered(g, btn, button.x, bw, button.y + 24);

            if (state == State.MENU) {
                g.setFont(new Font(FONT, Font.PLAIN, 11));
                g.setColor(MUTED);
                String[] help = {"← →  mover     ↓  descer", "↑ / X  girar     Espaço  derrubar",
                        "C  guardar     P  pausar"};
                int hy = cy + ch + 26;
                for (String h : help) { centered(g, h, ox, WELL_W, hy); hy += 17; }
            }
        }

        static void centered(Graphics2D g, String s, int x, int w, int baseline) {
            g.drawString(s, x + (w - g.getFontMetrics().stringWidth(s)) / 2, baseline);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            tetris t = new tetris();
            t.setVisible(true);
            t.getContentPane().requestFocusInWindow();
        });
    }
}