package view;

import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import controller.AdminController;
import model.Question;

/** Login-protected local question editor. */
public final class AdminScreen extends JPanel {
	private float animationTime = 0f;
	private final Timer backgroundTimer = new Timer(60, e -> {
	    animationTime += 0.04f;
	    repaint();
	});
	
	@Override
	public void addNotify() {
	    super.addNotify();
	    backgroundTimer.start();
	}
	
	@Override
	public void removeNotify() {
	    backgroundTimer.stop();
	    super.removeNotify();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
	    super.paintComponent(g);
	
	    Graphics2D g2 = (Graphics2D) g.create();
	    Theme.applyQuality(g2);
	    Theme.paintBackdrop(
	        g2, getWidth(), getHeight(), animationTime
	    );
	    g2.dispose();
	}

    private static final Color BG = new Color(12,21,46), TEXT = new Color(239,243,255);
    private static final Color LAVENDER = new Color(198, 166, 255);
    private final Runnable back;
    private final AdminController controller;
    private final DefaultListModel<Question> listModel = new DefaultListModel<>();
    private final JList<Question> list = new JList<>(listModel);

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
        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Administrator Login",
            Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setUndecorated(true);
        dialog.setSize(420, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        Color navy = new Color(17, 31, 62);
        Color green = new Color(83, 204, 146);

		JPanel container = new JPanel(new BorderLayout(0, 20)) {
		    @Override
		    protected void paintComponent(Graphics g) {
		        Graphics2D g2 = (Graphics2D) g.create();
		        Theme.applyQuality(g2);
		
		        g2.setPaint(new GradientPaint(
		            0, 0, new Color(42, 32, 80),
		            getWidth(), getHeight(),
		            new Color(18, 37, 68)
		        ));
		
		        g2.fillRoundRect(
		            2, 2,
		            getWidth() - 4,
		            getHeight() - 4,
		            26, 26
		        );
		
		        g2.setColor(new Color(174, 139, 245));
		        g2.setStroke(new BasicStroke(2f));
		
		        g2.drawRoundRect(
		            2, 2,
		            getWidth() - 4,
		            getHeight() - 4,
		            26, 26
		        );
		
		        g2.dispose();
		    }
		};
		
		container.setOpaque(false);
		container.setBorder(
		    BorderFactory.createEmptyBorder(25, 30, 25, 30)
		);
		
        JLabel title = new JLabel("ADMIN LOGIN", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(Color.WHITE);
        container.add(title, BorderLayout.NORTH);

        loginPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        container.add(loginPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 15, 0));
        buttons.setOpaque(false);

        JButton cancel = new JButton("CANCEL");
        cancel.setBackground(new Color(55, 75, 115));
        cancel.setForeground(Color.WHITE);
        cancel.setFont(new Font("SansSerif", Font.BOLD, 14));
        cancel.setFocusPainted(false);
        Theme.solidButton(cancel);

        JButton login = new JButton("LOGIN");
        login.setBackground(green);
        login.setForeground(new Color(17, 31, 62));
        login.setFont(new Font("SansSerif", Font.BOLD, 14));
        login.setFocusPainted(false);
        Theme.solidButton(login);

        final int[] result = {JOptionPane.CANCEL_OPTION};

        cancel.addActionListener(e -> dialog.dispose());

        login.addActionListener(e -> {
            result[0] = JOptionPane.OK_OPTION;
            dialog.dispose();
        });

        buttons.add(cancel);
        buttons.add(login);

        container.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(container);
        dialog.getRootPane().setDefaultButton(login);
        dialog.setVisible(true);

        return result[0];
    }
    private void buildEditor() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(new Font("SansSerif",Font.PLAIN,15));
        list.setFixedCellHeight(58);
        list.setBackground(new Color(25, 40, 72));
        list.setForeground(new Color(235, 242, 255));

        list.setSelectionBackground(new Color(55, 95, 160));
        list.setSelectionForeground(Color.WHITE);

        list.setBorder(
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        );

		final int[] hoveredRow = {-1};
		
