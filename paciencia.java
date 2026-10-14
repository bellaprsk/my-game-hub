import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class paciencia extends JFrame {

    static final Color TABLE = new Color(0xf3ecfc);
    static final Color TABLE_2 = new Color(0xebe0f9);
    static final Color SLOT = new Color(0xd9c9f0);
    static final Color INK = new Color(0x3b1a6b);
    static final Color SOFT_INK = new Color(0x8a72b0);
    static final Color PURPLE = new Color(0x6d28d9);
    static final Color PINK = new Color(0xdb2777);
    static final Color CARD = new Color(0xfffcff);
    static final Color CARD_EDGE = new Color(0xe2d3f5);
    static final Color BACK_1 = new Color(0x8b5cf6);
    static final Color BACK_2 = new Color(0xc084fc);
    static final Color BTN = new Color(0x8b5cf6);
    static final Color BTN_SOFT = new Color(0xe4d7f7);
    static final Color HILITE = new Color(0xf472b6);
    static final String FONT = "SansSerif";

    static final int CW = 72, CH = 100, GAP = 12, MARGIN = 14;
    static final int W = MARGIN * 2 + 7 * CW + 6 * GAP;
    static final int TOP = 12;
    static final int TAB_Y = TOP + CH + 22;
    static final int TABLE_H = 560;
    static final String[] SUITS = {"♠", "♥", "♦", "♣"};
    static final String[] RANKS = {"", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

    static class Card {
        final int rank, suit; boolean up;
        Card(int rank, int suit, boolean up) { this.rank = rank; this.suit = suit; this.up = up; }
        boolean pink() { return suit == 1 || suit == 2; }
        Card copy() { return new Card(rank, suit, up); }
    }

    static class State {
        List<Card> stock = new ArrayList<>(), waste = new ArrayList<>();
        List<List<Card>> found = new ArrayList<>(), tab = new ArrayList<>();
        int moves;
        State() {
            for (int i = 0; i < 4; i++) found.add(new ArrayList<>());
            for (int i = 0; i < 7; i++) tab.add(new ArrayList<>());
        }
        State copy() {
            State s = new State();
            for (Card c : stock) s.stock.add(c.copy());
            for (Card c : waste) s.waste.add(c.copy());
            for (int i = 0; i < 4; i++) for (Card c : found.get(i)) s.found.get(i).add(c.copy());
            for (int i = 0; i < 7; i++) for (Card c : tab.get(i)) s.tab.get(i).add(c.copy());
            s.moves = moves;
            return s;
        }
    }

    static final int STOCK = 0, WASTE = 1, FOUND = 2, TAB = 3;

    final Preferences prefs = tryPrefs();
    final Random rng = new Random();
    State st = new State();
    final Deque<State> undo = new ArrayDeque<>();
    int drawCount = 1;
    boolean playing = false, won = false;
    int seconds = 0;
    int gameId = 0;
    int played, wins, bestTime;

    List<Card> drag = null;
    int dragType, dragIdx;
    int dragX, dragY, offX, offY, pressX, pressY;
    boolean dragMoved;

    int hintType = -1, hintIdx, hintPos; long hintUntil;

    boolean autoFinishing = false;

    final Table table = new Table();
    final JLabel timeLbl = new JLabel("00:00"), movesLbl = new JLabel("0 jogadas"), statsLbl = new JLabel();
    final Overlay overlay = new Overlay();
    final javax.swing.Timer clock = new javax.swing.Timer(1000, e -> { seconds++; updateHud(); });
    final javax.swing.Timer auto = new javax.swing.Timer(70, e -> autoStep());
    Pill finishBtn;

    public paciencia() {
        super("Paciência");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        loadStats();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(TABLE);
        setContentPane(root);

        JPanel hud = new JPanel(new BorderLayout());
        hud.setBackground(TABLE_2);
        hud.setBorder(BorderFactory.createEmptyBorder(8, MARGIN, 8, MARGIN));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("<html><span style='color:#8b5cf6'>Paci</span><span style='color:#db2777'>ência</span></html>");
        title.setFont(new Font(FONT, Font.BOLD, 20));
        left.add(title);
        left.add(Box.createHorizontalStrut(14));
        for (JLabel l : new JLabel[]{timeLbl, movesLbl}) {
            l.setFont(new Font(FONT, Font.BOLD, 13));
            l.setForeground(SOFT_INK);
            left.add(l);
            left.add(Box.createHorizontalStrut(12));
        }
        hud.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        finishBtn = new Pill("Finalizar", PINK, Color.WHITE, 84, 30, this::startAutoFinish);
        finishBtn.setVisible(false);
        right.add(finishBtn);
        right.add(new Pill("Desfazer", BTN_SOFT, INK, 84, 30, this::undoMove));
        right.add(new Pill("Dica", BTN_SOFT, INK, 56, 30, this::hint));
        right.add(new Pill("Novo jogo", BTN, Color.WHITE, 90, 30, () -> overlay.showStart(false)));
        hud.add(right, BorderLayout.EAST);
        root.add(hud, BorderLayout.NORTH);
        root.add(table, BorderLayout.CENTER);

        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.META_DOWN_MASK), "undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "hint");
        am.put("undo", new AbstractAction() { public void actionPerformed(ActionEvent e) { if (!overlay.isVisible()) undoMove(); } });
        am.put("hint", new AbstractAction() { public void actionPerformed(ActionEvent e) { if (!overlay.isVisible()) hint(); } });

        setGlassPane(overlay);
        pack();
        setLocationRelativeTo(null);
        deal();
        overlay.showStart(true);
    }

    static Preferences tryPrefs() {
        try { return Preferences.userRoot().node("paciencia-diva"); } catch (Exception e) { return null; }
    }

    void loadStats() {
        if (prefs == null) return;
        played = prefs.getInt("played", 0); wins = prefs.getInt("wins", 0); bestTime = prefs.getInt("best", 0);
    }

    void saveStats() {
        if (prefs == null) return;
        try { prefs.putInt("played", played); prefs.putInt("wins", wins); prefs.putInt("best", bestTime); } catch (Exception ignored) {}
    }

    void deal() {
        List<Card> deck = new ArrayList<>();
        for (int s = 0; s < 4; s++) for (int r = 1; r <= 13; r++) deck.add(new Card(r, s, false));
        Collections.shuffle(deck, rng);
        st = new State();
        gameId++;
        int k = 0;
        for (int col = 0; col < 7; col++)
            for (int row = 0; row <= col; row++) {
                Card c = deck.get(k++);
                c.up = row == col;
                st.tab.get(col).add(c);
            }
        while (k < deck.size()) st.stock.add(deck.get(k++));
        undo.clear();
        seconds = 0;
        won = false;
        autoFinishing = false;
        auto.stop();
        drag = null;
        updateHud();
        table.repaint();
    }

    void newGame(int draw) {
        if (playing && !won && st.moves > 0) { played++; saveStats(); }
        drawCount = draw;
        deal();
        playing = true;
        clock.restart();
        overlay.setVisible(false);
    }

    void snapshot() {
        undo.push(st.copy());
        while (undo.size() > 300) undo.removeLast();
    }

    void undoMove() {
        if (undo.isEmpty() || won || autoFinishing) return;
        st = undo.pop();
        updateHud();
        table.repaint();
    }

    void afterMove() {
        st.moves++;
        for (List<Card> col : st.tab) if (!col.isEmpty() && !col.get(col.size() - 1).up) col.get(col.size() - 1).up = true;
        hintType = -1;
        updateHud();
        table.repaint();
        checkWin();
    }

    void drawFromStock() {
        if (!playing || won) return;
        snapshot();
        if (st.stock.isEmpty()) {
            if (st.waste.isEmpty()) { undo.pop(); return; }
            for (int i = st.waste.size() - 1; i >= 0; i--) { Card c = st.waste.get(i); c.up = false; st.stock.add(c); }
            st.waste.clear();
        } else {
            for (int i = 0; i < drawCount && !st.stock.isEmpty(); i++) {
                Card c = st.stock.remove(st.stock.size() - 1);
                c.up = true;
                st.waste.add(c);
            }
        }
        afterMove();
    }

    boolean canFound(Card c, int f) {
        List<Card> p = st.found.get(f);
        if (p.isEmpty()) return c.rank == 1;
        Card top = p.get(p.size() - 1);
        return top.suit == c.suit && c.rank == top.rank + 1;
    }

    boolean canTab(Card c, int t) {
        List<Card> p = st.tab.get(t);
        if (p.isEmpty()) return c.rank == 13;
        Card top = p.get(p.size() - 1);
        return top.up && top.pink() != c.pink() && c.rank == top.rank - 1;
    }

    List<Card> pile(int type, int idx) {
        switch (type) {
            case STOCK: return st.stock;
            case WASTE: return st.waste;
            case FOUND: return st.found.get(idx);
            default: return st.tab.get(idx);
        }
    }

    boolean tryMove(int fromType, int fromIdx, int count, int toType, int toIdx) {
        if (fromType == toType && fromIdx == toIdx) return false;
        List<Card> src = pile(fromType, fromIdx);
        if (count <= 0 || count > src.size()) return false;
        Card first = src.get(src.size() - count);
        if (!first.up) return false;
        if (toType == FOUND) { if (count != 1 || !canFound(first, toIdx)) return false; }
        else if (toType == TAB) { if (!canTab(first, toIdx)) return false; }
        else return false;
        snapshot();
        List<Card> moving = new ArrayList<>(src.subList(src.size() - count, src.size()));
        for (int i = 0; i < count; i++) src.remove(src.size() - 1);
        pile(toType, toIdx).addAll(moving);
        afterMove();
        return true;
    }

    void autoMove(int type, int idx, int count) {
        List<Card> src = pile(type, idx);
        if (src.isEmpty()) return;
        if (count == 1) for (int f = 0; f < 4; f++) if (tryMove(type, idx, 1, FOUND, f)) return;
        Card first = src.get(src.size() - count);
        for (int t = 0; t < 7; t++) if (!st.tab.get(t).isEmpty() && tryMove(type, idx, count, TAB, t)) return;
        boolean wholeColumn = type == TAB && count == src.size();
        if (first.rank == 13 && !wholeColumn)
            for (int t = 0; t < 7; t++) if (st.tab.get(t).isEmpty() && tryMove(type, idx, count, TAB, t)) return;
        flashHint(type, idx, src.size() - count);
    }

    void checkWin() {
        int n = 0;
        for (List<Card> f : st.found) n += f.size();
        if (n == 52 && !won) {
            won = true;
            playing = false;
            clock.stop();
            auto.stop();
            autoFinishing = false;
            played++; wins++;
            boolean record = bestTime == 0 || seconds < bestTime;
            if (record) bestTime = seconds;
            saveStats();
            int id = gameId;
            javax.swing.Timer t = new javax.swing.Timer(500, e -> { if (id == gameId && won) overlay.showWin(record); });
            t.setRepeats(false);
            t.start();
        }
        finishBtn.setVisible(canAutoFinish());
    }

    boolean canAutoFinish() {
        if (won || !playing) return false;
        if (!st.stock.isEmpty() || !st.waste.isEmpty()) return false;
        for (List<Card> col : st.tab) for (Card c : col) if (!c.up) return false;
        return true;
    }

    void startAutoFinish() {
        if (!canAutoFinish()) return;
        autoFinishing = true;
        auto.start();
    }

    void autoStep() {
        for (int t = 0; t < 7; t++) {
            List<Card> col = st.tab.get(t);
            if (col.isEmpty()) continue;
            for (int f = 0; f < 4; f++)
                if (canFound(col.get(col.size() - 1), f)) {
                    st.found.get(f).add(col.remove(col.size() - 1));
                    st.moves++;
                    updateHud();
                    table.repaint();
                    checkWin();
                    return;
                }
        }
        auto.stop();
        autoFinishing = false;
    }

    void hint() {
        if (!playing || won) return;
        for (int t = 0; t < 7; t++) {
            List<Card> col = st.tab.get(t);
            if (!col.isEmpty()) for (int f = 0; f < 4; f++) if (canFound(col.get(col.size() - 1), f)) { flashHint(TAB, t, col.size() - 1); return; }
        }
        if (!st.waste.isEmpty()) for (int f = 0; f < 4; f++) if (canFound(st.waste.get(st.waste.size() - 1), f)) { flashHint(WASTE, 0, st.waste.size() - 1); return; }
        for (int t = 0; t < 7; t++) {
            List<Card> col = st.tab.get(t);
            int firstUp = firstUp(col);
            if (firstUp < 0) continue;
            Card c = col.get(firstUp);
            if (firstUp == 0 && c.rank == 13) continue;
            for (int d = 0; d < 7; d++) if (d != t && canTab(c, d) && !(st.tab.get(d).isEmpty() && firstUp == 0)) { flashHint(TAB, t, firstUp); return; }
        }
        if (!st.waste.isEmpty()) {
            Card c = st.waste.get(st.waste.size() - 1);
            for (int d = 0; d < 7; d++) if (canTab(c, d)) { flashHint(WASTE, 0, st.waste.size() - 1); return; }
        }
        if (!st.stock.isEmpty() || !st.waste.isEmpty()) { flashHint(STOCK, 0, 0); return; }
        flashHint(-2, 0, 0);
    }

    static int firstUp(List<Card> col) {
        for (int i = 0; i < col.size(); i++) if (col.get(i).up) return i;
        return -1;
    }

    void flashHint(int type, int idx, int pos) {
        hintType = type; hintIdx = idx; hintPos = pos;
        hintUntil = System.currentTimeMillis() + 1300;
        table.repaint();
        javax.swing.Timer t = new javax.swing.Timer(1350, e -> table.repaint());
        t.setRepeats(false);
        t.start();
    }

    void updateHud() {
        timeLbl.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        movesLbl.setText(st.moves + (st.moves == 1 ? " jogada" : " jogadas"));
        if (finishBtn != null) finishBtn.setVisible(canAutoFinish() && !autoFinishing);
    }

    static int colX(int i) { return MARGIN + i * (CW + GAP); }

    int[] offsets(List<Card> col) {
        int down = 10, up = 26;
        int downs = 0, ups = 0;
        for (int i = 0; i < col.size() - 1; i++) { if (col.get(i).up) ups++; else downs++; }
        int avail = TABLE_H - TAB_Y - CH - 12;
        while (downs * down + ups * up > avail && up > 14) up--;
        while (downs * down + ups * up > avail && down > 5) down--;
        return new int[]{down, up};
    }

    int cardY(List<Card> col, int pos) {
        int[] o = offsets(col);
        int y = TAB_Y;
        for (int i = 0; i < pos; i++) y += col.get(i).up ? o[1] : o[0];
        return y;
    }

    Rectangle stockRect() { return new Rectangle(colX(0), TOP, CW, CH); }
    Rectangle foundRect(int f) { return new Rectangle(colX(3 + f), TOP, CW, CH); }

    int wasteShown() { return Math.min(st.waste.size(), drawCount == 3 ? 3 : 1); }

    Rectangle wasteTopRect() {
        int shown = wasteShown();
        int x = colX(1) + Math.max(0, shown - 1) * 18;
        return new Rectangle(x, TOP, CW, CH);
    }

    class Table extends JComponent {
        Table() {
            setPreferredSize(new Dimension(W, TABLE_H));
            MouseAdapter m = new MouseAdapter() {
                public void mousePressed(MouseEvent e) { press(e); }
                public void mouseDragged(MouseEvent e) { dragTo(e); }
                public void mouseReleased(MouseEvent e) { release(e); }
                public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) doubleClick(e); }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        void press(MouseEvent e) {
            if (!playing || won || autoFinishing) return;
            int x = e.getX(), y = e.getY();
            pressX = x; pressY = y; dragMoved = false;
            if (stockRect().contains(x, y)) { drawFromStock(); return; }
            if (!st.waste.isEmpty() && wasteTopRect().contains(x, y)) { startDrag(WASTE, 0, st.waste.size() - 1, wasteTopRect().x, TOP, x, y); return; }
            for (int f = 0; f < 4; f++)
                if (foundRect(f).contains(x, y) && !st.found.get(f).isEmpty()) { startDrag(FOUND, f, st.found.get(f).size() - 1, colX(3 + f), TOP, x, y); return; }
            for (int t = 0; t < 7; t++) {
                List<Card> col = st.tab.get(t);
                if (x < colX(t) || x > colX(t) + CW) continue;
                for (int i = col.size() - 1; i >= 0; i--) {
                    int cy = cardY(col, i);
                    if (y >= cy && y <= cy + CH) {
                        if (col.get(i).up) startDrag(TAB, t, i, colX(t), cy, x, y);
                        return;
                    }
                }
            }
        }

        void startDrag(int type, int idx, int pos, int cx, int cy, int mx, int my) {
            List<Card> src = pile(type, idx);
            drag = new ArrayList<>(src.subList(pos, src.size()));
            dragType = type; dragIdx = idx;
            offX = mx - cx; offY = my - cy;
            dragX = cx; dragY = cy;
        }

        void dragTo(MouseEvent e) {
            if (drag == null) return;
            if (Math.abs(e.getX() - pressX) + Math.abs(e.getY() - pressY) > 4) dragMoved = true;
            dragX = e.getX() - offX;
            dragY = e.getY() - offY;
            repaint();
        }

        void release(MouseEvent e) {
            if (drag == null) return;
            List<Card> moving = drag;
            drag = null;
            if (!dragMoved) { repaint(); return; }
            Rectangle r = new Rectangle(dragX, dragY, CW, CH);
            int bestType = -1, bestIdx = -1, bestArea = 0;
            for (int f = 0; f < 4; f++) {
                int a = area(r, foundRect(f));
                if (a > bestArea) { bestArea = a; bestType = FOUND; bestIdx = f; }
            }
            for (int t = 0; t < 7; t++) {
                List<Card> col = st.tab.get(t);
                int top = col.isEmpty() ? TAB_Y : cardY(col, col.size() - 1);
                int a = area(r, new Rectangle(colX(t), top, CW, CH + (col.isEmpty() ? 0 : 30)));
                if (a > bestArea) { bestArea = a; bestType = TAB; bestIdx = t; }
            }
            if (bestType >= 0) tryMove(dragType, dragIdx, moving.size(), bestType, bestIdx);
            repaint();
        }

        int area(Rectangle a, Rectangle b) {
            Rectangle i = a.intersection(b);
            return i.isEmpty() ? 0 : i.width * i.height;
        }

        void doubleClick(MouseEvent e) {
            if (!playing || won || autoFinishing) return;
            int x = e.getX(), y = e.getY();
            if (!st.waste.isEmpty() && wasteTopRect().contains(x, y)) { autoMove(WASTE, 0, 1); return; }
            for (int t = 0; t < 7; t++) {
                List<Card> col = st.tab.get(t);
                if (col.isEmpty() || x < colX(t) || x > colX(t) + CW) continue;
                for (int i = col.size() - 1; i >= 0; i--) {
                    int cy = cardY(col, i);
                    if (y >= cy && y <= cy + CH) {
                        if (col.get(i).up) autoMove(TAB, t, col.size() - i);
                        return;
                    }
                }
            }
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, TABLE, 0, getHeight(), TABLE_2));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 255, 255, 90));
            for (int y = 8; y < getHeight(); y += 28)
                for (int x = (y / 28 % 2) * 14 + 6; x < getWidth(); x += 28) g.fillOval(x, y, 3, 3);

            boolean hinting = System.currentTimeMillis() < hintUntil;

            Rectangle sr = stockRect();
            if (st.stock.isEmpty()) {
                slot(g, sr.x, sr.y, "");
                if (!st.waste.isEmpty()) recycleIcon(g, sr.x + CW / 2, sr.y + CH / 2);
            } else {
                int layers = Math.min(3, (st.stock.size() + 9) / 10);
                for (int i = 0; i < layers; i++) back(g, sr.x + i * 2, sr.y - i * 2);
                g.setFont(new Font(FONT, Font.BOLD, 10));
                g.setColor(SOFT_INK);
                String n = String.valueOf(st.stock.size());
                g.drawString(n, sr.x + CW - g.getFontMetrics().stringWidth(n) - 2, sr.y + CH + 13);
            }
            if (hinting && hintType == STOCK) glow(g, sr.x, sr.y);

            int shown = wasteShown();
            if (st.waste.isEmpty()) slot(g, colX(1), TOP, "");
            boolean draggingWaste = drag != null && dragType == WASTE;
            for (int i = 0; i < shown; i++) {
                int idx = st.waste.size() - shown + i;
                if (draggingWaste && idx == st.waste.size() - 1) continue;
                face(g, st.waste.get(idx), colX(1) + i * 18, TOP);
            }
            if (hinting && hintType == WASTE) glow(g, wasteTopRect().x, TOP);

            for (int f = 0; f < 4; f++) {
                Rectangle fr = foundRect(f);
                List<Card> p = st.found.get(f);
                boolean draggingThis = drag != null && dragType == FOUND && dragIdx == f;
                int visible = p.size() - (draggingThis ? 1 : 0);
                if (visible <= 0) slot(g, fr.x, fr.y, "A");
                else face(g, p.get(visible - 1), fr.x, fr.y);
            }

            for (int t = 0; t < 7; t++) {
                List<Card> col = st.tab.get(t);
                boolean draggingThis = drag != null && dragType == TAB && dragIdx == t;
                int limit = draggingThis ? col.size() - drag.size() : col.size();
                if (col.isEmpty() || limit == 0) slot(g, colX(t), TAB_Y, "K");
                for (int i = 0; i < limit; i++) {
                    Card c = col.get(i);
                    int y = cardY(col, i);
                    if (c.up) face(g, c, colX(t), y); else back(g, colX(t), y);
                }
                if (hinting && hintType == TAB && hintIdx == t && hintPos < col.size()) glow(g, colX(t), cardY(col, hintPos));
            }

            if (hinting && hintType == -2) {
                g.setFont(new Font(FONT, Font.BOLD, 14));
                g.setColor(PINK);
                String s = "Sem jogadas por aqui... tente desfazer ou começar outra!";
                g.drawString(s, (getWidth() - g.getFontMetrics().stringWidth(s)) / 2, TAB_Y - 6);
            }

            if (drag != null && dragMoved) {
                g.setColor(new Color(80, 30, 140, 40));
                g.fillRoundRect(dragX + 4, dragY + 6, CW, CH + (drag.size() - 1) * 26, 12, 12);
            }
            if (drag != null) for (int i = 0; i < drag.size(); i++) face(g, drag.get(i), dragX, dragY + i * 26);
            g.dispose();
        }

        void slot(Graphics2D g, int x, int y, String label) {
            g.setColor(new Color(255, 255, 255, 120));
            g.fillRoundRect(x, y, CW, CH, 12, 12);
            g.setColor(SLOT);
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{6, 5}, 0));
            g.drawRoundRect(x + 1, y + 1, CW - 2, CH - 2, 12, 12);
            g.setStroke(new BasicStroke(1));
            if (!label.isEmpty()) {
                g.setFont(new Font(FONT, Font.BOLD, 26));
                g.setColor(SLOT);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label, x + (CW - fm.stringWidth(label)) / 2, y + (CH + fm.getAscent() - fm.getDescent()) / 2);
            }
        }

        void recycleIcon(Graphics2D g, int cx, int cy) {
            g.setColor(SLOT.darker());
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(cx - 14, cy - 14, 28, 28, 60, 290, Arc2D.OPEN));
            double ang = Math.toRadians(60);
            int ax = (int) (cx + 14 * Math.cos(ang)), ay = (int) (cy - 14 * Math.sin(ang));
            g.fillPolygon(new int[]{ax - 7, ax + 5, ax + 3}, new int[]{ay - 5, ay - 6, ay + 6}, 3);
            g.setStroke(new BasicStroke(1));
        }

        void glow(Graphics2D g, int x, int y) {
            g.setColor(HILITE);
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(x - 2, y - 2, CW + 4, CH + 4, 14, 14);
            g.setStroke(new BasicStroke(1));
        }
    }

    static void back(Graphics2D g, int x, int y) {
        g.setColor(new Color(80, 30, 140, 30));
        g.fillRoundRect(x + 1, y + 2, CW, CH, 12, 12);
        g.setPaint(new GradientPaint(x, y, BACK_1, x + CW, y + CH, BACK_2));
        g.fillRoundRect(x, y, CW, CH, 12, 12);
        Shape old = g.getClip();
        g.clip(new RoundRectangle2D.Double(x + 5, y + 5, CW - 10, CH - 10, 8, 8));
        g.setColor(new Color(255, 255, 255, 55));
        g.setStroke(new BasicStroke(1.2f));
        for (int k = -CH; k < CW + CH; k += 10) {
            g.drawLine(x + k, y, x + k + CH, y + CH);
            g.drawLine(x + k, y + CH, x + k + CH, y);
        }
        g.setClip(old);
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x + 4, y + 4, CW - 8, CH - 8, 8, 8);
        g.setColor(Color.WHITE);
        g.setFont(new Font(FONT, Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("♥", x + (CW - fm.stringWidth("♥")) / 2, y + CH / 2 + 7);
        g.setStroke(new BasicStroke(1));
    }

    static void face(Graphics2D g, Card c, int x, int y) {
        g.setColor(new Color(80, 30, 140, 30));
        g.fillRoundRect(x + 1, y + 2, CW, CH, 12, 12);
        g.setColor(CARD);
        g.fillRoundRect(x, y, CW, CH, 12, 12);
        g.setColor(CARD_EDGE);
        g.drawRoundRect(x, y, CW - 1, CH - 1, 12, 12);
        Color ink = c.pink() ? PINK : PURPLE;
        String r = RANKS[c.rank], s = SUITS[c.suit];

        g.setColor(ink);
        corner(g, r, s, x, y);
        AffineTransform at = g.getTransform();
        g.rotate(Math.PI, x + CW / 2.0, y + CH / 2.0);
        corner(g, r, s, x, y);
        g.setTransform(at);

        if (c.rank >= 11) {
            Color bg = c.pink() ? new Color(0xfce7f3) : new Color(0xede4fd);
            g.setColor(bg);
            g.fillRoundRect(x + 18, y + 22, CW - 36, CH - 44, 10, 10);
            g.setColor(ink);
            g.setFont(new Font(FONT, Font.BOLD, 26));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(r, x + (CW - fm.stringWidth(r)) / 2, y + CH / 2 + 4);
            g.setFont(new Font(FONT, Font.PLAIN, 12));
            fm = g.getFontMetrics();
            g.drawString(s, x + (CW - fm.stringWidth(s)) / 2, y + CH / 2 + 20);
            g.setFont(new Font(FONT, Font.BOLD, 11));
            fm = g.getFontMetrics();
            g.drawString("♛", x + (CW - fm.stringWidth("♛")) / 2, y + 36);
        } else {
            g.setFont(new Font(FONT, Font.PLAIN, c.rank == 1 ? 40 : 30));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, x + (CW - fm.stringWidth(s)) / 2, y + (CH + fm.getAscent() - fm.getDescent()) / 2 + 2);
        }
    }

    static void corner(Graphics2D g, String r, String s, int x, int y) {
        g.setFont(new Font(FONT, Font.BOLD, 15));
        g.drawString(r, x + 6, y + 18);
        int rw = g.getFontMetrics().stringWidth(r);
        g.setFont(new Font(FONT, Font.PLAIN, 13));
        g.drawString(s, x + 7 + rw, y + 18);
    }

    static class Pill extends JComponent {
        final String text; final Color base, fg; boolean hover;
        Pill(String text, Color base, Color fg, int w, int h, Runnable action) {
            this.text = text; this.base = base; this.fg = fg;
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
            Color c = hover ? new Color(Math.max(0, base.getRed() - 18), Math.max(0, base.getGreen() - 18), Math.max(0, base.getBlue() - 10)) : base;
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g.setColor(fg);
            g.setFont(new Font(FONT, Font.BOLD, 12));
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
                g.setColor(new Color(0xfdfaff));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g.setColor(new Color(0xd8c4f5));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
                g.dispose();
            }
        };

        Overlay() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
            add(card);
            addMouseListener(new MouseAdapter() {});
            addMouseMotionListener(new MouseMotionAdapter() {});
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(90, 50, 150, 90));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        JLabel lbl(String html, int size, Color c, boolean bold) {
            JLabel l = new JLabel("<html><div style='text-align:center'>" + html + "</div></html>");
            l.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size));
            l.setForeground(c);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            return l;
        }

        void put(JComponent c, int gap) {
            c.setAlignmentX(CENTER_ALIGNMENT);
            card.add(c);
            if (gap > 0) card.add(Box.createVerticalStrut(gap));
        }

        String stats() {
            String pct = played == 0 ? "0" : String.valueOf(Math.round(100.0 * wins / played));
            String best = bestTime == 0 ? "—" : String.format("%02d:%02d", bestTime / 60, bestTime % 60);
            return played + " jogos  ·  " + wins + " vitórias (" + pct + "%)  ·  recorde " + best;
        }

        void showStart(boolean first) {
            if (!first && playing) clock.stop();
            card.removeAll();
            put(lbl("<span style='color:#8b5cf6'>Paci</span><span style='color:#db2777'>ência</span>", 30, INK, true), 4);
            put(lbl("Como você quer jogar, diva?", 14, SOFT_INK, false), 16);
            put(new Pill("Fácil  ·  vira 1 carta", BTN, Color.WHITE, 250, 42, () -> newGame(1)), 8);
            put(new Pill("Desafio  ·  vira 3 cartas", PINK, Color.WHITE, 250, 42, () -> newGame(3)), 12);
            put(lbl(stats(), 11, SOFT_INK, false), 0);
            if (!first && playing && !won) {
                card.add(Box.createVerticalStrut(10));
                put(new Pill("Voltar pra partida", BTN_SOFT, INK, 250, 36, () -> { setVisible(false); clock.start(); }), 0);
            }
            open();
        }

        void showWin(boolean record) {
            card.removeAll();
            put(lbl(record ? "NOVO RECORDE! ♛" : "ZERADA! SLAY ♥", 26, PINK, true), 6);
            put(lbl("Tempo " + String.format("%02d:%02d", seconds / 60, seconds % 60) + "  ·  " + st.moves + " jogadas", 14, INK, true), 6);
            put(lbl(stats(), 11, SOFT_INK, false), 16);
            put(new Pill("Próxima partida  ·  " + (drawCount == 1 ? "vira 1" : "vira 3"), BTN, Color.WHITE, 260, 42, () -> newGame(drawCount)), 8);
            put(new Pill("Trocar modo", BTN_SOFT, INK, 260, 36, () -> showStart(true)), 0);
            open();
        }

        void open() {
            card.revalidate();
            setVisible(true);
            revalidate();
            repaint();
        }
    }

    public void hubPause() { clock.stop(); }
    public void hubResume() { if (playing && !won && !overlay.isVisible()) clock.start(); }
    public void hubStop() { clock.stop(); auto.stop(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new paciencia().setVisible(true));
    }
}