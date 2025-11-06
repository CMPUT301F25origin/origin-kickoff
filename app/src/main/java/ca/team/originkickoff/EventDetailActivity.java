package ca.team.originkickoff;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.button.MaterialButton;

import ca.team.originkickoff.data.repository.UserRepository;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.models.User;
import ca.team.originkickoff.services.WaitingListService;
import ca.team.originkickoff.WaitingListActivity; // explicit import

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
    private MaterialButton btnJoinWaitingList;
    private MaterialButton btnLotteryCriteria;
    private ImageView ivQrCode;
    private LinearLayout qrCodeSection;
    private CardView locationCard;
    private ImageView imageMapPreview;

    private FirebaseFirestore db;
    private String eventId;
    private Event currentEvent;

    private final WaitingListService waitingListService = new WaitingListService();
    private final UserRepository userRepository = new UserRepository();
    private User currentUser; // resolved from device_id

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

        // Resolve user from device ID
        resolveCurrentUser();

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
        locationCard = findViewById(R.id.locationCard);
        imageMapPreview = findViewById(R.id.imageMapPreview);
    }

    private void setupListeners() {
        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Action buttons
        btnJoinWaitingList.setOnClickListener(v -> {
            if (currentEvent != null && currentUser != null) {
                toggleJoinLeave();
            } else {
                Toast.makeText(this, "Loading user...", Toast.LENGTH_SHORT).show();
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
        navEvents.setOnClickListener(v -> Toast.makeText(this, "My Events", Toast.LENGTH_SHORT).show());

        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        navNotifications.setOnClickListener(v -> {
            Intent i = new Intent(this, NotificationsActivity.class);
            startActivity(i);
        });

        LinearLayout navProfile = findViewById(R.id.navProfile);
        navProfile.setOnClickListener(v -> Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show());

        // Location card click
        locationCard.setOnClickListener(v -> {
            if (currentEvent != null && currentEvent.getLocation() != null) {
                openMapPreview();
            }
        });
    }

    private void resolveCurrentUser() {
        // Use ANDROID_ID as device id source
        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        userRepository.findUserByDeviceId(deviceId).observe(this, user -> {
            currentUser = user;
            // After we know user, refresh join button label
            refreshJoinButton();
        });
    }

    private void refreshJoinButton() {
        if (currentEvent == null || currentUser == null) return;
        waitingListService.isOnWaitlist(currentEvent.getId(), currentUser.getId())
                .addOnSuccessListener(isOn -> {
                    updateJoinLeaveButtonStyle(isOn);
                });
    }

    private void updateJoinLeaveButtonStyle(boolean isOnList) {
        if (isOnList) {
            btnJoinWaitingList.setText("Leave Waiting List");
            btnJoinWaitingList.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF3B30"))); // red
            btnJoinWaitingList.setTextColor(Color.WHITE);
        } else {
            btnJoinWaitingList.setText("Join Waiting List");
            btnJoinWaitingList.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DE8C0"))); // teal
            btnJoinWaitingList.setTextColor(Color.parseColor("#003932"));
        }
    }

    private void toggleJoinLeave() {
        String eventId = currentEvent.getId();
        String userId = currentUser.getId();
        waitingListService.isOnWaitlist(eventId, userId)
                .addOnSuccessListener(isOn -> {
                    if (isOn) {
                        waitingListService.leave(eventId, userId)
                                .addOnSuccessListener(changed -> {
                                    if (changed) Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                                    currentEvent.setWaitlistCount(Math.max(0, currentEvent.getWaitlistCount() - (changed ? 1 : 0)));
                                    updateUI();
                                    updateJoinLeaveButtonStyle(false);
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        showJoinConfirmationDialog(eventId, userId);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showJoinConfirmationDialog(String eventId, String userId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(R.layout.dialog_join_waitlist, null, false);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(content);
        // Make background transparent so our card keeps rounded corners
        android.widget.FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) sheet.setBackgroundResource(android.R.color.transparent);
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        content.findViewById(R.id.btnYes).setOnClickListener(v -> {
            dialog.dismiss();
            doJoin(eventId, userId);
        });
        content.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void doJoin(String eventId, String userId) {
        waitingListService.join(eventId, userId, false, null, null, null, "list")
                .addOnSuccessListener(changed -> {
                    if (changed) Toast.makeText(this, "Joined waiting list", Toast.LENGTH_SHORT).show();
                    currentEvent.setWaitlistCount(currentEvent.getWaitlistCount() + (changed ? 1 : 0));
                    updateUI();
                    updateJoinLeaveButtonStyle(true);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                            currentEvent.setPosterBase64(documentSnapshot.getString("posterBase64"));

                            // Load location coordinates
                            Double latitude = documentSnapshot.getDouble("locationLatitude");
                            Double longitude = documentSnapshot.getDouble("locationLongitude");
                            String placeId = documentSnapshot.getString("locationPlaceId");

                            if (latitude != null) {
                                currentEvent.setLocationLatitude(latitude);
                            }
                            if (longitude != null) {
                                currentEvent.setLocationLongitude(longitude);
                            }
                            if (placeId != null) {
                                currentEvent.setLocationPlaceId(placeId);
                            }

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
                            refreshJoinButton();

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

        // Load poster image from base64
        if (currentEvent.getPosterBase64() != null && !currentEvent.getPosterBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(currentEvent.getPosterBase64(), Base64.DEFAULT);
                Bitmap posterBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                posterImage.setImageBitmap(posterBitmap);
                Log.d(TAG, "Poster image loaded from base64");
            } catch (Exception e) {
                Log.e(TAG, "Error decoding Base64 poster image", e);
                // Keep default placeholder if decoding fails
            }
        } else {
            Log.d(TAG, "No base64 poster image available");
            // Keep default placeholder
        }

        // When organizer views, show button to open waiting list entrants screen
        if (currentUser != null && currentEvent != null && currentUser.isOrganizer() &&
                currentEvent.getOrganizerId() != null && currentEvent.getOrganizerId().equals(currentUser.getId())) {
            // repurpose second button for viewing list
            btnLotteryCriteria.setText("View Waiting List");
            btnLotteryCriteria.setOnClickListener(v -> {
                Intent i = new Intent(this, ca.team.originkickoff.WaitingListActivity.class);
                i.putExtra(ca.team.originkickoff.WaitingListActivity.EXTRA_EVENT_ID, currentEvent.getId());
                startActivity(i);
            });
        }
        // TODO: Load map preview image
        // imageMapPreview.setImageBitmap(...);
    }

    private void openLotteryCriteria() {
        Toast.makeText(this, "Opening lottery criteria", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to lottery criteria screen or show dialog
    }

    private void openMapPreview() {
        if (currentEvent.getLocationLatitude() != 0.0 && currentEvent.getLocationLongitude() != 0.0) {
            // Open full-screen map viewer
            Intent intent = new Intent(this, MapViewActivity.class);
            intent.putExtra(MapViewActivity.EXTRA_LATITUDE, currentEvent.getLocationLatitude());
            intent.putExtra(MapViewActivity.EXTRA_LONGITUDE, currentEvent.getLocationLongitude());
            intent.putExtra(MapViewActivity.EXTRA_LOCATION_NAME, currentEvent.getLocation());
            startActivity(intent);
        } else {
            // Fallback: Open location in Google Maps using address search
            String uri = "geo:0,0?q=" + Uri.encode(currentEvent.getLocation());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // If Google Maps not installed, open in browser
                String browserUri = "https://www.google.com/maps/search/?api=1&query=" +
                        Uri.encode(currentEvent.getLocation());
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri));
                startActivity(browserIntent);
            }
        }
    }
}
