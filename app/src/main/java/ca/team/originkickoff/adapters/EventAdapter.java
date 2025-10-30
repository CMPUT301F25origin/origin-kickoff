package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.Event;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<Event> events;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(OnEventClickListener listener) {
        this.events = new ArrayList<>();
        this.listener = listener;
    }

    // Constructor that accepts initial events list
    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
        this.listener = listener;
    }

    public void setEvents(List<Event> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Add updateEvents method for compatibility
    public void updateEvents(List<Event> events) {
        this.events.clear();
        if (events != null) {
            this.events.addAll(events);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivEventImage;
        private final TextView tvEventName;
        private final TextView tvEventDate;
        private final TextView tvSpotsLeft;
        private final TextView tvRequirements;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.ivEventImage);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvSpotsLeft = itemView.findViewById(R.id.tvSpotsLeft);
            tvRequirements = itemView.findViewById(R.id.tvRequirements);
        }

        public void bind(Event event, OnEventClickListener listener) {
            tvEventName.setText(event.getName());

            // Format date
            if (event.getEventDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvEventDate.setText(dateFormat.format(event.getEventDate()));
            } else {
                tvEventDate.setText("Date TBD");
            }

            // Calculate spots left
            int spotsLeft = event.getCapacity() - event.getWaitlistCount();
            if (spotsLeft < 0) spotsLeft = 0;
            tvSpotsLeft.setText(spotsLeft + " spots left");

            // Show requirements
            if (event.isGeolocationRequired()) {
                tvRequirements.setText("Req: Geolocation");
            } else {
                tvRequirements.setText("");
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });

            // TODO: Load image from posterUrl using Glide or Picasso
            // For now, use a placeholder
            ivEventImage.setImageResource(R.drawable.sample_event_1);
        }
    }
}
