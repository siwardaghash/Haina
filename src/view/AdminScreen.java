package view;

import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import controller.AdminController;
import model.Question;

/** Login-protected local question editor. */
public final class AdminScreen extends NeonScreen {
  
    private static final Color LAVENDER = new Color(198, 166, 255);
    private final Runnable back;
    private final AdminController controller;
    private final DefaultListModel<Question> listModel = new DefaultListModel<>();
    private final JList<Question> list = new JList<>(listModel);

    /** Which difficulty the list shows ("all", "easy", "medium" or "hard") and the search text. */
    private String filter = "all";
    private final JTextField search = new JTextField(18);
    private final JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

    /** Shows the list, or the "no questions" message when the list is empty. */
    private final JPanel listCards = new JPanel(new CardLayout());
    private boolean bankEmpty;

    /** Short neon message under the title after saving or deleting. */
    private String toastText;
    private boolean toastGood;
    private long toastStart;
    private final JComponent toast = new JComponent() {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            paintToast((Graphics2D) g, getWidth(), getHeight());
        }
    };

    public AdminScreen(AdminController controller, Runnable back) {
        this.controller=controller;
        this.back=back;
        setOpaque(false);
        setLayout(new BorderLayout(12, 12));        setBorder(BorderFactory.createEmptyBorder(22,28,22,28));
		NeonButton returnButton = new NeonButton(
		    "Back to Menu",
		    new Color(174, 139, 245),
		    NeonButton.Glyph.BACK,
		    null
		);
		
		returnButton.setChevron(false);
		returnButton.setPreferredSize(new Dimension(240, 76));

        returnButton.addActionListener(e -> back.run());
        JLabel title = Theme.neonHeading("QUESTION BANK", LAVENDER, 36f);
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        header.add(returnButton, BorderLayout.EAST);
        header.add(title, BorderLayout.WEST);
        header.add(toast, BorderLayout.CENTER);
        add(header,BorderLayout.NORTH);
        SwingUtilities.invokeLater(this::authenticate);
    }

    private void authenticate() {
        try {
            // SRS 3.6.3: a single predefined administrator; players cannot create accounts.
            if (!controller.hasAdministrator()) {
                Theme.neonMessage(this, "Administrator not found",
                        "The administrator file (data/admin.xml) is missing.\n"
                        + "Question management is not available.", Theme.RED);
                back.run();
                return;
            }
            JTextField username = new JTextField(20);
            JPasswordField password = new JPasswordField(20);
            Theme.styleNeonField(username, LAVENDER);
            Theme.styleNeonField(password, LAVENDER);

            JPanel loginPanel = new JPanel(new GridLayout(4, 1, 8, 8));
            loginPanel.setOpaque(false);
            loginPanel.add(fieldLabel("USERNAME"));
            loginPanel.add(username);
            loginPanel.add(fieldLabel("PASSWORD"));
            loginPanel.add(password);

            int result = showLoginDialog(loginPanel);
            if (result!=JOptionPane.OK_OPTION) {back.run();return;}
            char[] pwd=password.getPassword();boolean valid;
            try { valid=controller.login(username.getText(),pwd); }
            finally { Arrays.fill(pwd,'\0'); }
            if (!valid) {
                Theme.neonMessage(this, "Access denied", "Invalid username or password.", Theme.RED);
                SwingUtilities.invokeLater(this::authenticate);   // show the login again
                return;
            }         buildEditor();
        } catch (Exception ex) {
            Theme.neonMessage(this, "Question bank error", ex.getMessage(), Theme.RED);
            back.run();
        }
    }
    /** Small label above an input field. */
    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.semibold(13f));
        label.setForeground(Theme.MUTED);
        return label;
    }

    /** Rounded neon card used as the background of the login and question popups. */
    private static JPanel neonCard(LayoutManager layout, Color accent) {
        JPanel card = new JPanel(layout) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, new Color(30, 26, 70), 0, h, new Color(12, 17, 42)));
                g2.fillRoundRect(2, 2, w - 4, h - 4, 28, 28);
                Theme.glow(g2, w / 2f, 0, w * 0.55f, accent, 60);
                g2.setColor(Theme.alpha(accent, 230));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(2, 2, w - 5, h - 5, 28, 28);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    /** Glowing round badge with a padlock, shown above the login form. */
    private static JComponent lockBadge(Color accent) {
        JComponent badge = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                Theme.glow(g2, cx, cy, 44, accent, 110);
                g2.setColor(Theme.alpha(accent, 70));
                g2.fill(new java.awt.geom.Ellipse2D.Float(cx - 26, cy - 26, 52, 52));
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new java.awt.geom.Ellipse2D.Float(cx - 26, cy - 26, 52, 52));
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new java.awt.geom.Arc2D.Float(cx - 8, cy - 15, 16, 18, 0, 180, java.awt.geom.Arc2D.OPEN));
                g2.draw(new java.awt.geom.Line2D.Float(cx - 8, cy - 6, cx - 8, cy - 2));
                g2.draw(new java.awt.geom.Line2D.Float(cx + 8, cy - 6, cx + 8, cy - 2));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(cx - 12, cy - 3, 24, 18, 5, 5));
                g2.setColor(accent);
                g2.fill(new java.awt.geom.Ellipse2D.Float(cx - 2.5f, cy + 3, 5, 5));
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(80, 72));
        return badge;
    }
    private int showLoginDialog(JPanel loginPanel) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Administrator Login", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        Theme.transparentCorners(dialog);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel container = neonCard(new BorderLayout(0, 14), LAVENDER);
        container.setBorder(BorderFactory.createEmptyBorder(18, 34, 24, 34));

        // lock badge and glowing title
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        JComponent badge = lockBadge(LAVENDER);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titleRow.setOpaque(false);
        titleRow.add(Theme.neonHeading("ADMIN LOGIN", LAVENDER, 28f));
        top.add(badge);
        top.add(titleRow);
        container.add(top, BorderLayout.NORTH);
        container.add(loginPanel, BorderLayout.CENTER);

        NeonButton cancel = new NeonButton("CANCEL", Theme.VIOLET, NeonButton.Glyph.BACK, null);
        NeonButton login = new NeonButton("LOGIN", Theme.GREEN, NeonButton.Glyph.LOCK, null);
        final int[] result = {JOptionPane.CANCEL_OPTION};
        for (NeonButton button : new NeonButton[]{cancel, login}) {
            button.setChevron(false);
            button.setFocusable(false);
            button.setPreferredSize(new Dimension(200, 68));
        }
        cancel.addActionListener(e -> dialog.dispose());
        login.addActionListener(e -> {
            result[0] = JOptionPane.OK_OPTION;
            dialog.dispose();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(login);
        container.add(buttons, BorderLayout.SOUTH);

        // Esc = cancel, Enter = login
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setContentPane(container);
        dialog.getRootPane().setDefaultButton(login);
        dialog.setSize(500, 470);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return result[0];
    }
    private static Color levelColor(String level) {
        switch (level.toLowerCase()) {
            case "easy": return Theme.GREEN;
            case "medium": return Theme.YELLOW;
            default: return Theme.RED;
        }
    }
    /** One question in the list: difficulty stripe, ID, question text and the correct answer. */
    private static JComponent questionCard(Question entry, boolean selected, boolean hovered) {
        Color accent = levelColor(entry.getLevel());
        boolean lit = selected || hovered;
        JComponent card = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                int w = getWidth();
                int h = getHeight();
                java.awt.Shape body = new java.awt.geom.RoundRectangle2D.Float(4, 5, w - 8, h - 10, 18, 18);

                g2.setPaint(new GradientPaint(0, 0, Theme.alpha(accent, selected ? 70 : lit ? 45 : 22),
                        w * 0.6f, 0, Theme.GLASS));
                g2.fill(body);
                g2.setColor(Theme.alpha(accent, selected ? 255 : lit ? 200 : 80));
                g2.setStroke(new BasicStroke(selected ? 2.2f : 1.2f));
                g2.draw(body);

                // difficulty stripe on the left
                g2.setColor(accent);
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(4, 5, 6, h - 10, 6, 6));

                // ID and difficulty
                g2.setFont(Theme.mono(17f));
                g2.setColor(accent);
                g2.drawString(entry.getId(), 28, h / 2f - 3);
                g2.setFont(Theme.semibold(11f));
                g2.setColor(Theme.alpha(accent, 210));
                Theme.drawTracked(g2, entry.getLevel().toUpperCase(), 28, h / 2f + 16, 2);

                // question text and the correct answer under it
                g2.setFont(Theme.semibold(17f));
                g2.setColor(Theme.TEXT);
                g2.drawString(entry.getText(), 130, h / 2f - 4);

                float ay = h / 2f + 17;
                g2.setColor(Theme.GREEN);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                java.awt.geom.Path2D tick = new java.awt.geom.Path2D.Float();
                tick.moveTo(131, ay - 5);
                tick.lineTo(135, ay - 1);
                tick.lineTo(143, ay - 10);
                g2.draw(tick);
                g2.setFont(Theme.ui(14f, Font.PLAIN));
                g2.setColor(Theme.MUTED);
                g2.drawString("Answer " + entry.getCorrectNumber() + "  ·  "
                        + entry.getAnswers()[entry.getCorrect()], 152, ay);
                g2.dispose();
            }
        };
        return card;
    }
    /** Neon pill for the difficulty filter; filled when it is the active filter. */
    private JButton filterChip(String key, String text, int count, Color accent) {
        boolean active = filter.equals(key);
        JButton chip = new JButton(text + "  " + count) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                boolean hover = getModel().isRollover();
                int w = getWidth();
                int h = getHeight();
                g2.setColor(Theme.alpha(accent, active ? 110 : hover ? 60 : 25));
                g2.fillRoundRect(1, 1, w - 2, h - 2, h, h);
                g2.setColor(Theme.alpha(accent, active || hover ? 255 : 150));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, h, h);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(active ? Color.WHITE : accent);
                g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2f,
                        (h + fm.getAscent() - fm.getDescent()) / 2f);
                g2.dispose();
            }
        };
        chip.setFont(Theme.semibold(13f));
        chip.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        chip.setContentAreaFilled(false);
        chip.setFocusPainted(false);
        chip.setFocusable(false);
        chip.setOpaque(false);
        chip.setRolloverEnabled(true);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addActionListener(e -> { filter = key; refresh(); });
        return chip;
    }
    private void buildEditor() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(78);
        list.setOpaque(false);
        list.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 10));

        final int[] hoveredRow = {-1};
        list.setCellRenderer((jList, entry, index, selected, focus) ->
                questionCard(entry, selected, hoveredRow[0] == index));

        list.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0 && !list.getCellBounds(index, index).contains(e.getPoint())) index = -1;
                if (hoveredRow[0] != index) { hoveredRow[0] = index; list.repaint(); }
            }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) { hoveredRow[0] = -1; list.repaint(); }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) editQuestion(list.getSelectedValue());
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        Theme.neonScrollBar(scrollPane, LAVENDER);

        // filter chips on the left, search box on the right
        filterRow.setOpaque(false);
        Theme.styleNeonField(search, LAVENDER);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        JPanel searchBox = new JPanel(new BorderLayout(10, 0));
        searchBox.setOpaque(false);
        JLabel searchLabel = fieldLabel("SEARCH");
        searchBox.add(searchLabel, BorderLayout.WEST);
        searchBox.add(search, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        toolbar.add(filterRow, BorderLayout.WEST);
        toolbar.add(searchBox, BorderLayout.EAST);

        JPanel middle = new JPanel(new BorderLayout(0, 10));
        middle.setOpaque(false);
        middle.add(toolbar, BorderLayout.NORTH);
        listCards.setOpaque(false);
        listCards.add(scrollPane, "list");
        listCards.add(emptyState(), "empty");
        middle.add(listCards, BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);

        NeonButton add = new NeonButton("ADD QUESTION", Theme.GREEN, NeonButton.Glyph.PLUS, null);
        NeonButton edit = new NeonButton("EDIT SELECTED", LAVENDER, NeonButton.Glyph.EDIT, null);
        NeonButton delete = new NeonButton("DELETE SELECTED", Theme.RED, NeonButton.Glyph.TRASH, null);
        NeonButton logout = new NeonButton("LOG OUT", Theme.VIOLET, NeonButton.Glyph.POWER, null);
        JPanel controls = new JPanel(new GridLayout(1, 4, 14, 0));
        controls.setOpaque(false);
        controls.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        for (NeonButton button : new NeonButton[]{add, edit, delete, logout}) {
            button.setChevron(false);
            button.setFocusable(false);
            button.setPreferredSize(new Dimension(260, 70));
            controls.add(button);
        }
        add.addActionListener(e -> editQuestion(null));
        edit.addActionListener(e -> { Question q = list.getSelectedValue(); if (q != null) editQuestion(q); });
        delete.addActionListener(e -> deleteQuestion());
        logout.addActionListener(e -> back.run());
        add(controls, BorderLayout.SOUTH);

        refresh();
        revalidate();
        repaint();
    }
    /** Starts a toast; NeonScreen's timer keeps repainting, so it fades in and out by itself. */
    private void showToast(String text, boolean good) {
        toastText = text;
        toastGood = good;
        toastStart = System.currentTimeMillis();
        toast.repaint();
    }

    private void paintToast(Graphics2D g, int w, int h) {
        if (toastText == null) return;
        long age = System.currentTimeMillis() - toastStart;
        if (age > 2600) { toastText = null; return; }
        float fade = Math.min(1f, Math.min(age / 250f, (2600 - age) / 400f));
        Color accent = toastGood ? Theme.GREEN : Theme.RED;

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQuality(g2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
        g2.setFont(Theme.semibold(15f));
        FontMetrics fm = g2.getFontMetrics();
        int pw = Math.min(w - 10, fm.stringWidth(toastText) + 74);
        int ph = 44;
        int px = (w - pw) / 2;
        int py = (h - ph) / 2 - (int) ((1 - fade) * 12);   // slides down while it fades in

        Theme.glow(g2, w / 2f, py + ph / 2f, pw / 2f, accent, 45);
        g2.setColor(new Color(16, 18, 44, 235));
        g2.fillRoundRect(px, py, pw, ph, ph, ph);
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(px, py, pw, ph, ph, ph);

        // round icon: a tick when it went well, an X when something was removed
        float ix = px + 26;
        float iy = py + ph / 2f;
        g2.fill(new java.awt.geom.Ellipse2D.Float(ix - 11, iy - 11, 22, 22));
        g2.setColor(new Color(16, 18, 44));
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        java.awt.geom.Path2D icon = new java.awt.geom.Path2D.Float();
        if (toastGood) {
            icon.moveTo(ix - 5, iy);
            icon.lineTo(ix - 1.5f, iy + 4);
            icon.lineTo(ix + 5.5f, iy - 4.5f);
        } else {
            icon.moveTo(ix - 4.5f, iy - 4.5f);
            icon.lineTo(ix + 4.5f, iy + 4.5f);
            icon.moveTo(ix + 4.5f, iy - 4.5f);
            icon.lineTo(ix - 4.5f, iy + 4.5f);
        }
        g2.draw(icon);

        g2.setColor(Theme.TEXT);
        g2.drawString(toastText, px + 48, py + (ph + fm.getAscent() - fm.getDescent()) / 2f);
        g2.dispose();
    }
    /** Big glowing message shown when no question matches the filter or the search. */
    private JComponent emptyState() {
        JComponent empty = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                int w = getWidth();
                int h = getHeight();
                float cx = w / 2f;
                float cy = h / 2f - 40;

                // dashed neon frame around the empty area
                g2.setColor(Theme.alpha(LAVENDER, 90));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        10f, new float[]{8f, 8f}, 0f));
                g2.drawRoundRect(6, 6, w - 12, h - 12, 24, 24);

                // glowing magnifying glass
                Theme.glow(g2, cx, cy, 70, LAVENDER, 70);
                g2.setColor(LAVENDER);
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new java.awt.geom.Ellipse2D.Float(cx - 24, cy - 26, 40, 40));
                g2.draw(new java.awt.geom.Line2D.Float(cx + 10, cy + 8, cx + 26, cy + 24));

                String title = bankEmpty ? "NO QUESTIONS YET" : "NO QUESTIONS MATCH";
                String hint = bankEmpty ? "Click ADD QUESTION to create the first one"
                        : "Try another difficulty or clear the search";
                g2.setFont(Theme.display(26f));
                float tw = Theme.trackedWidth(g2, title, 3);
                g2.setColor(Theme.TEXT);
                Theme.drawTracked(g2, title, cx - tw / 2f, cy + 70, 3);
                g2.setFont(Theme.ui(15f, Font.PLAIN));
                g2.setColor(Theme.MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, cx - fm.stringWidth(hint) / 2f, cy + 100);
                g2.dispose();
            }
        };
        empty.setOpaque(false);
        return empty;
    }

    /** Refills the list from the question bank, using the active filter and the search text. */
    private void refresh() {
        java.util.List<Question> all = controller.questions();
        String text = search.getText().trim().toLowerCase();
        bankEmpty = all.isEmpty();
        listModel.clear();
        int easy = 0, medium = 0, hard = 0;
        for (Question q : all) {
            String level = q.getLevel().toLowerCase();
            if (level.equals("easy")) easy++; else if (level.equals("medium")) medium++; else hard++;
            boolean levelOk = filter.equals("all") || level.equals(filter);
            boolean textOk = text.isEmpty() || q.getText().toLowerCase().contains(text)
                    || q.getId().toLowerCase().contains(text);
            if (levelOk && textOk) listModel.addElement(q);
        }
        filterRow.removeAll();
        filterRow.add(filterChip("all", "ALL", all.size(), LAVENDER));
        filterRow.add(filterChip("easy", "EASY", easy, Theme.GREEN));
        filterRow.add(filterChip("medium", "MEDIUM", medium, Theme.YELLOW));
        filterRow.add(filterChip("hard", "HARD", hard, Theme.RED));
        filterRow.revalidate();
        filterRow.repaint();
        ((CardLayout) listCards.getLayout()).show(listCards, listModel.isEmpty() ? "empty" : "list");
        listCards.repaint();
        
    }
    /** A label with its input underneath, added to the form. */
    private static void addFormRow(JPanel form, String label, JComponent input) {
        JLabel l = fieldLabel(label);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, input.getPreferredSize().height));
        form.add(Box.createVerticalStrut(12));
        form.add(l);
        form.add(Box.createVerticalStrut(6));
        form.add(input);
    }

    /** Row of pills that works like the combo box: clicking a pill selects that item. */
    private static JPanel pillGroup(JComboBox<String> combo, String[] labels, Color[] colors) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            Color accent = colors[i];
            JButton pill = new JButton(labels[i]) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.applyQuality(g2);
                    boolean on = combo.getSelectedIndex() == index;
                    int w = getWidth();
                    int h = getHeight();
                    g2.setColor(Theme.alpha(accent, on ? 120 : 25));
                    g2.fillRoundRect(1, 1, w - 2, h - 2, h, h);
                    g2.setColor(Theme.alpha(accent, on ? 255 : 140));
                    g2.setStroke(new BasicStroke(on ? 2f : 1.3f));
                    g2.drawRoundRect(1, 1, w - 3, h - 3, h, h);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    g2.setColor(on ? Color.WHITE : accent);
                    g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2f,
                            (h + fm.getAscent() - fm.getDescent()) / 2f);
                    g2.dispose();
                }
            };
            pill.setFont(Theme.semibold(13f));
            pill.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
            pill.setContentAreaFilled(false);
            pill.setFocusable(false);
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.addActionListener(e -> { combo.setSelectedIndex(index); row.repaint(); });
            row.add(pill);
        }
        return row;
    }
    /** One answer: a round "correct" toggle followed by the answer field. */
    private static JPanel answerRow(int index, JTextField field, JComboBox<Integer> correct) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton mark = new JButton() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                boolean on = correct.getSelectedIndex() == index;
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                if (on) Theme.glow(g2, cx, cy, 24, Theme.GREEN, 90);
                g2.setColor(on ? Theme.GREEN : Theme.alpha(Theme.MUTED, 160));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new java.awt.geom.Ellipse2D.Float(cx - 13, cy - 13, 26, 26));
                if (on) {
                    g2.fill(new java.awt.geom.Ellipse2D.Float(cx - 13, cy - 13, 26, 26));
                    g2.setColor(new Color(12, 30, 26));
                    g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    java.awt.geom.Path2D tick = new java.awt.geom.Path2D.Float();
                    tick.moveTo(cx - 6, cy);
                    tick.lineTo(cx - 1.5f, cy + 5);
                    tick.lineTo(cx + 7, cy - 5);
                    g2.draw(tick);
                } else {
                    g2.setFont(Theme.semibold(13f));
                    FontMetrics fm = g2.getFontMetrics();
                    String n = String.valueOf(index + 1);
                    g2.drawString(n, cx - fm.stringWidth(n) / 2f, cy + (fm.getAscent() - fm.getDescent()) / 2f);
                }
                g2.dispose();
            }
        };
        mark.setPreferredSize(new Dimension(40, 40));
        mark.setContentAreaFilled(false);
        mark.setBorderPainted(false);
        mark.setFocusable(false);
        mark.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mark.addActionListener(e -> {
            correct.setSelectedIndex(index);
            row.getParent().repaint();
        });
        row.add(mark, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(42, field.getPreferredSize().height)));
        return row;
    }
    private void editQuestion(Question existing) {
        JTextField id = new JTextField(existing == null ? "" : existing.getId(), 22);
        JTextArea prompt = new JTextArea(existing == null ? "" : existing.getText(), 3, 32);
        prompt.setLineWrap(true); prompt.setWrapStyleWord(true);
        JComboBox<String> level = new JComboBox<>(new String[]{"easy", "medium", "hard"});
        if (existing != null) level.setSelectedItem(existing.getLevel());
        JComboBox<Integer> correct = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        if (existing != null) correct.setSelectedItem(existing.getCorrectNumber());
        JTextField[] answers = new JTextField[4];

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 14));

        Theme.styleNeonField(id, LAVENDER);
        Theme.styleNeonField(prompt, LAVENDER);
        addFormRow(panel, "QUESTION ID", id);
        addFormRow(panel, "DIFFICULTY", pillGroup(level, new String[]{"EASY", "MEDIUM", "HARD"},
                new Color[]{Theme.GREEN, Theme.YELLOW, Theme.RED}));
        addFormRow(panel, "QUESTION", prompt);

        panel.add(Box.createVerticalStrut(12));
        JLabel answersLabel = fieldLabel("ANSWERS  ·  click the circle of the correct one");
        answersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(answersLabel);
        for (int i = 0; i < 4; i++) {
            answers[i] = new JTextField(existing == null ? "" : existing.getAnswers()[i], 22);
            Theme.styleNeonField(answers[i], LAVENDER);
            panel.add(Box.createVerticalStrut(8));
            panel.add(answerRow(i, answers[i], correct));
        }

        JScrollPane formScroll = new JScrollPane(panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        Theme.neonScrollBar(formScroll, LAVENDER);

        int result = showQuestionDialog(formScroll, existing == null ? "ADD QUESTION" : "EDIT QUESTION",
                () -> checkFields(id, prompt, answers[0], answers[1], answers[2], answers[3]));
        if (result != JOptionPane.OK_OPTION) return;
        String[] values = new String[4];
        for (int i = 0; i < 4; i++) values[i] = answers[i].getText().trim();
        Question entry = new Question(id.getText().trim(), (String) level.getSelectedItem(),
                prompt.getText().trim(), values, (Integer) correct.getSelectedItem() - 1);
        try {
            if (existing == null) controller.add(entry); else controller.update(existing.getId(), entry);
            refresh();
            showToast(existing == null ? "Question " + entry.getId() + " added"
                    : "Changes to " + entry.getId() + " saved", true);
        } catch (Exception ex) { Theme.neonMessage(this, "Cannot save", ex.getMessage(), Theme.RED); }
    }
    /** Paints empty fields red and returns true only when every field has text. */
    private static boolean checkFields(javax.swing.text.JTextComponent... fields) {
        boolean ok = true;
        for (javax.swing.text.JTextComponent field : fields) {
            boolean empty = field.getText().trim().isEmpty();
            Theme.styleNeonField(field, empty ? Theme.RED : LAVENDER);
            field.repaint();
            if (empty && ok) field.requestFocusInWindow();   // jump to the first empty field
            if (empty) ok = false;
        }
        return ok;
    }

    /** Quick left-right wiggle of the popup, like a wrong password. */
    private static void shake(JDialog dialog) {
        if (dialog.getRootPane().getClientProperty("shaking") != null) return;
        dialog.getRootPane().putClientProperty("shaking", true);
        Point start = dialog.getLocation();
        int[] step = {0};
        Timer timer = new Timer(25, null);
        timer.addActionListener(e -> {
            step[0]++;
            int dx = (int) (Math.sin(step[0] * 1.8) * 12 * (1 - step[0] / 14.0));
            dialog.setLocation(start.x + dx, start.y);
            if (step[0] >= 14) {
                dialog.setLocation(start);
                dialog.getRootPane().putClientProperty("shaking", null);
                timer.stop();
            }
        });
        timer.start();
    }
    private int showQuestionDialog(JScrollPane formScroll, String titleText,
            java.util.function.BooleanSupplier check) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), titleText,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        Theme.transparentCorners(dialog);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel container = neonCard(new BorderLayout(0, 10), LAVENDER);
        container.setBorder(BorderFactory.createEmptyBorder(20, 30, 22, 30));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titleRow.setOpaque(false);
        titleRow.add(Theme.neonHeading(titleText, LAVENDER, 28f));
        container.add(titleRow, BorderLayout.NORTH);
        container.add(formScroll, BorderLayout.CENTER);

        NeonButton cancel = new NeonButton("CANCEL", Theme.VIOLET, NeonButton.Glyph.BACK, null);
        NeonButton save = new NeonButton("SAVE QUESTION", Theme.GREEN, NeonButton.Glyph.PLUS, null);
        final int[] result = {JOptionPane.CANCEL_OPTION};
        for (NeonButton button : new NeonButton[]{cancel, save}) {
            button.setChevron(false);
            button.setFocusable(false);
            button.setPreferredSize(new Dimension(260, 68));
        }
        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            if (!check.getAsBoolean()) { shake(dialog); return; }
            result[0] = JOptionPane.OK_OPTION;
            dialog.dispose();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        container.add(buttons, BorderLayout.SOUTH);

        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setContentPane(container);
        int screenHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
        dialog.setSize(620, Math.min(720, screenHeight - 60));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return result[0];
    }

    private void deleteQuestion() {
        Question selected=list.getSelectedValue();if(selected==null)return;
        if (!Theme.neonConfirm(this, "Confirm deletion",
                "Delete question " + selected.getId() + "? This cannot be undone.",
                "DELETE", Theme.RED)) return;      
        try {controller.delete(selected.getId());refresh();showToast("Question " + selected.getId() + " deleted", false);}
                catch(Exception ex) { Theme.neonMessage(this, "Cannot delete", ex.getMessage(), Theme.RED); }
                }

	
}
