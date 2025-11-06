package ca.team.originkickoff;

import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private EditText etName, etEmail, etPhone;
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editProfileRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        if (TextUtils.isEmpty(deviceId)) {
            Toast.makeText(this, "Cannot get device ID. Profile cannot be loaded or saved.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadUserData();

        View btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());

        MaterialButton btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> onSave());

        findViewById(R.id.btnEditPicture).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.edit_profile_picture), Toast.LENGTH_SHORT).show());
    }

    private void loadUserData() {
        db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String name = documentSnapshot.getString("display_name");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone");

                        etName.setText(name);
                        etEmail.setText(email);
                        etPhone.setText(phone);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void onSave() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Required");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            etEmail.requestFocus();
            return;
        }

        // Find the document with the matching device_id
        db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // A document with this device_id exists, get its actual ID and update it.
                            String docIdToUpdate = task.getResult().getDocuments().get(0).getId();

                            Map<String, Object> user = new HashMap<>();
                            user.put("display_name", name);
                            user.put("email", email);
                            user.put("phone", phone);
                            user.put("device_id", deviceId); // ensure device_id is consistent
                            user.put("updated_at", com.google.firebase.firestore.FieldValue.serverTimestamp());

                            db.collection("users").document(docIdToUpdate).set(user, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating profile", e);
                                        Toast.makeText(EditProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            // This case should not happen based on your app's logic (doc is created on launch).
                            // We will not create a new document here to prevent duplicates.
                            Log.e(TAG, "onSave: No existing document found for device_id: " + deviceId);
                            Toast.makeText(EditProfileActivity.this, "Error: Could not find your profile to update.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "Error finding user document for update", task.getException());
                        Toast.makeText(EditProfileActivity.this, "Failed to find profile: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
