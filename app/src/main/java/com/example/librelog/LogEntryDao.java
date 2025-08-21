package com.example.librelog;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LogEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LogEntry logEntry);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(LogEntry... logEntries); // Keep this if you use it

    @Update
    void update(LogEntry logEntry); // Keep this if you use it

    @Delete
    void delete(LogEntry logEntry); // Keep this if you use it

    @Query("DELETE FROM log_entries")
    void deleteAll(); // Keep this if you use it

    // **** MODIFIED METHOD ****
    @Query("SELECT * FROM log_entries WHERE event_type_id = :eventTypeId ORDER BY timestamp DESC")
    List<LogEntry> getAllLogEntries(long eventTypeId);

    // **** MODIFIED METHOD ****
    // If you want a version to get ALL entries regardless of type, you might need a separate method
    // or a more complex query if an "all types" ID is passed.
    // For now, this strictly filters by eventTypeId.
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    List<LogEntry> getAllLogEntriesNoFilter();


    // **** MODIFIED METHOD ****
    @Query("SELECT * FROM log_entries WHERE event_type_id = :eventTypeId ORDER BY timestamp DESC LIMIT :limit")
    List<LogEntry> getRecentLogEntries(long eventTypeId, int limit);

    // Keep your chart-related queries if they are in use
    // **** MODIFIED METHOD ****
    @Query("SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch', 'localtime')) as hour, COUNT(*) as count FROM log_entries WHERE event_type_id = :eventTypeId GROUP BY hour ORDER BY hour ASC")
    List<EventCountByHour> getEventCountByHour(long eventTypeId);

    // **** MODIFIED METHOD ****
    @Query("SELECT strftime('%d', datetime(timestamp/1000, 'unixepoch', 'localtime')) as day, COUNT(*) as count FROM log_entries WHERE event_type_id = :eventTypeId GROUP BY day ORDER BY day ASC")
    List<EventCountByDay> getEventCountByDay(long eventTypeId);

    // Ensure these inner classes (or separate files) for chart data exist if queries are used
    class EventCountByHour {
        public String hour;
        public int count;
    }

    class EventCountByDay {
        public String day;
        public int count;
    }
}
