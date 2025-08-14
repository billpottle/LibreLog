package com.example.librelog;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDatabase(getApplicationContext());

        Button recordButton = findViewById(R.id.button_record_event);
        recordButton.setOnClickListener(view -> {
            LogEntry logEntry = new LogEntry();
            logEntry.event = "default";
            logEntry.timestamp = new Date().getTime();
            logEntry.notes = "";

            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.logEntryDao().insert(logEntry);
                // You can add a Toast or Log message here to confirm insertion
            });
        });
    }
}
