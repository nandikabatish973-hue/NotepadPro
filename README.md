# NotepadPro-Advanced 1.0.0 (Java Swing + MySQL)

A desktop note-taking app built with **Java Swing** for the UI and
**MySQL** for storage. Same layout as the original: menu bar, top search
bar, categories tree, notes list, and a note editor panel.

## Features
- Categories sidebar (All Notes + General/Work/Personal/Ideas/Archive, plus custom categories)
- Notes list with search (by title/body/tags)
- Note editor: Title, Category, Tags, body text area with live word count
- Favorite and Archive toggles
- Status bar: save confirmation, word count, live clock
- Left icon rail: New / Save / Delete / Favorite / Archive

## Requirements
- Java 17+ (JDK, not just JRE, since you need `javac`/Maven to build)
- Maven 3.6+
- A running MySQL server (5.7+ or 8.x)

## 1. Set up the database

```bash
mysql -u root -p < schema.sql
```

This creates a `notepadpro` database with `categories` and `notes` tables,
and seeds the default categories.

## 2. Configure the connection

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/notepadpro?useSSL=false&serverTimezone=UTC
db.user=root
db.password=yourpassword
```

Alternatively, override at launch without editing the file:

```bash
java -Ddb.url=jdbc:mysql://localhost:3306/notepadpro \
     -Ddb.user=root -Ddb.password=yourpassword \
     -jar target/NotepadPro-Advanced.jar
```

## 3. Build

```bash
mvn clean package
```

This produces a runnable "fat jar" (MySQL driver bundled in) at:

```
target/NotepadPro-Advanced.jar
```

## 4. Run

```bash
java -jar target/NotepadPro-Advanced.jar
```

## Project structure

```
NotepadPro-Advanced-Swing/
├── pom.xml                                    # Maven build config (MySQL driver + shade plugin)
├── schema.sql                                 # MySQL schema + default categories
├── README.md
└── src/main/
    ├── java/com/notepadpro/
    │   ├── Main.java                          # entry point
    │   ├── Note.java                          # note data model
    │   ├── DatabaseManager.java               # JDBC/MySQL data access layer
    │   └── NotepadProFrame.java                # Swing UI (menu, search bar, tree, list, editor)
    └── resources/
        └── db.properties                      # DB connection settings
```

## Notes
This is a clean-room recreation inspired by the layout in the provided
screenshot. Feel free to extend it further — e.g. category renaming,
rich text formatting, or exporting notes.
