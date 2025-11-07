/**
 * Adapter presenting notification items with read/unread visual state.
 * Core component for the in-app notifications feed.
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
import ca.team.originkickoff.models.NotificationItem;

/**
 * RecyclerView adapter binding {@link ca.team.originkickoff.models.NotificationItem} data to views.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private final List<NotificationItem> items = new ArrayList<>();
    private final OnNotificationClickListener clickListener;

    /**
     * Callback for notification item taps.
     */
    public interface OnNotificationClickListener {
        /**
         * Invoked when a notification card is clicked.
         * @param notification the clicked notification model
         */
        void onNotificationClick(NotificationItem notification);
    }

    /**
     * Creates an adapter instance.
     * @param clickListener listener for click events (nullable)
     */
    public NotificationAdapter(OnNotificationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /**
     * Replaces the current list of notifications and refreshes the UI.
     * @param newItems new list of notifications (nullable)
     */
    public void setItems(List<NotificationItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * Inflates a notification item view.
     * @param parent parent view group
     * @param viewType unused view type
     * @return new holder instance
     */
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    /**
     * Binds data for the notification at the given position.
     * @param holder target view holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem it = items.get(position);
        holder.title.setText(it.getTitle());
        holder.message.setText(it.getMessage());
        holder.time.setText(it.getTimestamp());
        float alpha = it.isRead() ? 0.7f : 1.0f;
        holder.title.setAlpha(alpha);
        holder.message.setAlpha(alpha);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNotificationClick(it);
            }
        });
    }

    /**
     * @return count of notifications in adapter
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder representing a single notification card.
     */
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView message;
        final TextView time;

        /**
         * Constructs the holder and binds view references.
         * @param itemView inflated notification view
         */
        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            message = itemView.findViewById(R.id.tvMessage);
            time = itemView.findViewById(R.id.tvTime);
        }
    }
}
