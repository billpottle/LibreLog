package com.example.librelog;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.librelog.ui.HomeFragment; // Assuming MainActivity hosts HomeFragment

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomeFragmentTest {

    private AppDatabase db;
    private EventTypeDao eventTypeDao;
    private LogEntryDao logEntryDao;


    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        // It's often better to use a Test Double for the database in UI tests,
        // or ensure the database is in a known state.
        // For simplicity, we'll quickly add some data here.
        // Note: This interacts with the *actual* database of the app during the test.
        // Consider using a testing-specific database or clearing data.
        db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
                        AppDatabase.class, "libre-log-db")
                .allowMainThreadQueries() // Only for testing
                .build();
        eventTypeDao = db.eventTypeDao();
        logEntryDao = db.logEntryDao();

        // Clear previous data
        logEntryDao.deleteAllLogEntries(); // You'd need to add this to your DAO
        eventTypeDao.deleteAllEventTypes(); // You'd need to add this to your DAO


        EventType type1 = new EventType();
        type1.setEventTypeId(1);
        type1.setEventName("UI Test Event 1");
        eventTypeDao.insert(type1);

        EventType type2 = new EventType();
        type2.setEventTypeId(2);
        type2.setEventName("UI Test Event 2");
        eventTypeDao.insert(type2);


        LogEntry entry1 = new LogEntry();
        entry1.setEvent("Entry for UI Test 1");
        entry1.setEventTypeId(1);
        entry1.setTimestamp(System.currentTimeMillis());
        logEntryDao.insert(entry1);

        LogEntry entry2 = new LogEntry();
        entry2.setEvent("Entry for UI Test 2");
        entry2.setEventTypeId(2);
        entry2.setTimestamp(System.currentTimeMillis() - 10000); // older
        logEntryDao.insert(entry2);
    }

    @After
    public void tearDown() {
        // Clean up database if necessary, or close
         if (db != null && db.isOpen()) {
            db.close();
        }
    }

    @Test
    public void testDropdownSelection_UpdatesRecyclerView() throws InterruptedException {
        // Wait for data to load (important for UI tests)
        Thread.sleep(1000); // Simple wait, consider IdlingResource for robust tests

        // Check if "Entry for UI Test 1" is initially visible (assuming default is type 1)
        Espresso.onView(ViewMatchers.withId(R.id.recycler_view_log_entries))
                .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText("Entry for UI Test 1"))));

        // Open the dropdown
        Espresso.onView(ViewMatchers.withId(R.id.dropdown_event_types_autocomplete)).perform(ViewActions.click());

        // Select "UI Test Event 2" from the dropdown
        // Espresso.onData(is(instanceOf(String.class))).atPosition(1).perform(ViewActions.click()); // If simple ArrayAdapter
        Espresso.onData(allOf(is(instanceOf(String.class)), is("UI Test Event 2"))).perform(ViewActions.click());


        // Wait for UI to update
        Thread.sleep(1000);

        // Check if "Entry for UI Test 2" is now visible
        Espresso.onView(ViewMatchers.withId(R.id.recycler_view_log_entries))
                .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText("Entry for UI Test 2"))));

        // Check that "Entry for UI Test 1" is NOT visible (or not present if list clears)
        Espresso.onView(ViewMatchers.withId(R.id.recycler_view_log_entries))
                .check(ViewAssertions.doesNotExist(ViewMatchers.hasDescendant(ViewMatchers.withText("Entry for UI Test 1"))));
    }

     @Test
    public void testPagination_NextAndPrevious() throws InterruptedException {
        // Add 7 entries for Event Type "UI Test Event 1" to test pagination
        for(int i = 0; i < 7; i++) {
            LogEntry entry = new LogEntry();
            entry.setEvent("PageTest Entry " + i);
            entry.setEventTypeId(1);
            entry.setTimestamp(System.currentTimeMillis() + i * 100); // ensure different timestamps
            logEntryDao.insert(entry);
        }
        // Ensure that the "UI Test Event 1" is selected
        Espresso.onView(ViewMatchers.withId(R.id.dropdown_event_types_autocomplete)).perform(ViewActions.click());
        Espresso.onData(allOf(is(instanceOf(String.class)), is("UI Test Event 1"))).perform(ViewActions.click());
        Thread.sleep(1000); // Wait for data to load for the selected type

        // Initial state: Page 1 of 2 (7 items, 5 per page for UI Test Event 1. +1 original = 8)
        // (Total 8 entries of Type 1, so Page 1 of 2, if ITEMS_PER_PAGE = 5)
        Espresso.onView(ViewMatchers.withId(R.id.text_view_page_info))
                .check(ViewAssertions.matches(ViewMatchers.withText("Page 1 of 2"))); // Adjust expected total pages

        // Click Next
        Espresso.onView(ViewMatchers.withId(R.id.button_next_page)).perform(ViewActions.click());
        Thread.sleep(500); // Wait for page load
        Espresso.onView(ViewMatchers.withId(R.id.text_view_page_info))
                .check(ViewAssertions.matches(ViewMatchers.withText("Page 2 of 2")));

        // Verify "PageTest Entry 0" is visible (one of the last items on page 2)
         Espresso.onView(ViewMatchers.withId(R.id.recycler_view_log_entries))
                .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText("PageTest Entry 0"))));


        // Click Previous
        Espresso.onView(ViewMatchers.withId(R.id.button_previous_page)).perform(ViewActions.click());
        Thread.sleep(500);
        Espresso.onView(ViewMatchers.withId(R.id.text_view_page_info))
                .check(ViewAssertions.matches(ViewMatchers.withText("Page 1 of 2")));

        // Verify "PageTest Entry 6" (most recent) is visible on page 1
         Espresso.onView(ViewMatchers.withId(R.id.recycler_view_log_entries))
                .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText("PageTest Entry 6"))));
    }
}
