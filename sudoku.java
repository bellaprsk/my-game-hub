import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class sudoku extends JFrame {

    static final Color BG = new Color(0x1a0b2e);
    static final Color PANEL = new Color(0x2a1650);
    static final Color BOARD = new Color(0xf5edff);
    static final Color PEER = new Color(0xe6d4ff);
    static final Color SAME = new Color(0xcfa8ff);
    static final Color SELECT = new Color(0xa855f7);
    static final Color THIN = new Color(0xc4a7f5);
    static final Color THICK = new Color(0x4c1d95);
    static final Color GIVEN = new Color(0x2e1065);
    static final Color USER = new Color(0x7c3aed);
    static final Color WRONG = new Color(0xe11d74);
    static final Color TEXT = new Color(0xf3e8ff);
    static final Color ACCENT = new Color(0xc084fc);
    static final Color BTN = new Color(0x6d28d9);
    static final Color BTN_HOVER = new Color(0x8b5cf6);
    static final Color BTN_OFF = new Color(0x3b2566);
    static final Color BTN_OFF_TEXT = new Color(0x7b6a9b);

    static final String[] LEVEL_NAMES = {"Fácil", "Médio", "Difícil"};
    static final int[] LEVEL_REMOVE = {40, 49, 56};
    static final Color[] LEVEL_COLORS = {new Color(0xa78bfa), new Color(0x8b5cf6), new Color(0x6d28d9)};

    static final int CELL = 38;
    static final int PAD = 4;
    static final int SIZE = CELL * 9 + PAD * 2;
    static final String FONT = "SansSerif";

    static final int[][] PEERS = new int[81][];
    static {
        for (int i = 0; i < 81; i++) {
            int r = i / 9, c = i % 9, br = 3 * (r / 3), bc = 3 * (c / 3);
            Set<Integer> p = new LinkedHashSet<>();
            for (int k = 0; k < 9; k++) { p.add(r * 9 + k); p.add(k * 9 + c); }
            for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) p.add((br + a) * 9 + bc + b);
            p.remove(i);
            PEERS[i] = p.stream().mapToInt(Integer::intValue).toArray();
        }
    }
    static final Random RNG = new Random();

    static List<Integer> cands(int[] g, int i) {
        boolean[] used = new boolean[10];
        for (int p : PEERS[i]) used[g[p]] = true;
        List<Integer> out = new ArrayList<>();
        for (int v = 1; v <= 9; v++) if (!used[v]) out.add(v);
        return out;
    }

    static boolean fill(int[] g) {
        for (int i = 0; i < 81; i++) {
            if (g[i] == 0) {
                List<Integer> c = cands(g, i);
                Collections.shuffle(c, RNG);
                for (int v : c) {
                    g[i] = v;
                    if (fill(g)) return true;
                }
                g[i] = 0;
                return false;
            }
        }
        return true;
    }

    static int countSolutions(int[] g, int limit) {
        int best = -1;
        List<Integer> bestC = null;
        for (int i = 0; i < 81; i++) {
            if (g[i] == 0) {
                List<Integer> c = cands(g, i);
                if (c.isEmpty()) return 0;
                if (bestC == null || c.size() < bestC.size()) {
                    best = i; bestC = c;
                    if (c.size() == 1) break;
                }
            }
        }
        if (best == -1) return 1;
        int total = 0;
        for (int v : bestC) {
            g[best] = v;
            total += countSolutions(g, limit - total);
            if (total >= limit) break;
        }
        g[best] = 0;
        return total;
    }

    static int[][] makePuzzle(int toRemove) {
        int[] solution = new int[81];
        fill(solution);
        int[] puzzle = solution.clone();
        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < 81; i++) cells.add(i);
        Collections.shuffle(cells, RNG);
        int removed = 0;
        for (int i : cells) {
            if (removed >= toRemove) break;
            int backup = puzzle[i];
            puzzle[i] = 0;
            if (countSolutions(puzzle.clone(), 2) != 1) puzzle[i] = backup;
            else removed++;
        }
        return new int[][]{puzzle, solution};
    }

    int[] puzzle = new int[81], solution = new int[81], board = new int[81];
    int selected = -1, errors = 0, seconds = 0, wins = 0;
    String level = null;
    boolean playing = false, finished = false;

    final JLabel lblLevel = label("Sudoku Diva", ACCENT, 15, true);
    final JLabel lblErr = label("♥ 0 erros", TEXT, 13, false);
    final JLabel lblTime = label("00:00", TEXT, 13, false);
    final JLabel lblWins = label("♛ 0 vitórias", ACCENT, 13, true);
    final BoardPanel boardPanel = new BoardPanel();
    final PillButton[] numBtns = new PillButton[10];
    final Overlay overlay = new Overlay();
    final javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
        seconds++;
        lblTime.setText(fmtTime());
    });

    sudoku() {
        super("Sudoku");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        setContentPane(root);

        JPanel top = row();
        top.setLayout(new BorderLayout());
        top.add(lblLevel, BorderLayout.WEST);
        JPanel stats = row();
        stats.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        stats.add(lblErr);
        stats.add(lblTime);
        top.add(stats, BorderLayout.EAST);
        top.setMaximumSize(new Dimension(SIZE, 30));
        root.add(top);
        root.add(Box.createVerticalStrut(8));

        boardPanel.setAlignmentX(CENTER_ALIGNMENT);
        root.add(boardPanel);
        root.add(Box.createVerticalStrut(10));

        JPanel nums = row();
        nums.setLayout(new GridLayout(1, 9, 3, 0));
        for (int d = 1; d <= 9; d++) {
            final int v = d;
            numBtns[d] = new PillButton(String.valueOf(d), BTN, 16, () -> place(v));
            nums.add(numBtns[d]);
        }
        nums.setMaximumSize(new Dimension(SIZE, 36));
        nums.setPreferredSize(new Dimension(SIZE, 36));
        root.add(nums);
        root.add(Box.createVerticalStrut(8));

        JPanel actions = row();
        actions.setLayout(new BorderLayout());
        JPanel left = row();
        left.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.add(new PillButton("Apagar", BTN, 13, this::erase));
        left.add(Box.createHorizontalStrut(6));
        left.add(new PillButton("Novo jogo", BTN, 13, this::askLevel));
        actions.add(left, BorderLayout.WEST);
        actions.add(lblWins, BorderLayout.EAST);
        actions.setMaximumSize(new Dimension(SIZE, 34));
        root.add(actions);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED || !isActive() || overlay.isVisible()) return false;
            onKey(e);
            return false;
        });

        setGlassPane(overlay);

        refresh();
        pack();
        setLocationRelativeTo(null);
    }

    static JLabel label(String t, Color c, int size, boolean bold) {
        JLabel l = new JLabel(t);
        l.setForeground(c);
        l.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size));
        return l;
    }

    static JPanel row() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setAlignmentX(CENTER_ALIGNMENT);
        return p;
    }

    static class PillButton extends JComponent {
        final String text;
        Color base;
        boolean hover = false, enabledBtn = true;
        final int fontSize;

        PillButton(String text, Color base, int fontSize, Runnable action) {
            this.text = text;
            this.base = base;
            this.fontSize = fontSize;
            setFont(new Font(FONT, Font.BOLD, fontSize));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(getFont());
            setPreferredSize(new Dimension(fm.stringWidth(text) + 24, fm.getHeight() + 12));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseReleased(MouseEvent e) {
                    if (enabledBtn && contains(e.getPoint())) action.run();
                }
            });
        }

        void setOn(boolean on) {
            enabledBtn = on;
            setCursor(Cursor.getPredefinedCursor(on ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(!enabledBtn ? BTN_OFF : hover ? BTN_HOVER : base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.setColor(enabledBtn ? TEXT : BTN_OFF_TEXT);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    class BoardPanel extends JComponent {
        BoardPanel() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMaximumSize(new Dimension(SIZE, SIZE));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    int c = (e.getX() - PAD) / CELL, r = (e.getY() - PAD) / CELL;
                    if (e.getX() >= PAD && e.getY() >= PAD && r < 9 && c < 9) {
                        selected = r * 9 + c;
                        repaint();
                    }
                }
            });
        }

        boolean isPeer(int sel, int i) {
            for (int p : PEERS[sel]) if (p == i) return true;
            return false;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(THICK);
            g.fillRoundRect(0, 0, SIZE, SIZE, 10, 10);
            int selVal = selected >= 0 ? board[selected] : 0;
            Font bold = new Font(FONT, Font.BOLD, 20), plain = new Font(FONT, Font.PLAIN, 20);

            for (int i = 0; i < 81; i++) {
                int x = PAD + (i % 9) * CELL, y = PAD + (i / 9) * CELL;
                Color fillC = BOARD;
                if (selected >= 0) {
                    if (i == selected) fillC = SELECT;
                    else if (selVal != 0 && board[i] == selVal) fillC = SAME;
                    else if (isPeer(selected, i)) fillC = PEER;
                }
                g.setColor(fillC);
                g.fillRect(x, y, CELL, CELL);

                int v = board[i];
                if (v != 0) {
                    Color col; Font f;
                    if (puzzle[i] != 0) { col = GIVEN; f = bold; }
                    else if (v != solution[i]) { col = WRONG; f = bold; }
                    else { col = USER; f = plain; }
                    if (i == selected) col = Color.WHITE;
                    g.setColor(col);
                    g.setFont(f);
                    FontMetrics fm = g.getFontMetrics();
                    String s = String.valueOf(v);
                    g.drawString(s, x + (CELL - fm.stringWidth(s)) / 2,
                            y + (CELL - fm.getHeight()) / 2 + fm.getAscent());
                }
            }

            int end = PAD + 9 * CELL;
            g.setColor(THIN);
            g.setStroke(new BasicStroke(1));
            for (int k = 1; k < 9; k++) {
                if (k % 3 == 0) continue;
                int p = PAD + k * CELL;
                g.drawLine(p, PAD, p, end);
                g.drawLine(PAD, p, end, p);
            }
            g.setColor(THICK);
            g.setStroke(new BasicStroke(3));
            for (int k = 0; k <= 9; k += 3) {
                int p = PAD + k * CELL;
                g.drawLine(p, PAD, p, end);
                g.drawLine(PAD, p, end, p);
            }
            g.dispose();
        }
    }

    void refresh() {
        boardPanel.repaint();
        for (int d = 1; d <= 9; d++) {
            int count = 0;
            for (int i = 0; i < 81; i++) if (board[i] == d && solution[i] == d) count++;
            numBtns[d].setOn(count < 9);
        }
    }

    class Overlay extends JPanel {
        final JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PANEL);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g.setColor(THICK);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                g.dispose();
            }
        };

        Overlay() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
            add(card);
            addMouseListener(new MouseAdapter() {});
            addMouseMotionListener(new MouseMotionAdapter() {});
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(26, 11, 46, 200));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        void addCentered(JComponent c) {
            c.setAlignmentX(CENTER_ALIGNMENT);
            card.add(c);
        }

        void showLevels(String headline, String details, boolean canResume) {
            card.removeAll();
            if (headline != null) {
                addCentered(label(headline, ACCENT, 17, true));
                card.add(Box.createVerticalStrut(8));
            }
            if (details != null) {
                JLabel d = label("<html><div style='text-align:center'>" + details + "</div></html>", TEXT, 13, false);
                d.setHorizontalAlignment(SwingConstants.CENTER);
                addCentered(d);
                card.add(Box.createVerticalStrut(12));
            }
            addCentered(label("Qual o nível agora, diva?", TEXT, 14, true));
            card.add(Box.createVerticalStrut(12));
            for (int k = 0; k < 3; k++) {
                final String name = LEVEL_NAMES[k];
                PillButton b = new PillButton(name, LEVEL_COLORS[k], 15, () -> {
                    setVisible(false);
                    newGame(name);
                });
                b.setMaximumSize(new Dimension(200, 38));
                b.setPreferredSize(new Dimension(200, 38));
                addCentered(b);
                card.add(Box.createVerticalStrut(7));
            }
            if (canResume) {
                card.add(Box.createVerticalStrut(4));
                PillButton back = new PillButton("Voltar pro jogo", BTN_OFF, 13, () -> {
                    setVisible(false);
                    playing = true;
                    timer.start();
                });
                back.setMaximumSize(new Dimension(200, 32));
                back.setPreferredSize(new Dimension(200, 32));
                addCentered(back);
            }
            card.revalidate();
            setVisible(true);
            revalidate();
            repaint();
        }
    }

    void askLevel() {
        boolean canResume = playing;
        playing = false;
        timer.stop();
        overlay.showLevels(null, null, canResume);
    }

    void newGame(String name) {
        int idx = Arrays.asList(LEVEL_NAMES).indexOf(name);
        int[][] gen = makePuzzle(LEVEL_REMOVE[idx]);
        puzzle = gen[0];
        solution = gen[1];
        board = puzzle.clone();
        level = name;
        selected = -1;
        errors = 0;
        seconds = 0;
        finished = false;
        playing = true;
        lblLevel.setText("♥ " + name);
        lblErr.setText("♥ 0 erros");
        lblTime.setText("00:00");
        refresh();
        timer.restart();
    }

    String fmtTime() {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    void onKey(KeyEvent e) {
        char ch = e.getKeyChar();
        int code = e.getKeyCode();
        if (ch >= '1' && ch <= '9') place(ch - '0');
        else if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE || ch == '0') erase();
        else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN
                || code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
            if (selected < 0) selected = 0;
            else {
                int r = selected / 9, c = selected % 9;
                if (code == KeyEvent.VK_UP) r = (r + 8) % 9;
                if (code == KeyEvent.VK_DOWN) r = (r + 1) % 9;
                if (code == KeyEvent.VK_LEFT) c = (c + 8) % 9;
                if (code == KeyEvent.VK_RIGHT) c = (c + 1) % 9;
                selected = r * 9 + c;
            }
            boardPanel.repaint();
        }
    }

    void place(int v) {
        if (!playing || selected < 0) return;
        int i = selected;
        if (puzzle[i] != 0 || board[i] == solution[i]) return;
        board[i] = v;
        if (v != solution[i]) {
            errors++;
            lblErr.setText("♥ " + errors + (errors == 1 ? " erro" : " erros"));
        }
        refresh();
        if (Arrays.equals(board, solution)) win();
    }

    void erase() {
        if (!playing || selected < 0) return;
        int i = selected;
        if (puzzle[i] == 0 && board[i] != solution[i]) {
            board[i] = 0;
            refresh();
        }
    }

    void win() {
        playing = false;
        finished = true;
        timer.stop();
        wins++;
        lblWins.setText("♛ " + wins + (wins == 1 ? " vitória" : " vitórias"));
        javax.swing.Timer delay = new javax.swing.Timer(300, e -> overlay.showLevels(
                "SLAY! Arrasou, diva! ♛",
                "Nível: " + level + "<br>Tempo: " + fmtTime() + "<br>Erros: " + errors,
                false));
        delay.setRepeats(false);
        delay.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            sudoku app = new sudoku();
            app.setVisible(true);
            SwingUtilities.invokeLater(app::askLevel);
        });
    }
}