import javax.swing.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class gamehub extends JFrame {

    static final Color BG = new Color(0x1a0b2e);
    static final Color CARD = new Color(0x2a1650);
    static final Color CARD_HOVER = new Color(0x3a1f6b);
    static final Color BORDER = new Color(0x4c1d95);
    static final Color TEXT = new Color(0xf3e8ff);
    static final Color MUTED = new Color(0xa78bca);
    static final Color ACCENT = new Color(0xc084fc);
    static final Color PINK = new Color(0xec4899);
    static final Color BTN = new Color(0x7c3aed);
    static final Color DIM = new Color(0x2c2140);
    static final Color BAR = new Color(0x120720);
    static final String FONT = "SansSerif";

    static class Game {
        final String file, title, desc, tag; final Color color; final int icon;
        ClassLoader loader; long stamp;
        Game(String file, String title, String desc, String tag, Color color, int icon) {
            this.file = file; this.title = title; this.desc = desc; this.tag = tag; this.color = color; this.icon = icon;
        }
        String className() { return file.substring(0, file.length() - 5); }
    }

    static final List<Game> GAMES = List.of(
            new Game("sudoku.java", "Sudoku", "Fácil, médio e difícil, infinito.", "LÓGICA", new Color(0xa855f7), 0),
            new Game("tetris.java", "Tetris", "Peças roxinhas, recorde salvo e hold.", "ARCADE", new Color(0xd946ef), 1),
            new Game("wordle.java", "Wordle", "A palavra em inglês em 6 tentativas.", "PALAVRAS", new Color(0xec4899), 2),
            new Game("swiftle.java", "Swiftle", "Adivinhe a música da Taylor pelas pistas.", "TAYLOR", new Color(0xCBA863), 3),
            new Game("swiftrank.java", "Swift Rank", "Ranking ou chaveamento por álbum.", "TAYLOR", new Color(0x8BB5D2), 4),
            new Game("featconnect.java", "Feat Chain", "Ligue dois artistas pelos feats deles.", "MÚSICA", new Color(0x7c3aed), 5),
            new Game("paciencia.java", "Paciência", "Cartas roxas e rosas numa mesa pastel.", "CARTAS", new Color(0xf472b6), 6),
            new Game("game2048.java", "2048", "Junte os números até o 2048.", "NÚMEROS", new Color(0x9333ea), 7),
            new Game("campominado.java", "Campo Minado", "Fácil, médio e difícil. Cuidado!", "LÓGICA", new Color(0x6d28d9), 8),
            new Game("hitorflop.java", "Hit ou Flop", "Qual chegou mais alto na Billboard?", "MÚSICA", new Color(0xdb2777), 9),
            new Game("conexoes.java", "Conexões", "16 artistas, 4 grupos escondidos.", "MÚSICA", new Color(0xa855f7), 10));

    static final int W = 900, H = 640, BAR_H = 44;
    final Preferences prefs = tryPrefs();
    Path folder;

    final JPanel menu = new JPanel(new BorderLayout(0, 14));
    final JPanel stage = new JPanel(new BorderLayout());
    final JLabel status = new JLabel(" ");
    final JLabel folderLbl = new JLabel();
    final List<GameCard> cards = new ArrayList<>();
    final PauseOverlay pause = new PauseOverlay();

    Game current, loading;
    JFrame gameFrame;
    JRootPane gameRoot;
    final JLabel barTitle = new JLabel();

    public gamehub() {
        super("My Game Hub");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        folder = findFolder();
        buildMenu();
        buildStage();
        setGlassPane(pause);
        setContentPane(menu);
        updateFolder();
        pack();
        setLocationRelativeTo(null);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED || e.getKeyCode() != KeyEvent.VK_ESCAPE || current == null || !isActive()) return false;
            if (pause.isVisible()) resumeGame(); else pauseGame();
            return true;
        });
    }

    static Preferences tryPrefs() {
        try { return Preferences.userRoot().node("gamehub-diva"); } catch (Exception e) { return null; }
    }

    void buildMenu() {
        menu.setBackground(BG);
        menu.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        menu.setPreferredSize(new Dimension(W, H));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("<html><span style='color:#c084fc'>My Game</span> Hub</html>");
        title.setFont(new Font(FONT, Font.BOLD, 30));
        title.setForeground(TEXT);
        head.add(title);
        head.add(Box.createVerticalStrut(2));
        JLabel sub = new JLabel("Qual vai ser hoje, diva? Escolhe e arrasa ♛");
        sub.setFont(new Font(FONT, Font.PLAIN, 14));
        sub.setForeground(MUTED);
        head.add(sub);
        menu.add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 4, 12, 12));
        grid.setOpaque(false);
        for (Game g : GAMES) {
            GameCard c = new GameCard(g);
            cards.add(c);
            grid.add(c);
        }
        for (int i = GAMES.size(); i < 12; i++) grid.add(new SoonCard());
        menu.add(grid, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout(8, 0));
        foot.setOpaque(false);
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        status.setFont(new Font(FONT, Font.BOLD, 12));
        status.setForeground(ACCENT);
        folderLbl.setFont(new Font(FONT, Font.PLAIN, 10));
        folderLbl.setForeground(new Color(0x6f5a90));
        left.add(status);
        left.add(Box.createVerticalStrut(3));
        left.add(folderLbl);
        foot.add(left, BorderLayout.CENTER);
        foot.add(new Pill("Trocar pasta", DIM, 120, 34, this::chooseFolder), BorderLayout.EAST);
        menu.add(foot, BorderLayout.SOUTH);
    }

    void buildStage() {
        stage.setBackground(BG);
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(BAR);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(BORDER);
                g.fillRect(0, getHeight() - 2, getWidth(), 2);
                g.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(10, BAR_H));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new Pill("‹ Jogos", DIM, 76, 30, this::backToMenu));
        barTitle.setFont(new Font(FONT, Font.BOLD, 15));
        barTitle.setForeground(TEXT);
        left.add(barTitle);
        bar.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(new Pill("Pausar  ·  Esc", BTN, 116, 30, this::pauseGame));
        bar.add(right, BorderLayout.EAST);
        stage.add(bar, BorderLayout.NORTH);
    }

    static int countGames(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return 0;
        int n = 0;
        for (Game g : GAMES) if (Files.exists(dir.resolve(g.file))) n++;
        return n;
    }

    Path findFolder() {
        List<Path> candidates = new ArrayList<>();
        if (prefs != null) {
            String saved = prefs.get("folder", null);
            if (saved != null) try { candidates.add(Paths.get(saved)); } catch (Exception ignored) {}
        }
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        candidates.add(cwd);
        candidates.add(cwd.resolve("my-game-hub"));
        try {
            Path p = Paths.get(gamehub.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (int i = 0; i < 5 && p != null; i++, p = p.getParent()) {
                candidates.add(p);
                candidates.add(p.resolve("my-game-hub"));
            }
        } catch (Exception ignored) {}
        Path home = Paths.get(System.getProperty("user.home"));
        for (String s : new String[]{"my-game-hub", "Desktop/my-game-hub", "Documents/my-game-hub",
                "Área de Trabalho/my-game-hub", "Documentos/my-game-hub", "OneDrive/Desktop/my-game-hub",
                "OneDrive/Documents/my-game-hub", "OneDrive/Área de Trabalho/my-game-hub"})
            try { candidates.add(home.resolve(s)); } catch (Exception ignored) {}

        Path best = null;
        int bestN = 0;
        for (Path c : candidates) {
            int n;
            try { n = countGames(c); } catch (Exception e) { continue; }
            if (n > bestN) { best = c; bestN = n; }
            if (n == GAMES.size()) break;
        }
        return best;
    }

    void chooseFolder() {
        JFileChooser fc = new JFileChooser(folder != null ? folder.toFile() : new File(System.getProperty("user.home")));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Escolha a pasta my-game-hub");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            folder = fc.getSelectedFile().toPath();
            if (prefs != null) try { prefs.put("folder", folder.toString()); } catch (Exception ignored) {}
            for (Game g : GAMES) g.loader = null;
            updateFolder();
        }
    }

    void updateFolder() {
        int n = countGames(folder);
        status.setForeground(ACCENT);
        if (folder == null || n == 0) {
            folderLbl.setText("Não achei os jogos. Clique em “Trocar pasta” e escolha a my-game-hub");
            status.setText("Cadê os jogos, diva?");
        } else {
            String shown = folder.toString();
            if (shown.length() > 48) shown = "…" + shown.substring(shown.length() - 47);
            folderLbl.setText("<html>" + shown.replace("&", "&amp;").replace("<", "&lt;") + "  ·  " + n + " de " + GAMES.size() + " jogos</html>");
            status.setText("Clique num jogo pra começar");
        }
        cards.forEach(Component::repaint);
    }

    boolean available(Game g) {
        return folder != null && Files.exists(folder.resolve(g.file));
    }

    void launch(Game g) {
        if (loading != null) return;
        if (!available(g)) {
            status.setForeground(PINK);
            status.setText(g.file + " não está na pasta. Coloque ele lá ou troque a pasta!");
            return;
        }
        loading = g;
        status.setForeground(ACCENT);
        status.setText("Carregando " + g.title + "... (a primeira vez demora uns segundinhos)");
        cards.forEach(Component::repaint);
        new Thread(() -> {
            try {
                Class<?> cls = prepare(g);
                Class.forName(cls.getName(), true, cls.getClassLoader());
                SwingUtilities.invokeLater(() -> mount(g, cls));
            } catch (Throwable ex) {
                SwingUtilities.invokeLater(() -> { loading = null; cards.forEach(Component::repaint); showError(g, ex); });
            }
        }, "load-" + g.className()).start();
    }

    Class<?> prepare(Game g) throws Exception {
        Path src = folder.resolve(g.file);
        long stamp = Files.getLastModifiedTime(src).toMillis() ^ Files.size(src);
        if (g.loader != null && g.stamp == stamp) return g.loader.loadClass(g.className());

        Path out = Paths.get(System.getProperty("user.home"), ".my-game-hub", "classes", g.className());
        Path stampFile = out.resolve("stamp.txt");
        boolean fresh = Files.exists(stampFile)
                && new String(Files.readAllBytes(stampFile), StandardCharsets.UTF_8).trim().equals(String.valueOf(stamp))
                && Files.exists(out.resolve(g.className() + ".class"));
        if (!fresh) {
            JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
            if (jc == null) throw new IllegalStateException("O hub precisa do JDK (não só do JRE) pra carregar os jogos.");
            if (Files.exists(out)) try (var walk = Files.walk(out)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
            Files.createDirectories(out);
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            int rc = jc.run(null, null, err, "-encoding", "UTF-8", "-nowarn", "-d", out.toString(), src.toString());
            if (rc != 0) throw new IllegalStateException("Erro ao compilar " + g.file + ":\n" + err.toString(StandardCharsets.UTF_8));
            Files.write(stampFile, String.valueOf(stamp).getBytes(StandardCharsets.UTF_8));
        }
        g.loader = new URLClassLoader(new URL[]{out.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
        g.stamp = stamp;
        return g.loader.loadClass(g.className());
    }

    void mount(Game g, Class<?> cls) {
        try {
            java.lang.reflect.Constructor<?> ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            JFrame f = (JFrame) ctor.newInstance();
            Point keep = getLocation();
            gameFrame = f;
            gameRoot = f.getRootPane();
            current = g;
            loading = null;
            stage.add(gameRoot, BorderLayout.CENTER);
            barTitle.setText("<html><span style='color:" + hex(g.color) + "'>●</span>&nbsp; " + g.title + "</html>");
            setTitle("My Game Hub  ·  " + g.title);
            pause.setVisible(false);
            setContentPane(stage);
            pack();
            keepOnScreen(keep);
            revalidate();
            repaint();
            call("hubStart", () -> gameRoot.getContentPane().requestFocusInWindow());
            status.setText("Clique num jogo pra começar");
            cards.forEach(Component::repaint);
        } catch (Throwable ex) {
            loading = null;
            current = null;
            showError(g, ex);
        }
    }

    void keepOnScreen(Point p) {
        Rectangle screen = getGraphicsConfiguration().getBounds();
        int x = Math.max(screen.x, Math.min(p.x, screen.x + screen.width - getWidth()));
        int y = Math.max(screen.y, Math.min(p.y, screen.y + screen.height - getHeight()));
        setLocation(x, y);
    }

    void call(String hook, Runnable fallback) {
        if (gameFrame == null) return;
        try {
            Method m = gameFrame.getClass().getMethod(hook);
            m.setAccessible(true);
            m.invoke(gameFrame);
        } catch (NoSuchMethodException e) {
            if (fallback != null) fallback.run();
        } catch (Throwable ignored) {}
    }

    void unmount() {
        if (gameFrame == null) return;
        call("hubStop", null);
        stage.remove(gameRoot);
        gameFrame.dispose();
        gameFrame = null;
        gameRoot = null;
        current = null;
    }

    void pauseGame() {
        if (current == null || pause.isVisible()) return;
        call("hubPause", null);
        pause.open();
    }

    void resumeGame() {
        pause.setVisible(false);
        call("hubResume", null);
    }

    void restartGame() {
        Game g = current;
        pause.setVisible(false);
        unmount();
        launch(g);
    }

    void backToMenu() {
        Point keep = getLocation();
        pause.setVisible(false);
        unmount();
        setTitle("My Game Hub");
        setContentPane(menu);
        pack();
        keepOnScreen(keep);
        revalidate();
        repaint();
        cards.forEach(Component::repaint);
    }

    class PauseOverlay extends JPanel {
        final JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(CARD);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g.setColor(BORDER);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
                g.dispose();
            }
        };

        PauseOverlay() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
            add(card);
            addMouseListener(new MouseAdapter() {});
            addMouseMotionListener(new MouseMotionAdapter() {});
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(12, 4, 24, 215));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        void put(JComponent c, int gap) {
            c.setAlignmentX(CENTER_ALIGNMENT);
            card.add(c);
            if (gap > 0) card.add(Box.createVerticalStrut(gap));
        }

        JLabel lbl(String html, int size, Color c, boolean bold) {
            JLabel l = new JLabel("<html><div style='text-align:center'>" + html + "</div></html>");
            l.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size));
            l.setForeground(c);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            return l;
        }

        void open() {
            card.removeAll();
            put(lbl("Pausado", 26, TEXT, true), 4);
            put(lbl("<span style='color:" + hex(current.color) + "'>" + current.title + "</span>  ·  respira, diva ♥", 13, MUTED, false), 18);
            put(new Pill("Continuar  ·  Esc", BTN, 240, 42, gamehub.this::resumeGame), 8);
            put(new Pill("Reiniciar " + current.title, DIM, 240, 40, gamehub.this::restartGame), 8);
            put(new Pill("Voltar aos jogos", DIM, 240, 40, gamehub.this::backToMenu), 0);
            card.revalidate();
            setVisible(true);
            revalidate();
            repaint();
        }
    }

    static String hex(Color c) { return String.format("#%06x", c.getRGB() & 0xffffff); }

    void showError(Game g, Throwable ex) {
        status.setForeground(PINK);
        status.setText(g.title + " não abriu :( detalhes na janelinha");
        StringWriter sw = new StringWriter();
        Throwable root = ex;
        while (root.getCause() != null) root = root.getCause();
        String msg = root.getMessage() != null && root.getMessage().contains("\n") ? root.getMessage() : null;
        if (msg == null) root.printStackTrace(new PrintWriter(sw));
        String last = msg != null ? msg : sw.toString();
        if (last.length() > 2000) last = last.substring(0, 2000) + "\n...";
        JTextArea area = new JTextArea(last, 12, 50);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(new JLabel("O " + g.title + " não conseguiu abrir. Isso foi o que o Java disse:"), BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, p, "Ops!", JOptionPane.WARNING_MESSAGE);
    }

    static class SoonCard extends JComponent {
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0x3a2a5c));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{6, 6}, 0));
            g.drawRoundRect(1, 4, getWidth() - 3, getHeight() - 6, 20, 20);
            g.setFont(new Font(FONT, Font.BOLD, 13));
            g.setColor(new Color(0x6f5a90));
            String s = "+ em breve";
            g.drawString(s, (getWidth() - g.getFontMetrics().stringWidth(s)) / 2, getHeight() / 2 + 6);
            g.dispose();
        }
    }

    class GameCard extends JComponent {
        final Game g; boolean hover;
        GameCard(Game g) {
            this.g = g;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint())) launch(g); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g2 = (Graphics2D) g0.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean ok = available(g), busy = loading == g;
            int lift = hover && ok ? 3 : 0;

            g2.setColor(hover && ok ? CARD_HOVER : CARD);
            g2.fillRoundRect(0, 3 - lift, w - 1, h - 4, 20, 20);
            if ((hover && ok) || busy) {
                g2.setColor(g.color);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 4 - lift, w - 3, h - 6, 20, 20);
            }

            int ix = 14, iy = 14 - lift, is = 48;
            g2.setColor(ok ? g.color : DIM);
            g2.fillRoundRect(ix, iy, is, is, 16, 16);
            drawIcon(g2, g.icon, ix, iy, is, ok);

            int tx = ix + is + 12;
            g2.setFont(new Font(FONT, Font.BOLD, 9));
            g2.setColor(ok ? g.color.brighter() : MUTED);
            g2.drawString(g.tag, tx, iy + 14);
            g2.setFont(new Font(FONT, Font.BOLD, 18));
            g2.setColor(ok ? TEXT : MUTED);
            g2.drawString(g.title, tx, iy + 36);

            g2.setFont(new Font(FONT, Font.PLAIN, 12));
            g2.setColor(MUTED);
            List<String> lines = wrap(g2, ok ? g.desc : "Arquivo " + g.file + " não encontrado", w - 28);
            int ly = iy + is + 22;
            for (String l : lines.subList(0, Math.min(2, lines.size()))) { g2.drawString(l, 14, ly); ly += 16; }

            String badge = busy ? "Carregando..." : ok ? "Jogar ›" : "";
            if (!badge.isEmpty()) {
                g2.setFont(new Font(FONT, Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int bw = fm.stringWidth(badge) + 18, bh = 22, bx = w - bw - 12, by = h - bh - 12 - lift;
                g2.setColor(busy || hover ? g.color : DIM);
                g2.fillRoundRect(bx, by, bw, bh, 11, 11);
                g2.setColor(busy || hover ? Color.WHITE : TEXT);
                g2.drawString(badge, bx + 9, by + 15);
            }
            g2.dispose();
        }
    }

    static List<String> wrap(Graphics2D g, String s, int width) {
        List<String> out = new ArrayList<>();
        FontMetrics fm = g.getFontMetrics();
        String line = "";
        for (String w : s.split(" ")) {
            String t = line.isEmpty() ? w : line + " " + w;
            if (fm.stringWidth(t) <= width) line = t;
            else { out.add(line); line = w; }
        }
        if (!line.isEmpty()) out.add(line);
        return out;
    }

    static void drawIcon(Graphics2D g, int type, int x, int y, int s, boolean ok) {
        Color fg = ok ? Color.WHITE : MUTED;
        Color soft = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 120);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = x + s / 2, cy = y + s / 2;
        switch (type) {
            case 0: {
                int a = 30, ox = cx - a / 2, oy = cy - a / 2;
                g.setColor(soft);
                for (int k = 1; k < 3; k++) {
                    g.drawLine(ox + k * a / 3, oy, ox + k * a / 3, oy + a);
                    g.drawLine(ox, oy + k * a / 3, ox + a, oy + k * a / 3);
                }
                g.setColor(fg);
                g.drawRoundRect(ox, oy, a, a, 4, 4);
                g.setFont(new Font(FONT, Font.BOLD, 9));
                g.drawString("1", ox + 3, oy + 9);
                g.drawString("5", ox + 13, oy + 19);
                g.drawString("9", ox + 23, oy + 29);
                break;
            }
            case 1: {
                int b = 9;
                g.setColor(fg);
                int[][] t = {{0, 0}, {1, 0}, {2, 0}, {1, 1}};
                for (int[] p : t) g.fillRoundRect(cx - 18 + p[0] * (b + 1), cy - 12 + p[1] * (b + 1), b, b, 3, 3);
                g.setColor(soft);
                int[][] l = {{0, 0}, {0, 1}, {1, 1}};
                for (int[] p : l) g.fillRoundRect(cx + 4 + p[0] * (b + 1), cy - 2 + p[1] * (b + 1), b, b, 3, 3);
                break;
            }
            case 2: {
                int b = 10;
                for (int k = 0; k < 3; k++) {
                    g.setColor(k == 1 ? soft : fg);
                    g.fillRoundRect(cx - 17 + k * 12, cy - 5, b, b, 3, 3);
                }
                g.setColor(soft);
                for (int k = 0; k < 3; k++) g.drawRoundRect(cx - 17 + k * 12, cy + 8, b, b, 3, 3);
                g.setColor(fg);
                for (int k = 0; k < 3; k++) g.drawRoundRect(cx - 17 + k * 12, cy - 18, b, b, 3, 3);
                break;
            }
            case 3: {
                g.setColor(fg);
                g.fill(new Ellipse2D.Double(cx - 13, cy + 3, 11, 8));
                g.fill(new Ellipse2D.Double(cx + 3, cy, 11, 8));
                g.setStroke(new BasicStroke(2.5f));
                g.drawLine(cx - 3, cy + 6, cx - 3, cy - 13);
                g.drawLine(cx + 13, cy + 3, cx + 13, cy - 16);
                g.drawLine(cx - 3, cy - 13, cx + 13, cy - 16);
                g.drawLine(cx - 3, cy - 8, cx + 13, cy - 11);
                break;
            }
            case 4: {
                g.setColor(soft);
                g.fillRoundRect(cx - 18, cy - 2, 11, 16, 3, 3);
                g.fillRoundRect(cx + 7, cy + 3, 11, 11, 3, 3);
                g.setColor(fg);
                g.fillRoundRect(cx - 6, cy - 9, 12, 23, 3, 3);
                g.setFont(new Font(FONT, Font.BOLD, 11));
                g.drawString("♛", cx - 5, cy - 12);
                break;
            }
            case 6: {
                AffineTransform at = g.getTransform();
                g.rotate(Math.toRadians(-12), cx - 4, cy);
                g.setColor(soft);
                g.fillRoundRect(cx - 16, cy - 15, 20, 28, 5, 5);
                g.setTransform(at);
                g.rotate(Math.toRadians(10), cx + 4, cy);
                g.setColor(fg);
                g.fillRoundRect(cx - 7, cy - 14, 20, 28, 5, 5);
                g.setColor(ok ? new Color(0xf472b6) : DIM);
                g.setFont(new Font(FONT, Font.BOLD, 13));
                g.drawString("♥", cx - 2, cy + 5);
                g.setTransform(at);
                break;
            }
            case 7: {
                g.setFont(new Font(FONT, Font.BOLD, 9));
                int b = 15;
                String[] n = {"2", "4", "8", "16"};
                for (int k = 0; k < 4; k++) {
                    int bx = cx - b - 1 + (k % 2) * (b + 2), by = cy - b - 1 + (k / 2) * (b + 2);
                    g.setColor(k == 3 ? fg : soft);
                    g.fillRoundRect(bx, by, b, b, 4, 4);
                    g.setColor(k == 3 ? (ok ? new Color(0x9333ea) : DIM) : fg);
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(n[k], bx + (b - fm.stringWidth(n[k])) / 2, by + 11);
                }
                break;
            }
            case 8: {
                g.setColor(fg);
                double r = 9;
                g.setStroke(new BasicStroke(2.2f));
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 4 * k;
                    g.draw(new Line2D.Double(cx - Math.cos(a) * 14, cy - Math.sin(a) * 14, cx + Math.cos(a) * 14, cy + Math.sin(a) * 14));
                }
                g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
                g.setColor(ok ? new Color(0x6d28d9) : DIM);
                g.fill(new Ellipse2D.Double(cx - 5, cy - 6, 5, 5));
                break;
            }
            case 9: {
                g.setColor(fg);
                g.fillPolygon(new int[]{cx - 12, cx - 2, cx - 22}, new int[]{cy - 14, cy - 2, cy - 2}, 3);
                g.fillRect(cx - 15, cy - 3, 6, 15);
                g.setColor(soft);
                g.fillPolygon(new int[]{cx + 12, cx + 2, cx + 22}, new int[]{cy + 14, cy + 2, cy + 2}, 3);
                g.fillRect(cx + 9, cy - 12, 6, 15);
                break;
            }
            case 10: {
                for (int k = 0; k < 4; k++) {
                    g.setColor(k % 2 == 0 ? fg : soft);
                    g.fillRoundRect(cx - 16, cy - 16 + k * 8, 32, 6, 4, 4);
                }
                break;
            }
            default: {
                g.setColor(fg);
                g.setStroke(new BasicStroke(3f));
                g.draw(new RoundRectangle2D.Double(cx - 18, cy - 7, 20, 14, 14, 14));
                g.setColor(soft);
                g.draw(new RoundRectangle2D.Double(cx - 2, cy - 7, 20, 14, 14, 14));
                break;
            }
        }
    }

    static class Pill extends JComponent {
        final String text; final Color base; boolean hover;
        Pill(String text, Color base, int w, int h, Runnable action) {
            this.text = text; this.base = base;
            Dimension d = new Dimension(w, h);
            setPreferredSize(d); setMaximumSize(d); setMinimumSize(d);
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
            g.setFont(new Font(FONT, Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new gamehub().setVisible(true));
    }
}