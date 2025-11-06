package ca.team.originkickoff;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private SwitchMaterial switchLottery;
    private TextView tvDeviceId;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private ImageView ivProfile;
    private String deviceId;
    private FirebaseFirestore db;

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
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        View btnEdit = findViewById(R.id.btnEditProfile);
        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, EditProfileActivity.class);
            startActivity(i);
        });
    }

    private void setupToggles() {
        switchLottery = findViewById(R.id.switchLottery);

        if (TextUtils.isEmpty(deviceId)) return;

        db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Boolean notifService = doc.getBoolean("notif_service");
                        switchLottery.setChecked(notifService != null && notifService);
                    }
                });

        switchLottery.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            String docId = task.getResult().getDocuments().get(0).getId();
                            db.collection("users").document(docId).update("notif_service", isChecked);
                        }
                    });
        });
    }

    private void setupButtons() {
        MaterialButton btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.bottomsheet_delete_profile);

        MaterialButton btnCancel = bottomSheetDialog.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = bottomSheetDialog.findViewById(R.id.btnDelete);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                clearUserData();
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    private void clearUserData() {
        if (TextUtils.isEmpty(deviceId)) {
            Toast.makeText(this, "Error: Could not get device ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String docId = task.getResult().getDocuments().get(0).getId();

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

                        db.collection("users").document(docId).update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ProfileActivity.this, "Profile data cleared.", Toast.LENGTH_SHORT).show();
                                    updateProfileHeader();
                                    setupToggles();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to clear profile data", e);
                                    Toast.makeText(ProfileActivity.this, "Failed to clear profile.", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e(TAG, "Could not find profile to clear", task.getException());
                        Toast.makeText(ProfileActivity.this, "Could not find profile to clear.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBottomBar() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navEvents = findViewById(R.id.navEvents);
        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        });
        navEvents.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MyEventsActivity.class));
            finish();
        });
        navNotifications.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, NotificationsActivity.class));
            finish();
        });
        navProfile.setOnClickListener(v -> {});
    }

    private void setupDeviceId() {
        tvDeviceId = findViewById(R.id.tvDeviceId);
        tvDeviceId.setText(getString(R.string.device_id, deviceId != null ? deviceId : "-"));
    }

    private void updateProfileHeader() {
        if (tvUserName == null) tvUserName = findViewById(R.id.tvUserName);
        if (tvUserEmail == null) tvUserEmail = findViewById(R.id.tvUserEmail);

        if (TextUtils.isEmpty(deviceId)) {
            tvUserName.setText("");
            tvUserEmail.setText("");
            showPlaceholderImage();
            return;
        }

        db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        tvUserName.setText(doc.getString("display_name"));
                        tvUserEmail.setText(doc.getString("email"));

                        String imageId = doc.getString("profile_image_id");
                        if (imageId != null && !imageId.isEmpty()) {
                            loadAndSetProfileImage(imageId);
                        } else {
                            showPlaceholderImage();
                        }
                    } else {
                        tvUserName.setText("");
                        tvUserEmail.setText("");
                        showPlaceholderImage();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data from Firestore", e);
                    tvUserName.setText("");
                    tvUserEmail.setText("");
                    showPlaceholderImage();
                });
    }

    private void loadAndSetProfileImage(String imageId) {
        db.collection("images").document(imageId).get().addOnSuccessListener(imageDoc -> {
            if (imageDoc.exists()) {
                String base64Image = imageDoc.getString("storage_path");
                if (base64Image != null && !base64Image.isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        ivProfile.setImageTintList(null);
                        Glide.with(ProfileActivity.this)
                                .load(decodedString)
                                .apply(RequestOptions.circleCropTransform())
                                .into(ivProfile);
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
        ivProfile.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ko_teal)));
        Glide.with(this).load(R.drawable.ic_person).apply(RequestOptions.circleCropTransform()).into(ivProfile);
    }
}