		list.setCellRenderer((jList, entry, index, selected, focus) -> {
		
		    Color accent;
		    switch (entry.getLevel().toLowerCase()) {
		        case "easy":
		            accent = new Color(86, 232, 201);
		            break;
		        case "medium":
		            accent = new Color(196, 166, 255);
		            break;
		        default:
		            accent = new Color(255, 145, 214);
		    }
		
		    boolean highlight = selected || hoveredRow[0] == index;
		
		    JPanel row = new JPanel(new BorderLayout(18, 0)) {
		        @Override
		        protected void paintComponent(Graphics g) {
		            Graphics2D g2 = (Graphics2D) g.create();
		            Theme.applyQuality(g2);
		
		            int w = getWidth();
		            int h = getHeight();
		
		            g2.setPaint(new GradientPaint(
		                0, 0, highlight
		                    ? new Color(65, 50, 115)
		                    : new Color(34, 32, 73),
		                w, h, new Color(20, 35, 66)
		            ));
		
		            g2.fillRoundRect(4, 3, w - 8, h - 6, 18, 18);
		
		            g2.setColor(highlight
		                ? accent
		                : new Color(105, 95, 160));
		
		            g2.setStroke(new BasicStroke(
		                highlight ? 1.8f : 0.8f
		            ));
		
		            g2.drawRoundRect(4, 3, w - 8, h - 6, 18, 18);
		            g2.dispose();
		        }
		    };
		
		    row.setOpaque(false);
		    row.setBorder(
		        BorderFactory.createEmptyBorder(10, 18, 10, 18)
		    );
		
		    JLabel badge = new JLabel(
		        entry.getId() + "  " + entry.getLevel().toUpperCase()
		    );
		    badge.setFont(new Font("SansSerif", Font.BOLD, 13));
		    badge.setForeground(accent);
		
		    JLabel question = new JLabel(entry.getText() + "     \u2714 " + entry.getCorrectNumber() + ". " + entry.getAnswers()[entry.getCorrect()]);
		    question.setFont(new Font("SansSerif", Font.PLAIN, 15));
		    question.setForeground(new Color(235, 238, 255));
		
		    row.add(badge, BorderLayout.WEST);
		    row.add(question, BorderLayout.CENTER);
		
		    return row;
		});
		
		list.addMouseMotionListener(
		    new java.awt.event.MouseMotionAdapter() {
		        @Override
		        public void mouseMoved(java.awt.event.MouseEvent e) {
		            int index = list.locationToIndex(e.getPoint());
		
		            if (index >= 0 && !list.getCellBounds(
		                    index, index).contains(e.getPoint())) {
		                index = -1;
		            }
		
		            if (hoveredRow[0] != index) {
		                hoveredRow[0] = index;
		                list.repaint();
		            }
		        }
		    }
		);
		
		list.addMouseListener(new java.awt.event.MouseAdapter() {
		    @Override
		    public void mouseExited(java.awt.event.MouseEvent e) {
		        hoveredRow[0] = -1;
		        list.repaint();
		    }
		});

        JScrollPane scrollPane = new JScrollPane(list);

        scrollPane.setBorder(
            BorderFactory.createLineBorder(new Color(65, 90, 140), 1)
        );

        scrollPane.getViewport().setBackground(new Color(25, 40, 72));

