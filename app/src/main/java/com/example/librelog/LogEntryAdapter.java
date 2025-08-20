package com.example.librelog; 

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogEntryAdapter extends RecyclerView.Adapter<LogEntryAdapter.LogEntryViewHolder> {

    private List<LogEntry> logEntries;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public LogEntryAdapter(List<LogEntry> logEntries) {
        this.logEntries = logEntries;
    }

    @NonNull
    @Override
    public LogEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_log_entry, parent, false);
        return new LogEntryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LogEntryViewHolder holder, int position) {
        LogEntry currentEntry = logEntries.get(position);
        holder.textViewEvent.setText(currentEntry.getEvent());

        // Format the timestamp
        String formattedDate = dateFormat.format(new Date(currentEntry.getTimestamp()));
        holder.textViewTimestamp.setText(formattedDate);

        if (currentEntry.getNotes() != null && !currentEntry.getNotes().isEmpty()) {
            holder.textViewNotes.setText(currentEntry.getNotes());
            holder.textViewNotes.setVisibility(View.VISIBLE);
        } else {
            holder.textViewNotes.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return logEntries != null ? logEntries.size() : 0;
    }

    public void setLogEntries(List<LogEntry> newLogEntries) {
        this.logEntries = newLogEntries;
        notifyDataSetChanged(); // Notify the adapter to refresh the view
    }

    static class LogEntryViewHolder extends RecyclerView.ViewHolder {
        TextView textViewEvent;
        TextView textViewTimestamp;
        TextView textViewNotes;

        LogEntryViewHolder(View itemView) {
            super(itemView);
            textViewEvent = itemView.findViewById(R.id.text_view_event);
            textViewTimestamp = itemView.findViewById(R.id.text_view_timestamp);
            textViewNotes = itemView.findViewById(R.id.text_view_notes);
        }
    }
}
