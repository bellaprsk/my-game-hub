import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class campominado extends JFrame {

    static final Color BG = new Color(0x1a0b2e);
    static final Color PANEL = new Color(0x2a1650);
    static final Color HIDDEN_1 = new Color(0x8b5cf6);
    static final Color HIDDEN_2 = new Color(0x7c3aed);
    static final Color OPEN = new Color(0xf1e9fc);
    static final Color OPEN_LINE = new Color(0xdccdf3);
    static final Color TEXT = new Color(0xf3e8ff);
    static final Color MUTED = new Color(0xa78bca);
    static final Color ACCENT = new Color(0xc084fc);
    static final Color PINK = new Color(0xec4899);
    static final Color BTN = new Color(0x7c3aed);
    static final Color DIM = new Color(0x2c2140);
    static final Color[] NUM = {null, new Color(0x7c3aed), new Color(0xdb2777), new Color(0x6d28d9), new Color(0x4c1d95),
            new Color(0xbe185d), new Color(0xa21caf), new Color(0x1e1b4b), new Color(0x6b5b8a)};
    static final String FONT = "SansSerif";

    static final String[] LEVELS = {"Fácil", "Médio", "Difícil"};
    static final int[][] SIZES = {{9, 9, 10}, {16, 16, 40}, {16, 30, 99}};
    static final Color[] LEVEL_COLORS = {new Color(0xa78bfa), new Color(0x8b5cf6), new Color(0x6d28d9)};
    static final int AREA_W = 660, AREA_H = 400;

    final Preferences prefs = tryPrefs();
    final Random rng = new Random();
    int level = 0, rows, cols, mines;
    boolean[][] mine, open, flag;
    int[][] count;
    boolean started, over, won;
    int exR = -1, exC = -1, opened, flags, seconds;
    final int[] best = new int[3];

    final Board board = new Board();
    final JLabel minesLbl = new JLabel(), timeLbl = new JLabel(), levelLbl = new JLabel();
    final Overlay overlay = new Overlay();
    final javax.swing.Timer clock = new javax.swing.Timer(1000, e -> { seconds++; hud(); });

    public campominado() {
        super("Campo Minado");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        for (int i = 0; i < 3; i++) best[i] = prefs != null ? prefs.getInt("best" + i, 0) : 0;

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(14, 18, 18, 18));
        setContentPane(root);

        JPanel hud = new JPanel(new BorderLayout());
        hud.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("<html><span style='color:#c084fc'>Campo</span> Minado</html>");
        title.setFont(new Font(FONT, Font.BOLD, 22));
        title.setForeground(TEXT);
        left.add(title);
        left.add(Box.createHorizontalStrut(14));
        levelLbl.setFont(new Font(FONT, Font.BOLD, 12));
        levelLbl.setForeground(MUTED);
        left.add(levelLbl);
        hud.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        for (JLabel l : new JLabel[]{minesLbl, timeLbl}) {
            l.setFont(new Font(FONT, Font.BOLD, 15));
            l.setForeground(TEXT);
            right.add(l);
        }
        right.add(Box.createHorizontalStrut(6));
        right.add(new Pill("Novo jogo", BTN, 96, 32, () -> overlay.showStart()));
        hud.add(right, BorderLayout.EAST);
        root.add(hud, BorderLayout.NORTH);
        root.add(board, BorderLayout.CENTER);
        JLabel tip = new JLabel("Clique = abrir  ·  Clique direito ou Shift+clique = bandeira  ·  Clique no número = abre os vizinhos");
        tip.setFont(new Font(FONT, Font.PLAIN, 11));
        tip.setForeground(new Color(0x7b6a9b));
        tip.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(tip, BorderLayout.SOUTH);

        setGlassPane(overlay);
        newGame(0);
        pack();
        setLocationRelativeTo(null);
        overlay.showStart();
    }

    static Preferences tryPrefs() {
        try { return Preferences.userRoot().node("campominado-diva"); } catch (Exception e) { return null; }
    }

    void newGame(int lvl) {
        level = lvl;
        rows = SIZES[lvl][0]; cols = SIZES[lvl][1]; mines = SIZES[lvl][2];
        mine = new boolean[rows][cols]; open = new boolean[rows][cols]; flag = new boolean[rows][cols];
        count = new int[rows][cols];
        started = false; over = false; won = false;
        exR = exC = -1; opened = 0; flags = 0; seconds = 0;
        clock.stop();
        overlay.setVisible(false);
        hud();
        board.repaint();
    }

    void plant(int sr, int sc) {
        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++)
            if (Math.abs(r - sr) > 1 || Math.abs(c - sc) > 1) cells.add(new int[]{r, c});
        Collections.shuffle(cells, rng);
        for (int i = 0; i < mines; i++) mine[cells.get(i)[0]][cells.get(i)[1]] = true;
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int n = 0;
            for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                int rr = r + dr, cc = c + dc;
                if (rr >= 0 && rr < rows && cc >= 0 && cc < cols && mine[rr][cc]) n++;
            }
            count[r][c] = n;
        }
        started = true;
        clock.restart();
    }

    void reveal(int r, int c) {
        if (over || flag[r][c]) return;
        if (!started) plant(r, c);
        if (open[r][c]) { chord(r, c); return; }
        if (mine[r][c]) { lose(r, c); return; }
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{r, c});
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int pr = p[0], pc = p[1];
            if (open[pr][pc] || flag[pr][pc]) continue;
            open[pr][pc] = true;
            opened++;
            if (count[pr][pc] == 0)
                for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                    int rr = pr + dr, cc = pc + dc;
                    if (rr >= 0 && rr < rows && cc >= 0 && cc < cols && !open[rr][cc] && !mine[rr][cc]) q.add(new int[]{rr, cc});
                }
        }
        checkWin();
    }

    void chord(int r, int c) {
        int n = count[r][c];
        if (n == 0) return;
        int f = 0;
        for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
            int rr = r + dr, cc = c + dc;
            if (rr >= 0 && rr < rows && cc >= 0 && cc < cols && flag[rr][cc]) f++;
        }
        if (f != n) return;
        for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
            int rr = r + dr, cc = c + dc;
            if (rr >= 0 && rr < rows && cc >= 0 && cc < cols && !open[rr][cc] && !flag[rr][cc]) {
                if (mine[rr][cc]) { lose(rr, cc); return; }
                reveal(rr, cc);
                if (over) return;
            }
        }
    }

    void toggleFlag(int r, int c) {
        if (over || open[r][c]) return;
        flag[r][c] = !flag[r][c];
        flags += flag[r][c] ? 1 : -1;
        hud();
    }

    void lose(int r, int c) {
        over = true; won = false;
        exR = r; exC = c;
        clock.stop();
        hud();
        later(900, () -> overlay.showEnd());
    }

    void checkWin() {
        if (opened == rows * cols - mines && !over) {
            over = true; won = true;
            clock.stop();
            for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) if (mine[r][c] && !flag[r][c]) { flag[r][c] = true; flags++; }
            boolean record = best[level] == 0 || seconds < best[level];
            if (record) { best[level] = seconds; if (prefs != null) try { prefs.putInt("best" + level, seconds); } catch (Exception ignored) {} }
            hud();
            later(600, () -> overlay.showEnd());
        }
    }

    void later(int ms, Runnable run) {
        javax.swing.Timer t = new javax.swing.Timer(ms, e -> run.run());
        t.setRepeats(false);
        t.start();
    }

    static String fmt(int s) { return String.format("%02d:%02d", s / 60, s % 60); }

    void hud() {
        minesLbl.setText("<html><span style='color:#f472b6'>●</span> " + (mines - flags) + " bombas</html>");
        timeLbl.setText(fmt(seconds));
        levelLbl.setText(LEVELS[level] + "  ·  " + rows + "×" + cols + "  ·  " + mines + " bombas");
        board.repaint();
    }

    class Board extends JComponent {
        int hr = -1, hc = -1;
        Board() {
            setPreferredSize(new Dimension(AREA_W, AREA_H));
            MouseAdapter m = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    int[] rc = cell(e.getX(), e.getY());
                    if (rc == null || overlay.isVisible()) return;
                    if (SwingUtilities.isRightMouseButton(e) || e.isShiftDown()) toggleFlag(rc[0], rc[1]);
                    else if (SwingUtilities.isLeftMouseButton(e)) reveal(rc[0], rc[1]);
                    hud();
                }
                public void mouseMoved(MouseEvent e) {
                    int[] rc = cell(e.getX(), e.getY());
                    int nr = rc == null ? -1 : rc[0], nc = rc == null ? -1 : rc[1];
                    if (nr != hr || nc != hc) { hr = nr; hc = nc; repaint(); }
                }
                public void mouseExited(MouseEvent e) { hr = hc = -1; repaint(); }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        int cs() { return Math.min(40, Math.min(AREA_W / cols, AREA_H / rows)); }
        int ox() { return (getWidth() - cs() * cols) / 2; }
        int oy() { return (getHeight() - cs() * rows) / 2; }

        int[] cell(int x, int y) {
            int s = cs(), c = (x - ox()) / s, r = (y - oy()) / s;
            if (x < ox() || y < oy() || r < 0 || r >= rows || c < 0 || c >= cols) return null;
            return new int[]{r, c};
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int s = cs(), ox = ox(), oy = oy();
            g.setColor(PANEL);
            g.fillRoundRect(ox - 8, oy - 8, s * cols + 16, s * rows + 16, 16, 16);
            Font numFont = new Font(FONT, Font.BOLD, (int) (s * 0.58));
            for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
                int x = ox + c * s, y = oy + r * s;
                boolean showMine = over && mine[r][c] && !won;
                if (open[r][c] || showMine) {
                    g.setColor(r == exR && c == exC ? PINK : OPEN);
                    g.fillRect(x, y, s, s);
                    g.setColor(OPEN_LINE);
                    g.drawRect(x, y, s, s);
                    if (mine[r][c]) {
                        if (flag[r][c]) drawFlag(g, x, y, s); else drawMine(g, x, y, s);
                    } else if (count[r][c] > 0) {
                        g.setFont(numFont);
                        g.setColor(NUM[count[r][c]]);
                        String t = String.valueOf(count[r][c]);
                        FontMetrics fm = g.getFontMetrics();
                        g.drawString(t, x + (s - fm.stringWidth(t)) / 2, y + (s + fm.getAscent() - fm.getDescent()) / 2);
                    }
                } else {
                    boolean hover = r == hr && c == hc && !over;
                    g.setPaint(new GradientPaint(x, y, hover ? HIDDEN_1.brighter() : HIDDEN_1, x, y + s, HIDDEN_2));
                    g.fillRoundRect(x + 1, y + 1, s - 2, s - 2, Math.max(4, s / 5), Math.max(4, s / 5));
                    g.setColor(new Color(255, 255, 255, 50));
                    g.fillRoundRect(x + 3, y + 2, s - 6, Math.max(2, s / 5), 4, 4);
                    if (flag[r][c]) {
                        drawFlag(g, x, y, s);
                        if (over && !won && !mine[r][c]) {
                            g.setColor(Color.WHITE);
                            g.setStroke(new BasicStroke(2));
                            g.drawLine(x + 5, y + 5, x + s - 5, y + s - 5);
                            g.drawLine(x + s - 5, y + 5, x + 5, y + s - 5);
                            g.setStroke(new BasicStroke(1));
                        }
                    }
                }
            }
            g.dispose();
        }

        void drawFlag(Graphics2D g, int x, int y, int s) {
            g.setColor(new Color(0x3b1a6b));
            g.setStroke(new BasicStroke(Math.max(1.5f, s / 14f)));
            int px = x + (int) (s * 0.38);
            g.drawLine(px, y + (int) (s * 0.2), px, y + (int) (s * 0.8));
            g.drawLine(x + (int) (s * 0.25), y + (int) (s * 0.8), x + (int) (s * 0.65), y + (int) (s * 0.8));
            g.setStroke(new BasicStroke(1));
            g.setColor(new Color(0xf472b6));
            g.fillPolygon(new int[]{px, x + (int) (s * 0.78), px}, new int[]{y + (int) (s * 0.18), y + (int) (s * 0.34), y + (int) (s * 0.52)}, 3);
        }

        void drawMine(Graphics2D g, int x, int y, int s) {
            double cx = x + s / 2.0, cy = y + s / 2.0, rad = s * 0.24;
            g.setColor(new Color(0x2e1065));
            g.setStroke(new BasicStroke(Math.max(1.5f, s / 12f)));
            for (int k = 0; k < 4; k++) {
                double a = Math.PI / 4 * k;
                g.draw(new Line2D.Double(cx - Math.cos(a) * rad * 1.5, cy - Math.sin(a) * rad * 1.5, cx + Math.cos(a) * rad * 1.5, cy + Math.sin(a) * rad * 1.5));
            }
            g.setStroke(new BasicStroke(1));
            g.fill(new Ellipse2D.Double(cx - rad, cy - rad, rad * 2, rad * 2));
            g.setColor(new Color(255, 255, 255, 170));
            g.fill(new Ellipse2D.Double(cx - rad * 0.5, cy - rad * 0.6, rad * 0.55, rad * 0.55));
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
                g.setColor(PANEL);
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
            g.setColor(new Color(20, 8, 36, 190));
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
        void levels() {
            for (int i = 0; i < 3; i++) {
                final int k = i;
                String b = best[i] == 0 ? "" : "  ·  recorde " + fmt(best[i]);
                put(new Pill(LEVELS[i] + "  ·  " + SIZES[i][2] + " bombas" + b, LEVEL_COLORS[i], 280, 40, () -> newGame(k)), 8);
            }
        }
        void showStart() {
            boolean wasRunning = started && !over;
            clock.stop();
            card.removeAll();
            put(lbl("<span style='color:#c084fc'>Campo</span> Minado", 28, TEXT, true), 4);
            put(lbl("Escolhe o nível, diva:", 13, MUTED, false), 16);
            levels();
            if (wasRunning) {
                card.add(Box.createVerticalStrut(4));
                put(new Pill("Voltar pra partida", DIM, 280, 36, () -> { setVisible(false); clock.start(); }), 0);
            }
            open();
        }
        void showEnd() {
            card.removeAll();
            if (won) {
                boolean record = best[level] == seconds;
                put(lbl(record ? "NOVO RECORDE! ♛" : "Campo limpo! SLAY ♥", 26, new Color(0xf472b6), true), 6);
                put(lbl(LEVELS[level] + " em " + fmt(seconds), 14, TEXT, true), 16);
            } else {
                put(lbl("BOOM!", 30, PINK, true), 6);
                put(lbl("Essa bomba estava escondidinha...", 13, MUTED, false), 16);
            }
            put(new Pill("Jogar de novo  ·  " + LEVELS[level], BTN, 260, 40, () -> newGame(level)), 8);
            put(new Pill("Trocar nível", DIM, 260, 36, this::showStart), 8);
            put(new Pill("Ver o tabuleiro", DIM, 260, 32, () -> setVisible(false)), 0);
            open();
        }
        void open() { card.revalidate(); setVisible(true); revalidate(); repaint(); }
    }

    public void hubPause() { clock.stop(); }
    public void hubResume() { if (started && !over && !overlay.isVisible()) clock.start(); }
    public void hubStop() { clock.stop(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new campominado().setVisible(true));
    }
}