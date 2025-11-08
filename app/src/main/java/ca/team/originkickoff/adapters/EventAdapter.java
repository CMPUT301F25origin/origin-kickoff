/**
 * RecyclerView adapter rendering event cards and handling click navigation.
 * Central to listing joined/available events with status and images.
 */
package ca.team.originkickoff.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
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

/**
 * Adapter that binds {@link ca.team.originkickoff.models.Event} data to item_event_card views.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<Event> events;
    private OnEventClickListener listener;
    private java.util.Map<String, String> eventStatusMap;

    /**
     * Listener for event card taps.
     */
    public interface OnEventClickListener {
        /**
         * Called when an event item is tapped.
         * @param event the event associated with the clicked item
         */
        void onEventClick(Event event);
    }

    /**
     * Creates an adapter with an empty list.
     * @param listener callback for click events; may be null
     */
    public EventAdapter(OnEventClickListener listener) {
        this.events = new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Creates an adapter with an initial list of events.
     * @param events initial events list (nullable)
     * @param listener callback for click events; may be null
     */
    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Replaces the current list of events and refreshes the UI.
     * Clears any previously provided status map.
     * @param events new events list (nullable)
     */
    public void setEvents(List<Event> events) {
        this.events = events != null ? events : new ArrayList<>();
        this.eventStatusMap = null;
        notifyDataSetChanged();
    }

    /**
     * Updates events for compatibility with older callers.
     * Clears any previously provided status map.
     * @param events new events list (nullable)
     */
    public void updateEvents(List<Event> events) {
        this.events.clear();
        if (events != null) {
            this.events.addAll(events);
        }
        this.eventStatusMap = null;
        notifyDataSetChanged();
    }

    /**
     * Sets events together with a per-event status message to display instead of spots left.
     * @param events events to render (nullable)
     * @param statusMap map from eventId to status text (e.g., YOU WERE SELECTED)
     */
    public void setEventsWithStatus(List<Event> events, java.util.Map<String, String> statusMap) {
        this.events = events != null ? events : new ArrayList<>();
        this.eventStatusMap = statusMap;
        notifyDataSetChanged();
    }

    /**
     * Inflates and creates a new {@link EventViewHolder}.
     * @param parent the parent view group
     * @param viewType unused view type
     * @return a new view holder instance
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds the event at the given position to the holder.
     * @param holder target view holder
     * @param position adapter position to bind
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, listener, eventStatusMap);
    }

    /**
     * @return the number of events currently in the adapter
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder that displays an event card and forwards click events.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivEventImage;
        private final TextView tvEventName;
        private final TextView tvEventDate;
        private final TextView tvSpotsLeft;
        private final TextView tvRequirements;

        /**
         * Creates a holder bound to the given item view.
         * @param itemView the inflated item view
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.ivEventImage);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvSpotsLeft = itemView.findViewById(R.id.tvSpotsLeft);
            tvRequirements = itemView.findViewById(R.id.tvRequirements);
        }

        /**
         * Binds view content and click behavior for an event.
         * Handles date formatting, status color coding, and image loading.
         * @param event the event to display
         * @param listener click listener (nullable)
         * @param statusMap optional map of eventId to status text for joined events
         */
        public void bind(Event event, OnEventClickListener listener, java.util.Map<String, String> statusMap) {
            tvEventName.setText(event.getName());

            if (event.getEventDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvEventDate.setText(dateFormat.format(event.getEventDate()));
            } else {
                tvEventDate.setText("Date TBD");
            }

            if (statusMap != null && statusMap.containsKey(event.getId())) {
                String status = statusMap.get(event.getId());
                tvSpotsLeft.setText(status);

                if ("YOU WERE SELECTED".equals(status)) {
                    tvSpotsLeft.setTextColor(0xFF4DE8C0);
                } else if ("YOU WERE NOT SELECTED".equals(status)) {
                    tvSpotsLeft.setTextColor(0xFFFF3B30);
                } else {
                    tvSpotsLeft.setTextColor(0xFFFFD60A);
                }
            } else {
                int spotsLeft = event.getCapacity() - event.getWaitlistCount();
                if (spotsLeft < 0) spotsLeft = 0;
                tvSpotsLeft.setText(spotsLeft + " spots left");
                tvSpotsLeft.setTextColor(0xFFFFFFFF);
            }

            if (event.isGeolocationRequired()) {
                tvRequirements.setText("Req: Geolocation");
            } else {
                tvRequirements.setText("");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });

            boolean imageSet = false;
            String b64 = event.getPosterBase64();
            if (b64 != null && !b64.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(b64, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) {
                        ivEventImage.setImageBitmap(bmp);
                        imageSet = true;
                    }
                } catch (Exception ignored) {}
            }
            if (!imageSet) {
                String url = event.getPosterUrl();
                if (url != null && !url.isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(url)
                            .placeholder(R.drawable.sample_event_1)
                            .error(R.drawable.sample_event_1)
                            .into(ivEventImage);
                    imageSet = true;
                }
            }
            if (!imageSet) {
                ivEventImage.setImageResource(R.drawable.sample_event_1);
            }
        }
    }
}
