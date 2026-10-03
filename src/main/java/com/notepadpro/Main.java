package com.notepadpro;

import javax.swing.*;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseManager db = new DatabaseManager();
                Runtime.getRuntime().addShutdownHook(new Thread(db::close));
                NotepadProFrame frame = new NotepadProFrame(db);
                frame.setVisible(true);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to MySQL.\n\n" +
                                "Please check that:\n" +
                                "1. MySQL is running\n" +
                                "2. You ran schema.sql to create the 'notepadpro' database\n" +
                                "3. src/main/resources/db.properties has the correct URL/user/password\n\n" +
                                "Details: " + ex.getMessage(),
                        "Database Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
