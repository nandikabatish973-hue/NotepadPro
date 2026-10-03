package com.notepadpro;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Handles all JDBC/MySQL access for the app: categories and notes CRUD.
 */
public class DatabaseManager {

    private final Connection connection;

    public DatabaseManager() throws SQLException {
        Properties props = loadProperties();

        String url = System.getProperty("db.url", props.getProperty("db.url"));
        String user = System.getProperty("db.user", props.getProperty("db.user"));
        String password = System.getProperty("db.password", props.getProperty("db.password", ""));

        this.connection = DriverManager.getConnection(url, user, password);
        ensureDefaultCategories();
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Could not load db.properties: " + e.getMessage());
        }
        return props;
    }

    private void ensureDefaultCategories() throws SQLException {
        String[] defaults = {"General", "Work", "Personal", "Ideas", "Archive"};
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT COUNT(*) FROM categories")) {
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO categories(name) VALUES (?)")) {
                    for (String name : defaults) {
                        insert.setString(1, name);
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    // ---- Categories --------------------------------------------------

    public List<String> getCategories() throws SQLException {
        List<String> result = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM categories ORDER BY id")) {
            while (rs.next()) {
                result.add(rs.getString("name"));
            }
        }
        return result;
    }

    public boolean addCategory(String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO categories(name) VALUES (?)")) {
            ps.setString(1, name);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            return false;
        }
    }

    public void deleteCategory(String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM categories WHERE name=?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    // ---- Notes ---------------------------------------------------------

    public List<Note> allNotes(String category, String query, Boolean archived) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM notes WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (category != null && !category.equals("All Notes")) {
            sql.append(" AND category=?");
            params.add(category);
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (title LIKE ? OR body LIKE ? OR tags LIKE ?)");
            String like = "%" + query + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (archived != null) {
            sql.append(" AND archived=?");
            params.add(archived ? 1 : 0);
        }
        sql.append(" ORDER BY updated_at DESC");

        List<Note> notes = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapRow(rs));
                }
            }
        }
        return notes;
    }

    public Note getNote(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM notes WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int createNote(String title, String body, String category, String tags) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO notes(title, body, category, tags, favorite, archived) VALUES (?, ?, ?, ?, 0, 0)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, body);
            ps.setString(3, category);
            ps.setString(4, tags);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void updateNote(int id, String title, String body, String category, String tags) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE notes SET title=?, body=?, category=?, tags=? WHERE id=?")) {
            ps.setString(1, title);
            ps.setString(2, body);
            ps.setString(3, category);
            ps.setString(4, tags);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    public void deleteNote(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM notes WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void toggleFavorite(int id) throws SQLException {
        Note n = getNote(id);
        if (n == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE notes SET favorite=? WHERE id=?")) {
            ps.setInt(1, n.isFavorite() ? 0 : 1);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void toggleArchive(int id) throws SQLException {
        Note n = getNote(id);
        if (n == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE notes SET archived=? WHERE id=?")) {
            ps.setInt(1, n.isArchived() ? 0 : 1);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        return new Note(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("category"),
                rs.getString("tags"),
                rs.getBoolean("favorite"),
                rs.getBoolean("archived")
        );
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
