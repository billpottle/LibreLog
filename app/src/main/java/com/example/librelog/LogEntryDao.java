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
    void insertAll(LogEntry... logEntries);

    @Update
    void update(LogEntry logEntry);

    @Delete
    void delete(LogEntry logEntry);

    @Query("DELETE FROM log_entries")
    void deleteAll();

    @Query("SELECT * FROM log_entries WHERE event_type_id = :eventTypeId ORDER BY timestamp DESC")
    List<LogEntry> getAllLogEntries(long eventTypeId);

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    List<LogEntry> getAllLogEntriesNoFilter();

    // **** MODIFIED METHOD for PAGINATION ****
    @Query("SELECT * FROM log_entries WHERE event_type_id = :eventTypeId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    List<LogEntry> getRecentLogEntries(long eventTypeId, int limit, int offset);

    // **** NEW METHOD for PAGINATION ****
    @Query("SELECT COUNT(*) FROM log_entries WHERE event_type_id = :eventTypeId")
    long getCountLogEntries(long eventTypeId);

    @Query("SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch', 'localtime')) as hour, COUNT(*) as count FROM log_entries WHERE event_type_id = :eventTypeId GROUP BY hour ORDER BY hour ASC")
    List<EventCountByHour> getEventCountByHour(long eventTypeId);

    @Query("SELECT strftime('%d', datetime(timestamp/1000, 'unixepoch', 'localtime')) as day, COUNT(*) as count FROM log_entries WHERE event_type_id = :eventTypeId AND strftime('%Y-%m', datetime(timestamp/1000, 'unixepoch', 'localtime')) = strftime('%Y-%m', 'now', 'localtime') GROUP BY day ORDER BY day ASC")
    List<EventCountByDay> getEventCountByDay(long eventTypeId);

    @Query("SELECT strftime('%m', datetime(timestamp/1000, 'unixepoch', 'localtime')) as month, COUNT(*) as count FROM log_entries WHERE event_type_id = :eventTypeId AND datetime(timestamp/1000, 'unixepoch', 'localtime') >= datetime('now', 'start of month', '-11 months', 'localtime') GROUP BY month ORDER BY month ASC")
    List<EventCountByMonth> getEventCountByMonthLast12(long eventTypeId);

    class EventCountByHour {
        public String hour;
        public int count;
    }

    class EventCountByDay {
        public String day;
        public int count;
    }

    class EventCountByMonth {
        public String month;
        public int count;
    }
}
