package com.example.librelog.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
// import android.widget.TextView; // Removed importNotesTextView related import
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
// import androidx.room.OnConflictStrategy; // Removed unused import

import com.example.librelog.AppDatabase;
import com.example.librelog.LogEntry;
import com.example.librelog.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImportFragment extends Fragment {

    private AppDatabase db;
    // private Button selectFileButton; // Made local
    // private TextView importNotesTextView; // Removed as it was unused

    private final ActivityResultLauncher<Intent> openFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importDataFromUri(uri);
                    } else {
                        Toast.makeText(getContext(), "Failed to get file location.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);

        db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        Button selectFileButton = view.findViewById(R.id.button_select_import_file); // Now a local variable
        // importNotesTextView = view.findViewById(R.id.text_import_notes); // Removed assignment for unused field

        selectFileButton.setOnClickListener(v -> launchOpenFileIntent());

        return view;
    }

    private void launchOpenFileIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv"); // Or "*/*" to allow all file types, then filter by extension
        openFileLauncher.launch(intent);
    }

    private void importDataFromUri(Uri uri) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LogEntry> logEntriesToImport = new ArrayList<>();
            int successfullyImportedCount = 0;
            int failedImportCount = 0;
            String errorMessage = null;

            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                boolean isHeader = true; // To skip the header row
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue; // Skip header row
                    }

                    String[] tokens = line.split(",", -1);

                    if (tokens.length == 4) { // ID, Timestamp, Event, Notes
                        try {
                            LogEntry entry = new LogEntry();
                            entry.setId(Integer.parseInt(tokens[0].trim()));
                            Date timestampDate = dateFormat.parse(tokens[1].trim());
                            entry.setTimestamp(timestampDate != null ? timestampDate.getTime() : 0);
                            entry.setEvent(unquoteCsvString(tokens[2].trim()));
                            entry.setNotes(unquoteCsvString(tokens[3].trim()));
                            logEntriesToImport.add(entry);
                        } catch (NumberFormatException | ParseException e) {
                            failedImportCount++;
                            if (errorMessage == null) errorMessage = "Malformed data in CSV line: " + e.getMessage();
                        }
                    } else {
                         failedImportCount++;
                         if (errorMessage == null) errorMessage = "Incorrect number of columns in CSV line.";
                    }
                }

                if (!logEntriesToImport.isEmpty()) {
                    db.logEntryDao().insertAll(logEntriesToImport.toArray(new LogEntry[0]));
                    successfullyImportedCount = logEntriesToImport.size();
                }

            } catch (IOException e) {
                errorMessage = "Error reading file: " + e.getMessage();
                failedImportCount = logEntriesToImport.size() + failedImportCount;
                successfullyImportedCount = 0;
            }

            final String finalErrorMessage = errorMessage;
            final int finalSuccessfullyImportedCount = successfullyImportedCount;
            final int finalFailedImportCount = failedImportCount;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    String message;
                    if (finalSuccessfullyImportedCount > 0 && finalFailedImportCount == 0) {
                        message = finalSuccessfullyImportedCount + " entries imported successfully.";
                    } else if (finalSuccessfullyImportedCount > 0 && finalFailedImportCount > 0) {
                        message = finalSuccessfullyImportedCount + " entries imported, " + finalFailedImportCount + " failed. Check data format.";
                    } else if (finalFailedImportCount > 0) {
                        message = "Import failed. " + finalFailedImportCount + " entries could not be imported. " + (finalErrorMessage != null ? finalErrorMessage : "Check data format.");
                    } else {
                        message = "No new data to import or file was empty.";
                    }
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String unquoteCsvString(String data) {
        if (data == null) return "";
        String trimmedData = data.trim();
        if (trimmedData.startsWith("\"") && trimmedData.endsWith("\"")) {
            trimmedData = trimmedData.substring(1, trimmedData.length() - 1);
            return trimmedData.replaceAll("\"\"", "\"");
        }
        return trimmedData;
    }
}
