/**
 * Adapter listing entrants on an event's waiting list with relative join time.
 * Fetches display names and profile images lazily via Firestore with simple caching.
 */
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

/**
 * RecyclerView adapter binding {@link ca.team.originkickoff.models.WaitingListEntry} items for display.
 */
public class WaitingListAdapter extends RecyclerView.Adapter<WaitingListAdapter.VH> {
    private final List<WaitingListEntry> items = new ArrayList<>();
    private final Map<String, String> nameCache = new HashMap<>();
    private final Map<String, String> imageCache = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Inflates a waiting-list row view.
     * @param parent parent view group
     * @param viewType unused view type
     * @return new view holder
     */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_waiting_list_entry, parent, false);
        return new VH(v);
    }

    /**
     * Binds the waiting list entry at position, showing name, avatar and relative time.
     * @param holder target holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        WaitingListEntry e = items.get(position);
        String userId = e.getUserId();

        holder.userId.setVisibility(View.GONE);
        holder.source.setVisibility(View.GONE);

        String joinedText = calculateJoinedAgo(e.getJoinedAt());
        holder.joinedAt.setText(joinedText);

        holder.userName.setText(nameCache.containsKey(userId) ? nameCache.get(userId) : "Loading...");
        if (!nameCache.containsKey(userId)) {
            fetchAndCacheName(userId, holder.getBindingAdapterPosition());
        }

        loadProfilePicture(holder, userId);
    }

    /**
     * Converts a timestamp into a human-readable relative phrase.
     * @param joinedAt Firestore timestamp when user joined the waitlist
     * @return string like "Joined 2 days ago"
     */
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

    /**
     * Fetches a user's display name and caches the result, refreshing the row.
     * @param userId users/{id} document id
     * @param adapterPos adapter position for targeted notify
     */
    private void fetchAndCacheName(String userId, int adapterPos) {
        if (userId == null || userId.isEmpty()) return;
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            String name = extractName(doc);
            nameCache.put(userId, name);

            String imageId = doc.getString("profile_image_id");
            if (imageId != null && !imageId.isEmpty()) {
                imageCache.put(userId, imageId);
            } else {
                imageCache.put(userId, "");
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

    /**
     * Loads profile picture for a given user id, using cached image id when available.
     * @param holder row view holder
     * @param userId users/{id} document id
     */
    private void loadProfilePicture(VH holder, String userId) {
        if (userId == null || userId.isEmpty()) {
            showPlaceholderImage(holder);
            return;
        }

        if (imageCache.containsKey(userId)) {
            String imageId = imageCache.get(userId);
            if (imageId != null && !imageId.isEmpty()) {
                loadImageFromFirestore(holder, imageId);
            } else {
                showPlaceholderImage(holder);
            }
        } else {
            showPlaceholderImage(holder);
        }
    }

    /**
     * Retrieves and decodes a Base64 image by its images/{id} document id.
     * @param holder row view holder
     * @param imageId images/{id} document id
     */
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

    /**
     * Shows a placeholder avatar when no image is available.
     * @param holder row view holder
     */
    private void showPlaceholderImage(VH holder) {
        holder.profilePic.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.ko_teal)));
        Glide.with(holder.itemView.getContext())
                .load(R.drawable.ic_person)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.profilePic);
    }

    /**
     * Attempts to extract a preferred display name from a user document.
     * @param doc Firestore user document snapshot
     * @return display name or fallback
     */
    private String extractName(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return "Unknown entrant";
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

    /**
     * @return number of entries in the adapter
     */
    @Override
    public int getItemCount() { return items.size(); }

    /**
     * Submits a new list, clearing caches and refreshing.
     * @param in waiting list entries (nullable)
     */
    public void submit(List<WaitingListEntry> in) {
        items.clear();
        nameCache.clear();
        imageCache.clear();
        if (in != null) items.addAll(in);
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for a waiting-list row.
     */
    static class VH extends RecyclerView.ViewHolder {
        TextView userName, userId, joinedAt, source;
        ImageView profilePic;
        /**
         * Constructs the holder and binds view references.
         * @param itemView inflated item view
         */
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
