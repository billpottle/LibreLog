package com.example.librelog.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.librelog.AppDatabase;
import com.example.librelog.LogEntry;
import com.example.librelog.LogEntryAdapter;
import com.example.librelog.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment {

    private AppDatabase db;
    private RecyclerView recyclerViewLogEntries;
    private LogEntryAdapter logEntryAdapter;
    private TextView textViewNoEntries;
    private FloatingActionButton fabAddLogEntry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        if (getContext() != null) {
            db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewLogEntries = view.findViewById(R.id.recycler_view_log_entries);
        textViewNoEntries = view.findViewById(R.id.text_view_no_entries);
        fabAddLogEntry = view.findViewById(R.id.fab_add_log_entry);

        if (recyclerViewLogEntries != null) {
            recyclerViewLogEntries.setLayoutManager(new LinearLayoutManager(getContext()));
            logEntryAdapter = new LogEntryAdapter(new ArrayList<>()); // Make sure LogEntryAdapter is correctly imported/defined
            recyclerViewLogEntries.setAdapter(logEntryAdapter);
        }

        if (fabAddLogEntry != null) {
            fabAddLogEntry.setOnClickListener(v -> showAddLogEntryDialog());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndDisplayLogEntries();
    }

    private void showAddLogEntryDialog() {
        if (getContext() == null || getActivity() == null) {
            if (getContext() != null) { // Check context before showing Toast
                Toast.makeText(requireContext().getApplicationContext(), "Cannot show dialog, context error.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_log_entry, null); // Make sure R.layout.dialog_add_log_entry exists
        builder.setView(dialogView);

        final EditText editTextEventName = dialogView.findViewById(R.id.edit_text_event_name);
        final EditText editTextNotes = dialogView.findViewById(R.id.edit_text_notes);

        builder.setTitle("Record New Event")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String eventName = editTextEventName.getText().toString().trim();
                String notes = editTextNotes.getText().toString().trim();

                if (TextUtils.isEmpty(eventName)) {
                    editTextEventName.setError("Event name cannot be empty.");
                    if(getContext() != null) {
                        Toast.makeText(getContext(), "Event name cannot be empty.", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                LogEntry newLogEntry = new LogEntry(); // Make sure LogEntry is correctly imported/defined
                newLogEntry.setTimestamp(new Date().getTime());
                newLogEntry.setEvent(eventName);

                if (!TextUtils.isEmpty(notes)) {
                    newLogEntry.setNotes(notes);
                } else {
                    newLogEntry.setNotes(null);
                }

                if (db == null) {
                    if(getContext() != null) {
                        Toast.makeText(getContext(), "Database not available.", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    if (db.logEntryDao() != null) { // Check if DAO is available
                        db.logEntryDao().insert(newLogEntry);
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if(getContext() != null) {
                                Toast.makeText(getContext(), "Event recorded.", Toast.LENGTH_SHORT).show();
                            }
                            loadAndDisplayLogEntries();
                            dialog.dismiss();
                        });
                    }
                });
            });
        });
        dialog.show();
    }

    private void loadAndDisplayLogEntries() {
        if (db == null || logEntryAdapter == null) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (textViewNoEntries != null) {
                        textViewNoEntries.setText("App components not ready.");
                        textViewNoEntries.setVisibility(View.VISIBLE);
                    }
                    if (recyclerViewLogEntries != null) {
                        recyclerViewLogEntries.setVisibility(View.GONE);
                    }
                });
            }
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (db.logEntryDao() == null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (textViewNoEntries != null) {
                            textViewNoEntries.setText("Database access error.");
                            textViewNoEntries.setVisibility(View.VISIBLE);
                        }
                        if (recyclerViewLogEntries != null) {
                            recyclerViewLogEntries.setVisibility(View.GONE);
                        }
                    });
                }
                return;
            }
            List<LogEntry> logEntries = db.logEntryDao().getAllLogEntries();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (logEntries != null && !logEntries.isEmpty()) {
                        logEntryAdapter.setLogEntries(logEntries);
                        if(recyclerViewLogEntries != null) recyclerViewLogEntries.setVisibility(View.VISIBLE);
                        if (textViewNoEntries != null) {
                            textViewNoEntries.setVisibility(View.GONE);
                        }
                    } else {
                        if (logEntryAdapter != null) { // Ensure adapter is not null before setting
                            logEntryAdapter.setLogEntries(new ArrayList<>());
                        }
                        if(recyclerViewLogEntries != null) recyclerViewLogEntries.setVisibility(View.GONE);
                        if (textViewNoEntries != null) {
                            textViewNoEntries.setVisibility(View.VISIBLE);
                            textViewNoEntries.setText("No log entries yet.");
                        }
                    }
                });
            }
        });
    }
}
