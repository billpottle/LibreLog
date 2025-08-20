package com.example.librelog;

import androidx.annotation.NonNull; // Import NonNull
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "event_types",
        indices = {@Index(value = "event_name", unique = true)})
public class EventType {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "event_type_id")
    private int eventTypeId;

    @NonNull // Added NonNull
    @ColumnInfo(name = "event_name")
    private String eventName;

    // Constructors
    public EventType(@NonNull String eventName) { // Added NonNull
        this.eventName = eventName;
    }

    // Getters
    public int getEventTypeId() {
        return eventTypeId;
    }

    @NonNull
    public String getEventName() {
        return eventName;
    }

    // Setters
    public void setEventTypeId(int eventTypeId) {
        this.eventTypeId = eventTypeId;
    }

    public void setEventName(@NonNull String eventName) { // Added NonNull
        this.eventName = eventName;
    }
}
