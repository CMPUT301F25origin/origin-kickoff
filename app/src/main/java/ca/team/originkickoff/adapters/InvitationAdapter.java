/**
 * RecyclerView adapter presenting invitation statuses for an event.
 * Handles lazy loading of user names and profile images with Firestore caching.
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.InvitationStatus;

/**
 * Adapter for displaying {@link ca.team.originkickoff.models.InvitationStatus} items.
 */
public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.ViewHolder> {

    public interface OnRemoveEntrantListener {
        void onRemoveEntrant(InvitationStatus invitation);
    }

    private List<InvitationStatus> invitations;
    private final Map<String, String> nameCache = new HashMap<>();
    private final Map<String, String> imageCache = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private OnRemoveEntrantListener removeEntrantListener;
    private String status;

    /**
     * Creates a new adapter instance.
     * @param invitations initial invitation list (nullable)
     */
    public InvitationAdapter(List<InvitationStatus> invitations, OnRemoveEntrantListener listener, String status) {
        this.invitations = invitations;
        this.removeEntrantListener = listener;
        this.status = status;
    }

    /**
     * Replaces current dataset and clears name & image caches.
     * @param newInvitations new list of invitation statuses
     */
    public void updateData(List<InvitationStatus> newInvitations) {
        this.invitations = newInvitations;
        nameCache.clear();
        imageCache.clear();
        notifyDataSetChanged();
    }

    /**
     * Inflates a new invitation item view.
     * @param parent parent ViewGroup
     * @param viewType unused view type
     * @return view holder for invitation item
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invitation, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the invitation status at the given position.
     * @param holder holder to bind
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InvitationStatus invitation = invitations.get(position);
        holder.bind(invitation, position);
    }

    /**
     * @return number of invitations displayed
     */
    @Override
    public int getItemCount() {
        return invitations.size();
    }

    /**
     * ViewHolder representing a single invitation status row.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUserName;
        private final TextView tvSelectedDate;
        private final ImageView ivProfilePic;
        private final MaterialButton btnRemoveEntrant;

        /**
         * Constructs the holder and binds view references.
         * @param itemView inflated item view
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvSelectedDate = itemView.findViewById(R.id.tv_selected_date);
            ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
            btnRemoveEntrant = itemView.findViewById(R.id.btn_remove_entrant);
        }

        /**
         * Populates the row with invitation details and triggers async user fetch.
         * @param invitation the invitation status model
         * @param position adapter position (used for targeted refresh)
         */
        public void bind(InvitationStatus invitation, int position) {
            String userId = invitation.getUserId();

            tvUserName.setText(nameCache.containsKey(userId) ? nameCache.get(userId) : "Loading...");
            if (!nameCache.containsKey(userId)) {
                fetchUserData(userId, position);
            }

            if (invitation.getInvitedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                String dateStr = sdf.format(invitation.getInvitedAt().toDate());
                tvSelectedDate.setText("Selected: " + dateStr);
            } else {
                tvSelectedDate.setText("Selected: Unknown");
            }

            loadProfilePicture(userId);

            if ("chosen".equals(status)) {
                btnRemoveEntrant.setVisibility(View.VISIBLE);
                btnRemoveEntrant.setOnClickListener(v -> {
                    if (removeEntrantListener != null) {
                        removeEntrantListener.onRemoveEntrant(invitation);
                    }
                });
            } else {
                btnRemoveEntrant.setVisibility(View.GONE);
            }
        }

        /**
         * Fetches user metadata (name & profile image id) and updates caches.
         * @param userId Firestore user document ID
         * @param adapterPos position to refresh after data loads
         */
        private void fetchUserData(String userId, int adapterPos) {
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

                if (adapterPos >= 0 && adapterPos < invitations.size()) {
                    notifyItemChanged(adapterPos);
                }
            }).addOnFailureListener(e -> {
                nameCache.put(userId, "Unknown User");
                imageCache.put(userId, "");
                if (adapterPos >= 0 && adapterPos < invitations.size()) {
                    notifyItemChanged(adapterPos);
                }
            });
        }

        /**
         * Loads a cached or fetched profile picture for a user.
         * @param userId Firestore user document ID
         */
        private void loadProfilePicture(String userId) {
            if (userId == null || userId.isEmpty()) {
                showPlaceholderImage();
                return;
            }

            if (imageCache.containsKey(userId)) {
                String imageId = imageCache.get(userId);
                if (imageId != null && !imageId.isEmpty()) {
                    loadImageFromFirestore(imageId);
                } else {
                    showPlaceholderImage();
                }
            } else {
                showPlaceholderImage();
            }
        }

        /**
         * Retrieves the image document and attempts to decode the Base64 image.
         * @param imageId Firestore image document ID
         */
        private void loadImageFromFirestore(String imageId) {
            db.collection("images").document(imageId).get().addOnSuccessListener(imageDoc -> {
                if (imageDoc.exists()) {
                    String base64Image = imageDoc.getString("storage_path");
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            ivProfilePic.setImageTintList(null);
                            Glide.with(itemView.getContext())
                                    .load(decodedString)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(ivProfilePic);
                        } catch (Exception e) {
                            showPlaceholderImage();
                        }
                    } else {
                        showPlaceholderImage();
                    }
                } else {
                    showPlaceholderImage();
                }
            }).addOnFailureListener(e -> showPlaceholderImage());
        }

        /**
         * Shows a placeholder avatar when no custom image is available.
         */
        private void showPlaceholderImage() {
            ivProfilePic.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), R.color.ko_teal)));
            Glide.with(itemView.getContext())
                    .load(R.drawable.ic_person)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivProfilePic);
        }

        /**
         * Attempts to derive a displayable name from a user document.
         * @param doc Firestore user document snapshot
         * @return best-effort display name or fallback string
         */
        private String extractName(DocumentSnapshot doc) {
            if (doc == null || !doc.exists()) return "Unknown User";
            String[] keys = new String[]{"display_name", "displayName", "name", "username", "email"};
            for (String k : keys) {
                Object v = doc.get(k);
                if (v instanceof String) {
                    String s = ((String) v).trim();
                    if (!s.isEmpty()) return s;
                }
            }
            return "Unknown User";
        }
    }
}
