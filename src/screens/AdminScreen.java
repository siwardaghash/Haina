package screens;

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
            if (!auth.isInitialized()) {
                int choice=JOptionPane.showConfirmDialog(this,
                    "No administrator has been provisioned. Set up the single local administrator now?",
                    "Administrator setup",JOptionPane.YES_NO_OPTION);
                if (choice!=JOptionPane.YES_OPTION) {back.run();return;}
                JTextField username=new JTextField(); JPasswordField password=new JPasswordField();
                int result=JOptionPane.showConfirmDialog(this,new Object[]{"Admin username:",username,
                    "Admin password (8+ characters):",password},"First-run administrator provisioning",JOptionPane.OK_CANCEL_OPTION);
                if (result!=JOptionPane.OK_OPTION) {back.run();return;}
                char[] pwd=password.getPassword();
                try { auth.initialize(username.getText(),pwd); }
                finally { Arrays.fill(pwd,'\0'); }
            }
            JTextField username=new JTextField(); JPasswordField password=new JPasswordField();
            int result=JOptionPane.showConfirmDialog(this,new Object[]{"Username:",username,"Password:",password},
                "Administrator login",JOptionPane.OK_CANCEL_OPTION);
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

    private void buildEditor() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(new Font("SansSerif",Font.PLAIN,15));
        list.setFixedCellHeight(34);
        add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.CENTER,12,12)); controls.setOpaque(false);
        JButton add=new JButton("Add question"); JButton edit=new JButton("Edit selected");
        JButton delete=new JButton("Delete selected"); JButton logout=new JButton("Log out");
        add.addActionListener(e -> editQuestion(null));
        edit.addActionListener(e -> {Entry q=list.getSelectedValue();if(q!=null)editQuestion(q);});
        delete.addActionListener(e -> deleteQuestion());
        logout.addActionListener(e -> back.run());
        controls.add(add);controls.add(edit);controls.add(delete);controls.add(logout);
        add(controls,BorderLayout.SOUTH); refresh();
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
        JPanel panel=new JPanel(new GridLayout(0,1,6,5));
        panel.setBorder(BorderFactory.createEmptyBorder(12,14,12,14));
        panel.add(new JLabel("Question ID (unique):"));panel.add(id);
        panel.add(new JLabel("Difficulty:"));panel.add(level);
        panel.add(new JLabel("Question:"));panel.add(new JScrollPane(prompt));
        for (int i=0;i<4;i++) {
            answers[i]=new JTextField(existing==null?"":existing.answers[i],22);
            panel.add(new JLabel("Answer " + (i+1) + ":"));panel.add(answers[i]);
        }
        JComboBox<Integer> correct=new JComboBox<>(new Integer[]{1,2,3,4});
        if(existing!=null)correct.setSelectedItem(existing.correct);
        panel.add(new JLabel("Correct answer number:"));panel.add(correct);
        // Limit the content height so the lower fields and dialog buttons remain accessible.
        JScrollPane formScroll = new JScrollPane(panel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        int screenHeight = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getMaximumWindowBounds().height;
        formScroll.setPreferredSize(new Dimension(480, Math.min(490, Math.max(240, screenHeight - 230))));
        int result=JOptionPane.showConfirmDialog(this,formScroll,
            existing==null?"Add question":"Edit question",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result!=JOptionPane.OK_OPTION)return;
        String[] values=new String[4];for(int i=0;i<4;i++)values[i]=answers[i].getText().trim();
        Entry entry=new Entry(id.getText().trim(),(String)level.getSelectedItem(),prompt.getText().trim(),values,(Integer)correct.getSelectedItem());
        try {
            if(existing==null)manager.add(entry);else manager.update(existing.id,entry);
            refresh();
        } catch(Exception ex) {JOptionPane.showMessageDialog(this,ex.getMessage(),"Cannot save",JOptionPane.ERROR_MESSAGE);}
    }
    private void deleteQuestion() {
        Entry selected=list.getSelectedValue();if(selected==null)return;
        if(JOptionPane.showConfirmDialog(this,"Delete " + selected.id + "?", "Confirm deletion",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try {manager.delete(selected.id);refresh();}
        catch(Exception ex) {JOptionPane.showMessageDialog(this,ex.getMessage(),"Cannot delete",JOptionPane.ERROR_MESSAGE);}
    }
}
