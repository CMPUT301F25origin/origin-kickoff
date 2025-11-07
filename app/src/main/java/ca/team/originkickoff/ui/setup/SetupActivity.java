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

public class SetupActivity extends AppCompatActivity {

    private FirebaseFirestore db;

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
