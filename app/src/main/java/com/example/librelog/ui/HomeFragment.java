package com.example.librelog.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private LogEntryDao logEntryDao;
    private EventTypeDao eventTypeDao;
    private LogEntryAdapter logEntryAdapter;
    private RecyclerView recyclerViewLogEntries;
    private TextView textViewNoEntries;
    private FloatingActionButton fabAddLogEntry;
    private AppDatabase db;

    private BarChart barChartHourlyEvents;
    private BarChart barChartDailyEvents; // Renamed from barChartMonthlyEvents for clarity with DAO
    private AutoCompleteTextView dropdownEventTypesAutocomplete;

    private List<EventType> availableEventTypes = new ArrayList<>();
    private Map<String, Long> eventTypeNameToIdMap = new HashMap<>();
    private long selectedEventTypeId = 1L; // Default to Event Type ID 1

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
        barChartDailyEvents = view.findViewById(R.id.bar_chart_monthly_events); // Assuming this is the correct ID from your XML
        dropdownEventTypesAutocomplete = view.findViewById(R.id.dropdown_event_types_autocomplete);

        setupRecyclerView();
        fabAddLogEntry.setOnClickListener(v -> showAddLogEntryDialog());
        
        fetchEventTypesAndSetupDropdown(); // This will also trigger initial data load

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Data is now loaded/refreshed via fetchEventTypesAndSetupDropdown or dropdown selection
        // If event types haven't changed, but data might have, you could add a direct refresh here:
        // refreshAllEventData(); 
        // However, fetchEventTypesAndSetupDropdown in onResume already triggers refreshAllEventData
        // if event types are loaded.
    }

    private void fetchEventTypesAndSetupDropdown() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            availableEventTypes = eventTypeDao.getAllEventTypes();
            eventTypeNameToIdMap.clear();
            if (availableEventTypes != null) {
                for (EventType type : availableEventTypes) {
                    eventTypeNameToIdMap.put(type.getEventName(), (long) type.getEventTypeId());
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    setupEventTypeDropdown();
                    refreshAllEventData(); // Load data with default or current selectedEventTypeId
                });
            }
        });
    }

    private void setupEventTypeDropdown() {
        if (getContext() == null || dropdownEventTypesAutocomplete == null || availableEventTypes == null) {
            return;
        }

        List<String> eventTypeNames = availableEventTypes.stream()
                .map(EventType::getEventName)
                .collect(Collectors.toList());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, eventTypeNames);
        dropdownEventTypesAutocomplete.setAdapter(adapter);

        // Set initial text for the dropdown based on selectedEventTypeId
        String initialDropdownText = "";
        Optional<EventType> defaultEventType = availableEventTypes.stream()
                .filter(et -> et.getEventTypeId() == selectedEventTypeId)
                .findFirst();

        if (defaultEventType.isPresent()) {
            initialDropdownText = defaultEventType.get().getEventName();
        } else if (!eventTypeNames.isEmpty()) {
            // Fallback to the first available if ID 1 doesn't exist or isn't in the list
            // and update selectedEventTypeId accordingly
            initialDropdownText = eventTypeNames.get(0);
            selectedEventTypeId = eventTypeNameToIdMap.getOrDefault(initialDropdownText, -1L);
        }
        
        dropdownEventTypesAutocomplete.setText(initialDropdownText, false);


        if (eventTypeNames.isEmpty()) {
            dropdownEventTypesAutocomplete.setHint("No event types defined");
        } else {
             dropdownEventTypesAutocomplete.setHint("Select Event Type");
        }

        dropdownEventTypesAutocomplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            long newSelectedId = eventTypeNameToIdMap.getOrDefault(selectedName, -1L);

            if (newSelectedId != -1L && newSelectedId != selectedEventTypeId) {
                selectedEventTypeId = newSelectedId;
                refreshAllEventData();
            } else if (newSelectedId == -1L) {
                Toast.makeText(getContext(), "Could not find ID for selected event type.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void refreshAllEventData() {
        if (selectedEventTypeId == -1L && !eventTypeNameToIdMap.isEmpty()) {
             // If selected ID is somehow invalid but we have types, try to pick the first one.
             Optional<String> firstName = eventTypeNameToIdMap.keySet().stream().findFirst();
             if(firstName.isPresent()){
                 selectedEventTypeId = eventTypeNameToIdMap.get(firstName.get());
             } else {
                 // No event types available to select
                 if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        clearAllUIData();
                        Toast.makeText(getContext(), "No event types available to filter by.", Toast.LENGTH_LONG).show();
                    });
                }
                return;
             }
        } else if (eventTypeNameToIdMap.isEmpty()) {
            // No event types at all
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::clearAllUIData);
            }
            return;
        }


        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Ensure DAO methods are called with the currently selectedEventTypeId
            List<LogEntry> recentLogEntries = logEntryDao.getRecentLogEntries(selectedEventTypeId, 5);
            List<LogEntryDao.EventCountByHour> hourlyData = logEntryDao.getEventCountByHour(selectedEventTypeId);
            List<LogEntryDao.EventCountByDay> dailyData = logEntryDao.getEventCountByDay(selectedEventTypeId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updateRecentEventsUI(recentLogEntries);
                    updateHourlyEventsChart(hourlyData);
                    updateDailyEventsChart(dailyData); // Renamed from setupMonthlyEventsChart
                });
            }
        });
    }
    
    private void clearAllUIData() {
        if (logEntryAdapter != null) logEntryAdapter.setLogEntries(new ArrayList<>());
        if (textViewNoEntries != null) {
            textViewNoEntries.setText("No data to display. Select an event type.");
            textViewNoEntries.setVisibility(View.VISIBLE);
        }
        if (recyclerViewLogEntries != null) recyclerViewLogEntries.setVisibility(View.GONE);
        if (barChartHourlyEvents != null) {
            barChartHourlyEvents.clear();
            barChartHourlyEvents.invalidate();
        }
        if (barChartDailyEvents != null) {
            barChartDailyEvents.clear();
            barChartDailyEvents.invalidate();
        }
    }


    private void setupRecyclerView() {
        if (recyclerViewLogEntries == null) return;
        recyclerViewLogEntries.setLayoutManager(new LinearLayoutManager(getContext()));
        logEntryAdapter = new LogEntryAdapter(new ArrayList<>());
        recyclerViewLogEntries.setAdapter(logEntryAdapter);
    }

    // Updated to accept data as a parameter
    private void updateRecentEventsUI(List<LogEntry> logEntries) {
        if (logEntryAdapter == null) return;

        if (logEntries != null && !logEntries.isEmpty()) {
            logEntryAdapter.setLogEntries(logEntries);
            if (recyclerViewLogEntries != null) recyclerViewLogEntries.setVisibility(View.VISIBLE);
            if (textViewNoEntries != null) textViewNoEntries.setVisibility(View.GONE);
        } else {
            logEntryAdapter.setLogEntries(new ArrayList<>());
            if (recyclerViewLogEntries != null) recyclerViewLogEntries.setVisibility(View.GONE);
            if (textViewNoEntries != null) {
                textViewNoEntries.setText("No recent log entries for this event type.");
                textViewNoEntries.setVisibility(View.VISIBLE);
            }
        }
    }

    // Updated to accept data as a parameter
    private void updateHourlyEventsChart(List<LogEntryDao.EventCountByHour> hourlyData) {
        if (barChartHourlyEvents == null) return;

        if (hourlyData == null || hourlyData.isEmpty()) {
            barChartHourlyEvents.clear();
            barChartHourlyEvents.setNoDataText("No hourly data for this event type.");
            barChartHourlyEvents.invalidate();
            return;
        }

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        // Assuming EventCountByHour has 'hour' (String "00"-"23") and 'count' (int)
        for (int i = 0; i < 24; i++) { // Ensure all hours are represented, even if count is 0
            String hourString = String.format(Locale.US, "%02d", i);
            int count = 0;
            for(LogEntryDao.EventCountByHour item : hourlyData){
                if(item.hour.equals(hourString)){
                    count = item.count;
                    break;
                }
            }
            barEntries.add(new BarEntry(i, count));
        }


        BarDataSet dataSet = new BarDataSet(barEntries, "Events per Hour");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value == 0 ? "" : String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChartHourlyEvents.getDescription().setEnabled(false);
        barChartHourlyEvents.setDrawGridBackground(false);
        barChartHourlyEvents.setFitBars(true);
        barChartHourlyEvents.setData(barData);
        barChartHourlyEvents.setDrawBorders(false);
        barChartHourlyEvents.getLegend().setEnabled(false);

        XAxis xAxis = barChartHourlyEvents.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(24, false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.US, "%02d", (int)value);
            }
        });
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChartHourlyEvents.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        leftAxis.setDrawGridLines(true); // Keep grid lines for Y-axis for better readability

        barChartHourlyEvents.getAxisRight().setEnabled(false);
        barChartHourlyEvents.animateY(1000);
        barChartHourlyEvents.invalidate();
    }

    // Renamed from setupMonthlyEventsChart and updated to accept data
    private void updateDailyEventsChart(List<LogEntryDao.EventCountByDay> dailyData) {
         if (barChartDailyEvents == null) return;

        if (dailyData == null || dailyData.isEmpty()) {
            barChartDailyEvents.clear();
            barChartDailyEvents.setNoDataText("No daily data for this event type.");
            barChartDailyEvents.invalidate();
            return;
        }

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        // Assuming EventCountByDay has 'day' (String "01"-"31") and 'count' (int)
        // For simplicity, let's assume we want to show up to 31 days.
        // A more robust solution might use the actual days from the data or current month.
        Map<Integer, Integer> dayCountsMap = new HashMap<>();
        for (LogEntryDao.EventCountByDay item : dailyData) {
            try {
                dayCountsMap.put(Integer.parseInt(item.day), item.count);
            } catch (NumberFormatException e) {
                // Skip malformed day string
            }
        }
        
        int maxDay = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH); // Days in current month

        for (int i = 1; i <= maxDay; i++) {
            barEntries.add(new BarEntry(i, dayCountsMap.getOrDefault(i, 0)));
        }


        BarDataSet dataSet = new BarDataSet(barEntries, "Events per Day");
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value == 0 ? "" : String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChartDailyEvents.getDescription().setEnabled(false);
        barChartDailyEvents.setDrawGridBackground(false);
        barChartDailyEvents.setFitBars(true);
        barChartDailyEvents.setData(barData);
        barChartDailyEvents.setDrawBorders(false);
        barChartDailyEvents.getLegend().setEnabled(false);

        XAxis xAxis = barChartDailyEvents.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        //xAxis.setLabelCount(maxDay, false); // Adjust label count if needed
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // Day number
            }
        });
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChartDailyEvents.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        leftAxis.setDrawGridLines(true);

        barChartDailyEvents.getAxisRight().setEnabled(false);
        barChartDailyEvents.animateY(1000);
        barChartDailyEvents.invalidate();
    }

    private void showAddLogEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_log_entry, null);
        builder.setView(dialogView);

        Spinner spinnerEventType = dialogView.findViewById(R.id.spinner_event_type);
        TextInputEditText editTextNotes = dialogView.findViewById(R.id.edit_text_notes);

        List<String> eventTypeNamesForDialog = new ArrayList<>();
        if (availableEventTypes != null) {
            for (EventType type : availableEventTypes) {
                eventTypeNamesForDialog.add(type.getEventName());
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, eventTypeNamesForDialog);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEventType.setAdapter(spinnerAdapter);

        if (eventTypeNamesForDialog.isEmpty()) {
            Toast.makeText(getContext(), "No event types defined. Please add some in Settings.", Toast.LENGTH_LONG).show();
        }

        builder.setPositiveButton("Add Entry", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int selectedPosition = spinnerEventType.getSelectedItemPosition();
                String notes = editTextNotes.getText() != null ? editTextNotes.getText().toString().trim() : "";

                if (availableEventTypes == null || availableEventTypes.isEmpty() || selectedPosition == Spinner.INVALID_POSITION) {
                    Toast.makeText(getContext(), "Please select an event type or add one in Settings.", Toast.LENGTH_LONG).show();
                    return;
                }

                EventType selectedEventTypeForDialog = availableEventTypes.get(selectedPosition);

                LogEntry newLogEntry = new LogEntry();
                newLogEntry.setTimestamp(new Date().getTime());
                newLogEntry.setEventTypeId(selectedEventTypeForDialog.getEventTypeId()); // This should be fine if EventType.eventTypeId is int
                newLogEntry.setEvent(selectedEventTypeForDialog.getEventName());
                newLogEntry.setNotes(notes);

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    logEntryDao.insert(newLogEntry);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // If the new entry's type matches the currently selected filter, refresh
                            if (selectedEventTypeForDialog.getEventTypeId() == selectedEventTypeId) { // Direct comparison of int and long is fine here
                                refreshAllEventData();
                            }
                            Toast.makeText(getContext(), "Log entry added.", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
                dialog.dismiss();
            });
        });
        dialog.show();
    }
}
