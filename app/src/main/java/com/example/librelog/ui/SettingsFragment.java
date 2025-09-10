package com.example.librelog.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.librelog.AppDatabase;
import com.example.librelog.EventType;
import com.example.librelog.EventTypeAdapter;
import com.example.librelog.EventTypeDao;
import io.github.billpottle.librelog.R;
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

    private static final int DEFAULT_EVENT_TYPE_ID = 1; // Assuming 'Default Event' created by callback gets ID 1

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
                // Allow editing for all, including the default one
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
            builder.setPositiveButton("Save Changes", null);
        } else {
            dialogTitle.setText("Add New Event Type");
            builder.setPositiveButton("Add", null);
        }
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newEventName = editTextEventTypeName.getText().toString().trim();
                if (TextUtils.isEmpty(newEventName)) {
                    textInputLayoutEventName.setError("Event type name cannot be empty.");
                    return;
                } else {
                    textInputLayoutEventName.setError(null);
                }

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    EventType existingByName = eventTypeDao.findByName(newEventName);
                    boolean isUniqueOrSameItem = true;

                    if (existingByName != null) {
                        if (eventTypeToEdit == null) { // Adding new
                            isUniqueOrSameItem = false;
                        } else { // Editing existing
                            if (eventTypeToEdit.getEventTypeId() != existingByName.getEventTypeId()) {
                                // Name matches another existing item's name that is not itself
                                isUniqueOrSameItem = false;
                            }
                        }
                    }

                    if (!isUniqueOrSameItem) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                textInputLayoutEventName.setError("This event type name already exists.");
                                Toast.makeText(getContext(), "Event type name already exists.", Toast.LENGTH_SHORT).show();
                            });
                        }
                        return;
                    }

                    if (eventTypeToEdit != null) {
                        eventTypeToEdit.setEventName(newEventName);
                        eventTypeDao.update(eventTypeToEdit);
                    } else {
                        EventType newEventType = new EventType(newEventName);
                        eventTypeDao.insert(newEventType);
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            loadEventTypes();
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
        // Prevent deletion if the event type ID is the default one.
        if (eventType.getEventTypeId() == DEFAULT_EVENT_TYPE_ID) {
            Toast.makeText(getContext(), "'" + eventType.getEventName() + "' is the default event type and cannot be deleted.", Toast.LENGTH_LONG).show();
            return;
        }

        String message = "Are you sure you want to delete \"" + eventType.getEventName() + "\"? " +
                "All log entries associated with this event type will have their event type cleared (set to null). " +
                "This action cannot be undone. Consider exporting your data first if you want to keep a record.";

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event Type")
                .setMessage(message)
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
