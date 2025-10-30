package ca.team.originkickoff;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

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

        // Set up click listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Add Event button (plus icon)
        ImageView ivAddEvent = findViewById(R.id.ivAddEvent);
        ivAddEvent.setOnClickListener(v -> {
            Toast.makeText(this, "Add Event clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Add Event button clicked");
        });

        // Scan QR button
        Button btnScanQR = findViewById(R.id.btnScanQR);
        btnScanQR.setOnClickListener(v -> {
            Toast.makeText(this, "Scan QR clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Scan QR button clicked");
        });

        // Bottom Navigation
        LinearLayout navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Home navigation clicked");
        });

        LinearLayout navEvents = findViewById(R.id.navEvents);
        navEvents.setOnClickListener(v -> {
            Toast.makeText(this, "My Events clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "My Events navigation clicked");
        });

        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        navNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Notifications navigation clicked");
        });

        LinearLayout navProfile = findViewById(R.id.navProfile);
        navProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Profile navigation clicked");
        });

        // Event Cards - Add IDs to the cards in the layout first
        // We'll need to update the layout to add IDs to make them clickable
        setupEventCardListeners();
    }

    private void setupEventCardListeners() {
        // Event Card 1 - Tech Conference 2024
        LinearLayout eventCard1 = findViewById(R.id.eventCard1);
        eventCard1.setOnClickListener(v -> {
            Toast.makeText(this, "Tech Conference 2024 clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Event Card 1 clicked - Tech Conference 2024");
        });

        // Event Card 2 - Music Festival
        LinearLayout eventCard2 = findViewById(R.id.eventCard2);
        eventCard2.setOnClickListener(v -> {
            Toast.makeText(this, "Music Festival clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Event Card 2 clicked - Music Festival");
        });

        // Event Card 3 - Art Exhibition
        LinearLayout eventCard3 = findViewById(R.id.eventCard3);
        eventCard3.setOnClickListener(v -> {
            Toast.makeText(this, "Art Exhibition clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Event Card 3 clicked - Art Exhibition");
        });
    }
}