package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.WaitingListEntry;

public class WaitingListAdapter extends RecyclerView.Adapter<WaitingListAdapter.VH> {
    private final List<WaitingListEntry> items = new ArrayList<>();
    private final Map<String, String> nameCache = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_waiting_list_entry, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        WaitingListEntry e = items.get(position);
        String userId = e.getUserId();

        // Hide user ID and source fields
        holder.userId.setVisibility(View.GONE);
        holder.source.setVisibility(View.GONE);

        // Calculate and display "Joined x days ago"
        String joinedText = calculateJoinedAgo(e.getJoinedAt());
        holder.joinedAt.setText(joinedText);

        // Display user name (fetch from users collection)
        holder.userName.setText(nameCache.containsKey(userId) ? nameCache.get(userId) : "Loading...");
        if (!nameCache.containsKey(userId)) {
            fetchAndCacheName(userId, holder.getBindingAdapterPosition());
        }
    }

    private String calculateJoinedAgo(com.google.firebase.Timestamp joinedAt) {
        if (joinedAt == null) return "Joined recently";

        long joinedMillis = joinedAt.getSeconds() * 1000;
        long nowMillis = System.currentTimeMillis();
        long diffMillis = nowMillis - joinedMillis;

        long days = diffMillis / (1000 * 60 * 60 * 24);

        if (days == 0) {
            long hours = diffMillis / (1000 * 60 * 60);
            if (hours == 0) {
                long minutes = diffMillis / (1000 * 60);
                if (minutes <= 1) {
                    return "Joined just now";
                }
                return "Joined " + minutes + " minutes ago";
            } else if (hours == 1) {
                return "Joined 1 hour ago";
            } else {
                return "Joined " + hours + " hours ago";
            }
        } else if (days == 1) {
            return "Joined 1 day ago";
        } else {
            return "Joined " + days + " days ago";
        }
    }

    private void fetchAndCacheName(String userId, int adapterPos) {
        if (userId == null || userId.isEmpty()) return;
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            String name = extractName(doc);
            nameCache.put(userId, name);
            if (adapterPos >= 0 && adapterPos < items.size()) {
                notifyItemChanged(adapterPos);
            } else {
                notifyDataSetChanged();
            }
        }).addOnFailureListener(e -> {
            nameCache.put(userId, "Unknown entrant");
            if (adapterPos >= 0 && adapterPos < items.size()) notifyItemChanged(adapterPos);
        });
    }

    private String extractName(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return "Unknown entrant";
        // Try common name fields used across this project
        String[] keys = new String[]{"display_name", "displayName", "name", "username", "email"};
        for (String k : keys) {
            Object v = doc.get(k);
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (!s.isEmpty()) return s;
            }
        }
        return "Unknown entrant";
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void submit(List<WaitingListEntry> in) {
        items.clear();
        nameCache.clear();
        if (in != null) items.addAll(in);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView userName, userId, joinedAt, source;
        VH(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.tvUserName);
            userId = itemView.findViewById(R.id.tvUserId);
            joinedAt = itemView.findViewById(R.id.tvJoinedAt);
            source = itemView.findViewById(R.id.tvSource);
        }
    }
}
