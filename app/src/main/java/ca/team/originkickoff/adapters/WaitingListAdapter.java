package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        holder.userId.setText(userId);
        holder.source.setText(e.getSource());
        String when = e.getJoinedAt() != null ?
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(e.getJoinedAt().getSeconds()*1000)) :
                "";
        holder.joinedAt.setText(when);

        // Default: show a short form of ID while name loads
        holder.userName.setText(nameCache.containsKey(userId) ? nameCache.get(userId) : shortId(userId));
        if (!nameCache.containsKey(userId)) {
            fetchAndCacheName(userId, holder.getBindingAdapterPosition());
        }
    }

    private String shortId(String userId) {
        if (userId == null) return "";
        if (userId.length() <= 8) return userId;
        return userId.substring(0, 8) + "…";
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
