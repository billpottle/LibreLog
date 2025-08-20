package com.example.librelog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class EventTypeAdapter extends ListAdapter<EventType, EventTypeAdapter.EventTypeViewHolder> {

    private OnItemInteractionListener listener;

    public EventTypeAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<EventType> DIFF_CALLBACK = new DiffUtil.ItemCallback<EventType>() {
        @Override
        public boolean areItemsTheSame(@NonNull EventType oldItem, @NonNull EventType newItem) {
            return oldItem.getEventTypeId() == newItem.getEventTypeId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull EventType oldItem, @NonNull EventType newItem) {
            return oldItem.getEventName().equals(newItem.getEventName());
        }
    };

    @NonNull
    @Override
    public EventTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_event_type, parent, false);
        return new EventTypeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EventTypeViewHolder holder, int position) {
        EventType currentEventType = getItem(position);
        holder.textViewName.setText(currentEventType.getEventName());
    }

    public EventType getEventTypeAt(int position) {
        return getItem(position);
    }

    class EventTypeViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewName;
        private ImageButton buttonEdit;
        private ImageButton buttonDelete;

        public EventTypeViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_event_type_name);
            buttonEdit = itemView.findViewById(R.id.button_edit_event_type);
            buttonDelete = itemView.findViewById(R.id.button_delete_event_type);

            buttonEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onEditClick(getItem(position));
                }
            });

            buttonDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(getItem(position));
                }
            });
        }
    }

    public interface OnItemInteractionListener {
        void onEditClick(EventType eventType);
        void onDeleteClick(EventType eventType);
    }

    public void setOnItemInteractionListener(OnItemInteractionListener listener) {
        this.listener = listener;
    }
}
