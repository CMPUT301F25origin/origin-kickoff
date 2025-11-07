package ca.team.originkickoff.adapters;

import android.content.res.ColorStateList;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
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
    private final Map<String, String> imageCache = new HashMap<>();
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

        // Load profile picture
        loadProfilePicture(holder, userId);
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

            // Cache profile image ID if available
            String imageId = doc.getString("profile_image_id");
            if (imageId != null && !imageId.isEmpty()) {
                imageCache.put(userId, imageId);
            } else {
                imageCache.put(userId, ""); // Mark as checked but empty
            }

            if (adapterPos >= 0 && adapterPos < items.size()) {
                notifyItemChanged(adapterPos);
            } else {
                notifyDataSetChanged();
            }
        }).addOnFailureListener(e -> {
            nameCache.put(userId, "Unknown entrant");
            imageCache.put(userId, "");
            if (adapterPos >= 0 && adapterPos < items.size()) notifyItemChanged(adapterPos);
        });
    }

    private void loadProfilePicture(VH holder, String userId) {
        if (userId == null || userId.isEmpty()) {
            showPlaceholderImage(holder);
            return;
        }

        // Check if we already have the image ID cached
        if (imageCache.containsKey(userId)) {
            String imageId = imageCache.get(userId);
            if (imageId != null && !imageId.isEmpty()) {
                loadImageFromFirestore(holder, imageId);
            } else {
                showPlaceholderImage(holder);
            }
        } else {
            // Will be loaded when name is fetched
            showPlaceholderImage(holder);
        }
    }

    private void loadImageFromFirestore(VH holder, String imageId) {
        db.collection("images").document(imageId).get().addOnSuccessListener(imageDoc -> {
            if (imageDoc.exists()) {
                String base64Image = imageDoc.getString("storage_path");
                if (base64Image != null && !base64Image.isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        holder.profilePic.setImageTintList(null);
                        Glide.with(holder.itemView.getContext())
                                .load(decodedString)
                                .apply(RequestOptions.circleCropTransform())
                                .into(holder.profilePic);
                    } catch (Exception e) {
                        showPlaceholderImage(holder);
                    }
                } else {
                    showPlaceholderImage(holder);
                }
            } else {
                showPlaceholderImage(holder);
            }
        }).addOnFailureListener(e -> showPlaceholderImage(holder));
    }

    private void showPlaceholderImage(VH holder) {
        holder.profilePic.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.ko_teal)));
        Glide.with(holder.itemView.getContext())
                .load(R.drawable.ic_person)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.profilePic);
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
        imageCache.clear();
        if (in != null) items.addAll(in);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView userName, userId, joinedAt, source;
        ImageView profilePic;
        VH(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.tvUserName);
            userId = itemView.findViewById(R.id.tvUserId);
            joinedAt = itemView.findViewById(R.id.tvJoinedAt);
            source = itemView.findViewById(R.id.tvSource);
            profilePic = itemView.findViewById(R.id.ivProfilePic);
        }
    }
}
