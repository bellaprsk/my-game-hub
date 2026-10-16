import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class game2048 extends JFrame {

    static final Color BG = new Color(0x1a0b2e);
    static final Color BOARD = new Color(0x2a1650);
    static final Color EMPTY = new Color(0x3a2366);
    static final Color TEXT = new Color(0xf3e8ff);
    static final Color MUTED = new Color(0xa78bca);
    static final Color ACCENT = new Color(0xc084fc);
    static final Color BTN = new Color(0x7c3aed);
    static final Color DIM = new Color(0x2c2140);
    static final String FONT = "SansSerif";

    static final int N = 4, CELL = 90, GAP = 12;
    static final int BOARD_PX = N * CELL + (N + 1) * GAP;
    static final int PAD = 18;
    static final int W = BOARD_PX + PAD * 2;
    static final int ANIM_MS = 110, POP_MS = 140;

    static Color tileColor(int v) {
        switch (v) {
            case 2: return new Color(0xf3e8ff);
            case 4: return new Color(0xe9d5ff);
            case 8: return new Color(0xd8b4fe);
            case 16: return new Color(0xc084fc);
            case 32: return new Color(0xa855f7);
            case 64: return new Color(0x9333ea);
            case 128: return new Color(0x7e22ce);
            case 256: return new Color(0x6b21a8);
            case 512: return new Color(0xf472b6);
            case 1024: return new Color(0xdb2777);
            case 2048: return new Color(0xec4899);
            default: return new Color(0x4c1d95);
        }
    }

    static class Tile {
        int v, r, c, pr, pc;
        boolean born, popped;
        Tile(int v, int r, int c) { this.v = v; this.r = r; this.c = c; pr = r; pc = c; }
    }

    final Preferences prefs = tryPrefs();
    final Random rng = new Random();
    Tile[][] grid = new Tile[N][N];
    final List<Tile> tiles = new ArrayList<>(), dying = new ArrayList<>();
    final Deque<int[]> history = new ArrayDeque<>();
    int score = 0, best = 0;
    boolean won = false, keepGoing = false, over = false;
    long animStart = 0;

    final Board board = new Board();
    final ScoreBox scoreBox = new ScoreBox("PONTOS"), bestBox = new ScoreBox("RECORDE");
    final Overlay overlay = new Overlay();
    final javax.swing.Timer anim = new javax.swing.Timer(15, e -> {
        board.repaint();
        if (System.currentTimeMillis() - animStart > ANIM_MS + POP_MS) ((javax.swing.Timer) e.getSource()).stop();
    });

    public game2048() {
        super("2048");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        best = prefs != null ? prefs.getInt("best", 0) : 0;

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, PAD, PAD, PAD));
        setContentPane(root);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("<html><span style='color:#c084fc'>20</span>48</html>");
        title.setFont(new Font(FONT, Font.BOLD, 44));
        title.setForeground(TEXT);
        head.add(title, BorderLayout.WEST);
        JPanel boxes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        boxes.setOpaque(false);
        boxes.add(scoreBox);
        boxes.add(bestBox);
        head.add(boxes, BorderLayout.EAST);

        JPanel sub = new JPanel(new BorderLayout());
        sub.setOpaque(false);
        JLabel tip = new JLabel("Junte os números e chegue no 2048, diva!");
        tip.setFont(new Font(FONT, Font.PLAIN, 13));
        tip.setForeground(MUTED);
        sub.add(tip, BorderLayout.WEST);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        btns.add(new Pill("Desfazer", DIM, 90, 32, this::undo));
        btns.add(new Pill("Novo jogo", BTN, 96, 32, this::newGame));
        sub.add(btns, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(head, BorderLayout.NORTH);
        top.add(sub, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);
        root.add(board, BorderLayout.CENTER);

        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        int[][] keys = {{KeyEvent.VK_UP, 0}, {KeyEvent.VK_W, 0}, {KeyEvent.VK_DOWN, 1}, {KeyEvent.VK_S, 1},
                {KeyEvent.VK_LEFT, 2}, {KeyEvent.VK_A, 2}, {KeyEvent.VK_RIGHT, 3}, {KeyEvent.VK_D, 3}};
        for (int[] k : keys) {
            String name = "move" + k[1];
            im.put(KeyStroke.getKeyStroke(k[0], 0), name);
            final int dir = k[1];
            am.put(name, new AbstractAction() { public void actionPerformed(ActionEvent e) { if (!overlay.isVisible()) move(dir); } });
        }
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0), "undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "undo");
        am.put("undo", new AbstractAction() { public void actionPerformed(ActionEvent e) { if (!overlay.isVisible()) undo(); } });

        setGlassPane(overlay);
        pack();
        setLocationRelativeTo(null);
        newGame();
    }

    static Preferences tryPrefs() {
        try { return Preferences.userRoot().node("game2048-diva"); } catch (Exception e) { return null; }
    }

    void newGame() {
        grid = new Tile[N][N];
        tiles.clear(); dying.clear(); history.clear();
        score = 0; won = false; keepGoing = false; over = false;
        spawn(); spawn();
        overlay.setVisible(false);
        startAnim();
        updateBoxes();
    }

    void spawn() {
        List<int[]> free = new ArrayList<>();
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) if (grid[r][c] == null) free.add(new int[]{r, c});
        if (free.isEmpty()) return;
        int[] p = free.get(rng.nextInt(free.size()));
        Tile t = new Tile(rng.nextInt(10) == 0 ? 4 : 2, p[0], p[1]);
        t.born = true;
        grid[p[0]][p[1]] = t;
        tiles.add(t);
    }

    int[] snapshot() {
        int[] s = new int[N * N + 1];
        s[0] = score;
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) s[1 + r * N + c] = grid[r][c] == null ? 0 : grid[r][c].v;
        return s;
    }

    void restore(int[] s) {
        grid = new Tile[N][N];
        tiles.clear(); dying.clear();
        score = s[0];
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            int v = s[1 + r * N + c];
            if (v > 0) { Tile t = new Tile(v, r, c); grid[r][c] = t; tiles.add(t); }
        }
    }

    void undo() {
        if (history.isEmpty()) return;
        restore(history.pop());
        over = false;
        overlay.setVisible(false);
        updateBoxes();
        board.repaint();
    }

    void move(int dir) {
        if (over || (won && !keepGoing)) return;
        int dr = dir == 0 ? -1 : dir == 1 ? 1 : 0, dc = dir == 2 ? -1 : dir == 3 ? 1 : 0;
        int[] before = snapshot();
        dying.clear();
        for (Tile t : tiles) { t.pr = t.r; t.pc = t.c; t.born = false; t.popped = false; }
        boolean[][] merged = new boolean[N][N];
        boolean moved = false;
        int[] order = {0, 1, 2, 3};
        if (dr == 1 || dc == 1) order = new int[]{3, 2, 1, 0};
        for (int a : order) for (int b : order) {
            int r = dr != 0 ? a : b, c = dc != 0 ? a : b;
            if (dr == 0) { r = b; c = a; }
            Tile t = grid[r][c];
            if (t == null) continue;
            int nr = r, nc = c;
            while (true) {
                int tr = nr + dr, tc = nc + dc;
                if (tr < 0 || tr >= N || tc < 0 || tc >= N || grid[tr][tc] != null) break;
                nr = tr; nc = tc;
            }
            int tr = nr + dr, tc = nc + dc;
            if (tr >= 0 && tr < N && tc >= 0 && tc < N && grid[tr][tc] != null && grid[tr][tc].v == t.v && !merged[tr][tc]) {
                Tile other = grid[tr][tc];
                grid[r][c] = null;
                t.r = tr; t.c = tc;
                dying.add(t); dying.add(other);
                tiles.remove(t); tiles.remove(other);
                Tile m = new Tile(t.v * 2, tr, tc);
                m.popped = true;
                grid[tr][tc] = m;
                tiles.add(m);
                merged[tr][tc] = true;
                score += m.v;
                if (m.v == 2048 && !won) won = true;
                moved = true;
            } else if (nr != r || nc != c) {
                grid[r][c] = null;
                grid[nr][nc] = t;
                t.r = nr; t.c = nc;
                moved = true;
            }
        }
        if (!moved) return;
        history.push(before);
        while (history.size() > 30) history.removeLast();
        spawn();
        if (score > best) { best = score; if (prefs != null) try { prefs.putInt("best", best); } catch (Exception ignored) {} }
        updateBoxes();
        startAnim();
        if (won && !keepGoing) later(ANIM_MS + 250, () -> overlay.showWin());
        else if (!canMove()) { over = true; later(ANIM_MS + 350, () -> overlay.showLose()); }
    }

    boolean canMove() {
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            if (grid[r][c] == null) return true;
            int v = grid[r][c].v;
            if (r + 1 < N && grid[r + 1][c] != null && grid[r + 1][c].v == v) return true;
            if (c + 1 < N && grid[r][c + 1] != null && grid[r][c + 1].v == v) return true;
        }
        return false;
    }

    void later(int ms, Runnable r) {
        javax.swing.Timer t = new javax.swing.Timer(ms, e -> r.run());
        t.setRepeats(false);
        t.start();
    }

    void startAnim() { animStart = System.currentTimeMillis(); anim.restart(); }

    void updateBoxes() {
        scoreBox.value = score; bestBox.value = best;
        scoreBox.repaint(); bestBox.repaint();
    }

    class Board extends JComponent {
        int sx, sy;
        Board() {
            setPreferredSize(new Dimension(BOARD_PX, BOARD_PX));
            MouseAdapter m = new MouseAdapter() {
                public void mousePressed(MouseEvent e) { sx = e.getX(); sy = e.getY(); requestFocusInWindow(); }
                public void mouseReleased(MouseEvent e) {
                    int dx = e.getX() - sx, dy = e.getY() - sy;
                    if (Math.max(Math.abs(dx), Math.abs(dy)) < 30 || overlay.isVisible()) return;
                    if (Math.abs(dx) > Math.abs(dy)) game2048.this.move(dx > 0 ? 3 : 2); else game2048.this.move(dy > 0 ? 1 : 0);
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        int px(int c) { return GAP + c * (CELL + GAP); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int ox = (getWidth() - BOARD_PX) / 2;
            g.translate(ox, 0);
            g.setColor(BOARD);
            g.fillRoundRect(0, 0, BOARD_PX, BOARD_PX, 18, 18);
            for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
                g.setColor(EMPTY);
                g.fillRoundRect(px(c), px(r), CELL, CELL, 12, 12);
            }
            long t = System.currentTimeMillis() - animStart;
            double slide = Math.min(1, t / (double) ANIM_MS);
            slide = 1 - Math.pow(1 - slide, 3);
            if (t < ANIM_MS) for (Tile d : dying) draw(g, d, slide, 1);
            for (Tile tile : tiles) {
                double scale = 1;
                if (tile.born || tile.popped) {
                    if (t < ANIM_MS) continue;
                    double p = Math.min(1, (t - ANIM_MS) / (double) POP_MS);
                    scale = tile.born ? 0.3 + 0.7 * p : 1 + 0.18 * Math.sin(p * Math.PI);
                }
                draw(g, tile, slide, scale);
            }
            g.dispose();
        }

        void draw(Graphics2D g, Tile t, double p, double scale) {
            double x = px(t.pc) + (px(t.c) - px(t.pc)) * p, y = px(t.pr) + (px(t.r) - px(t.pr)) * p;
            if (t.born || t.popped) { x = px(t.c); y = px(t.r); }
            double s = CELL * scale, off = (CELL - s) / 2;
            Color col = tileColor(t.v);
            if (t.v >= 1024) {
                g.setColor(new Color(236, 72, 153, 70));
                g.fillRoundRect((int) (x + off) - 4, (int) (y + off) - 4, (int) s + 8, (int) s + 8, 18, 18);
            }
            g.setColor(col);
            g.fillRoundRect((int) (x + off), (int) (y + off), (int) s, (int) s, 12, 12);
            String txt = String.valueOf(t.v);
            int size = txt.length() <= 2 ? 40 : txt.length() == 3 ? 34 : txt.length() == 4 ? 27 : 22;
            g.setFont(new Font(FONT, Font.BOLD, (int) (size * scale)));
            g.setColor(t.v <= 8 ? new Color(0x4c1d95) : Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(txt, (int) (x + CELL / 2.0 - fm.stringWidth(txt) / 2.0), (int) (y + CELL / 2.0 + (fm.getAscent() - fm.getDescent()) / 2.0));
        }
    }

    class ScoreBox extends JComponent {
        final String label; int value;
        ScoreBox(String label) { this.label = label; setPreferredSize(new Dimension(92, 54)); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(BOARD);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.setFont(new Font(FONT, Font.BOLD, 10));
            g.setColor(MUTED);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, (getWidth() - fm.stringWidth(label)) / 2, 18);
            String v = String.valueOf(value);
            g.setFont(new Font(FONT, Font.BOLD, v.length() > 6 ? 16 : 20));
            g.setColor(TEXT);
            fm = g.getFontMetrics();
            g.drawString(v, (getWidth() - fm.stringWidth(v)) / 2, 42);
            g.dispose();
        }
    }

    static class Pill extends JComponent {
        final String text; final Color base; boolean hover;
        Pill(String text, Color base, int w, int h, Runnable action) {
            this.text = text; this.base = base;
            Dimension d = new Dimension(w, h);
            setPreferredSize(d); setMaximumSize(d); setMinimumSize(d);
            setAlignmentX(CENTER_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint())) action.run(); }
            });
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color c = base;
            if (hover) c = new Color(Math.min(255, c.getRed() + 25), Math.min(255, c.getGreen() + 25), Math.min(255, c.getBlue() + 25));
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g.setColor(TEXT);
            g.setFont(new Font(FONT, Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    class Overlay extends JPanel {
        final JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(BOARD);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g.setColor(new Color(0x4c1d95));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
                g.dispose();
            }
        };
        Overlay() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
            add(card);
            addMouseListener(new MouseAdapter() {});
        }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(20, 8, 36, 200));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        JLabel lbl(String t, int size, Color c, boolean bold) {
            JLabel l = new JLabel("<html><div style='text-align:center'>" + t + "</div></html>");
            l.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size));
            l.setForeground(c);
            l.setAlignmentX(CENTER_ALIGNMENT);
            return l;
        }
        void put(JComponent c, int gap) { c.setAlignmentX(CENTER_ALIGNMENT); card.add(c); if (gap > 0) card.add(Box.createVerticalStrut(gap)); }
        void showWin() {
            card.removeAll();
            put(lbl("2048! SLAY ♛", 30, new Color(0xf472b6), true), 6);
            put(lbl(score + " pontos  ·  você é icônica", 13, TEXT, false), 16);
            put(new Pill("Continuar jogando", BTN, 220, 40, () -> { keepGoing = true; setVisible(false); }), 8);
            put(new Pill("Novo jogo", DIM, 220, 36, game2048.this::newGame), 0);
            open();
        }
        void showLose() {
            card.removeAll();
            put(lbl("Sem jogadas!", 28, ACCENT, true), 6);
            put(lbl(score + " pontos" + (score >= best && score > 0 ? "  ·  NOVO RECORDE ♛" : "  ·  recorde " + best), 13, TEXT, false), 16);
            put(new Pill("Jogar de novo", BTN, 220, 40, game2048.this::newGame), 8);
            put(new Pill("Desfazer a última", DIM, 220, 36, game2048.this::undo), 0);
            open();
        }
        void open() { card.revalidate(); setVisible(true); revalidate(); repaint(); }
    }

    public void hubStart() { getContentPane().requestFocusInWindow(); }
    public void hubResume() { getContentPane().requestFocusInWindow(); }
    public void hubStop() { anim.stop(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new game2048().setVisible(true));
    }
}