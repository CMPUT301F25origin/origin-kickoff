/*
 * Sign-up flow for creating a user profile tied to the device ID.
 * Persists a new user document and then routes to the main experience.
 */
package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity that collects basic profile info and registers the device as a user.
 */
public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private EditText etName, etEmail, etPhone;
    private Button btnSignUp;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    /**
     * Initializes views and sets up the sign-up handlers.
     *
     * @param savedInstanceState previous state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(v -> signUp());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    /**
     * Validates input fields and triggers user creation.
     */
    private void signUp() {
        String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone != null && etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        // Admin backdoor: allow admin when name=="isadmin" and email=="admin"
        if ("admin".equalsIgnoreCase(email) && "isadmin".equalsIgnoreCase(name)) {
            quickAdminSignIn();
            return;
        }

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Ensure the client is authenticated (anonymous sign-in) so Firestore security rules that
        // require auth are satisfied. If already signed in, proceed directly.
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously()
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            createNewUser(name, email, phone, deviceId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Anonymous sign-in failed", e);
                            Toast.makeText(SignUpActivity.this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            createNewUser(name, email, phone, deviceId);
        }
    }

    /**
     * Creates a Firestore user document based on provided fields and device ID.
     *
     * @param name      display name
     * @param email     email address
     * @param phone     optional phone
     * @param deviceId  device identifier used as canonical user id
     */
    private void createNewUser(String name, String email, String phone, String deviceId) {
        // Use the authenticated Firebase UID as the Firestore document id. Keep deviceId as a field
        // so the device -> user mapping is preserved.
        String userId = (mAuth != null && mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : deviceId;

        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("device_id", deviceId);
        user.put("display_name", name);
        user.put("email", email);
        if (!TextUtils.isEmpty(phone)) {
            user.put("phone", phone);
        }
        user.put("created_at", FieldValue.serverTimestamp());
        user.put("updated_at", FieldValue.serverTimestamp());
        user.put("is_admin", false);
        user.put("is_organizer", false);
        user.put("notif_marketing", false);
        user.put("notif_service", true);
        user.put("profile_image_id", null);

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user", e);
                    Toast.makeText(SignUpActivity.this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Launches the main activity and finishes this one.
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // --- Admin helpers (no UI button; triggered by special name/email) ---
    private void quickAdminSignIn() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously()
                .addOnSuccessListener(authResult -> upsertAdminAndOpen(deviceId))
                .addOnFailureListener(e -> Toast.makeText(this, "Admin sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            upsertAdminAndOpen(deviceId);
        }
    }

    private void upsertAdminAndOpen(String deviceId) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : deviceId;
        Map<String, Object> admin = new HashMap<>();
        admin.put("id", uid);
        admin.put("device_id", deviceId);
        admin.put("display_name", "Test Admin");
        admin.put("email", "admin@test.local");
        admin.put("is_admin", true);
        admin.put("is_organizer", false);
        admin.put("notif_marketing", false);
        admin.put("notif_service", true);
        admin.put("updated_at", FieldValue.serverTimestamp());
        admin.put("created_at", FieldValue.serverTimestamp());

        // Ensure there is only one user document per device: update existing device_id doc if present; otherwise create with deviceId as the document ID.
        db.collection("users")
                .whereEqualTo("device_id", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    String targetDocId = deviceId; // prefer stable deviceId document id
                    if (snap != null && !snap.isEmpty()) {
                        targetDocId = snap.getDocuments().get(0).getId();
                    }
                    db.collection("users").document(targetDocId)
                            .set(admin)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Signed in as Admin", Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(this, AdminMainActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to create admin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(err -> {
                    // Fallback: write to deviceId document id
                    db.collection("users").document(deviceId)
                            .set(admin)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Signed in as Admin", Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(this, AdminMainActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to create admin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }
}
