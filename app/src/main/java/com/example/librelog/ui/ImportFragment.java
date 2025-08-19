package com.example.librelog.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
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

    private static final String TAG = "ImportFragment";

    private AppDatabase db;
    private Button buttonSelectImportFile;
    private TextView textImportStatus;

    // Defines the CSV format: ID,Timestamp (yyyy-MM-dd HH:mm:ss),Event,Notes
    private final SimpleDateFormat csvDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());


    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        String fileName = getFileName(uri);
                        if (textImportStatus != null) {
                            textImportStatus.setText("Selected file: " + fileName + "\nProcessing...");
                        }
                        Log.d(TAG, "Selected file URI: " + uri.toString());
                        processImportFile(uri);
                    } else {
                        if (textImportStatus != null) {
                            textImportStatus.setText("Error: Could not get file URI.");
                        }
                        Log.e(TAG, "File URI was null");
                    }
                } else {
                    if (textImportStatus != null) {
                        textImportStatus.setText("No file selected or file selection cancelled.");
                    }
                    Log.d(TAG, "File selection cancelled or failed.");
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);

        buttonSelectImportFile = view.findViewById(R.id.button_select_import_file);
        textImportStatus = view.findViewById(R.id.text_import_status);

        if (buttonSelectImportFile != null) {
            buttonSelectImportFile.setOnClickListener(v -> openFilePicker());
        } else {
            Log.e(TAG, "button_select_import_file not found in layout.");
        }

        if (textImportStatus == null) {
            Log.e(TAG, "text_import_status not found in layout.");
        }


        return view;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Set the primary MIME type specifically to CSV
        intent.setType("text/csv");

        // Optionally, provide an array of other acceptable MIME types as a fallback
        String[] mimeTypes = {
                "text/comma-separated-values",
                "application/csv",
                "text/plain", // If .txt files are sometimes used for CSV data
                "application/octet-stream" // General fallback
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        try {
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching file picker", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Error: Cannot open file picker.", Toast.LENGTH_SHORT).show();
                if (textImportStatus != null) {
                    textImportStatus.setText("Error: Could not open file picker.");
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "Unknown file";
        if (getContext() == null || uri == null) return fileName;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from content URI", e);
            }
        } else if (uri.getPath() != null) {
            fileName = uri.getLastPathSegment();
            if (fileName == null) fileName = "Unknown file";
        }
        return fileName;
    }

    private void processImportFile(Uri fileUri) {
        if (db == null) {
            Log.e(TAG, "Database not initialized. Cannot import.");
            if (isAdded() && textImportStatus != null) {
                getActivity().runOnUiThread(() -> textImportStatus.setText("Error: Database not available."));
            }
            return;
        }
        if (getContext() == null) {
            Log.e(TAG, "Context is null in processImportFile. Cannot import.");
            if (isAdded() && textImportStatus != null) {
                getActivity().runOnUiThread(() -> textImportStatus.setText("Error: Cannot access file."));
            }
            return;
        }


        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LogEntry> logEntriesToImport = new ArrayList<>();
            int successfullyImportedCount = 0;
            int failedLinesCount = 0;
            StringBuilder errors = new StringBuilder();
            String finalImportResultMessage;

            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                int lineNumber = 0;
                boolean isHeader = true; // To skip the header row if your CSV export includes one

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (isHeader) { // Assuming the CSV export format includes headers
                        isHeader = false;
                        // You might want to validate headers here if necessary
                        Log.d(TAG, "Skipping header line: " + line);
                        continue;
                    }

                    // CSV format: ID,Timestamp (yyyy-MM-dd HH:mm:ss),Event,Notes
                    String[] tokens = line.split(",", -1); // -1 to keep trailing empty strings

                    if (tokens.length >= 4) { // Expect at least ID, timestamp, event, notes
                        try {
                            // We don't necessarily need to use the imported ID if the DB auto-generates it
                            // int id = Integer.parseInt(tokens[0].trim());
                            String timestampStr = tokens[1].trim();
                            String event = unquoteCsvString(tokens[2].trim());
                            String notes = unquoteCsvString(tokens[3].trim());

                            Date timestampDate = csvDateFormat.parse(timestampStr);
                            if (timestampDate == null) throw new ParseException("Parsed date was null", 0);

                            LogEntry entry = new LogEntry();
                            // entry.setId(id); // If you want to preserve original IDs, ensure your LogEntry allows it and DAO handles conflicts
                            entry.setTimestamp(timestampDate.getTime());
                            entry.setEvent(event);
                            entry.setNotes(notes);

                            logEntriesToImport.add(entry);
                            // successfullyImportedCount++; // Count will be size of list
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Skipping line " + lineNumber + " due to ID parsing error: " + line, e);
                            errors.append("Line ").append(lineNumber).append(": Invalid ID format.\n");
                            failedLinesCount++;
                        } catch (ParseException e) {
                            Log.w(TAG, "Skipping line " + lineNumber + " due to timestamp parsing error: " + line, e);
                            errors.append("Line ").append(lineNumber).append(": Invalid timestamp format (expected yyyy-MM-dd HH:mm:ss).\n");
                            failedLinesCount++;
                        } catch (Exception e) { // Catch other unexpected parsing issues
                            Log.w(TAG, "Skipping line " + lineNumber + " due to unexpected error: " + line, e);
                            errors.append("Line ").append(lineNumber).append(": ").append(e.getMessage()).append("\n");
                            failedLinesCount++;
                        }
                    } else if (!line.trim().isEmpty()) { // Don't count empty lines as errors
                        Log.w(TAG, "Skipping line " + lineNumber + " due to incorrect column count: " + line);
                        errors.append("Line ").append(lineNumber).append(": Incorrect column count (expected 4, got ").append(tokens.length).append(").\n");
                        failedLinesCount++;
                    }
                }

                if (!logEntriesToImport.isEmpty()) {
                    // Using OnConflictStrategy.REPLACE. Change if needed.
                    db.logEntryDao().insertAll(logEntriesToImport.toArray(new LogEntry[0]));
                    successfullyImportedCount = logEntriesToImport.size();
                }

                if (failedLinesCount == 0 && successfullyImportedCount > 0) {
                    finalImportResultMessage = "Successfully imported " + successfullyImportedCount + " entries.";
                } else if (successfullyImportedCount > 0) {
                    finalImportResultMessage = "Imported " + successfullyImportedCount + " entries. " +
                            failedLinesCount + " lines had errors.\nDetails:\n" + errors.toString();
                } else if (failedLinesCount > 0) {
                    finalImportResultMessage = "Import failed. " + failedLinesCount + " lines had errors.\nFile might be empty or in wrong format.\nDetails:\n" + errors.toString();
                } else {
                    finalImportResultMessage = "No new entries found or imported from the file.";
                }


            } catch (IOException e) {
                Log.e(TAG, "Error reading or processing file URI: " + fileUri.toString(), e);
                finalImportResultMessage = "Error during import: " + e.getMessage();
            } catch (Exception e) { // Catch any other unexpected errors during processing
                Log.e(TAG, "Unexpected error during import processing URI: " + fileUri.toString(), e);
                finalImportResultMessage = "An unexpected error occurred during import: " + e.getMessage();
            }

            Log.d(TAG, "Import result: " + finalImportResultMessage);

            // Create an effectively final variable for the UI thread lambda
            final String messageForUi = finalImportResultMessage;

            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (textImportStatus != null) {
                        textImportStatus.setText(messageForUi); // Use the new variable
                    }
                    if (getContext() != null) { // Check context for Toast
                        Toast.makeText(getContext(), "Import finished. Check status for details.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    // Helper function to remove surrounding quotes and unescape double quotes from CSV fields
    private String unquoteCsvString(String data) {
        if (data == null) return "";
        String trimmedData = data.trim();
        if (trimmedData.startsWith("\"") && trimmedData.endsWith("\"")) {
            // Remove surrounding quotes
            trimmedData = trimmedData.substring(1, trimmedData.length() - 1);
            // Replace "" with "
            return trimmedData.replaceAll("\"\"", "\"");
        }
        return trimmedData; // Return as is if not quoted
    }
}
