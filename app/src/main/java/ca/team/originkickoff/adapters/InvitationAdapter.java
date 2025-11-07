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

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.InvitationStatus;

/**
 * Adapter for displaying invitation status items
 */
public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.ViewHolder> {
    private List<InvitationStatus> invitations;
    private final Map<String, String> nameCache = new HashMap<>();
    private final Map<String, String> imageCache = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public InvitationAdapter(List<InvitationStatus> invitations) {
        this.invitations = invitations;
    }

    public void updateData(List<InvitationStatus> newInvitations) {
        this.invitations = newInvitations;
        nameCache.clear();
        imageCache.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invitation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InvitationStatus invitation = invitations.get(position);
        holder.bind(invitation, position);
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUserName;
        private final TextView tvSelectedDate;
        private final ImageView ivProfilePic;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvSelectedDate = itemView.findViewById(R.id.tv_selected_date);
            ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
        }

        public void bind(InvitationStatus invitation, int position) {
            String userId = invitation.getUserId();

            // Display user name (fetch from users collection)
            tvUserName.setText(nameCache.containsKey(userId) ? nameCache.get(userId) : "Loading...");
            if (!nameCache.containsKey(userId)) {
                fetchUserData(userId, position);
            }

            // Format date as MM/DD/YYYY
            if (invitation.getInvitedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                String dateStr = sdf.format(invitation.getInvitedAt().toDate());
                tvSelectedDate.setText("Selected: " + dateStr);
            } else {
                tvSelectedDate.setText("Selected: Unknown");
            }

            // Load profile picture
            loadProfilePicture(userId);
        }

        private void fetchUserData(String userId, int adapterPos) {
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

        private void loadProfilePicture(String userId) {
            if (userId == null || userId.isEmpty()) {
                showPlaceholderImage();
                return;
            }

            // Check if we already have the image ID cached
            if (imageCache.containsKey(userId)) {
                String imageId = imageCache.get(userId);
                if (imageId != null && !imageId.isEmpty()) {
                    loadImageFromFirestore(imageId);
                } else {
                    showPlaceholderImage();
                }
            } else {
                // Will be loaded when user data is fetched
                showPlaceholderImage();
            }
        }

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

        private void showPlaceholderImage() {
            ivProfilePic.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), R.color.ko_teal)));
            Glide.with(itemView.getContext())
                    .load(R.drawable.ic_person)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivProfilePic);
        }

        private String extractName(DocumentSnapshot doc) {
            if (doc == null || !doc.exists()) return "Unknown User";
            // Try common name fields used across this project
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
