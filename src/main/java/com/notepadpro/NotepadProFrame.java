package com.notepadpro;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Main application window: menu bar, search bar, categories tree,
 * notes list, and note editor panel — modeled on the NotepadPro-Advanced
 * desktop layout.
 */
public class NotepadProFrame extends JFrame {

    private static final Color BLUE = new Color(47, 111, 237);

    private final DatabaseManager db;
    private Integer currentNoteId = null;
    private String currentCategory = "All Notes";

    // Top search bar
    private JTextField searchField;
    private JComboBox<String> scopeCombo;

    // Categories
    private JTree categoryTree;
    private DefaultMutableTreeNode categoryRoot;

    // Notes list
    private JTextField notesSearchField;
    private DefaultListModel<Note> notesListModel;
    private JList<Note> notesList;
    private JLabel notesCountLabel;

    // Editor
    private JTextField titleField;
    private JComboBox<String> categoryCombo;
    private JTextField tagsField;
    private JTextArea bodyArea;

    // Status bar
    private JLabel statusLabel;
    private JLabel wordsLabel;
    private JLabel clockLabel;

    public NotepadProFrame(DatabaseManager db) {
        super("NotepadPro-Advanced - 1.0.0");
        this.db = db;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1366, 768);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);

        setJMenuBar(buildMenuBar());

        JPanel content = new JPanel(new BorderLayout());
        content.add(buildToolbar(), BorderLayout.NORTH);
        content.add(buildBody(), BorderLayout.CENTER);
        content.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(content);

        refreshCategories();
        refreshNotes();
        startClock();
    }

    // ---- Menu bar ------------------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(menuItem("New Note", e -> newNote()));
        fileMenu.add(menuItem("Save Note", e -> saveNote()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", e -> dispose()));
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(menuItem("Delete Note", e -> deleteNote()));
        editMenu.add(menuItem("Toggle Favorite", e -> toggleFavorite()));
        editMenu.add(menuItem("Toggle Archive", e -> toggleArchive()));
        menuBar.add(editMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.add(menuItem("Refresh", e -> refreshNotes()));
        menuBar.add(viewMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(menuItem("About", e -> showAbout()));
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JMenuItem menuItem(String label, java.awt.event.ActionListener action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(action);
        return item;
    }

    // ---- Toolbar (search bar) ------------------------------------------

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));

        bar.add(new JLabel("Search:"));
        searchField = new JTextField(45);
        searchField.addActionListener(e -> doSearch());
        bar.add(searchField);

        scopeCombo = new JComboBox<>(new String[]{"All", "Title", "Body", "Tags"});
        bar.add(scopeCombo);

        bar.add(blueButton("Search", e -> doSearch()));
        bar.add(blueButton("Clear", e -> clearSearch()));
        bar.add(blueButton("Advanced", e -> showAdvanced()));

        return bar;
    }

    private JButton blueButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.setBackground(BLUE);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.addActionListener(action);
        return btn;
    }

    // ---- Main body -------------------------------------------------------

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());

        body.add(buildLeftRail(), BorderLayout.WEST);

        JPanel middle = buildMiddlePanel();
        middle.setPreferredSize(new Dimension(320, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, middle, buildEditorPanel());
        split.setDividerLocation(320);
        split.setResizeWeight(0);
        body.add(split, BorderLayout.CENTER);

        return body;
    }

    private JPanel buildLeftRail() {
        JPanel rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBackground(new Color(0xEC, 0xEC, 0xEC));
        rail.setPreferredSize(new Dimension(90, 0));

        String[][] actions = {
                {"New"}, {"Save"}, {"Delete"}, {"Favorite"}, {"Archive"}
        };
        Runnable[] handlers = {
                this::newNote, this::saveNote, this::deleteNote,
                this::toggleFavorite, this::toggleArchive
        };

        for (int i = 0; i < actions.length; i++) {
            JButton btn = new JButton(actions[i][0]);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(80, 28));
            final Runnable handler = handlers[i];
            btn.addActionListener(e -> handler.run());
            rail.add(Box.createVerticalStrut(8));
            rail.add(btn);
        }
        rail.add(Box.createVerticalGlue());
        return rail;
    }

    private JPanel buildMiddlePanel() {
        JPanel middle = new JPanel(new BorderLayout());

        // Categories
        JPanel catPanel = new JPanel(new BorderLayout());
        catPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Categories"));

        categoryRoot = new DefaultMutableTreeNode("All Notes");
        categoryTree = new JTree(new DefaultTreeModel(categoryRoot));
        categoryTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        categoryTree.addTreeSelectionListener(e -> onCategorySelect());
        catPanel.add(new JScrollPane(categoryTree), BorderLayout.CENTER);

        JPanel catBtns = new JPanel(new GridLayout(1, 3, 4, 4));
        catBtns.add(blueButton("+", e -> addCategory()));
        catBtns.add(blueButton("-", e -> removeCategory()));
        catBtns.add(blueButton("...", e -> renameCategory()));
        catPanel.add(catBtns, BorderLayout.SOUTH);
        catPanel.setPreferredSize(new Dimension(300, 220));

        // Notes list
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Notes"));

        JPanel notesTop = new JPanel(new BorderLayout(4, 4));
        notesSearchField = new JTextField();
        notesTop.add(notesSearchField, BorderLayout.CENTER);
        notesTop.add(blueButton("Search", e -> doSearch()), BorderLayout.EAST);

        JPanel notesBtnRow = new JPanel(new GridLayout(1, 3, 4, 4));
        notesBtnRow.add(blueButton("+ New", e -> newNote()));
        notesBtnRow.add(blueButton("- Delete", e -> deleteNote()));
        notesBtnRow.add(blueButton("Refresh", e -> refreshNotes()));

        JPanel notesHeader = new JPanel(new BorderLayout(4, 4));
        notesHeader.add(notesTop, BorderLayout.NORTH);
        notesHeader.add(notesBtnRow, BorderLayout.SOUTH);
        notesPanel.add(notesHeader, BorderLayout.NORTH);

        notesCountLabel = new JLabel("0 notes", SwingConstants.RIGHT);
        notesPanel.add(notesCountLabel, BorderLayout.SOUTH);

        notesListModel = new DefaultListModel<>();
        notesList = new JList<>(notesListModel);
        notesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onNoteSelect();
        });
        JScrollPane notesScroll = new JScrollPane(notesList);
        notesPanel.add(notesScroll, BorderLayout.CENTER);

        middle.add(catPanel, BorderLayout.NORTH);
        middle.add(notesPanel, BorderLayout.CENTER);
        return middle;
    }

    private JPanel buildEditorPanel() {
        JPanel editor = new JPanel(new BorderLayout());
        editor.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Note Editor"));

        // Top fields: Title / Category / Tags
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        fields.add(new JLabel("Title:"), gc);
        titleField = new JTextField();
        gc.gridx = 1; gc.weightx = 1; gc.gridwidth = 3;
        fields.add(titleField, gc);

        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        fields.add(new JLabel("Category:"), gc);
        categoryCombo = new JComboBox<>();
        gc.gridx = 1; gc.weightx = 0.4;
        fields.add(categoryCombo, gc);

        gc.gridx = 2; gc.weightx = 0;
        fields.add(new JLabel("Tags:"), gc);
        tagsField = new JTextField();
        gc.gridx = 3; gc.weightx = 0.6;
        fields.add(tagsField, gc);

        JPanel fieldsWrapper = new JPanel(new BorderLayout());
        fieldsWrapper.add(fields, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        btnBar.add(blueButton("Cancel", e -> cancelEdit()));
        btnBar.add(blueButton("Save", e -> saveNote()));

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.add(fieldsWrapper, BorderLayout.CENTER);
        topArea.add(btnBar, BorderLayout.SOUTH);

        editor.add(topArea, BorderLayout.NORTH);

        bodyArea = new JTextArea();
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateWordCount(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateWordCount(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateWordCount(); }
        });
        editor.add(new JScrollPane(bodyArea), BorderLayout.CENTER);

        return editor;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createLoweredBevelBorder());

        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        bar.add(statusLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 2));
        wordsLabel = new JLabel("Words: 0");
        clockLabel = new JLabel();
        right.add(wordsLabel);
        right.add(clockLabel);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private void startClock() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> clockLabel.setText(fmt.format(new Date())));
            }
        }, 0, 1000);
    }

    // ------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------

    private void refreshCategories() {
        try {
            categoryRoot.removeAllChildren();
            for (String name : db.getCategories()) {
                categoryRoot.add(new DefaultMutableTreeNode(name));
            }
            ((DefaultTreeModel) categoryTree.getModel()).reload();
            categoryTree.expandRow(0);

            Vector<String> cats = new Vector<>(db.getCategories());
            categoryCombo.setModel(new DefaultComboBoxModel<>(cats));
        } catch (SQLException ex) {
            showError("Failed to load categories", ex);
        }
    }

    private void onCategorySelect() {
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) categoryTree.getLastSelectedPathComponent();
        if (node == null) return;
        currentCategory = node.toString();
        refreshNotes();
    }

    private void doSearch() {
        refreshNotes();
    }

    private void clearSearch() {
        searchField.setText("");
        notesSearchField.setText("");
        refreshNotes();
    }

    private void showAdvanced() {
        JOptionPane.showMessageDialog(this, "Advanced search options coming soon.",
                "Advanced Search", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshNotes() {
        try {
            String query = notesSearchField.getText().trim();
            if (query.isEmpty()) query = searchField.getText().trim();
            List<Note> notes = db.allNotes(currentCategory, query.isEmpty() ? null : query, false);

            notesListModel.clear();
            for (Note n : notes) notesListModel.addElement(n);
            notesCountLabel.setText(notes.size() + " notes");
            statusLabel.setText("Ready");
        } catch (SQLException ex) {
            showError("Failed to load notes", ex);
        }
    }

    private void onNoteSelect() {
        Note selected = notesList.getSelectedValue();
        if (selected == null) return;
        currentNoteId = selected.getId();
        titleField.setText(selected.getTitle());
        categoryCombo.setSelectedItem(selected.getCategory());
        tagsField.setText(selected.getTags());
        bodyArea.setText(selected.getBody());
        updateWordCount();
    }

    private void newNote() {
        currentNoteId = null;
        titleField.setText("");
        if (categoryCombo.getItemCount() > 0) categoryCombo.setSelectedIndex(0);
        tagsField.setText("");
        bodyArea.setText("");
        statusLabel.setText("Creating new note...");
        titleField.requestFocusInWindow();
    }

    private void cancelEdit() {
        newNote();
        statusLabel.setText("Edit cancelled");
    }

    private void saveNote() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a title for the note.",
                    "Missing Title", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String body = bodyArea.getText();
        String category = categoryCombo.getSelectedItem() != null
                ? categoryCombo.getSelectedItem().toString() : "General";
        String tags = tagsField.getText().trim();

        try {
            if (currentNoteId != null) {
                db.updateNote(currentNoteId, title, body, category, tags);
            } else {
                currentNoteId = db.createNote(title, body, category, tags);
            }
            statusLabel.setText("Note saved successfully!");
            refreshNotes();
        } catch (SQLException ex) {
            showError("Failed to save note", ex);
        }
    }

    private void deleteNote() {
        if (currentNoteId == null) {
            JOptionPane.showMessageDialog(this, "No note selected.", "Delete Note",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this note?", "Delete Note",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                db.deleteNote(currentNoteId);
                newNote();
                refreshNotes();
                statusLabel.setText("Note deleted");
            } catch (SQLException ex) {
                showError("Failed to delete note", ex);
            }
        }
    }

    private void toggleFavorite() {
        if (currentNoteId == null) return;
        try {
            db.toggleFavorite(currentNoteId);
            refreshNotes();
            statusLabel.setText("Favorite toggled");
        } catch (SQLException ex) {
            showError("Failed to toggle favorite", ex);
        }
    }

    private void toggleArchive() {
        if (currentNoteId == null) return;
        try {
            db.toggleArchive(currentNoteId);
            refreshNotes();
            statusLabel.setText("Note archived/unarchived");
        } catch (SQLException ex) {
            showError("Failed to toggle archive", ex);
        }
    }

    private void addCategory() {
        String name = JOptionPane.showInputDialog(this, "Category name:", "New Category",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        try {
            if (db.addCategory(name.trim())) {
                refreshCategories();
            } else {
                JOptionPane.showMessageDialog(this, "That category already exists.",
                        "Category exists", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            showError("Failed to add category", ex);
        }
    }

    private void removeCategory() {
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) categoryTree.getLastSelectedPathComponent();
        if (node == null || node == categoryRoot) {
            JOptionPane.showMessageDialog(this, "Select a category (not 'All Notes') to remove.",
                    "Remove Category", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove category '" + node + "'?", "Remove Category",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                db.deleteCategory(node.toString());
                refreshCategories();
            } catch (SQLException ex) {
                showError("Failed to remove category", ex);
            }
        }
    }

    private void renameCategory() {
        JOptionPane.showMessageDialog(this, "Renaming categories is coming in a future update.",
                "Rename Category", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateWordCount() {
        String text = bodyArea.getText().trim();
        int words = text.isEmpty() ? 0 : text.split("\\s+").length;
        wordsLabel.setText("Words: " + words);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "NotepadPro-Advanced 1.0.0\n\n" +
                        "A lightweight note-taking app with categories,\n" +
                        "tags, search and archiving.\n\n" +
                        "Built with Java Swing & MySQL.",
                "About NotepadPro-Advanced", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(this, message + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
