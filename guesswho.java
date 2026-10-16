import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

public class guesswho extends JFrame {

    static final Color BG = new Color(0x1a0b2e);
    static final Color PANEL = new Color(0x2a1650);
    static final Color CARD = new Color(0x3a2366);
    static final Color CARD_HOVER = new Color(0x4a2d80);
    static final Color DOWN = new Color(0x221338);
    static final Color TEXT = new Color(0xf3e8ff);
    static final Color MUTED = new Color(0xa78bca);
    static final Color ACCENT = new Color(0xc084fc);
    static final Color BTN = new Color(0x7c3aed);
    static final Color PINK = new Color(0xec4899);
    static final Color YES = new Color(0xa855f7);
    static final Color DIM = new Color(0x2c2140);
    static final String FONT = "SansSerif";

    static final String[] DATA = {
            "Taylor Swift|C|F|1989|NA|EUA|", "Beyoncé|C|F|1981|NA|EUA|b", "Rihanna|C|F|1988|LA|Barbados|m",
            "Ariana Grande|C|F|1993|NA|EUA|dm", "Billie Eilish|C|F|2001|NA|EUA|o", "Olivia Rodrigo|C|F|2003|NA|EUA|d",
            "Sabrina Carpenter|C|F|1999|NA|EUA|d", "Dua Lipa|C|F|1995|EU|Reino Unido|", "Harry Styles|C|M|1994|EU|Reino Unido|bs",
            "Ed Sheeran|C|M|1991|EU|Reino Unido|", "Bruno Mars|C|M|1985|NA|EUA|b", "The Weeknd|C|M|1990|NA|Canadá|",
            "Drake|C|M|1986|NA|Canadá|", "Justin Bieber|C|M|1994|NA|Canadá|", "Bad Bunny|C|M|1994|LA|Porto Rico|",
            "Shakira|C|F|1977|LA|Colômbia|", "Anitta|C|F|1993|LA|Brasil|", "Lady Gaga|C|F|1986|NA|EUA|som",
            "Katy Perry|C|F|1984|NA|EUA|", "Selena Gomez|C|F|1992|NA|EUA|dmb", "Camila Cabello|C|F|1997|LA|Cuba|b",
            "Ivete Sangalo|C|F|1972|LA|Brasil|b", "Ludmilla|C|F|1995|LA|Brasil|",
            "Zendaya|A|F|1996|NA|EUA|ds", "Tom Holland|A|M|1996|EU|Reino Unido|s", "Timothée Chalamet|A|M|1995|NA|EUA|",
            "Margot Robbie|A|F|1990|OC|Austrália|s", "Leonardo DiCaprio|A|M|1974|NA|EUA|o", "Emma Stone|A|F|1988|NA|EUA|so",
            "Jennifer Lawrence|A|F|1990|NA|EUA|so", "Ryan Gosling|A|M|1980|NA|Canadá|db", "Ryan Reynolds|A|M|1976|NA|Canadá|s",
            "Dwayne Johnson|A|M|1972|NA|EUA|s", "Pedro Pascal|A|M|1975|LA|Chile|s", "Jenna Ortega|A|F|2002|NA|EUA|d",
            "Scarlett Johansson|A|F|1984|NA|EUA|s", "Chris Hemsworth|A|M|1983|OC|Austrália|s", "Anne Hathaway|A|F|1982|NA|EUA|so",
            "Will Smith|A|M|1968|NA|EUA|sob", "Fernanda Torres|A|F|1965|LA|Brasil|", "Bruna Marquezine|A|F|1995|LA|Brasil|s",
            "Neymar|E|M|1992|LA|Brasil|", "Lionel Messi|E|M|1987|LA|Argentina|", "Cristiano Ronaldo|E|M|1985|EU|Portugal|",
            "Serena Williams|E|F|1981|NA|EUA|", "Simone Biles|E|F|1997|NA|EUA|", "LeBron James|E|M|1984|NA|EUA|",
            "Rebeca Andrade|E|F|1999|LA|Brasil|",
            "Kim Kardashian|T|F|1980|NA|EUA|m", "Kylie Jenner|T|F|1997|NA|EUA|m", "Xuxa|T|F|1963|LA|Brasil|",
    };
    static final String[] PROF = {"C", "A", "E", "T"};
    static final String[] PROF_NAME = {"Cantor(a)", "Ator / atriz", "Atleta", "TV e negócios"};
    static final Color[] AVATAR = {new Color(0xa855f7), new Color(0xec4899), new Color(0x8b5cf6), new Color(0xd946ef),
            new Color(0x7c3aed), new Color(0xf472b6), new Color(0x9333ea), new Color(0xc026d3)};

