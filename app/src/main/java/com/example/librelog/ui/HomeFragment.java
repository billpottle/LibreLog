package com.example.librelog.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.librelog.AppDatabase;
import com.example.librelog.EventType;
import com.example.librelog.EventTypeDao;
import com.example.librelog.LogEntry;
import com.example.librelog.LogEntryAdapter;
import com.example.librelog.LogEntryDao;
import com.example.librelog.R;
import com.github.mikephil.charting.charts.BarChart;
// Ensure you have imports for chart data if you are using MPAndroidChart
// e.g., import com.github.mikephil.charting.data.BarData;
// import com.github.mikephil.charting.data.BarDataSet;
// import com.github.mikephil.charting.data.BarEntry;
// import com.github.mikephil.charting.utils.ColorTemplate;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
// import java.util.stream.Collectors; // Can be used for mapping if preferred

public class HomeFragment extends Fragment {

    private LogEntryDao logEntryDao;
    private EventTypeDao eventTypeDao;
    private LogEntryAdapter logEntryAdapter;
    private RecyclerView recyclerViewLogEntries;
    private TextView textViewNoEntries;
    private FloatingActionButton fabAddLogEntry;
    private AppDatabase db;

    // Chart related views
    private BarChart barChartHourlyEvents;
    private BarChart barChartMonthlyEvents;

    private List<EventType> availableEventTypes = new ArrayList<>(); // Cache for spinner items

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        logEntryDao = db.logEntryDao();
        eventTypeDao = db.eventTypeDao();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerViewLogEntries = view.findViewById(R.id.recycler_view_log_entries);
        textViewNoEntries = view.findViewById(R.id.text_view_no_entries);
        fabAddLogEntry = view.findViewById(R.id.fab_add_log_entry);

        barChartHourlyEvents = view.findViewById(R.id.bar_chart_hourly_events);
        barChartMonthlyEvents = view.findViewById(R.id.bar_chart_monthly_events);

        setupRecyclerView();
        loadAndDisplayLogEntries();
        // loadChartData(); // Uncomment and implement if you have chart loading logic

        fabAddLogEntry.setOnClickListener(v -> showAddLogEntryDialog());

        // Refresh data when fragment becomes visible again, e.g., after adding event types in settings
        // and returning to this fragment.
        AppDatabase.databaseWriteExecutor.execute(() -> {
            availableEventTypes = eventTypeDao.getAllEventTypes();
        });


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh log entries and potentially event types if settings were changed
        loadAndDisplayLogEntries();
        // Fetch event types again in case they were modified in Settings
        AppDatabase.databaseWriteExecutor.execute(() -> {
            availableEventTypes = eventTypeDao.getAllEventTypes();
        });
    }


    private void setupRecyclerView() {
        if (recyclerViewLogEntries == null) return;
        recyclerViewLogEntries.setLayoutManager(new LinearLayoutManager(getContext()));
// Corrected line for HomeFragment.java
        logEntryAdapter = new LogEntryAdapter(new ArrayList<>());        recyclerViewLogEntries.setAdapter(logEntryAdapter);
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
            List<LogEntry> logEntries = logEntryDao.getRecentLogEntries(5);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (logEntries != null && !logEntries.isEmpty()) {
                        logEntryAdapter.setLogEntries(logEntries); // Make sure LogEntryAdapter has setLogEntries
                        if (recyclerViewLogEntries != null) recyclerViewLogEntries.setVisibility(View.VISIBLE);
                        if (textViewNoEntries != null) textViewNoEntries.setVisibility(View.GONE);
                    } else {
                        if(logEntryAdapter != null) logEntryAdapter.setLogEntries(new ArrayList<>());
                        if (recyclerViewLogEntries != null) recyclerViewLogEntries.setVisibility(View.GONE);
                        if (textViewNoEntries != null) {
                            textViewNoEntries.setText("No recent log entries to display.");
                            textViewNoEntries.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
    }

    // private void loadChartData() { /* Your chart loading logic here */ }

    private void showAddLogEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_log_entry, null);
        builder.setView(dialogView);

        Spinner spinnerEventType = dialogView.findViewById(R.id.spinner_event_type);
        TextInputEditText editTextNotes = dialogView.findViewById(R.id.edit_text_notes);

        // Populate Spinner using the cached availableEventTypes
        // This list is updated in onResume and initially in onCreateView's background thread
        List<String> eventTypeNames = new ArrayList<>();
        if (availableEventTypes != null) {
            for (EventType type : availableEventTypes) {
                eventTypeNames.add(type.getEventName());
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, eventTypeNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEventType.setAdapter(spinnerAdapter);

        if (eventTypeNames.isEmpty()) {
            Toast.makeText(getContext(), "No event types defined. Please add some in Settings.", Toast.LENGTH_LONG).show();
            // Optionally, you could disable the "Add Entry" button or even prevent dialog from showing fully.
        }

        builder.setPositiveButton("Add Entry", null); // Set later for validation if needed
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int selectedPosition = spinnerEventType.getSelectedItemPosition();
                String notes = editTextNotes.getText().toString().trim();

                if (availableEventTypes == null || availableEventTypes.isEmpty() || selectedPosition == Spinner.INVALID_POSITION) {
                    if (availableEventTypes == null || availableEventTypes.isEmpty()) {
                        Toast.makeText(getContext(), "No event types available. Add them in Settings first.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Please select an event type.", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                EventType selectedEventType = availableEventTypes.get(selectedPosition);

                LogEntry newLogEntry = new LogEntry();
                newLogEntry.setTimestamp(new Date().getTime());
                newLogEntry.setEventTypeId(selectedEventType.getEventTypeId());
                newLogEntry.setEvent(selectedEventType.getEventName());
                newLogEntry.setNotes(notes);

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    logEntryDao.insert(newLogEntry);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::loadAndDisplayLogEntries);
                    }
                });
                dialog.dismiss();
                Toast.makeText(getContext(), "Log entry added.", Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
    }
}
