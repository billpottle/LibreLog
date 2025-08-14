package com.example.librelog.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.librelog.AppDatabase;
import com.example.librelog.LogEntry;
import com.example.librelog.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private AppDatabase db;
    private BarChart barChartHourlyEvents;
    private BarChart barChartMonthlyEvents; // Added for monthly chart

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = AppDatabase.getDatabase(requireContext().getApplicationContext());

        Button recordButton = view.findViewById(R.id.button_record_event);
        recordButton.setOnClickListener(v -> {
            LogEntry logEntry = new LogEntry();
            logEntry.setEvent("default");
            logEntry.setTimestamp(new Date().getTime());
            logEntry.setNotes("");

            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.logEntryDao().insert(logEntry);
                // Refresh chart data after inserting new entry
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setupHourlyEventsChart();
                        setupMonthlyEventsChart(); // Refresh monthly chart too
                    });
                }
            });
        });

        barChartHourlyEvents = view.findViewById(R.id.bar_chart_hourly_events);
        setupHourlyEventsChart();

        barChartMonthlyEvents = view.findViewById(R.id.bar_chart_monthly_events); // Initialize monthly chart
        setupMonthlyEventsChart(); // Setup monthly chart

        return view;
    }

    private void setupHourlyEventsChart() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LogEntry> entries = db.logEntryDao().getAllLogEntries();
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (entries == null || entries.isEmpty()) {
                    barChartHourlyEvents.clear();
                    barChartHourlyEvents.invalidate();
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
                        return String.valueOf((int) value);
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
                leftAxis.setDrawGridLines(false);

                barChartHourlyEvents.getAxisRight().setEnabled(false);
                barChartHourlyEvents.animateY(1000);
                barChartHourlyEvents.invalidate();
            });
        });
    }

    private void setupMonthlyEventsChart() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LogEntry> entries = db.logEntryDao().getAllLogEntries();
            if (getActivity() == null) return;

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
                dataSet.setColors(ColorTemplate.PASTEL_COLORS); // Using different colors
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
}