    static class Celeb {
        final String name, prof, sex, region, country, flags; final int year;
        Celeb(String s) {
            String[] p = s.split("\\|", -1);
            name = p[0]; prof = p[1]; sex = p[2]; year = Integer.parseInt(p[3]); region = p[4]; country = p[5]; flags = p[6];
        }
        boolean has(char f) { return flags.indexOf(f) >= 0; }
        String initials() {
            String[] w = name.split(" ");
            return w.length == 1 ? name.substring(0, Math.min(2, name.length())).toUpperCase() : ("" + w[0].charAt(0) + w[w.length - 1].charAt(0)).toUpperCase();
        }
        String profName() {
            int i = Arrays.asList(PROF).indexOf(prof);
            String p = PROF_NAME[i];
            if (i == 0) p = sex.equals("F") ? "Cantora" : "Cantor";
            if (i == 1) p = sex.equals("F") ? "Atriz" : "Ator";
            return p;
        }
    }
    static final List<Celeb> ALL = new ArrayList<>();
    static { for (String s : DATA) ALL.add(new Celeb(s)); }

    static class Q {
        final String text; final Predicate<Celeb> test;
        Q(String text, Predicate<Celeb> test) { this.text = text; this.test = test; }
    }
    static final List<Q> QUESTIONS = List.of(
            new Q("É mulher?", c -> c.sex.equals("F")),
            new Q("É cantor(a)?", c -> c.prof.equals("C")),
            new Q("É ator / atriz?", c -> c.prof.equals("A")),
            new Q("É atleta?", c -> c.prof.equals("E")),
            new Q("É famoso(a) pela TV e pelos negócios?", c -> c.prof.equals("T")),
            new Q("É dos EUA ou do Canadá?", c -> c.region.equals("NA")),
            new Q("É dos EUA? (Porto Rico não conta)", c -> c.country.equals("EUA")),
            new Q("É da América Latina ou do Caribe?", c -> c.region.equals("LA")),
            new Q("É brasileiro(a)?", c -> c.country.equals("Brasil")),
            new Q("É da Europa?", c -> c.region.equals("EU")),
            new Q("É da Austrália?", c -> c.region.equals("OC")),
            new Q("Nasceu antes de 1980?", c -> c.year < 1980),
            new Q("Nasceu nos anos 80?", c -> c.year >= 1980 && c.year < 1990),
            new Q("Nasceu nos anos 90 ou depois?", c -> c.year >= 1990),
            new Q("Nasceu antes de 1987?", c -> c.year < 1987),
            new Q("Nasceu antes de 1993?", c -> c.year < 1993),
            new Q("Começou na Disney ou na Nickelodeon?", c -> c.has('d')),
            new Q("Já fez parte de uma banda ou grupo?", c -> c.has('b')),
            new Q("Já atuou em filme de super-herói?", c -> c.has('s')),
            new Q("Já ganhou um Oscar?", c -> c.has('o')),
            new Q("Tem (ou teve) marca própria de maquiagem?", c -> c.has('m')),
            new Q("O nome na carta é uma palavra só?", c -> !c.name.contains(" ")),
            new Q("O primeiro nome começa com vogal?", c -> "AEIOUÁÉÍÓÚ".indexOf(Character.toUpperCase(c.name.charAt(0))) >= 0),
            new Q("O nome tem a letra “Y”?", c -> c.name.toUpperCase().contains("Y")));

