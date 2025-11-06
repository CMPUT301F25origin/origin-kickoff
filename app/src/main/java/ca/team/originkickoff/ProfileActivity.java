package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private SwitchMaterial switchWon;
    private SwitchMaterial switchLost;
    private TextView tvDeviceId;
    private TextView tvUserName;
    private TextView tvUserEmail;

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

        setupTopBar();
        setupToggles();
        setupButtons();
        setupBottomBar();
        setupDeviceId();
        updateProfileHeader();
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
        switchWon = findViewById(R.id.switchWon);
        switchLost = findViewById(R.id.switchLost);

        boolean won = getSharedPreferences("profile", MODE_PRIVATE).getBoolean("won_updates", true);
        boolean lost = getSharedPreferences("profile", MODE_PRIVATE).getBoolean("lost_updates", true);
        switchWon.setChecked(won);
        switchLost.setChecked(lost);

        switchWon.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences("profile", MODE_PRIVATE).edit().putBoolean("won_updates", isChecked).apply());
        switchLost.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences("profile", MODE_PRIVATE).edit().putBoolean("lost_updates", isChecked).apply());
    }

    private void setupButtons() {
        MaterialButton btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile Data")
                .setMessage("Are you sure you want to clear your profile data? Your profile will be reset.")
                .setPositiveButton("Clear Data", (dialog, which) -> clearUserData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearUserData() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (TextUtils.isEmpty(deviceId)) {
            Toast.makeText(this, "Error: Could not get device ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String docId = task.getResult().getDocuments().get(0).getId();

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("display_name", "");
                        updates.put("email", null);
                        updates.put("phone", null);
                        updates.put("is_admin", false);
                        updates.put("is_organizer", false);
                        updates.put("notif_marketing", false);
                        updates.put("notif_service", true); // Reset to default
                        updates.put("updated_at", FieldValue.serverTimestamp());

                        FirebaseFirestore.getInstance().collection("users").document(docId).update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ProfileActivity.this, "Profile data cleared.", Toast.LENGTH_SHORT).show();
                                    getSharedPreferences("profile", MODE_PRIVATE).edit().clear().apply();
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
        navEvents.setOnClickListener(v -> Toast.makeText(this, "My Events coming soon", Toast.LENGTH_SHORT).show());
        navNotifications.setOnClickListener(v -> Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show());
        navProfile.setOnClickListener(v -> {}); // already here
    }

    private void setupDeviceId() {
        tvDeviceId = findViewById(R.id.tvDeviceId);
        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        tvDeviceId.setText(getString(R.string.device_id, id != null ? id : "-"));
    }

    private void updateProfileHeader() {
        if (tvUserName == null) tvUserName = findViewById(R.id.tvUserName);
        if (tvUserEmail == null) tvUserEmail = findViewById(R.id.tvUserEmail);

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (TextUtils.isEmpty(deviceId)) {
            tvUserName.setText("");
            tvUserEmail.setText("");
            return;
        }

        FirebaseFirestore.getInstance().collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String name = doc.getString("display_name");
                        String email = doc.getString("email");

                        tvUserName.setText(name);
                        tvUserEmail.setText(email);
                    } else {
                        tvUserName.setText("");
                        tvUserEmail.setText("");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data from Firestore", e);
                    tvUserName.setText("");
                    tvUserEmail.setText("");
                });
    }
}
