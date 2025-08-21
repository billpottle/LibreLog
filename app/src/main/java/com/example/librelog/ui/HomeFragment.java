package com.example.librelog.ui;

import android.graphics.Color;
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
import java.util.List;
import java.util.Locale;

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
        setupHourlyEventsChart();
        setupMonthlyEventsChart();

        fabAddLogEntry.setOnClickListener(v -> showAddLogEntryDialog());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            availableEventTypes = eventTypeDao.getAllEventTypes();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndDisplayLogEntries();
        setupHourlyEventsChart();
        setupMonthlyEventsChart();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            availableEventTypes = eventTypeDao.getAllEventTypes();
        });
    }

    private void setupRecyclerView() {
        if (recyclerViewLogEntries == null) return;
        recyclerViewLogEntries.setLayoutManager(new LinearLayoutManager(getContext()));
        logEntryAdapter = new LogEntryAdapter(new ArrayList<>());
        recyclerViewLogEntries.setAdapter(logEntryAdapter);
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
            List<LogEntry> logEntries = logEntryDao.getRecentLogEntries(5); // Still fetches 5 for the list

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (logEntries != null && !logEntries.isEmpty()) {
                        logEntryAdapter.setLogEntries(logEntries);
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

    private void setupHourlyEventsChart() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LogEntry> entries = db.logEntryDao().getAllLogEntries(); // Charts use all entries
            if (getActivity() == null || barChartHourlyEvents == null) return;

            getActivity().runOnUiThread(() -> {
                if (entries == null || entries.isEmpty()) {
                    barChartHourlyEvents.clear();
                    barChartHourlyEvents.invalidate(); // Refresh the chart to show it's empty
                    return;
                }

                int[] hourlyCounts = new int[24]; // For 24 hours
                Calendar calendar = Calendar.getInstance();

                for (LogEntry entry : entries) {
                    calendar.setTimeInMillis(entry.getTimestamp());
                    int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
                    if (hourOfDay >= 0 && hourOfDay < 24) {
                        hourlyCounts[hourOfDay]++;
                    }
                }

                ArrayList<BarEntry> barEntries = new ArrayList<>();
                for (int i = 0; i < 24; i++) {
                    barEntries.add(new BarEntry(i, hourlyCounts[i]));
                }

                BarDataSet dataSet = new BarDataSet(barEntries, "Events per Hour");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                dataSet.setValueTextColor(Color.BLACK); // Or use a theme color
                dataSet.setValueTextSize(10f);
                dataSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (value == 0) {
                            return ""; // Don't show zero values
                        }
                        return String.valueOf((int) value);
                    }
                });

                BarData barData = new BarData(dataSet);
                barData.setBarWidth(0.9f);

                barChartHourlyEvents.getDescription().setEnabled(false);
                barChartHourlyEvents.setDrawGridBackground(false);
                barChartHourlyEvents.setFitBars(true);
                barChartHourlyEvents.setData(barData);
                barChartHourlyEvents.setDrawBorders(false);
                barChartHourlyEvents.getLegend().setEnabled(false); // Hide legend

                XAxis xAxis = barChartHourlyEvents.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setGranularity(1f);
                xAxis.setLabelCount(24, false); // Show all 24 hour labels if possible
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf((int) value); // Display hour number
                    }
                });
                xAxis.setDrawGridLines(false); // No vertical grid lines

                YAxis leftAxis = barChartHourlyEvents.getAxisLeft();
                leftAxis.setAxisMinimum(0f); // Start Y-axis at 0
                leftAxis.setGranularity(1f); // Steps of 1
                leftAxis.setGranularityEnabled(true);
                leftAxis.setValueFormatter(new ValueFormatter() {
                     @Override
                     public String getFormattedValue(float value) {
                        return String.valueOf((int) value);
                    }
                });
                leftAxis.setDrawGridLines(false); // No horizontal grid lines

                barChartHourlyEvents.getAxisRight().setEnabled(false); // No right Y-axis
                barChartHourlyEvents.animateY(1000); // Animation
                barChartHourlyEvents.invalidate(); // Refresh chart
            });
        });
    }

    private void setupMonthlyEventsChart() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LogEntry> entries = db.logEntryDao().getAllLogEntries(); // Charts use all entries
            if (getActivity() == null || barChartMonthlyEvents == null) return;

            getActivity().runOnUiThread(() -> {
                if (entries == null || entries.isEmpty()) {
                    barChartMonthlyEvents.clear();
                    barChartMonthlyEvents.invalidate();
                    return;
                }

                int[] monthlyCounts = new int[12]; // For 12 months
                Calendar calendar = Calendar.getInstance();

                for (LogEntry entry : entries) {
                    calendar.setTimeInMillis(entry.getTimestamp());
                    int month = calendar.get(Calendar.MONTH); // 0 (Jan) to 11 (Dec)
                    if (month >= 0 && month < 12) {
                        monthlyCounts[month]++;
                    }
                }

                ArrayList<BarEntry> barEntries = new ArrayList<>();
                for (int i = 0; i < 12; i++) {
                    barEntries.add(new BarEntry(i, monthlyCounts[i]));
                }

                BarDataSet dataSet = new BarDataSet(barEntries, "Events per Month");
                dataSet.setColors(ColorTemplate.PASTEL_COLORS);
                dataSet.setValueTextColor(Color.BLACK);
                dataSet.setValueTextSize(10f);
                dataSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (value == 0) {
                            return "";
                        }
                        return String.valueOf((int) value);
                    }
                });

                BarData barData = new BarData(dataSet);
                barData.setBarWidth(0.9f);

                barChartMonthlyEvents.getDescription().setEnabled(false);
                barChartMonthlyEvents.setDrawGridBackground(false);
                barChartMonthlyEvents.setFitBars(true);
                barChartMonthlyEvents.setData(barData);
                barChartMonthlyEvents.setDrawBorders(false);
                barChartMonthlyEvents.getLegend().setEnabled(false);

                XAxis xAxis = barChartMonthlyEvents.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setGranularity(1f);
                xAxis.setLabelCount(12, false);
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        // To display month names e.g., Jan, Feb, Mar
                        // Calendar cal = Calendar.getInstance();
                        // cal.set(Calendar.MONTH, (int) value);
                        // return new SimpleDateFormat("MMM", Locale.getDefault()).format(cal.getTime());
                        return String.valueOf((int) value + 1); // Display 1-12 for months
                    }
                });
                xAxis.setDrawGridLines(false);

                YAxis leftAxis = barChartMonthlyEvents.getAxisLeft();
                leftAxis.setAxisMinimum(0f);
                leftAxis.setGranularity(1f);
                leftAxis.setGranularityEnabled(true);
                 leftAxis.setValueFormatter(new ValueFormatter() {
                     @Override
                     public String getFormattedValue(float value) {
                        return String.valueOf((int) value);
                    }
                });
                leftAxis.setDrawGridLines(false);

                barChartMonthlyEvents.getAxisRight().setEnabled(false);
                barChartMonthlyEvents.animateY(1000);
                barChartMonthlyEvents.invalidate();
            });
        });
    }

    private void showAddLogEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_log_entry, null);
        builder.setView(dialogView);

        Spinner spinnerEventType = dialogView.findViewById(R.id.spinner_event_type);
        TextInputEditText editTextNotes = dialogView.findViewById(R.id.edit_text_notes);

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
        }

        builder.setPositiveButton("Add Entry", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int selectedPosition = spinnerEventType.getSelectedItemPosition();
                String notes = editTextNotes.getText() != null ? editTextNotes.getText().toString().trim() : "";

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
                newLogEntry.setEvent(selectedEventType.getEventName()); // Storing the name for direct use if needed
                newLogEntry.setNotes(notes);

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    logEntryDao.insert(newLogEntry);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            loadAndDisplayLogEntries(); // Refresh the list
                            setupHourlyEventsChart();   // Refresh hourly chart
                            setupMonthlyEventsChart();  // Refresh monthly chart
                        });
                    }
                });
                dialog.dismiss();
                Toast.makeText(getContext(), "Log entry added.", Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
    }
}