    static final int BOARD_N = 24, COLS = 6;
    final Preferences prefs = tryPrefs();
    final Random rng = new Random();
    List<Celeb> board = new ArrayList<>();
    boolean[] down = new boolean[BOARD_N];
    boolean[] cpuDown = new boolean[BOARD_N];
    Celeb mySecret, cpuSecret;
    boolean versus = true, hard = false, autoFlip = true, showInfo = false;
    boolean myTurn = true, guessing = false, over = false, won = false;
    final Set<Q> asked = new HashSet<>(), cpuAsked = new HashSet<>();
    int myQuestions = 0;
    String lastAnswer = " ", cpuLog = " ";
    int wins, losses, bestSolo;

    final Board boardView = new Board();
    final JPanel qPanel = new JPanel();
    final JLabel answerLbl = new JLabel(" "), turnLbl = new JLabel(), cpuLbl = new JLabel(), statsLbl = new JLabel();
    final Pill guessBtn = new Pill("Chutar", PINK, 230, 40, this::toggleGuess);
    final Overlay overlay = new Overlay();

    public guesswho() {
        super("Cara a Cara");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        if (prefs != null) { wins = prefs.getInt("wins", 0); losses = prefs.getInt("losses", 0); bestSolo = prefs.getInt("bestSolo", 0); }

        JPanel root = new JPanel(new BorderLayout(14, 0));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setContentPane(root);

        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("<html><span style='color:#c084fc'>Cara</span> a <span style='color:#ec4899'>Cara</span></html>");
        title.setFont(new Font(FONT, Font.BOLD, 24));
        title.setForeground(TEXT);
        head.add(title, BorderLayout.WEST);
        turnLbl.setFont(new Font(FONT, Font.BOLD, 13));
        turnLbl.setForeground(ACCENT);
        head.add(turnLbl, BorderLayout.EAST);
        left.add(head, BorderLayout.NORTH);
        left.add(boardView, BorderLayout.CENTER);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tools.setOpaque(false);
        Pill autoBtn = new Pill("", DIM, 190, 32, null);
        autoBtn.action = () -> { autoFlip = !autoFlip; autoBtn.text = autoFlip ? "Abaixar sozinho: ON" : "Abaixar sozinho: OFF"; autoBtn.repaint(); };
        autoBtn.text = "Abaixar sozinho: ON";
        Pill infoBtn = new Pill("", DIM, 170, 32, null);
        infoBtn.action = () -> { showInfo = !showInfo; infoBtn.text = showInfo ? "Fichas nas cartas: ON" : "Fichas nas cartas: OFF"; infoBtn.repaint(); boardView.repaint(); };
        infoBtn.text = "Fichas nas cartas: OFF";
        tools.add(autoBtn);
        tools.add(infoBtn);
        tools.add(new Pill("Novo jogo", BTN, 110, 32, () -> overlay.showStart()));
        left.add(tools, BorderLayout.SOUTH);
        root.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(250, 10));
        JPanel rtop = new JPanel();
        rtop.setOpaque(false);
        rtop.setLayout(new BoxLayout(rtop, BoxLayout.Y_AXIS));
        answerLbl.setFont(new Font(FONT, Font.BOLD, 14));
        answerLbl.setForeground(TEXT);
        answerLbl.setAlignmentX(LEFT_ALIGNMENT);
        cpuLbl.setFont(new Font(FONT, Font.PLAIN, 12));
        cpuLbl.setForeground(MUTED);
        cpuLbl.setAlignmentX(LEFT_ALIGNMENT);
        JLabel qt = new JLabel("FAÇA UMA PERGUNTA");
        qt.setFont(new Font(FONT, Font.BOLD, 10));
        qt.setForeground(MUTED);
        qt.setAlignmentX(LEFT_ALIGNMENT);
        rtop.add(answerLbl);
        rtop.add(Box.createVerticalStrut(6));
        rtop.add(cpuLbl);
        rtop.add(Box.createVerticalStrut(10));
        rtop.add(qt);
        right.add(rtop, BorderLayout.NORTH);
        qPanel.setOpaque(false);
        qPanel.setLayout(new BoxLayout(qPanel, BoxLayout.Y_AXIS));
        JScrollPane qs = new JScrollPane(qPanel);
        qs.setBorder(null);
        qs.setOpaque(false);
        qs.getViewport().setOpaque(false);
        qs.getVerticalScrollBar().setUnitIncrement(14);
        purpleScroll(qs);
        right.add(qs, BorderLayout.CENTER);
        JPanel rb = new JPanel();
        rb.setOpaque(false);
        rb.setLayout(new BoxLayout(rb, BoxLayout.Y_AXIS));
        guessBtn.setAlignmentX(LEFT_ALIGNMENT);
        rb.add(guessBtn);
        rb.add(Box.createVerticalStrut(6));
        statsLbl.setFont(new Font(FONT, Font.PLAIN, 11));
        statsLbl.setForeground(new Color(0x7b6a9b));
        statsLbl.setAlignmentX(LEFT_ALIGNMENT);
        rb.add(statsLbl);
        right.add(rb, BorderLayout.SOUTH);
        root.add(right, BorderLayout.EAST);

