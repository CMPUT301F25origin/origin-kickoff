/*
 * One-time setup gate that routes users to SignUp or Main based on device registration.
 * Performs a Firestore lookup using the device ID and finishes itself after navigation.
 */
package ca.team.originkickoff.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import ca.team.originkickoff.MainActivity;
import ca.team.originkickoff.R;
import ca.team.originkickoff.SignUpActivity;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Initial setup activity that decides whether the user needs to sign up or can proceed.
 */
public class SetupActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    /**
     * Checks for an existing user mapped to the device ID and navigates accordingly.
     *
     * @param savedInstanceState prior state bundle
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        db = FirebaseFirestore.getInstance();

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No user found, navigate to SignUpActivity
                        startActivity(new Intent(this, SignUpActivity.class));
                    } else {
                        // User found, navigate to MainActivity
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish(); // Finish SetupActivity so it's not in the back stack
                })
                .addOnFailureListener(e -> {
                    // In case of error, you might want to default to sign-up
                    startActivity(new Intent(this, SignUpActivity.class));
                    finish();
                });
    }
}