        scrollPane.getVerticalScrollBar().setBackground(new Color(25, 40, 72));
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);

        add(scrollPane, BorderLayout.CENTER);        JPanel controls = new JPanel(new GridLayout(1, 4, 14, 0));
        controls.setOpaque(false);
        controls.setBorder(BorderFactory.createEmptyBorder(14, 45, 14, 45));
		JButton add = new GlowActionButton(
		    "ADD QUESTION", new Color(86, 232, 201)
		);
		
		JButton edit = new GlowActionButton(
		    "EDIT SELECTED", new Color(196, 166, 255)
		);
		
		JButton delete = new GlowActionButton(
		    "DELETE SELECTED", new Color(255, 110, 140)
		);
		
		JButton logout = new GlowActionButton(
		    "LOG OUT", new Color(145, 197, 255)
		);


        styleAdminButton(add, new Color(88, 201, 149), new Color(12, 21, 46));
        styleAdminButton(edit, new Color(80, 120, 210), Color.WHITE);
        styleAdminButton(delete, new Color(240, 96, 120), Color.WHITE);
        styleAdminButton(logout, new Color(55, 85, 140), Color.WHITE);
        add.addActionListener(e -> editQuestion(null));
        edit.addActionListener(e -> {Question q=list.getSelectedValue();if(q!=null)editQuestion(q);});
        delete.addActionListener(e -> deleteQuestion());
        logout.addActionListener(e -> back.run());
        controls.add(add);
        controls.add(edit);
        controls.add(delete);
        controls.add(logout);

        add(controls, BorderLayout.SOUTH);

        refresh();

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    private void refresh() {
        listModel.clear();
        for (Question e: controller.questions()) listModel.addElement(e);
    }

    private void editQuestion(Question existing) {
        JTextField id=new JTextField(existing==null?"":existing.getId(),22);
        JTextArea prompt=new JTextArea(existing==null?"":existing.getText(),3,32);
        prompt.setLineWrap(true);prompt.setWrapStyleWord(true);
        JComboBox<String> level=new JComboBox<>(new String[]{"easy","medium","hard"});
        if(existing!=null)level.setSelectedItem(existing.getLevel());
        JTextField[] answers=new JTextField[4];
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        panel.setBackground(new Color(25, 28, 60));
        panel.setBorder(
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        );
        id.setAlignmentX(Component.LEFT_ALIGNMENT);
        level.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(formLabel("Question ID (unique):"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(id);
        panel.add(formLabel("Difficulty:"));
        panel.add(level);

        panel.add(formLabel("Question:"));
        JScrollPane promptScroll = new JScrollPane(prompt);
        promptScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(promptScroll);
        for (int i=0;i<4;i++) {
            answers[i]=new JTextField(existing==null?"":existing.getAnswers()[i],22);
            answers[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(formLabel("Answer " + (i + 1) + ":"));
            panel.add(answers[i]);
        }
        JComboBox<Integer> correct=new JComboBox<>(new Integer[]{1,2,3,4});
        if(existing!=null)correct.setSelectedItem(existing.getCorrectNumber());
        panel.add(formLabel("Correct answer number:"));
        correct.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(correct);

		styleInput(id);
		styleInput(prompt);
		
		for (JTextField answer : answers) {
		    styleInput(answer);
		}
		
		Color fieldBG = new Color(32, 36, 76);
		Color fieldText = new Color(240, 235, 255);
		
		level.setBackground(fieldBG);
		level.setForeground(fieldText);
		correct.setBackground(fieldBG);
		correct.setForeground(fieldText);
		
		level.setFont(new Font("SansSerif", Font.PLAIN, 15));
		correct.setFont(new Font("SansSerif", Font.PLAIN, 15));

        // Limit the content height so the lower fields and dialog buttons remain accessible.
        JScrollPane formScroll = new JScrollPane(panel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        int screenHeight = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getMaximumWindowBounds().height;
        formScroll.setPreferredSize(new Dimension(480, Math.min(490, Math.max(240, screenHeight - 230))));
        int result = showQuestionDialog(
        	    formScroll,
        	    existing == null ? "ADD QUESTION" : "EDIT QUESTION"
        	);
        if(result!=JOptionPane.OK_OPTION)return;
        String[] values=new String[4];for(int i=0;i<4;i++)values[i]=answers[i].getText().trim();
        Question entry=new Question(id.getText().trim(),(String)level.getSelectedItem(),prompt.getText().trim(),values,(Integer)correct.getSelectedItem()-1);
        try {
            if(existing==null)controller.add(entry);else controller.update(existing.getId(),entry);
            refresh();
        } catch(Exception ex) { Theme.neonMessage(this, "Cannot save", ex.getMessage(), Theme.RED); }
        }

	private int showQuestionDialog(JScrollPane formScroll, String titleText) {
	    JDialog dialog = new JDialog(
	        SwingUtilities.getWindowAncestor(this),
	        titleText,
	        Dialog.ModalityType.APPLICATION_MODAL
	    );
	
	    dialog.setUndecorated(true);
	    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	
	    Color navy = new Color(17, 31, 62);
	    Color green = new Color(83, 204, 146);
	
		JPanel container = new JPanel(new BorderLayout(0, 16)) {
		    @Override
		    protected void paintComponent(Graphics g) {
		        Graphics2D g2 = (Graphics2D) g.create();
		        Theme.applyQuality(g2);
		
		        int w = getWidth();
		        int h = getHeight();
		
		        g2.setPaint(new GradientPaint(
		            0, 0, new Color(42, 32, 82),
		            w, h, new Color(18, 35, 69)
		        ));
		
		        g2.fillRoundRect(3, 3, w - 6, h - 6, 26, 26);
		
		        g2.setColor(new Color(174, 139, 245));
		        g2.setStroke(new BasicStroke(2f));
		        g2.drawRoundRect(3, 3, w - 6, h - 6, 26, 26);
		
		        g2.dispose();
		    }
		};
		
		container.setOpaque(false);
		container.setBorder(
		    BorderFactory.createEmptyBorder(24, 28, 24, 28)
		);

	    JLabel title = new JLabel(titleText, SwingConstants.CENTER);
	    title.setFont(new Font("SansSerif", Font.BOLD, 24));
	    title.setForeground(new Color(196, 166, 255));
	    container.add(title, BorderLayout.NORTH);
	
	    formScroll.getViewport().setBackground(navy);
	    formScroll.setBorder(BorderFactory.createEmptyBorder());
	    container.add(formScroll, BorderLayout.CENTER);
	
	    JPanel buttons = new JPanel(new GridLayout(1, 2, 15, 0));
	    buttons.setOpaque(false);

		JButton cancel = new GlowActionButton(
		    "CANCEL",
		    new Color(145, 197, 255)
		);
		
		JButton save = new GlowActionButton(
		    "SAVE QUESTION",
		    new Color(86, 232, 201)
		);
		
		cancel.setPreferredSize(new Dimension(170, 52));
		save.setPreferredSize(new Dimension(190, 52));


	    final int[] result = {JOptionPane.CANCEL_OPTION};
	
	    cancel.addActionListener(e -> dialog.dispose());
	
	    save.addActionListener(e -> {
	        result[0] = JOptionPane.OK_OPTION;
	        dialog.dispose();
	    });
	
	    buttons.add(cancel);
	    buttons.add(save);
	    container.add(buttons, BorderLayout.SOUTH);
	
	    dialog.setContentPane(container);
	
	    int screenHeight = GraphicsEnvironment
	        .getLocalGraphicsEnvironment()
	        .getMaximumWindowBounds().height;
	
	    dialog.setSize(580, Math.min(680, screenHeight - 80));
	    dialog.setLocationRelativeTo(this);
	    dialog.getRootPane().setDefaultButton(save);
	
	    dialog.setVisible(true);
	
	    return result[0];
	}

    private void deleteQuestion() {
        Question selected=list.getSelectedValue();if(selected==null)return;
        if (!Theme.neonConfirm(this, "Confirm deletion",
                "Delete question " + selected.getId() + "? This cannot be undone.",
                "DELETE", Theme.RED)) return;        try {controller.delete(selected.getId());refresh();}
                catch(Exception ex) { Theme.neonMessage(this, "Cannot delete", ex.getMessage(), Theme.RED); }
                }

	private static class GlowActionButton extends JButton {
	
	    private final Color accent;
	    private boolean hovering = false;
	
	    GlowActionButton(String text, Color accent) {
	        super(text);
	        this.accent = accent;
	
	        setFont(new Font("SansSerif", Font.BOLD, 14));
	        setForeground(Color.WHITE);
	        setFocusPainted(false);
	        setContentAreaFilled(false);
	        setOpaque(false);
	        setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
	        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	
	        addMouseListener(new java.awt.event.MouseAdapter() {
	            @Override
	            public void mouseEntered(java.awt.event.MouseEvent e) {
	                hovering = true;
	                repaint();
	            }
	
	            @Override
	            public void mouseExited(java.awt.event.MouseEvent e) {
	                hovering = false;
	                repaint();
	            }
	        });
	    }
	
	    @Override
	    protected void paintComponent(Graphics g) {
	        Graphics2D g2 = (Graphics2D) g.create();
	        Theme.applyQuality(g2);
	
	        int w = getWidth();
	        int h = getHeight();
	
	        g2.setPaint(new GradientPaint(
	            0, 0, hovering
	                ? accent.darker()
	                : new Color(39, 33, 79),
	            w, h, new Color(18, 32, 66)
	        ));
	        g2.fillRoundRect(3, 3, w - 6, h - 6, 20, 20);
	
	        g2.setColor(accent);
	        g2.setStroke(new BasicStroke(hovering ? 2.5f : 1.3f));
	        g2.drawRoundRect(3, 3, w - 6, h - 6, 20, 20);
	
	        g2.dispose();
	        super.paintComponent(g);
	    }
	}

    private void styleAdminButton(JButton button, Color bg, Color fg) {
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(170, 42));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.brighter(), 1),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        Theme.solidButton(button);
    }

		private void styleInput(
		        javax.swing.text.JTextComponent field) {
		
		    field.setBackground(new Color(32, 36, 76));
		    field.setForeground(new Color(240, 235, 255));
		    field.setCaretColor(new Color(196, 166, 255));
		    field.setSelectionColor(new Color(100, 75, 160));
		
		    field.setFont(new Font("SansSerif", Font.PLAIN, 15));
		
		    field.setBorder(BorderFactory.createCompoundBorder(
		        BorderFactory.createLineBorder(
		            new Color(110, 92, 165), 1, true
		        ),
		        BorderFactory.createEmptyBorder(8, 12, 8, 12)
		    ));
		
		    field.addFocusListener(new java.awt.event.FocusAdapter() {
		        @Override
		        public void focusGained(java.awt.event.FocusEvent e) {
		            field.setBorder(BorderFactory.createCompoundBorder(
		                BorderFactory.createLineBorder(
		                    new Color(196, 166, 255), 2, true
		                ),
		                BorderFactory.createEmptyBorder(7, 11, 7, 11)
		            ));
		        }
		
		        @Override
		        public void focusLost(java.awt.event.FocusEvent e) {
		            field.setBorder(BorderFactory.createCompoundBorder(
		                BorderFactory.createLineBorder(
		                    new Color(110, 92, 165), 1, true
		                ),
		                BorderFactory.createEmptyBorder(8, 12, 8, 12)
		            ));
		        }
		    });
		}

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(235, 242, 255));
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
