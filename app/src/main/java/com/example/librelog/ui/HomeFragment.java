package com.example.librelog.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.librelog.AppDatabase;
import com.example.librelog.LogEntry;
import com.example.librelog.R;
import java.util.Date;

public class HomeFragment extends Fragment {

    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = AppDatabase.getDatabase(requireContext().getApplicationContext());

        Button recordButton = view.findViewById(R.id.button_record_event);
        recordButton.setOnClickListener(v -> {
            LogEntry logEntry = new LogEntry();
            logEntry.setEvent("default");
            logEntry.setTimestamp(new Date().getTime()); // Changed to getTime()
            logEntry.setNotes("");

            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.logEntryDao().insert(logEntry);
                // Optionally show a toast or update UI on the main thread
                // requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Event recorded", Toast.LENGTH_SHORT).show());
            });
        });

        return view;
    }
}
