package com.example.librelog.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView; // Make sure this is imported
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // If you were using ViewModel, but direct DB access for now
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.librelog.AppDatabase;
import com.example.librelog.EventType;
import com.example.librelog.EventTypeAdapter;
import com.example.librelog.EventTypeDao;
import com.example.librelog.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


import java.util.List;

public class SettingsFragment extends Fragment {

    private EventTypeDao eventTypeDao;
    private EventTypeAdapter adapter;
    private RecyclerView recyclerViewEventTypes;
    private TextView textViewNoEventTypes;
    private AppDatabase db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        eventTypeDao = db.eventTypeDao();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        recyclerViewEventTypes = view.findViewById(R.id.recycler_view_event_types);
        textViewNoEventTypes = view.findViewById(R.id.text_view_no_event_types);
        FloatingActionButton fabAddEventType = view.findViewById(R.id.fab_add_event_type);

        recyclerViewEventTypes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventTypeAdapter();
        recyclerViewEventTypes.setAdapter(adapter);

        loadEventTypes();

        fabAddEventType.setOnClickListener(v -> showAddEditEventTypeDialog(null));

        adapter.setOnItemInteractionListener(new EventTypeAdapter.OnItemInteractionListener() {
            @Override
            public void onEditClick(EventType eventType) {
                showAddEditEventTypeDialog(eventType);
            }

            @Override
            public void onDeleteClick(EventType eventType) {
                confirmDeleteEventType(eventType);
            }
        });

        return view;
    }

    private void loadEventTypes() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<EventType> eventTypes = eventTypeDao.getAllEventTypes();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.submitList(eventTypes);
                    if (eventTypes == null || eventTypes.isEmpty()) {
                        textViewNoEventTypes.setVisibility(View.VISIBLE);
                        recyclerViewEventTypes.setVisibility(View.GONE);
                    } else {
                        textViewNoEventTypes.setVisibility(View.GONE);
                        recyclerViewEventTypes.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void showAddEditEventTypeDialog(@Nullable EventType eventTypeToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_event_type, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.text_dialog_title);
        TextInputLayout textInputLayoutEventName = dialogView.findViewById(R.id.text_input_layout_event_name);
        TextInputEditText editTextEventTypeName = dialogView.findViewById(R.id.edit_text_event_type_name);

        if (eventTypeToEdit != null) {
            dialogTitle.setText("Edit Event Type");
            editTextEventTypeName.setText(eventTypeToEdit.getEventName());
            builder.setPositiveButton("Save Changes", null); // Set later for validation
        } else {
            dialogTitle.setText("Add New Event Type");
            builder.setPositiveButton("Add", null); // Set later for validation
        }
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String eventName = editTextEventTypeName.getText().toString().trim();
                if (TextUtils.isEmpty(eventName)) {
                    textInputLayoutEventName.setError("Event type name cannot be empty.");
                    return;
                } else {
                    textInputLayoutEventName.setError(null); // Clear error
                }

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    // Check for uniqueness before inserting/updating if the name changed
                    EventType existing = eventTypeDao.findByName(eventName);
                    boolean isUnique = true;
                    if (existing != null) {
                        if (eventTypeToEdit == null) { // Adding new, name already exists
                            isUnique = false;
                        } else if (eventTypeToEdit.getEventTypeId() != existing.getEventTypeId()) { // Editing, name changed to another existing name
                            isUnique = false;
                        }
                    }

                    if (!isUnique) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                textInputLayoutEventName.setError("This event type name already exists.");
                                Toast.makeText(getContext(), "Event type name already exists.", Toast.LENGTH_SHORT).show();
                            });
                        }
                        return; // Stop execution
                    }

                    // Proceed with insert or update
                    if (eventTypeToEdit != null) {
                        eventTypeToEdit.setEventName(eventName);
                        eventTypeDao.update(eventTypeToEdit);
                    } else {
                        EventType newEventType = new EventType(eventName);
                        eventTypeDao.insert(newEventType);
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            loadEventTypes(); // Refresh list
                            dialog.dismiss();
                            String message = eventTypeToEdit == null ? "Event type added." : "Event type updated.";
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            });
        });
        dialog.show();
    }


    private void confirmDeleteEventType(EventType eventType) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event Type")
                .setMessage("Are you sure you want to delete \"" + eventType.getEventName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        eventTypeDao.delete(eventType);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                loadEventTypes();
                                Toast.makeText(getContext(), "Event type deleted.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
