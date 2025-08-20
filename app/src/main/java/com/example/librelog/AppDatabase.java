package com.example.librelog;

import android.content.Context;
import android.database.Cursor; // Needed for callback
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {LogEntry.class, EventType.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract LogEntryDao logEntryDao();
    public abstract EventTypeDao eventTypeDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. Create the event_types table
            database.execSQL("CREATE TABLE IF NOT EXISTS `event_types` (" +
                    "`event_type_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`event_name` TEXT NOT NULL)"); // event_name is NOT NULL
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_event_types_event_name` ON `event_types` (`event_name`)");

            // 2. Recreate log_entries table with the new schema including the foreign key
            database.execSQL("CREATE TABLE `log_entries_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`event` TEXT, " +
                    "`notes` TEXT, " +
                    "`event_type_id` INTEGER, " +
                    "FOREIGN KEY(`event_type_id`) REFERENCES `event_types`(`event_type_id`) ON UPDATE CASCADE ON DELETE SET NULL)");

            // Copy data from the old log_entries table to the new one
            database.execSQL("INSERT INTO `log_entries_new` (id, timestamp, event, notes, event_type_id) " +
                    "SELECT id, timestamp, event, notes, NULL FROM `log_entries`"); // event_type_id is NULL for old entries

            // Drop the old log_entries table
            database.execSQL("DROP TABLE `log_entries`");

            // Rename the new table to the original name
            database.execSQL("ALTER TABLE `log_entries_new` RENAME TO `log_entries`");

            // Re-create index for event_type_id (as defined in LogEntry entity)
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_log_entries_event_type_id` ON `log_entries` (`event_type_id`)");
        }
    };

    private static RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            databaseWriteExecutor.execute(() -> {
                // Check if default event types exist and add if not
                // CORRECTED LINE: Pass new Object[]{} instead of null for bindArgs
                Cursor cursor = db.query("SELECT COUNT(*) FROM event_types WHERE event_name = 'Default Event'", new Object[]{});

                if (cursor != null) { // Always good to check if cursor is null before using it
                    try {
                        if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                            // Default Event does not exist, so insert it
                            db.execSQL("INSERT INTO event_types (event_name) VALUES ('Default Event')");
                            // You can add more default event types here if needed
                            // db.execSQL("INSERT INTO event_types (event_name) VALUES ('Another Default')");
                        }
                    } finally {
                        cursor.close(); // Ensure cursor is always closed
                    }
                }
            });
        }
    };


    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "log_database")
                            .addMigrations(MIGRATION_1_2)
                            .addCallback(roomDatabaseCallback) // Add the callback
                            // For development, if migration issues persist and you're OK with data loss:
                            // .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
