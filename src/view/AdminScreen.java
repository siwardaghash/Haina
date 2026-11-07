package view;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import javax.swing.*;
import model.AdminAuth;
import model.QuestionManager;
import model.QuestionManager.Entry;

/** Login-protected local question editor. */
public final class AdminScreen extends JPanel {
    private static final Color BG = new Color(12,21,46), TEXT = new Color(239,243,255);
    private final Runnable back;
    private final AdminAuth auth = new AdminAuth(new File("data/admin.xml"));
    private QuestionManager manager;
    private final DefaultListModel<Entry> listModel = new DefaultListModel<>();
    private final JList<Entry> list = new JList<>(listModel);

    public AdminScreen(Runnable back) {
        this.back=back;
        setBackground(BG); setLayout(new BorderLayout(12,12));
        setBorder(BorderFactory.createEmptyBorder(22,28,22,28));
        JButton returnButton = new JButton("← Back to Menu");

        returnButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        returnButton.setBackground(new Color(55, 85, 140));
        returnButton.setForeground(Color.WHITE);
        returnButton.setFocusPainted(false);
        returnButton.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        Theme.solidButton(returnButton);

        returnButton.addActionListener(e -> back.run());
        JLabel title = new JLabel("QUESTION BANK  /  ADMIN",SwingConstants.CENTER);
        title.setFont(new Font("SansSerif",Font.BOLD,27)); title.setForeground(TEXT);
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        header.add(returnButton, BorderLayout.EAST);
        header.add(title,BorderLayout.CENTER);
        add(header,BorderLayout.NORTH);
        SwingUtilities.invokeLater(this::authenticate);
    }

    private void authenticate() {
        try {
            // SRS 3.6.3: a single predefined administrator; players cannot create accounts.
            if (!auth.isInitialized()) {
                JOptionPane.showMessageDialog(this,
                    "The administrator file (data/admin.xml) is missing.\n"
                    + "Question management is not available.",
                    "Administrator not found", JOptionPane.ERROR_MESSAGE);
                back.run();
                return;
            }
            JTextField username = new JTextField(20);
            JPasswordField password = new JPasswordField(20);

            JPanel loginPanel = new JPanel(new GridLayout(4, 1, 8, 8));
            loginPanel.setBackground(new Color(17, 31, 62));
            loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

            JLabel usernameLabel = new JLabel("USERNAME");
            JLabel passwordLabel = new JLabel("PASSWORD");

            usernameLabel.setForeground(TEXT);
            passwordLabel.setForeground(TEXT);

            usernameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            passwordLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

            loginPanel.add(usernameLabel);
            loginPanel.add(username);
            loginPanel.add(passwordLabel);
            loginPanel.add(password);

            int result = showLoginDialog(loginPanel);
            if (result!=JOptionPane.OK_OPTION) {back.run();return;}
            char[] pwd=password.getPassword();boolean valid;
            try { valid=auth.authenticate(username.getText(),pwd); }
            finally { Arrays.fill(pwd,'\0'); }
            if (!valid) {JOptionPane.showMessageDialog(this,"Invalid username or password.","Access denied",JOptionPane.ERROR_MESSAGE);back.run();return;}
            manager=new QuestionManager(new File("data/questions.xml"));
            buildEditor();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,ex.getMessage(),"Question bank error",JOptionPane.ERROR_MESSAGE);
            back.run();
        }
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

