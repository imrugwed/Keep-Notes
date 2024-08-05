package com.example.notesapp;

import android.provider.BaseColumns;

public final class NotesContract {

    private NotesContract() {}

    public static class NoteEntry implements BaseColumns {
        public static final String TABLE_NAME = "notes";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_CONTENT = "content";
    }
}

