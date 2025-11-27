/*
 * Launch-time router that decides whether to open Main or SignUp screens.
 * Supports both legacy doc ID and field-based device mapping in Firestore.
 */

package ca.team.originkickoff.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import ca.team.originkickoff.AdminEventsActivity;
import ca.team.originkickoff.MainActivity;
import ca.team.originkickoff.R;
import ca.team.originkickoff.SignUpActivity;

/**
 * Splash screen activity responsible for quick user routing based on registration status.
 */
public class SplashActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    /**
     * Determines the next screen by checking for an existing user record for this device.
     *
     * @param savedInstanceState prior state bundle
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        db = FirebaseFirestore.getInstance();

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // First try legacy approach: document id == deviceId
        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(legacyDoc -> {
                    if (legacyDoc.exists()) {
                        // Check if user is admin
                        Boolean isAdmin = legacyDoc.getBoolean("is_admin");
                        if (isAdmin != null && isAdmin) {
                            startActivity(new Intent(this, AdminEventsActivity.class));
                        } else {
                            startActivity(new Intent(this, MainActivity.class));
                        }
                        finish();
                    } else {
                        // Fallback: query by device_id field (new format where doc id != device id)
                        db.collection("users")
                                .whereEqualTo("device_id", deviceId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnapshots -> {
                                    if (!querySnapshots.isEmpty()) {
                                        // Check if user is admin
                                        Boolean isAdmin = querySnapshots.getDocuments().get(0).getBoolean("is_admin");
                                        if (isAdmin != null && isAdmin) {
                                            startActivity(new Intent(this, AdminEventsActivity.class));
                                        } else {
                                            startActivity(new Intent(this, MainActivity.class));
                                        }
                                    } else {
                                        startActivity(new Intent(this, SignUpActivity.class));
                                    }
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    startActivity(new Intent(this, SignUpActivity.class));
                                    finish();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // In case of error, fallback to field query
                    db.collection("users")
                            .whereEqualTo("device_id", deviceId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(querySnapshots -> {
                                if (!querySnapshots.isEmpty()) {
                                    // Check if user is admin
                                    Boolean isAdmin = querySnapshots.getDocuments().get(0).getBoolean("is_admin");
                                    if (isAdmin != null && isAdmin) {
                                        startActivity(new Intent(this, AdminEventsActivity.class));
                                    } else {
                                        startActivity(new Intent(this, MainActivity.class));
                                    }
                                } else {
                                    startActivity(new Intent(this, SignUpActivity.class));
                                }
                                finish();
                            })
                            .addOnFailureListener(err -> {
                                startActivity(new Intent(this, SignUpActivity.class));
                                finish();
                            });
                });
    }
}
