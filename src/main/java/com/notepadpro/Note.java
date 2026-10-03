package com.notepadpro;

/**
 * Simple data holder representing one row from the `notes` table.
 */
public class Note {
    private final int id;
    private final String title;
    private final String body;
    private final String category;
    private final String tags;
    private final boolean favorite;
    private final boolean archived;

    public Note(int id, String title, String body, String category,
                String tags, boolean favorite, boolean archived) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.category = category;
        this.tags = tags;
        this.favorite = favorite;
        this.archived = archived;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCategory() { return category; }
    public String getTags() { return tags; }
    public boolean isFavorite() { return favorite; }
    public boolean isArchived() { return archived; }

    @Override
    public String toString() {
        return (favorite ? "\u2605 " : "") + title;
    }
}
