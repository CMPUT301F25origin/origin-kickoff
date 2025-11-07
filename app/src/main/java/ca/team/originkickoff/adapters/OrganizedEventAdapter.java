/**
 * Adapter showing events organized by the current user with a status pill.
 * Helps organizers quickly scan registration / lottery progression.
 */
package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.Event;

/**
 * RecyclerView adapter for organizer-owned {@link ca.team.originkickoff.models.Event} cards with status labels.
 */
public class OrganizedEventAdapter extends RecyclerView.Adapter<OrganizedEventAdapter.ViewHolder> {
    private List<Event> events = new ArrayList<>();
    private final OnEventClickListener listener;

    /**
     * Click listener for organized events.
     */
    public interface OnEventClickListener {
        /**
         * Called when an organized event card is tapped.
         * @param event the associated event model
         */
        void onEventClick(Event event);
    }

    /**
     * Constructs the adapter.
     * @param listener click listener (nullable)
     */
    public OrganizedEventAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces current event list and refreshes.
     * @param events new events (nullable -> treated as empty)
     */
    public void setEvents(List<Event> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Inflates the organized event card view.
     * @param parent parent view group
     * @param viewType unused view type
     * @return new view holder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card_organized, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds the event at the given adapter position.
     * @param holder target holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event e = events.get(position);
        holder.bind(e, listener);
    }

    /**
     * @return number of organized events displayed
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for organizer event card showing name and status pill.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatusPill;
        /**
         * Creates the holder and binds view references.
         * @param itemView inflated card view
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvStatusPill = itemView.findViewById(R.id.tvStatusPill);
        }

        /**
         * Populates the card with event name and computed status.
         * @param e the event to render
         * @param listener click listener (nullable)
         */
        void bind(Event e, OnEventClickListener listener) {
            tvName.setText(e.getName());
            tvStatusPill.setText(resolveStatus(e));
            itemView.setOnClickListener(v -> { if (listener != null) listener.onEventClick(e); });
        }

        /**
         * Derives a human-readable status for the event based on times and lottery state.
         * @param e event model
         * @return status string (Upcoming, accepting wait list entries, Awaiting Lottery, Lottery Conducted)
         */
        private String resolveStatus(Event e) {
            long now = System.currentTimeMillis();
            if (e.getLotteryStatus() != null && e.getLotteryStatus().equalsIgnoreCase("conducted")) {
                return "Lottery Conducted";
            }
            if (e.getRegistrationStartTime() != null && e.getRegistrationEndTime() != null) {
                long start = e.getRegistrationStartTime().getTime();
                long end = e.getRegistrationEndTime().getTime();
                if (now < start) {
                    return "Upcoming";
                } else if (now <= end) {
                    if (!e.hasAvailableSpots()) {
                        return "accepting wait list entries";
                    } else {
                        return "Upcoming";
                    }
                } else {
                    return "Awaiting Lottery";
                }
            }
            if (e.getEventDate() != null && now < e.getEventDate().getTime()) {
                return "Upcoming";
            }
            return "Awaiting Lottery";
        }
    }
}
