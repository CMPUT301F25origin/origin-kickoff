package ca.team.originkickoff;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import ca.team.originkickoff.ui.fragments.EventListFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase and test connection
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Test Firestore connection
            db.collection("events").limit(1).get()
                    .addOnSuccessListener(querySnapshot -> {
                        Log.d(TAG, "Firestore connection successful. Document count: " + querySnapshot.size());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firestore connection failed: " + e.getMessage(), e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Firebase error: " + e.getMessage(), e);
        }

        // Load EventListFragment if not already loaded
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new EventListFragment())
                    .commit();
        }
    }
}