package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.Event;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private final List<Event> events;
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = new ArrayList<>(events);
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateEvents(List<Event> newEvents) {
        this.events.clear();
        this.events.addAll(newEvents);
        notifyDataSetChanged();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        private final ImageView eventPoster;
        private final TextView eventName;
        private final TextView eventDate;
        private final TextView eventLocation;
        private final TextView eventOrganizerName;
        private final TextView eventPrice;
        private final TextView waitlistInfo;
        private Event currentEvent;

        public EventViewHolder(@NonNull View itemView, OnEventClickListener listener) {
            super(itemView);
            eventPoster = itemView.findViewById(R.id.eventPoster);
            eventName = itemView.findViewById(R.id.eventName);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventLocation = itemView.findViewById(R.id.eventLocation);
            eventOrganizerName = itemView.findViewById(R.id.eventOrganizerName);
            eventPrice = itemView.findViewById(R.id.eventPrice);
            waitlistInfo = itemView.findViewById(R.id.waitlistInfo);

            itemView.setOnClickListener(v -> {
                if (listener != null && currentEvent != null) {
                    listener.onEventClick(currentEvent);
                }
            });
        }

        public void bind(Event event) {
            this.currentEvent = event;
            eventName.setText(event.getName());

            // Format date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            if (event.getEventDate() != null) {
                String dateString = dateFormat.format(event.getEventDate());
                String timeString = timeFormat.format(event.getEventDate());
                eventDate.setText(itemView.getContext().getString(R.string.date_time_format, dateString, timeString));
            }

            eventLocation.setText(event.getLocation());
            eventOrganizerName.setText(itemView.getContext().getString(R.string.organizer_label, event.getOrganizerName()));

            // Format price
            if (event.getPrice() > 0) {
                eventPrice.setText(String.format(Locale.getDefault(), "$%.2f", event.getPrice()));
            } else {
                eventPrice.setText(R.string.free_label);
            }

            // Show waitlist info
            int availableSpots = event.getCapacity() - event.getWaitlistCount();
            waitlistInfo.setText(String.format(Locale.getDefault(),
                    "%d on waitlist • %d spots available",
                    event.getWaitlistCount(), availableSpots));

            // Load poster image if available
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(event.getPosterUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(eventPoster);
            } else {
                eventPoster.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }
}
