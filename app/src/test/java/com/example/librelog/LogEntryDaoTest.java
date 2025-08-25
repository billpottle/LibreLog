package com.example.librelog;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

@RunWith(AndroidJUnit4.class) // Needed for ApplicationProvider.getApplicationContext()
public class LogEntryDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private LogEntryDao logEntryDao;
    private EventTypeDao eventTypeDao;

    @Before
    public void createDb() {
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
                        AppDatabase.class)
                // Allowing main thread queries, just for testing.
                .allowMainThreadQueries()
                .build();
        logEntryDao = db.logEntryDao();
        eventTypeDao = db.eventTypeDao();

        // Setup some event types
        EventType type1 = new EventType("Test Event Type 1");
        type1.setEventTypeId(1); // Assuming setEventTypeId takes int
        eventTypeDao.insert(type1);

        EventType type2 = new EventType("Test Event Type 2");
        type2.setEventTypeId(2);
        eventTypeDao.insert(type2);
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertAndGetLogEntry() throws Exception {
        LogEntry logEntry = new LogEntry();
        logEntry.setEvent("Test Event");
        logEntry.setTimestamp(new Date().getTime());
        logEntry.setEventTypeId(1); // Use long if EventTypeId is long
        logEntryDao.insert(logEntry);

        List<LogEntry> allEntries = logEntryDao.getRecentLogEntries(1L, 10, 0); // Assuming ID 1 is Long
        assertEquals(allEntries.get(0).getEvent(), "Test Event");
    }

    @Test
    public void getLogEntriesByEventTypeId() throws Exception {
        LogEntry logEntry1 = new LogEntry();
        logEntry1.setEvent("Event Type 1 Entry 1");
        logEntry1.setTimestamp(new Date().getTime());
        logEntry1.setEventTypeId(1);
        logEntryDao.insert(logEntry1);

        Thread.sleep(10); // Ensure different timestamps

        LogEntry logEntry2 = new LogEntry();
        logEntry2.setEvent("Event Type 2 Entry 1");
        logEntry2.setTimestamp(new Date().getTime());
        logEntry2.setEventTypeId(2);
        logEntryDao.insert(logEntry2);

        Thread.sleep(10);

        LogEntry logEntry3 = new LogEntry();
        logEntry3.setEvent("Event Type 1 Entry 2");
        logEntry3.setTimestamp(new Date().getTime());
        logEntry3.setEventTypeId(1);
        logEntryDao.insert(logEntry3);

        List<LogEntry> type1Entries = logEntryDao.getRecentLogEntries(1L, 10, 0);
        assertEquals(2, type1Entries.size());
        assertEquals("Event Type 1 Entry 2", type1Entries.get(0).getEvent()); // Most recent first

        List<LogEntry> type2Entries = logEntryDao.getRecentLogEntries(2L, 10, 0);
        assertEquals(1, type2Entries.size());
        assertEquals("Event Type 2 Entry 1", type2Entries.get(0).getEvent());
    }

    @Test
    public void countLogEntriesByEventTypeId() throws Exception {
        LogEntry logEntry1 = new LogEntry();
        logEntry1.setEventTypeId(1);
        logEntryDao.insert(logEntry1);

        LogEntry logEntry2 = new LogEntry();
        logEntry2.setEventTypeId(2);
        logEntryDao.insert(logEntry2);

        LogEntry logEntry3 = new LogEntry();
        logEntry3.setEventTypeId(1);
        logEntryDao.insert(logEntry3);

        long countType1 = logEntryDao.getCountLogEntries(1L);
        assertEquals(2, countType1);

        long countType2 = logEntryDao.getCountLogEntries(2L);
        assertEquals(1, countType2);

        long countType3 = logEntryDao.getCountLogEntries(3L); // Non-existent type
        assertEquals(0, countType3);
    }

     @Test
    public void getLogEntriesWithPagination() throws Exception {
        // Insert 7 entries for event type 1
        for (int i = 0; i < 7; i++) {
            LogEntry entry = new LogEntry();
            entry.setEvent("Event " + i);
            entry.setTimestamp(new Date().getTime() + i * 100); // ensure order
            entry.setEventTypeId(1);
            logEntryDao.insert(entry);
        }

        // Page 1, 5 items
        List<LogEntry> page1 = logEntryDao.getRecentLogEntries(1L, 5, 0);
        assertEquals(5, page1.size());
        assertEquals("Event 6", page1.get(0).getEvent()); // Most recent is "Event 6"

        // Page 2, 5 items (should get remaining 2)
        List<LogEntry> page2 = logEntryDao.getRecentLogEntries(1L, 5, 5); // offset = 5
        assertEquals(2, page2.size());
        assertEquals("Event 1", page2.get(0).getEvent()); // Event 1 is the 6th entry in reverse chronological
    }
}