        setGlassPane(overlay);
        newGame(true, false);
        pack();
        setLocationRelativeTo(null);
        overlay.showStart();
    }

    static Preferences tryPrefs() {
        try { return Preferences.userRoot().node("guesswho-diva"); } catch (Exception e) { return null; }
    }

    void saveStats() {
        if (prefs == null) return;
        try { prefs.putInt("wins", wins); prefs.putInt("losses", losses); prefs.putInt("bestSolo", bestSolo); } catch (Exception ignored) {}
    }

    void newGame(boolean vs, boolean hardCpu) {
        versus = vs; hard = hardCpu;
        List<Celeb> pool = new ArrayList<>(ALL);
        Collections.shuffle(pool, rng);
        board = new ArrayList<>(pool.subList(0, BOARD_N));
        down = new boolean[BOARD_N];
        cpuDown = new boolean[BOARD_N];
        mySecret = board.get(rng.nextInt(BOARD_N));
        cpuSecret = board.get(rng.nextInt(BOARD_N));
        asked.clear(); cpuAsked.clear();
        myQuestions = 0;
        myTurn = true; guessing = false; over = false; won = false;
        lastAnswer = "Sua vez! Escolha uma pergunta.";
        cpuLog = versus ? "<html>O computador escolheu um famoso pra você adivinhar.<br>Você ficou com <b style='color:#f472b6'>" + cpuSecret.name + "</b> (ele vai tentar adivinhar).</html>"
                : "<html>Modo solo: adivinhe com o mínimo de perguntas!</html>";
        overlay.setVisible(false);
        buildQuestions();
        refresh();
    }

    void buildQuestions() {
        qPanel.removeAll();
        for (Q q : QUESTIONS) {
            Pill p = new Pill(q.text, asked.contains(q) ? DIM : CARD, 236, 34, () -> ask(q));
            p.small = true;
            p.on = !asked.contains(q) && myTurn && !over && !guessing;
            p.setAlignmentX(LEFT_ALIGNMENT);
            qPanel.add(p);
            qPanel.add(Box.createVerticalStrut(5));
        }
        qPanel.revalidate();
        qPanel.repaint();
    }

    void ask(Q q) {
        if (!myTurn || over || guessing || asked.contains(q)) return;
        asked.add(q);
        myQuestions++;
        boolean ans = q.test.test(mySecret);
        lastAnswer = "<html>" + esc(q.text) + "<br><span style='font-size:20px;color:" + (ans ? "#c084fc" : "#f472b6") + "'>" + (ans ? "SIM" : "NÃO") + "</span></html>";
        if (autoFlip) for (int i = 0; i < BOARD_N; i++) if (q.test.test(board.get(i)) != ans) down[i] = true;
        if (versus) { myTurn = false; refresh(); later(1100, this::cpuTurn); }
        else refresh();
    }

    void cpuTurn() {
        if (over) return;
        List<Integer> alive = new ArrayList<>();
        for (int i = 0; i < BOARD_N; i++) if (!cpuDown[i]) alive.add(i);
        if (alive.size() == 1) {
            Celeb g = board.get(alive.get(0));
            cpuLog = "<html>O computador chutou: <b>" + esc(g.name) + "</b>!</html>";
            if (g == cpuSecret) finish(false, "O computador adivinhou seu famoso: " + cpuSecret.name);
            myTurn = true; refresh();
            return;
        }
        Q best = null; double bestScore = -1;
        List<Q> useful = new ArrayList<>();
        for (Q q : QUESTIONS) {
            if (cpuAsked.contains(q)) continue;
            int yes = 0;
            for (int i : alive) if (q.test.test(board.get(i))) yes++;
            if (yes == 0 || yes == alive.size()) continue;
            useful.add(q);
            double score = Math.min(yes, alive.size() - yes);
            if (score > bestScore) { bestScore = score; best = q; }
        }
        Q q = hard || useful.isEmpty() ? best : useful.get(rng.nextInt(useful.size()));
        if (q == null) {
            Celeb g = board.get(alive.get(rng.nextInt(alive.size())));
            cpuLog = "<html>O computador chutou: <b>" + esc(g.name) + "</b>...</html>";
            if (g == cpuSecret) finish(false, "O computador adivinhou seu famoso: " + cpuSecret.name);
            else cpuDown[board.indexOf(g)] = true;
            myTurn = true; refresh();
            return;
        }
        cpuAsked.add(q);
        boolean ans = q.test.test(cpuSecret);
        for (int i : alive) if (q.test.test(board.get(i)) != ans) cpuDown[i] = true;
        int left = 0;
        for (boolean d : cpuDown) if (!d) left++;
        cpuLog = "<html>Computador perguntou: <i>" + esc(q.text) + "</i><br>Você (" + esc(cpuSecret.name) + ") respondeu <b>" + (ans ? "SIM" : "NÃO")
                + "</b>  ·  restam <b>" + left + "</b> pra ele</html>";
        myTurn = true;
        refresh();
    }

    void toggleGuess() {
        if (over || !myTurn) return;
        guessing = !guessing;
        refresh();
    }

    void guess(int i) {
        Celeb c = board.get(i);
        guessing = false;
        if (c == mySecret) {
            finish(true, "Era " + mySecret.name + "! Você usou " + myQuestions + (myQuestions == 1 ? " pergunta." : " perguntas."));
        } else {
            finish(false, "Não era " + c.name + "... era " + mySecret.name + "!");
        }
    }

    void finish(boolean win, String msg) {
        over = true; won = win;
        if (win) { wins++; if (!versus && (bestSolo == 0 || myQuestions < bestSolo)) bestSolo = myQuestions; } else losses++;
        saveStats();
        refresh();
        later(700, () -> overlay.showEnd(msg));
    }

    void later(int ms, Runnable r) {
        javax.swing.Timer t = new javax.swing.Timer(ms, e -> r.run());
        t.setRepeats(false);
        t.start();
    }

    void refresh() {
        int up = 0;
        for (boolean d : down) if (!d) up++;
        turnLbl.setText(over ? (won ? "Você venceu! ♛" : "Fim de jogo") : guessing ? "Clique na carta do seu chute" : myTurn ? "Sua vez  ·  " + up + " em pé" : "Vez do computador...");
        answerLbl.setText(lastAnswer.startsWith("<html>") ? lastAnswer : "<html>" + esc(lastAnswer) + "</html>");
        cpuLbl.setText(cpuLog);
        guessBtn.text = guessing ? "Cancelar chute" : "Chutar quem é";
        guessBtn.on = myTurn && !over;
        guessBtn.repaint();
        statsLbl.setText(wins + " vitórias · " + losses + " derrotas" + (bestSolo > 0 ? " · melhor solo: " + bestSolo + " perguntas" : ""));
        buildQuestions();
        boardView.repaint();
    }

    static final int CW = 104, CH = 128, GAP = 10;
    class Board extends JComponent {
        int hover = -1;
        Board() {
            setPreferredSize(new Dimension(COLS * CW + (COLS - 1) * GAP, 4 * CH + 3 * GAP));
            MouseAdapter m = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    int i = at(e.getX(), e.getY());
                    if (i < 0 || over || overlay.isVisible()) return;
                    if (guessing) { if (!down[i]) guess(i); return; }
                    down[i] = !down[i];
                    refresh();
                }
                public void mouseMoved(MouseEvent e) { int i = at(e.getX(), e.getY()); if (i != hover) { hover = i; repaint(); } }
                public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        int ox() { return (getWidth() - (COLS * CW + (COLS - 1) * GAP)) / 2; }
        Rectangle rect(int i) { return new Rectangle(ox() + (i % COLS) * (CW + GAP), (i / COLS) * (CH + GAP), CW, CH); }
        int at(int x, int y) { for (int i = 0; i < BOARD_N; i++) if (rect(i).contains(x, y)) return i; return -1; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (int i = 0; i < board.size(); i++) {
                Celeb c = board.get(i);
                Rectangle r = rect(i);
                boolean d = down[i];
                boolean reveal = over && c == mySecret;
                if (d && !reveal) {
                    g.setColor(DOWN);
                    g.fillRoundRect(r.x, r.y + CH - 40, CW, 40, 12, 12);
                    g.setColor(new Color(0x5b4a7a));
                    g.setFont(new Font(FONT, Font.BOLD, 10));
                    drawCentered(g, clip(g, c.name, CW - 10), r.x, CW, r.y + CH - 16);
                    if (i == hover && !guessing) {
                        g.setColor(new Color(0x7b6a9b));
                        g.setFont(new Font(FONT, Font.PLAIN, 9));
                        drawCentered(g, "clique pra levantar", r.x, CW, r.y + CH - 50);
                    }
                    continue;
                }
                g.setColor(i == hover ? CARD_HOVER : CARD);
                g.fillRoundRect(r.x, r.y, CW, CH, 14, 14);
                if (reveal || (guessing && i == hover)) {
                    g.setColor(guessing ? PINK : ACCENT);
                    g.setStroke(new BasicStroke(3));
                    g.drawRoundRect(r.x + 1, r.y + 1, CW - 3, CH - 3, 14, 14);
                    g.setStroke(new BasicStroke(1));
                }
                Color av = AVATAR[Math.abs(c.name.hashCode()) % AVATAR.length];
                int s = 54, ax = r.x + (CW - s) / 2, ay = r.y + 12;
                g.setPaint(new GradientPaint(ax, ay, av.brighter(), ax + s, ay + s, av.darker()));
                g.fillOval(ax, ay, s, s);
                g.setColor(Color.WHITE);
                g.setFont(new Font(FONT, Font.BOLD, 20));
                FontMetrics fm = g.getFontMetrics();
                String ini = c.initials();
                g.drawString(ini, ax + (s - fm.stringWidth(ini)) / 2, ay + (s + fm.getAscent() - fm.getDescent()) / 2);
                g.setColor(TEXT);
                g.setFont(new Font(FONT, Font.BOLD, 12));
                List<String> lines = wrap(g, c.name, CW - 10);
                int y = r.y + 84;
                for (String l : lines.subList(0, Math.min(2, lines.size()))) { drawCentered(g, l, r.x, CW, y); y += 14; }
                if (showInfo) {
                    g.setFont(new Font(FONT, Font.PLAIN, 9));
                    g.setColor(MUTED);
                    drawCentered(g, clip(g, c.profName() + " · " + c.country + " · " + c.year, CW - 8), r.x, CW, r.y + CH - 8);
                }
            }
            g.dispose();
        }
    }

    static void drawCentered(Graphics2D g, String s, int x, int w, int y) {
        g.drawString(s, x + (w - g.getFontMetrics().stringWidth(s)) / 2, y);
    }

    static List<String> wrap(Graphics2D g, String s, int width) {
        FontMetrics fm = g.getFontMetrics();
        List<String> out = new ArrayList<>();
        String line = "";
        for (String word : s.split(" ")) {
            String t = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(t) <= width) line = t;
            else { if (!line.isEmpty()) out.add(line); line = word; }
        }
        if (!line.isEmpty()) out.add(line);
        return out;
    }

    static String clip(Graphics2D g, String s, int max) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= max) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;"); }

    static void purpleScroll(JScrollPane sp) {
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar bar = sp.getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(8, 0));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = new Color(0x5b3a8f); trackColor = new Color(0x1a0b2e); }
            @Override protected JButton createDecreaseButton(int o) { return zero(); }
            @Override protected JButton createIncreaseButton(int o) { return zero(); }
            JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
            @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(thumbColor);
                g.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 6, 6);
                g.dispose();
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { g.setColor(trackColor); g.fillRect(r.x, r.y, r.width, r.height); }
        });
    }

    static class Pill extends JComponent {
        String text; final Color base; boolean hover, on = true, small, ring; Runnable action;
        Pill(String text, Color base, int w, int h, Runnable action) {
            this.text = text; this.base = base; this.action = action;
            Dimension d = new Dimension(w, h);
            setPreferredSize(d); setMaximumSize(d); setMinimumSize(d);
            setAlignmentX(CENTER_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseReleased(MouseEvent e) { if (on && contains(e.getPoint()) && Pill.this.action != null) Pill.this.action.run(); }
            });
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color c = on ? base : new Color(0x221a33);
            if (hover && on) c = new Color(Math.min(255, c.getRed() + 25), Math.min(255, c.getGreen() + 25), Math.min(255, c.getBlue() + 25));
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            if (ring) { g.setColor(Color.WHITE); g.setStroke(new BasicStroke(2)); g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14); }
            g.setColor(on ? TEXT : new Color(0x5e4d7d));
            g.setFont(new Font(FONT, small ? Font.PLAIN : Font.BOLD, small ? 11 : 13));
            FontMetrics fm = g.getFontMetrics();
            String t = clip(g, text, getWidth() - 16);
            int x = small ? 12 : (getWidth() - fm.stringWidth(t)) / 2;
            g.drawString(t, x, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    class Overlay extends JPanel {
        boolean pickVs = true, pickHard = false;
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
            card.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));
            add(card);
            addMouseListener(new MouseAdapter() {});
        }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(20, 8, 36, 215));
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
        void showStart() {
            card.removeAll();
            put(lbl("<span style='color:#c084fc'>Cara</span> a <span style='color:#ec4899'>Cara</span>", 30, TEXT, true), 4);
            put(lbl("das celebridades: cantores, atores, atletas e famosos da TV", 13, MUTED, false), 16);
            JPanel modes = new JPanel(new GridLayout(1, 2, 8, 0));
            modes.setOpaque(false);
            modes.setMaximumSize(new Dimension(380, 40));
            Pill a = new Pill("Contra o computador", pickVs ? YES : DIM, 186, 40, () -> { pickVs = true; showStart(); });
            Pill b = new Pill("Solo", !pickVs ? PINK : DIM, 186, 40, () -> { pickVs = false; showStart(); });
            a.ring = pickVs; b.ring = !pickVs;
            modes.add(a); modes.add(b);
            put(modes, 10);
            if (pickVs) {
                JPanel lv = new JPanel(new GridLayout(1, 2, 8, 0));
                lv.setOpaque(false);
                lv.setMaximumSize(new Dimension(380, 34));
                Pill e = new Pill("Computador: fácil", !pickHard ? BTN : DIM, 186, 34, () -> { pickHard = false; showStart(); });
                Pill h = new Pill("Computador: esperto", pickHard ? BTN : DIM, 186, 34, () -> { pickHard = true; showStart(); });
                e.ring = !pickHard; h.ring = pickHard;
                lv.add(e); lv.add(h);
                put(lv, 14);
            } else card.add(Box.createVerticalStrut(14));
            put(new Pill("Começar", PINK, 260, 44, () -> newGame(pickVs, pickHard)), 10);
            put(lbl("24 famosos por partida, sorteados entre " + ALL.size() + ".<br>Perguntas sobre fatos, nunca sobre aparência.", 11, new Color(0x7b6a9b), false), 0);
            open();
        }
        void showEnd(String msg) {
            card.removeAll();
            put(lbl(won ? "ACERTOU! SLAY ♛" : "Ahhh, não foi dessa vez", 26, won ? ACCENT : PINK, true), 8);
            put(lbl(esc(msg), 14, TEXT, false), 6);
            put(lbl(mySecret.profName() + " · " + mySecret.country + " · nasceu em " + mySecret.year, 12, MUTED, false), 16);
            put(new Pill("Jogar de novo", BTN, 260, 42, () -> newGame(versus, hard)), 8);
            put(new Pill("Trocar modo", DIM, 260, 36, this::showStart), 8);
            put(new Pill("Ver o tabuleiro", DIM, 260, 32, () -> setVisible(false)), 0);
            open();
        }
        void open() { card.revalidate(); setVisible(true); revalidate(); repaint(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new guesswho().setVisible(true));
    }
}