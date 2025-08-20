package com.example.librelog;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;

@Entity(tableName = "log_entries",
        foreignKeys = @ForeignKey(entity = EventType.class,
                parentColumns = "event_type_id",
                childColumns = "event_type_id",
                onDelete = ForeignKey.SET_NULL, // When an EventType is deleted, set event_type_id in LogEntry to NULL
                onUpdate = ForeignKey.CASCADE), // If an EventType's ID changes, update it here too
        indices = {@Index(value = "event_type_id")})
public class LogEntry {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private long timestamp;
    private String event; // This will store the event name text
    private String notes;

    @ColumnInfo(name = "event_type_id") // Allows null by default for Integer
    private Integer eventTypeId; // Use Integer to allow nulls, links to EventType table

    // Getters and setters for all fields
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public String getEvent() {
        return event;
    }
    public void setEvent(String event) {
        this.event = event;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getEventTypeId() {
        return eventTypeId;
    }

    public void setEventTypeId(Integer eventTypeId) {
        this.eventTypeId = eventTypeId;
    }
}
