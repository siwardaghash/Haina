package app;

import java.io.File;
import java.util.Arrays;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;
import model.AdminAuth;

/**
 * Team tool, not part of the game: adds an administrator account (one per team member)
 * to data/admin.xml. Run it from Eclipse (Run As > Java Application). Players cannot
 * create accounts from inside the game.
 */
public final class CreateAdmin {
    private CreateAdmin() { }

    public static void main(String[] args) throws Exception {
        AdminAuth auth = new AdminAuth(new File("data/admin.xml"));
        JTextField username = new JTextField(20);
        JPasswordField password = new JPasswordField(20);
        JPasswordField repeat = new JPasswordField(20);
        JPanel panel = new JPanel(new GridLayout(6, 1, 4, 4));
        panel.add(new JLabel("Username"));
        panel.add(username);
        panel.add(new JLabel("Password (at least 8 characters)"));
        panel.add(password);
        panel.add(new JLabel("Repeat password"));
        panel.add(repeat);
        if (JOptionPane.showConfirmDialog(null, panel, "Add an administrator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        char[] first = password.getPassword();
        char[] second = repeat.getPassword();
        try {
            if (!Arrays.equals(first, second)) {
                JOptionPane.showMessageDialog(null, "The passwords do not match.", "Not created",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            auth.addAdministrator(username.getText(), first);
            JOptionPane.showMessageDialog(null, "Administrator \"" + username.getText().trim()
                    + "\" added to data/admin.xml (password stored as a hash).");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Not created", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
        }
    }
}
