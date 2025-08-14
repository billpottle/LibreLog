package com.example.librelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "log_entries")
public class LogEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String event;
    public long timestamp;
    public String notes;
}
