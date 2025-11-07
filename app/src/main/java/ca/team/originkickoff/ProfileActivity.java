package ca.team.originkickoff;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private SwitchMaterial switchLottery;
    private TextView tvDeviceId;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private ImageView ivProfile;
    private LinearLayout eventHistoryLayout;
    private String deviceId;
    private FirebaseFirestore db;
    private String userDocId;

    // Debounce for bottom-nav taps
    private long lastNavTapAtMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        ivProfile = findViewById(R.id.ivProfile);
        eventHistoryLayout = findViewById(R.id.eventHistoryLayout);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        setupTopBar();
        setupToggles();
        setupButtons();
        setupBottomBar();
        setupDeviceId();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileHeader();
    }

    private void setupTopBar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
    }

    private void setupToggles() {
        switchLottery = findViewById(R.id.switchLottery);
        if (TextUtils.isEmpty(deviceId)) return;

        db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        userDocId = doc.getId();
                        Boolean notifService = doc.getBoolean("notif_service");
                        switchLottery.setChecked(notifService != null && notifService);
                    }
                });

        switchLottery.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(userDocId != null) {
                db.collection("users").document(userDocId).update("notif_service", isChecked);
            }
        });
    }

    private void setupButtons() {
        findViewById(R.id.btnDelete).setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.bottomsheet_delete_profile);
        MaterialButton btnCancel = bottomSheetDialog.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = bottomSheetDialog.findViewById(R.id.btnDelete);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> {
            clearUserData();
            bottomSheetDialog.dismiss();
        });
        bottomSheetDialog.show();
    }

    private void clearUserData() {
        if (userDocId == null) {
            Toast.makeText(this, "Error: Could not get user profile to clear.", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("display_name", "");
        updates.put("email", null);
        updates.put("phone", null);
        updates.put("profile_image_id", null);
        updates.put("is_admin", false);
        updates.put("is_organizer", false);
        updates.put("notif_marketing", false);
        updates.put("notif_service", true);
        updates.put("updated_at", FieldValue.serverTimestamp());

        db.collection("users").document(userDocId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile data cleared.", Toast.LENGTH_SHORT).show();
                    updateProfileHeader();
                    setupToggles();
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to clear profile.", Toast.LENGTH_SHORT).show());
    }

    // Helper to navigate between bottom-bar destinations smoothly with no transition animation
    private void navigateBottomTab(Class<?> targetActivity) {
        if (targetActivity == null) return;
        if (getClass().equals(targetActivity)) return; // already on this tab
        long now = SystemClock.elapsedRealtime();
        if (now - lastNavTapAtMs < 300) return; // debounce rapid taps
        lastNavTapAtMs = now;
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        // Remove activity transition animations to avoid jank
        overridePendingTransition(0, 0);
    }

    private void setupBottomBar() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            navigateBottomTab(MainActivity.class);
        });
        findViewById(R.id.navEvents).setOnClickListener(v -> {
            navigateBottomTab(MyEventsActivity.class);
        });
        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            navigateBottomTab(NotificationsActivity.class);
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {});
    }

    private void setupDeviceId() {
        tvDeviceId = findViewById(R.id.tvDeviceId);
        tvDeviceId.setText(getString(R.string.device_id, deviceId != null ? deviceId : "-"));
    }

    private void updateProfileHeader() {
        if (TextUtils.isEmpty(deviceId)) {
            showPlaceholderAndClearData();
            return;
        }

        db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        userDocId = doc.getId();
                        tvUserName.setText(doc.getString("display_name"));
                        tvUserEmail.setText(doc.getString("email"));
                        loadProfileImage(doc.getString("profile_image_id"));
                        loadEventHistory(userDocId);
                    } else {
                        showPlaceholderAndClearData();
                    }
                })
                .addOnFailureListener(e -> showPlaceholderAndClearData());
    }

    private void showPlaceholderAndClearData(){
        tvUserName.setText("");
        tvUserEmail.setText("");
        eventHistoryLayout.removeAllViews();
        showPlaceholderImage();
    }

    private void loadProfileImage(String imageId) {
        if (imageId != null && !imageId.isEmpty()) {
            db.collection("images").document(imageId).get().addOnSuccessListener(imageDoc -> {
                if (imageDoc.exists()) {
                    String base64Image = imageDoc.getString("storage_path");
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            ivProfile.setImageTintList(null);
                            Glide.with(this).load(decodedString).apply(RequestOptions.circleCropTransform()).into(ivProfile);
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
        } else {
            showPlaceholderImage();
        }
    }

    private void showPlaceholderImage() {
        ivProfile.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ko_teal)));
        Glide.with(this).load(R.drawable.ic_person).apply(RequestOptions.circleCropTransform()).into(ivProfile);
    }

    private void loadEventHistory(String userId) {
        eventHistoryLayout.removeAllViews();
        db.collection("waiting_list_entries").whereEqualTo("user_id", userId).get()
                .addOnSuccessListener(entries -> {
                    if (entries.isEmpty()) {
                        findViewById(R.id.tvEventHistoryTitle).setVisibility(View.GONE);
                    } else {
                        findViewById(R.id.tvEventHistoryTitle).setVisibility(View.VISIBLE);
                        for (QueryDocumentSnapshot entry : entries) {
                            String state = entry.getString("state");
                            if (!"left".equalsIgnoreCase(state)) {
                                String eventId = entry.getString("event_id");
                                if (eventId != null) {
                                    fetchEventAndDisplay(eventId, state);
                                }
                            }
                        }
                    }
                });
    }

    private void fetchEventAndDisplay(String eventId, String state) {
        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            if (eventDoc.exists()) {
                LayoutInflater inflater = LayoutInflater.from(this);
                View eventCard = inflater.inflate(R.layout.item_event_history, eventHistoryLayout, false);

                TextView tvStatus = eventCard.findViewById(R.id.tvStatus);
                TextView tvEventTitle = eventCard.findViewById(R.id.tvEventTitle);
                TextView tvEventLocation = eventCard.findViewById(R.id.tvEventLocation);
                ShapeableImageView ivEventImage = eventCard.findViewById(R.id.ivEventImage);

                tvEventTitle.setText(eventDoc.getString("name"));
                tvEventLocation.setText(eventDoc.getString("location_name"));
                
                String statusText = state != null ? state.substring(0, 1).toUpperCase() + state.substring(1) : "Unknown";
                tvStatus.setText(statusText);

                if ("selected".equalsIgnoreCase(state)) {
                    tvStatus.setTextColor(ContextCompat.getColor(this, R.color.ko_success));
                } else {
                    tvStatus.setTextColor(ContextCompat.getColor(this, R.color.ko_danger));
                }

                String base64Image = eventDoc.getString("posterBase64");
                if (base64Image != null && !base64Image.isEmpty()) {
                    try {
                        String pureBase64 = base64Image;
                        int commaIndex = base64Image.indexOf(',');
                        if (commaIndex != -1) {
                            pureBase64 = base64Image.substring(commaIndex + 1);
                        }
                        byte[] decodedString = Base64.decode(pureBase64, Base64.DEFAULT);
                        Glide.with(this)
                                .load(decodedString)
                                .placeholder(R.drawable.bg_event_image_placeholder)
                                .error(R.drawable.bg_event_image_placeholder)
                                .into(ivEventImage);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Bad Base64 string for event " + eventId, e);
                        ivEventImage.setImageResource(R.drawable.bg_event_image_placeholder);
                    }
                } else {
                    ivEventImage.setImageResource(R.drawable.bg_event_image_placeholder);
                }

                eventHistoryLayout.addView(eventCard);
            } else {
                Log.e(TAG, "Event with ID " + eventId + " not found.");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch event with ID " + eventId, e));
    }
}
