package ca.team.originkickoff.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;

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
        ImageView ivImage, ivMore;
        TextView tvName, tvDate, tvStatusPill, tvEntrants, tvLotteryStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivEventImage);
            ivMore = itemView.findViewById(R.id.ivMore);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvStatusPill = itemView.findViewById(R.id.tvStatusPill);
            tvEntrants = itemView.findViewById(R.id.tvEntrants);
            tvLotteryStatus = itemView.findViewById(R.id.tvLotteryStatus);
        }

        void bind(Event e, OnEventClickListener listener) {
            tvName.setText(e.getName());

            DateFormat df = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            if (e.getEventDate() != null) tvDate.setText(df.format(e.getEventDate()));
            else tvDate.setText(itemView.getContext().getString(R.string.date_tbd));

            // Determine status pill text based on registration window / event date
            String status = "";
            long now = System.currentTimeMillis();
            try {
                if (e.getRegistrationStartTime() != null && e.getRegistrationEndTime() != null) {
                    long start = e.getRegistrationStartTime().getTime();
                    long end = e.getRegistrationEndTime().getTime();
                    if (now >= start && now <= end) status = "Live";
                    else if (now < start) status = "Upcoming";
                    else status = "Ended";
                } else if (e.getEventDate() != null) {
                    if (now < e.getEventDate().getTime()) status = "Upcoming";
                    else status = "Ended";
                }
            } catch (Exception ignored) { }
            if (status.isEmpty()) status = "TBD";
            tvStatusPill.setText(status);

            // Show entrants (fallback to waitlistCount as a proxy)
            NumberFormat nf = NumberFormat.getIntegerInstance();
            int entrants = e.getWaitlistCount();
            tvEntrants.setText(itemView.getContext().getString(R.string.entrants_count, nf.format(entrants)));

            // Lottery status placeholder
            if ("Ended".equalsIgnoreCase(status) || "Live".equalsIgnoreCase(status))
                tvLotteryStatus.setText(itemView.getContext().getString(R.string.winners_selected));
            else
                tvLotteryStatus.setText(itemView.getContext().getString(R.string.lottery_pending));

            // Load image
            boolean imageSet = false;
            String b64 = e.getPosterBase64();
            if (b64 != null && !b64.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(b64, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) {
                        ivImage.setImageBitmap(bmp);
                        imageSet = true;
                    }
                } catch (Exception ignored) {}
            }
            if (!imageSet) {
                String url = e.getPosterUrl();
                if (url != null && !url.isEmpty()) {
                    Glide.with(itemView.getContext()).load(url).placeholder(R.drawable.sample_event_1).error(R.drawable.sample_event_1).into(ivImage);
                    imageSet = true;
                }
            }
            if (!imageSet) ivImage.setImageResource(R.drawable.sample_event_1);

            // Click listeners
            itemView.setOnClickListener(v -> { if (listener != null) listener.onEventClick(e); });

            ivMore.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(itemView.getContext(), ivMore);
                menu.getMenu().add(itemView.getContext().getString(R.string.more));
                menu.getMenu().add("Analytics");
                menu.getMenu().add(itemView.getContext().getString(R.string.delete));
                menu.setOnMenuItemClickListener((MenuItem item) -> {
                    // No-op for now; host can override by intercepting clicks if needed
                    return true;
                });
                menu.show();
            });
        }
    }
}
