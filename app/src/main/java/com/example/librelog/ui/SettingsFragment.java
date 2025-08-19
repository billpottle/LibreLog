package com.example.librelog.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.librelog.AppDatabase;
import com.example.librelog.R;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment"; // TAG for logging
    private AppDatabase db;
    private Button deleteAllDataButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Ensure db is initialized here if not already
        if (getContext() != null) {
            db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        }
        deleteAllDataButton = view.findViewById(R.id.button_delete_all_data);

        deleteAllDataButton.setOnClickListener(v -> {
            if (getContext() != null) { // Add null check for context before showing dialog
                showDeleteConfirmationDialog();
            } else {
                Log.e(TAG, "Context was null, cannot show delete confirmation dialog.");
            }
        });

        return view;
    }

    private void showDeleteConfirmationDialog() {
        // Ensure getContext() is not null when building the dialog
        if (getContext() == null) {
            Log.e(TAG, "Cannot show delete confirmation dialog, getContext() is null.");
            // It's safer to use requireActivity().getApplicationContext() for Toasts if getContext() might be null
            if (isAdded() && getActivity() != null) { // Check if fragment is added
                Toast.makeText(requireActivity().getApplicationContext(), "Error: Cannot perform action.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete all data? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteAllData();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteAllData() {
        // Ensure db is not null before trying to use it
        if (db == null) {
            Log.e(TAG, "Database instance is null in deleteAllData. Cannot delete.");
            if (isAdded() && getActivity() != null) { // Check if fragment is added
                getActivity().runOnUiThread(() -> {
                    if (getContext() != null) { // Check getContext() for Toast
                        Toast.makeText(getContext(), "Error: Database not available.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                db.logEntryDao().deleteAllEntries();
                Log.d(TAG, "deleteAllEntries executed successfully on background thread."); // Log success
                if (isAdded() && getActivity() != null) { // Check if fragment is added
                    getActivity().runOnUiThread(() -> {
                        if (getContext() != null) { // Check getContext() for Toast
                            Toast.makeText(getContext(), "All data deleted successfully.", Toast.LENGTH_SHORT).show();
                        }
                        Log.d(TAG, "Toast shown: All data deleted successfully.");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting all data", e); // Log any exception
                if (isAdded() && getActivity() != null) { // Check if fragment is added
                    getActivity().runOnUiThread(() -> {
                        if (getContext() != null) { // Check getContext() for Toast
                            Toast.makeText(getContext(), "Error deleting data. See logs.", Toast.LENGTH_LONG).show();
                        }
                        Log.d(TAG, "Toast shown: Error deleting data.");
                    });
                }
            }
        });
    }
}

