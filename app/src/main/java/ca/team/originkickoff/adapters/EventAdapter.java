package ca.team.originkickoff.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Settings;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.SessionManager;

/**
 * RecyclerView adapter responsible for rendering a list of {@link Event} objects into
 * the {@code item_event_card} layout. Supports two usage modes:
 * <ul>
 *     <li>Plain event lists (e.g., available events)</li>
 *     <li>Event lists annotated with a status message per event (e.g., lottery results)</li>
 * </ul>
 * Additional behaviours:
 * <ul>
 *     <li>Color coding for selection status (selected / not selected / other)</li>
 *     <li>Admin-only delete icon resolved dynamically by querying the current device's user doc</li>
 *     <li>Poster image loading via Base64 (inline) or remote URL with Glide fallback</li>
 * </ul>
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    /** Backing list of events currently displayed. */
    private List<Event> events;
    /** Optional click listener notified when a card is tapped. */
    private OnEventClickListener listener;
    /** Optional map from event ID to status text replacing the spots-left string. */
    private java.util.Map<String, String> eventStatusMap;

    /**
     * Listener interface for event card click events.
     */
    public interface OnEventClickListener {
        /**
         * Invoked when the user taps an event card.
         * @param event the {@link Event} associated with the clicked item (never null)
         */
        void onEventClick(Event event);
    }

    /**
     * Constructs an adapter with an empty initial list.
     * @param listener callback invoked on item taps; may be {@code null} if clicks are not needed
     */
    public EventAdapter(OnEventClickListener listener) {
        this.events = new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Constructs an adapter with a provided initial list.
     * @param events initial events (nullable -> treated as empty)
     * @param listener callback invoked on item taps; may be {@code null}
     */
    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Replaces the current list of events with the supplied list and clears any status map.
     * Use this when you do not need per-event status overlays.
     * @param events new list (nullable -> becomes empty list)
     */
    public void setEvents(List<Event> events) {
        this.events = events != null ? events : new ArrayList<>();
        this.eventStatusMap = null;
        notifyDataSetChanged();
    }

    /**
     * Convenience for legacy callers: clears and appends events to the existing list.
     * Status map is reset. Prefer {@link #setEvents(List)} unless differential updates matter.
     * @param events list to display (nullable -> results in empty list)
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
     * Sets events together with a status map used to override the spots-left label.
     * Typical for joined events where you want to show selection outcome.
     * @param events events to render (nullable -> empty)
     * @param statusMap mapping from event ID to status text; may be {@code null}
     */
    public void setEventsWithStatus(List<Event> events, java.util.Map<String, String> statusMap) {
        this.events = events != null ? events : new ArrayList<>();
        this.eventStatusMap = statusMap;
        notifyDataSetChanged();
    }

    /**
     * Inflates an {@code item_event_card} view and wraps it in a {@link EventViewHolder}.
     * @param parent the parent recycler view
     * @param viewType ignored (single view type)
     * @return a new holder instance
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds adapter data for the event positioned at {@code position} into the holder.
     * @param holder view holder to populate
     * @param position adapter index (0 <= position < {@link #getItemCount()})
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, listener, eventStatusMap);
    }

    /**
     * @return current number of events rendered by this adapter
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder encapsulating a single event card's UI elements and binding logic.
     * Handles:
     * <ul>
     *     <li>Displaying core event metadata (name, date, requirements)</li>
     *     <li>Rendering either spots-left or status text with color coding</li>
     *     <li>Loading poster image (Base64 inline or via URL / Glide)</li>
     *     <li>Admin delete icon visibility and deletion action</li>
     *     <li>Forwarding click events to adapter listener</li>
     * </ul>
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivEventImage;
        private final TextView tvEventName;
        private final TextView tvEventDate;
        private final TextView tvSpotsLeft;
        private final TextView tvRequirements;
        private final ImageView ivDeleteEvent;

        /**
         * Creates the holder and caches child view references for fast reuse.
         * @param itemView inflated view for the event card
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.ivEventImage);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvSpotsLeft = itemView.findViewById(R.id.tvSpotsLeft);
            tvRequirements = itemView.findViewById(R.id.tvRequirements);
            ivDeleteEvent = itemView.findViewById(R.id.ivDeleteEvent);
        }

        /**
         * Populates the card UI for a given {@link Event} instance.
         * Includes dynamic styling for status messages and conditional admin features.
         * @param event event whose data to display
         * @param listener optional click listener for opening event details
         * @param statusMap optional map from event ID to status text; if present and contains the
         *                  event ID, its value replaces the spots-left label
         */
        public void bind(Event event, OnEventClickListener listener, java.util.Map<String, String> statusMap) {
            tvEventName.setText(event.getName());

            if (event.getEventDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvEventDate.setText(dateFormat.format(event.getEventDate()));
            } else {
                tvEventDate.setText(itemView.getContext().getString(R.string.date_tbd_label));
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
                tvSpotsLeft.setText(itemView.getContext().getString(R.string.spots_left_format, spotsLeft));
                tvSpotsLeft.setTextColor(0xFFFFFFFF);
            }

            if (event.isGeolocationRequired()) {
                tvRequirements.setText(itemView.getContext().getString(R.string.req_geolocation));
            } else {
                tvRequirements.setText("");
            }

            // Ensure child views don't intercept clicks intended for the card.
            ivEventImage.setClickable(false);
            tvEventName.setClickable(false);
            tvEventDate.setClickable(false);
            tvSpotsLeft.setClickable(false);
            tvRequirements.setClickable(false);

            itemView.setClickable(true);
            itemView.setFocusable(false);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    Toast.makeText(v.getContext(), "Opening " + (event.getName() != null ? event.getName() : "event"), Toast.LENGTH_SHORT).show();
                    Log.d("EventAdapter", "Item clicked: " + event.getId());
                    listener.onEventClick(event);
                }
            });

            if (SessionManager.isForceUserMode()) {
                // In forced user mode, hide admin delete icon entirely
                ivDeleteEvent.setVisibility(View.GONE);
            } else {
                // Existing admin privilege resolution
                String deviceId = Settings.Secure.getString(itemView.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                FirebaseFirestore.getInstance().collection("users")
                    .whereEqualTo("device_id", deviceId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            Boolean isAdmin = task.getResult().getDocuments().get(0).getBoolean("is_admin");
                            if (Boolean.TRUE.equals(isAdmin)) {
                                ivDeleteEvent.setVisibility(View.VISIBLE);
                                ivDeleteEvent.setOnClickListener(v -> FirebaseFirestore.getInstance().collection("events").document(event.getId()).delete());
                            }
                        }
                    });
            }

            // Poster image loading sequence: Base64 inline -> URL -> default placeholder.
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
