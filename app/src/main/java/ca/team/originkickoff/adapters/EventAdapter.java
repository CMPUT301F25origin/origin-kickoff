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

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<Event> events;
    private OnEventClickListener listener;
    private java.util.Map<String, String> eventStatusMap;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(OnEventClickListener listener) {
        this.events = new ArrayList<>();
        this.listener = listener;
    }

    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
        this.listener = listener;
    }

    public void setEvents(List<Event> events) {
        this.events = events != null ? events : new ArrayList<>();
        this.eventStatusMap = null;
        notifyDataSetChanged();
    }

    public void updateEvents(List<Event> events) {
        this.events.clear();
        if (events != null) {
            this.events.addAll(events);
        }
        this.eventStatusMap = null;
        notifyDataSetChanged();
    }

    public void setEventsWithStatus(List<Event> events, java.util.Map<String, String> statusMap) {
        this.events = events != null ? events : new ArrayList<>();
        this.eventStatusMap = statusMap;
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
        holder.bind(event, listener, eventStatusMap);
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
        private final ImageView ivDeleteEvent;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.ivEventImage);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvSpotsLeft = itemView.findViewById(R.id.tvSpotsLeft);
            tvRequirements = itemView.findViewById(R.id.tvRequirements);
            ivDeleteEvent = itemView.findViewById(R.id.ivDeleteEvent);
        }

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

            String deviceId = Settings.Secure.getString(itemView.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);

            FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("device_id", deviceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        boolean isAdmin = task.getResult().getDocuments().get(0).getBoolean("is_admin");
                        if (isAdmin) {
                            ivDeleteEvent.setVisibility(View.VISIBLE);
                            ivDeleteEvent.setOnClickListener(v -> {
                                FirebaseFirestore.getInstance().collection("events").document(event.getId()).delete();
                            });
                        }
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
