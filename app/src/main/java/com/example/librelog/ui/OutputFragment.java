package com.example.librelog.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.librelog.AppDatabase;
import com.example.librelog.LogEntry;
import io.github.billpottle.librelog.R;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OutputFragment extends Fragment {

    private AppDatabase db;
    private Button exportButton;
    private TextView exportNotesTextView;

    private final ActivityResultLauncher<Intent> createFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        exportDataToUri(uri);
                    } else {
                        Toast.makeText(getContext(), "Failed to get file location.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_output, container, false);

        db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        exportButton = view.findViewById(R.id.button_export_csv);
        exportNotesTextView = view.findViewById(R.id.text_export_notes); // Although not directly used in logic, good to have reference if needed

        exportButton.setOnClickListener(v -> launchCreateFileIntent());

        return view;
    }

    private void launchCreateFileIntent() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        // Suggest a filename (the user can change it)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String suggestedName = "librelog_export_" + sdf.format(new Date()) + ".csv";
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
        createFileLauncher.launch(intent);
    }

    private void exportDataToUri(Uri uri) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LogEntry> logEntries = db.logEntryDao().getAllLogEntriesNoFilter(); // Assuming this method exists and fetches all data

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (logEntries == null || logEntries.isEmpty()) {
                    Toast.makeText(getContext(), "No data to export.", Toast.LENGTH_SHORT).show();
                    return;
                }

                try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                     OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {

                    // CSV Header
                    writer.append("ID,Timestamp,Event,Notes\n");

                    // CSV Data
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    for (LogEntry entry : logEntries) {
                        writer.append(String.valueOf(entry.getId()));
                        writer.append(",");
                        writer.append(dateFormat.format(new Date(entry.getTimestamp())));
                        writer.append(",");
                        writer.append("\"").append(escapeCsvString(entry.getEvent())).append("\"");
                        writer.append(",");
                        writer.append("\"").append(escapeCsvString(entry.getNotes())).append("\"");
                        writer.append("\n");
                    }
                    writer.flush();
                    Toast.makeText(getContext(), "Data exported successfully to " + getFileName(uri), Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Error exporting data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // Helper to escape strings for CSV (handles commas and quotes within fields)
    private String escapeCsvString(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\"", "\"\""); // Escape double quotes
        return escapedData;
    }

    // Helper method to get the file name from URI (optional, for display in Toast)
    private String getFileName(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                         fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }
        return fileName != null ? fileName : "selected file";
    }
}
