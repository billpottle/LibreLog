package com.example.librelog;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy; // Added this import
import androidx.room.Query;

import java.util.List;

@Dao
public interface LogEntryDao {
    @Insert
    void insert(LogEntry logEntry);

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Added this method
    void insertAll(LogEntry... logEntries);

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    List<LogEntry> getAllLogEntries();

    @Query("DELETE FROM log_entries") // This is the missing method
    void deleteAllEntries();
}
