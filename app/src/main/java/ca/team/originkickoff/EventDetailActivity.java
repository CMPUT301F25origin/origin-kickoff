package ca.team.originkickoff;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import ca.team.originkickoff.models.Event;

public class EventDetailActivity extends AppCompatActivity {
    private static final String TAG = "EventDetailActivity";
    public static final String EXTRA_EVENT_ID = "event_id";

    private ImageView posterImage;
    private TextView textTitle;
    private TextView textOrganizer;
    private TextView textDate;
    private TextView textLocationTitle;
    private TextView textLocationSubtitle;
    private TextView pillTotalEntrants;
    private TextView pillSpotsLeft;
    private TextView pillToBeSelected;
    private Button btnJoinWaitingList;
    private Button btnLotteryCriteria;
    private ImageView ivQrCode;
    private LinearLayout qrCodeSection;

    private FirebaseFirestore db;
    private String eventId;
    private Event currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get event ID from intent
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Error: Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Load event data
        loadEventData();
    }

    private void initializeViews() {
        posterImage = findViewById(R.id.posterImage);
        textTitle = findViewById(R.id.textTitle);
        textOrganizer = findViewById(R.id.textOrganizer);
        textDate = findViewById(R.id.textDate);
        textLocationTitle = findViewById(R.id.textLocationTitle);
        textLocationSubtitle = findViewById(R.id.textLocationSubtitle);
        pillTotalEntrants = findViewById(R.id.pillTotalEntrants);
        pillSpotsLeft = findViewById(R.id.pillSpotsLeft);
        pillToBeSelected = findViewById(R.id.pillToBeSelected);
        btnJoinWaitingList = findViewById(R.id.btnJoinWaitingList);
        btnLotteryCriteria = findViewById(R.id.btnLotteryCriteria);
        ivQrCode = findViewById(R.id.ivQrCode);
        qrCodeSection = findViewById(R.id.qrCodeSection);
    }

    private void setupListeners() {
        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Action buttons
        btnJoinWaitingList.setOnClickListener(v -> {
            if (currentEvent != null) {
                joinWaitingList();
            }
        });

        btnLotteryCriteria.setOnClickListener(v -> {
            if (currentEvent != null) {
                openLotteryCriteria();
            }
        });

        // Bottom navigation
        LinearLayout navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        LinearLayout navEvents = findViewById(R.id.navEvents);
        navEvents.setOnClickListener(v -> {
            Toast.makeText(this, "My Events", Toast.LENGTH_SHORT).show();
        });

        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        navNotifications.setOnClickListener(v -> {
            Intent i = new Intent(this, NotificationsActivity.class);
            startActivity(i);
        });

        LinearLayout navProfile = findViewById(R.id.navProfile);
        navProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadEventData() {
        Log.d(TAG, "Loading event data for ID: " + eventId);

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            currentEvent = new Event();
                            currentEvent.setId(documentSnapshot.getId());
                            currentEvent.setName(documentSnapshot.getString("name"));
                            currentEvent.setDescription(documentSnapshot.getString("description"));
                            currentEvent.setOrganizerId(documentSnapshot.getString("organizerId"));
                            currentEvent.setOrganizerName(documentSnapshot.getString("organizerName"));
                            currentEvent.setLocation(documentSnapshot.getString("location"));
                            currentEvent.setPosterUrl(documentSnapshot.getString("posterUrl"));
                            currentEvent.setQrCodeBase64(documentSnapshot.getString("qrCodeBase64"));

                            // Handle numeric fields
                            Long capacity = documentSnapshot.getLong("capacity");
                            currentEvent.setCapacity(capacity != null ? capacity.intValue() : 0);

                            Long waitlistCount = documentSnapshot.getLong("waitlistCount");
                            currentEvent.setWaitlistCount(waitlistCount != null ? waitlistCount.intValue() : 0);

                            Double price = documentSnapshot.getDouble("price");
                            currentEvent.setPrice(price != null ? price : 0.0);

                            Long createdAt = documentSnapshot.getLong("createdAt");
                            currentEvent.setCreatedAt(createdAt != null ? createdAt : 0L);

                            // Handle boolean fields
                            Boolean geolocationRequired = documentSnapshot.getBoolean("geolocationRequired");
                            currentEvent.setGeolocationRequired(geolocationRequired != null && geolocationRequired);

                            // Handle timestamp fields
                            com.google.firebase.Timestamp regStart = documentSnapshot.getTimestamp("registrationStartTime");
                            if (regStart != null) {
                                currentEvent.setRegistrationStartTime(regStart.toDate());
                            }

                            com.google.firebase.Timestamp regEnd = documentSnapshot.getTimestamp("registrationEndTime");
                            if (regEnd != null) {
                                currentEvent.setRegistrationEndTime(regEnd.toDate());
                            }

                            // Update UI
                            updateUI();

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event data", e);
                            Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event", e);
                    Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUI() {
        if (currentEvent == null) return;

        // Set title and organizer
        textTitle.setText(currentEvent.getName());
        textOrganizer.setText("Organized by: " + currentEvent.getOrganizerName());

        // Set location
        textLocationTitle.setText(currentEvent.getLocation());
        textLocationSubtitle.setText("Event Location"); // You can parse this from location string

        // Calculate statistics
        int totalEntrants = currentEvent.getWaitlistCount();
        int spotsLeft = currentEvent.getCapacity() - currentEvent.getWaitlistCount();
        if (spotsLeft < 0) spotsLeft = 0;
        int toBeSelected = currentEvent.getCapacity();

        pillTotalEntrants.setText("Total Entrants: " + totalEntrants);
        pillSpotsLeft.setText("Spots left: " + spotsLeft);
        pillToBeSelected.setText("To be selected: " + toBeSelected);

        // Set date (you can format this based on your needs)
        if (currentEvent.getRegistrationStartTime() != null) {
            textDate.setText("Registration Open"); // Format the date as needed
        }

        // Show QR code if it exists
        if (currentEvent.getQrCodeBase64() != null && !currentEvent.getQrCodeBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(currentEvent.getQrCodeBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivQrCode.setImageBitmap(decodedByte);
                qrCodeSection.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding Base64 QR code", e);
                qrCodeSection.setVisibility(View.GONE);
            }
        } else {
            qrCodeSection.setVisibility(View.GONE);
        }

        // TODO: Load poster image using Glide or Picasso
        // Glide.with(this).load(currentEvent.getPosterUrl()).into(posterImage);
    }

    private void joinWaitingList() {
        Toast.makeText(this, "Joining waiting list for: " + currentEvent.getName(), Toast.LENGTH_SHORT).show();

        // TODO: Implement join waiting list logic
        // 1. Check if user is already on the waiting list
        // 2. Check if geolocation is required and verify location
        // 3. Add user to the waiting list in Firebase
        // 4. Update the UI
    }

    private void openLotteryCriteria() {
        Toast.makeText(this, "Opening lottery criteria", Toast.LENGTH_SHORT).show();

        // TODO: Navigate to lottery criteria screen or show dialog
    }
}