        JPanel container = new JPanel(new BorderLayout(0, 20));
        container.setBackground(navy);
        container.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(75, 105, 160), 2),
            BorderFactory.createEmptyBorder(25, 30, 25, 30)
        ));

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
        list.setFixedCellHeight(34);
        list.setBackground(new Color(25, 40, 72));
        list.setForeground(new Color(235, 242, 255));

        list.setSelectionBackground(new Color(55, 95, 160));
        list.setSelectionForeground(Color.WHITE);

        list.setBorder(
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        );
        JScrollPane scrollPane = new JScrollPane(list);

        scrollPane.setBorder(
            BorderFactory.createLineBorder(new Color(65, 90, 140), 1)
        );

        scrollPane.getViewport().setBackground(new Color(25, 40, 72));

        scrollPane.getVerticalScrollBar().setBackground(new Color(25, 40, 72));
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);

        add(scrollPane, BorderLayout.CENTER);        JPanel controls = new JPanel(new GridLayout(1, 4, 14, 0));
        controls.setOpaque(false);
        controls.setBorder(BorderFactory.createEmptyBorder(14, 160, 14, 160));
        JButton add = new JButton("Add question");
        JButton edit = new JButton("Edit selected");
        JButton delete = new JButton("Delete selected");
        JButton logout = new JButton("Log out");

        styleAdminButton(add, new Color(88, 201, 149), new Color(12, 21, 46));
        styleAdminButton(edit, new Color(80, 120, 210), Color.WHITE);
        styleAdminButton(delete, new Color(240, 96, 120), Color.WHITE);
        styleAdminButton(logout, new Color(55, 85, 140), Color.WHITE);
        add.addActionListener(e -> editQuestion(null));
        edit.addActionListener(e -> {Entry q=list.getSelectedValue();if(q!=null)editQuestion(q);});
        delete.addActionListener(e -> deleteQuestion());
        logout.addActionListener(e -> back.run());
        controls.add(add);controls.add(edit);controls.add(delete);controls.add(logout);
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
        for (Entry e: manager.getAll()) listModel.addElement(e);
    }

    private void editQuestion(Entry existing) {
        JTextField id=new JTextField(existing==null?"":existing.id,22);
        JTextArea prompt=new JTextArea(existing==null?"":existing.text,3,32);
        prompt.setLineWrap(true);prompt.setWrapStyleWord(true);
        JComboBox<String> level=new JComboBox<>(new String[]{"easy","medium","hard"});
        if(existing!=null)level.setSelectedItem(existing.level);
        JTextField[] answers=new JTextField[4];
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        panel.setBackground(new Color(17, 31, 62));

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
            answers[i]=new JTextField(existing==null?"":existing.answers[i],22);
            answers[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(formLabel("Answer " + (i + 1) + ":"));
            panel.add(answers[i]);
        }
        JComboBox<Integer> correct=new JComboBox<>(new Integer[]{1,2,3,4});
        if(existing!=null)correct.setSelectedItem(existing.correct);
        panel.add(formLabel("Correct answer number:"));
        correct.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(correct);
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
        Entry entry=new Entry(id.getText().trim(),(String)level.getSelectedItem(),prompt.getText().trim(),values,(Integer)correct.getSelectedItem());
        try {
            if(existing==null)manager.add(entry);else manager.update(existing.id,entry);
            refresh();
        } catch(Exception ex) {JOptionPane.showMessageDialog(this,ex.getMessage(),"Cannot save",JOptionPane.ERROR_MESSAGE);}
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
	
	    JPanel container = new JPanel(new BorderLayout(0, 16));
	    container.setBackground(navy);
	    container.setBorder(BorderFactory.createCompoundBorder(
	        BorderFactory.createLineBorder(new Color(75, 105, 160), 2),
	        BorderFactory.createEmptyBorder(20, 24, 20, 24)
	    ));
	
	    JLabel title = new JLabel(titleText, SwingConstants.CENTER);
	    title.setFont(new Font("SansSerif", Font.BOLD, 24));
	    title.setForeground(Color.WHITE);
	    container.add(title, BorderLayout.NORTH);
	
	    formScroll.getViewport().setBackground(navy);
	    formScroll.setBorder(BorderFactory.createEmptyBorder());
	    container.add(formScroll, BorderLayout.CENTER);
	
	    JPanel buttons = new JPanel(new GridLayout(1, 2, 15, 0));
	    buttons.setOpaque(false);
	
	    JButton cancel = new JButton("CANCEL");
	    styleAdminButton(cancel, new Color(55, 85, 140), Color.WHITE);
	
	    JButton save = new JButton("SAVE QUESTION");
	    styleAdminButton(save, green, navy);
	
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
        Entry selected=list.getSelectedValue();if(selected==null)return;
        if(JOptionPane.showConfirmDialog(this,"Delete " + selected.id + "?", "Confirm deletion",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try {manager.delete(selected.id);refresh();}
        catch(Exception ex) {JOptionPane.showMessageDialog(this,ex.getMessage(),"Cannot delete",JOptionPane.ERROR_MESSAGE);}
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
    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(235, 242, 255));
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
