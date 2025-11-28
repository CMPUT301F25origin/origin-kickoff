package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.NotificationLog;

public class NotificationLogAdapter extends RecyclerView.Adapter<NotificationLogAdapter.Holder> {
    private final List<NotificationLog> items = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public void setItems(List<NotificationLog> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log_entry, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        NotificationLog log = items.get(position);
        h.tvEventName.setText(log.getEventName() != null ? log.getEventName() : log.getEventId());
        String sender = (log.getSenderName() != null && !log.getSenderName().isEmpty()) ? log.getSenderName() : log.getSenderId();
        String recipient = (log.getRecipientName() != null && !log.getRecipientName().isEmpty()) ? log.getRecipientName() : log.getRecipientId();
        String type = log.getType() != null ? log.getType() : "";
        String meta = "Sent by: " + (sender != null ? sender : "") +
                "\nRecipient: " + (recipient != null ? recipient : "") +
                "\nType: " + type;
        h.tvMeta.setText(meta);
        String ts = log.getCreatedAt() != null ? fmt.format(log.getCreatedAt().toDate()) : "";
        h.tvTimestamp.setText(ts);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvEventName, tvMeta, tvTimestamp;
        Holder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
