package ca.team.originkickoff;

import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ca.team.originkickoff.adapters.EventAdapter;
import ca.team.originkickoff.models.Event;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseFirestore db;
    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;

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

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up click listeners
        setupClickListeners();

        // Load events from Firestore
        loadEventsFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload events when returning to this activity
        loadEventsFromFirestore();
    }

    private void setupRecyclerView() {
        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(event -> {
            // Handle event click - you can open event details here
            Toast.makeText(MainActivity.this, "Clicked: " + event.getName(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Event clicked: " + event.getName());
            // TODO: Open EventDetailsActivity
        });

        rvEvents.setAdapter(eventAdapter);
    }

    private void loadEventsFromFirestore() {
        Log.d(TAG, "Loading events from Firestore...");

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = new Event();
                            event.setId(document.getId());
                            event.setName(document.getString("name"));
                            event.setDescription(document.getString("description"));
                            event.setOrganizerId(document.getString("organizerId"));
                            event.setOrganizerName(document.getString("organizerName"));
                            event.setLocation(document.getString("location"));
                            event.setPosterUrl(document.getString("posterUrl"));

                            // Handle numeric fields
                            Long capacity = document.getLong("capacity");
                            event.setCapacity(capacity != null ? capacity.intValue() : 0);

                            Long waitlistCount = document.getLong("waitlistCount");
                            event.setWaitlistCount(waitlistCount != null ? waitlistCount.intValue() : 0);

                            Double price = document.getDouble("price");
                            event.setPrice(price != null ? price : 0.0);

                            Long createdAt = document.getLong("createdAt");
                            event.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());

                            // Handle boolean fields
                            Boolean geolocationRequired = document.getBoolean("geolocationRequired");
                            event.setGeolocationRequired(geolocationRequired != null ? geolocationRequired : false);

                            // Handle date fields
                            com.google.firebase.Timestamp eventDateTimestamp = document.getTimestamp("eventDate");
                            if (eventDateTimestamp != null) {
                                event.setEventDate(eventDateTimestamp.toDate());
                            }

                            com.google.firebase.Timestamp regStartTimestamp = document.getTimestamp("registrationStartTime");
                            if (regStartTimestamp != null) {
                                event.setRegistrationStartTime(regStartTimestamp.toDate());
                            }

                            com.google.firebase.Timestamp regEndTimestamp = document.getTimestamp("registrationEndTime");
                            if (regEndTimestamp != null) {
                                event.setRegistrationEndTime(regEndTimestamp.toDate());
                            }

                            events.add(event);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event document: " + document.getId(), e);
                        }
                    }

                    Log.d(TAG, "Loaded " + events.size() + " events from Firestore");
                    eventAdapter.setEvents(events);

                    if (events.isEmpty()) {
                        Toast.makeText(this, "No events available", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events from Firestore", e);
                    Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        // Add Event button (plus icon)
        ImageView ivAddEvent = findViewById(R.id.ivAddEvent);
        ivAddEvent.setOnClickListener(v -> {
            // Open CreateEventActivity
            Intent intent = new Intent(MainActivity.this, CreateEventActivity.class);
            startActivity(intent);
            Log.d(TAG, "Add Event button clicked - opening CreateEventActivity");
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
    }
}