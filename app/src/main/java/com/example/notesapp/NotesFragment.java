package com.example.notesapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class NotesFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotesAdapter notesAdapter;
    private List<Note> notesList;
    private SQLiteDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        notesList = new ArrayList<>();
        notesAdapter = new NotesAdapter(notesList, note -> showNoteOptionsDialog(note));
        recyclerView.setAdapter(notesAdapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_note);
        fab.setOnClickListener(v -> showAddNoteDialog());

        // Load notes from database
        loadNotes();

        return view;
    }

    private void showNoteOptionsDialog(Note note) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note_options, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Note Options")
                .setView(dialogView)
                .setPositiveButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button updateButton = dialogView.findViewById(R.id.button_update);
        Button deleteButton = dialogView.findViewById(R.id.button_delete);

        updateButton.setOnClickListener(v -> {
            dialog.dismiss();
            showUpdateNoteDialog(note);
        });

        deleteButton.setOnClickListener(v -> {
            dialog.dismiss();
            deleteNoteFromDatabase(note);
        });
    }

    private void showUpdateNoteDialog(Note note) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_note, null);

        final EditText editTextTitle = dialogView.findViewById(R.id.edit_text_title);
        final EditText editTextContent = dialogView.findViewById(R.id.edit_text_content);

        editTextTitle.setText(note.getTitle());
        editTextContent.setText(note.getContent());

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Update Note")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String title = editTextTitle.getText().toString();
                    String content = editTextContent.getText().toString();
                    updateNoteInDatabase(note.getId(), title, content);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteNoteFromDatabase(Note note) {
        NotesDbHelper dbHelper = new NotesDbHelper(getContext());
        database = dbHelper.getWritableDatabase();

        int rowsDeleted = database.delete(
                NotesContract.NoteEntry.TABLE_NAME,
                NotesContract.NoteEntry._ID + " = ?",
                new String[]{String.valueOf(note.getId())}
        );

        if (rowsDeleted > 0) {
            notesList.remove(note);
            notesAdapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "Note deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error deleting note", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNoteInDatabase(long id, String title, String content) {
        NotesDbHelper dbHelper = new NotesDbHelper(getContext());
        database = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(NotesContract.NoteEntry.COLUMN_TITLE, title);
        values.put(NotesContract.NoteEntry.COLUMN_CONTENT, content);

        int rowsUpdated = database.update(
                NotesContract.NoteEntry.TABLE_NAME,
                values,
                NotesContract.NoteEntry._ID + " = ?",
                new String[]{String.valueOf(id)}
        );

        if (rowsUpdated > 0) {
            for (Note note : notesList) {
                if (note.getId() == id) {
                    note.setTitle(title);
                    note.setContent(content);
                    break;
                }
            }
            notesAdapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "Note updated", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error updating note", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddNoteDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_note, null);

        final EditText editTextTitle = dialogView.findViewById(R.id.edit_text_title);
        final EditText editTextContent = dialogView.findViewById(R.id.edit_text_content);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Note")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = editTextTitle.getText().toString();
                    String content = editTextContent.getText().toString();
                    addNoteToDatabase(title, content);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addNoteToDatabase(String title, String content) {
        NotesDbHelper dbHelper = new NotesDbHelper(getContext());
        database = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(NotesContract.NoteEntry.COLUMN_TITLE, title);
        values.put(NotesContract.NoteEntry.COLUMN_CONTENT, content);

        long newRowId = database.insert(NotesContract.NoteEntry.TABLE_NAME, null, values);

        if (newRowId != -1) {
            notesList.add(new Note(newRowId, title, content));
            notesAdapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "Note added", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error adding note", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNotes() {
        NotesDbHelper dbHelper = new NotesDbHelper(getContext());
        database = dbHelper.getReadableDatabase();

        Cursor cursor = database.query(
                NotesContract.NoteEntry.TABLE_NAME,
                null, null, null, null, null, null
        );

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(NotesContract.NoteEntry._ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(NotesContract.NoteEntry.COLUMN_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(NotesContract.NoteEntry.COLUMN_CONTENT));

            notesList.add(new Note(id, title, content));
        }
        cursor.close();

        notesAdapter.notifyDataSetChanged();
    }
}
