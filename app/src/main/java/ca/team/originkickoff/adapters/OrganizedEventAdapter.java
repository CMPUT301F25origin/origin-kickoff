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

public class OrganizedEventAdapter extends RecyclerView.Adapter<OrganizedEventAdapter.ViewHolder> {
    private List<Event> events = new ArrayList<>();
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public OrganizedEventAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setEvents(List<Event> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card_organized, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event e = events.get(position);
        holder.bind(e, listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatusPill;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvStatusPill = itemView.findViewById(R.id.tvStatusPill);
        }

        void bind(Event e, OnEventClickListener listener) {
            tvName.setText(e.getName());
            tvStatusPill.setText(resolveStatus(e));
            itemView.setOnClickListener(v -> { if (listener != null) listener.onEventClick(e); });
        }

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
                } else if (now <= end) { // registration open
                    if (!e.hasAvailableSpots()) {
                        return "accepting wait list entries"; // full, wait list only
                    } else {
                        return "Upcoming"; // still space, show Upcoming per spec
                    }
                } else { // after registration window
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
