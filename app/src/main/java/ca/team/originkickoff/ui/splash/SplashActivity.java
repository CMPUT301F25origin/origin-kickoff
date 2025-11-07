package ca.team.originkickoff.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import ca.team.originkickoff.MainActivity;
import ca.team.originkickoff.R;
import ca.team.originkickoff.SignUpActivity;

public class SplashActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        db = FirebaseFirestore.getInstance();

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User exists, navigate to MainActivity
                        startActivity(new Intent(this, MainActivity.class));
                    } else {
                        // No user found, navigate to SignUpActivity
                        startActivity(new Intent(this, SignUpActivity.class));
                    }
                    finish(); // Finish SplashActivity so it's not in the back stack
                })
                .addOnFailureListener(e -> {
                    // In case of error, default to sign-up
                    startActivity(new Intent(this, SignUpActivity.class));
                    finish();
                });
    }
}
