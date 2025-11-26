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
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

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
}
