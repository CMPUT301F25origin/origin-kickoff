package ca.team.originkickoff;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import ca.team.originkickoff.data.repository.UserRepository;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.models.User;
import ca.team.originkickoff.services.WaitingListService;

public class EventDetailActivity extends AppCompatActivity {
    private static final String TAG = "EventDetailActivity";
    public static final String EXTRA_EVENT_ID = "event_id";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

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
    private MaterialButton btnManageNotifications;
    private ImageView ivQrCode;
    private LinearLayout qrCodeSection;
    private CardView locationCard;
    private ImageView imageMapPreview;
    private ImageView btnEdit;

    private FirebaseFirestore db;
    private String eventId;
    private Event currentEvent;
    private String eventLotteryCriteria = null;

    private final WaitingListService waitingListService = new WaitingListService();
    private final UserRepository userRepository = new UserRepository();
    private User currentUser; // resolved from device_id
    private boolean isOrganizer = false;
    private FusedLocationProviderClient fusedLocationClient;

    // Debounce for bottom-nav taps
    private long lastNavTapAtMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
        btnManageNotifications = findViewById(R.id.btnManageNotifications);
        ivQrCode = findViewById(R.id.ivQrCode);
        qrCodeSection = findViewById(R.id.qrCodeSection);
        locationCard = findViewById(R.id.locationCard);
        imageMapPreview = findViewById(R.id.imageMapPreview);
        btnEdit = findViewById(R.id.btnEdit);

        // Safety: ensure lottery button is enabled and clickable and not obscured
        if (btnLotteryCriteria == null) {
            Log.e(TAG, "btnLotteryCriteria view not found (null)");
        } else {
            btnLotteryCriteria.setEnabled(true);
            btnLotteryCriteria.setClickable(true);
            btnLotteryCriteria.setVisibility(View.VISIBLE);
            // bring to front in case other views are overlaying - post to ensure after layout
            btnLotteryCriteria.post(() -> {
                try {
                    btnLotteryCriteria.bringToFront();
                    btnLotteryCriteria.invalidate();
                    Log.d(TAG, "btnLotteryCriteria brought to front (post)");
                } catch (Exception ex) {
                    Log.w(TAG, "Could not bring btnLotteryCriteria to front: " + ex.getMessage());
                }
            });

            // Add onTouch listener to detect any touch events reaching the view
            btnLotteryCriteria.setOnTouchListener((v, motionEvent) -> {
                try {
                    int action = motionEvent.getAction();
                    Log.d(TAG, "btnLotteryCriteria onTouch event: action=" + action);
                    // If this was a tap release, trigger performClick so accessibility/click handling runs
                    if (action == android.view.MotionEvent.ACTION_UP) {
                        // ensure normal click handling and accessibility fire
                        v.performClick();
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "onTouch handler exception: " + ex.getMessage());
                }
                // Return false so normal click handling also occurs
                return false;
            });

            // Fallback: attach a direct click listener here so the button will always provide feedback
            // even if other code later overwrites listeners. Organizer mode will override this when needed.
            btnLotteryCriteria.setOnClickListener(v -> {
                Log.d(TAG, "btnLotteryCriteria fallback click handler invoked");
                // Only open criteria if not in organizer mode (organizer mode sets its own behavior later)
                if (!isOrganizer) {
                    openLotteryCriteria();
                } else {
                    // organizer clicks should go to manage lottery
                    openManageLottery();
                }
            });

            // Ensure focusability
            btnLotteryCriteria.setFocusable(true);
            btnLotteryCriteria.setFocusableInTouchMode(true);

            // Fallback forwarder: if the surrounding container is tapped (or covered), forward events to the lottery button
            View actionContainer = findViewById(R.id.actionButtonsContainer);
            if (actionContainer != null) {
                try {
                    actionContainer.setClickable(true);
                    actionContainer.setFocusable(true);
                    actionContainer.setOnClickListener(v -> {
                        Log.d(TAG, "actionButtonsContainer clicked - forwarding to btnLotteryCriteria.performClick()");
                        btnLotteryCriteria.performClick();
                    });
                    actionContainer.setOnTouchListener((v, ev) -> {
                        if (ev.getAction() == android.view.MotionEvent.ACTION_UP) {
                            Log.d(TAG, "actionButtonsContainer onTouch ACTION_UP - forwarding");
                            btnLotteryCriteria.performClick();
                        }
                        return false;
                    });
                } catch (Exception ex) {
                    Log.w(TAG, "Could not set forwarder on actionButtonsContainer: " + ex.getMessage());
                }
            }

         Log.d(TAG, "btnLotteryCriteria initialized and set clickable/visible");
        }
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

        // Always show feedback and attempt to open criteria; openLotteryCriteria will handle missing eventId/currentEvent
        if (btnLotteryCriteria == null) {
            Log.e(TAG, "Attempted to set up listener but btnLotteryCriteria is null");
        } else {
            Log.d(TAG, "Wiring btnLotteryCriteria click listener (enabled=" + btnLotteryCriteria.isEnabled() + ", clickable=" + btnLotteryCriteria.isClickable() + ")");
            btnLotteryCriteria.setOnClickListener(v -> {
                Log.d(TAG, "btnLotteryCriteria clicked - opening criteria");
                openLotteryCriteria();
            });
        }

        // Bottom navigation
        LinearLayout navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> navigateBottomTab(MainActivity.class));

        LinearLayout navEvents = findViewById(R.id.navEvents);
        navEvents.setOnClickListener(v -> navigateBottomTab(MyEventsActivity.class));

        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        navNotifications.setOnClickListener(v -> navigateBottomTab(NotificationsActivity.class));

        LinearLayout navProfile = findViewById(R.id.navProfile);
        navProfile.setOnClickListener(v -> navigateBottomTab(ProfileActivity.class));

        // Location card click
        locationCard.setOnClickListener(v -> {
            if (currentEvent != null && currentEvent.getLocation() != null) {
                openMapPreview();
            }
        });
    }

    // Helper to navigate between bottom-bar destinations smoothly with no transition animation
    private void navigateBottomTab(Class<?> targetActivity) {
        if (targetActivity == null) return;
        if (getClass().equals(targetActivity)) return; // already on this tab
        long now = SystemClock.elapsedRealtime();
        if (now - lastNavTapAtMs < 300) return; // debounce rapid taps
        lastNavTapAtMs = now;
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void resolveCurrentUser() {
        // Use ANDROID_ID as device id source
        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        userRepository.findUserByDeviceId(deviceId).observe(this, user -> {
            currentUser = user;
            if (currentUser != null) {
                Log.d(TAG, "Current user loaded: " + currentUser.getId());
                // After user is loaded, check if we need to update the organizer view
                if (currentEvent != null) {
                    checkAndSetupOrganizerView();
                }
            }
            // After we know user, refresh join button label
            refreshJoinButton();
        });
    }

    private void refreshJoinButton() {
        if (currentEvent == null || currentUser == null) return;

        // Don't update button style if user is the organizer
        if (isOrganizer) {
            Log.d(TAG, "User is organizer, skipping join button refresh");
            return;
        }

        waitingListService.isOnWaitlist(currentEvent.getId(), currentUser.getId())
                .addOnSuccessListener(this::updateJoinLeaveButtonStyle);
    }

    private void updateJoinLeaveButtonStyle(boolean isOnList) {
        // Don't update button style if user is the organizer
        if (isOrganizer) {
            return;
        }

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
                        if (currentEvent.isGeolocationRequired()) {
                            requestLocationAndJoin();
                        } else {
                            showJoinConfirmationDialog(eventId, userId, null);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showJoinConfirmationDialog(String eventId, String userId, Location location) {
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
            doJoin(eventId, userId, location);
        });
        content.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void doJoin(String eventId, String userId, Location location) {
        Double latitude = location != null ? location.getLatitude() : null;
        Double longitude = location != null ? location.getLongitude() : null;
        boolean locationConsent = location != null;

        waitingListService.join(eventId, userId, locationConsent, latitude, longitude, null, "list")
                .addOnSuccessListener(changed -> {
                    if (changed) Toast.makeText(this, "Joined waiting list", Toast.LENGTH_SHORT).show();
                    currentEvent.setWaitlistCount(currentEvent.getWaitlistCount() + (changed ? 1 : 0));
                    updateUI();
                    updateJoinLeaveButtonStyle(true);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void requestLocationAndJoin() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationAndJoin();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndJoin();
            } else {
                Toast.makeText(this, "Location permission denied. Joining without location.", Toast.LENGTH_SHORT).show();
                showJoinConfirmationDialog(currentEvent.getId(), currentUser.getId(), null); // Proceed without location
            }
        }
    }

    private void getLocationAndJoin() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                showJoinConfirmationDialog(currentEvent.getId(), currentUser.getId(), location);
            } else {
                Toast.makeText(this, "Could not retrieve location. Joining without location.", Toast.LENGTH_SHORT).show();
                showJoinConfirmationDialog(currentEvent.getId(), currentUser.getId(), null); // Proceed without location
            }
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
                            // Cache lotteryCriteria if present on the document to avoid an extra read later
                            try {
                                Object rawLc = documentSnapshot.get("lotteryCriteria");
                                if (rawLc == null) rawLc = documentSnapshot.get("lottery_criteria");
                                Log.d(TAG, "Caching lotteryCriteria raw value=" + String.valueOf(rawLc) + " type=" + (rawLc != null ? rawLc.getClass().getName() : "null"));
                                if (rawLc instanceof String) {
                                    eventLotteryCriteria = ((String) rawLc).trim();
                                    Log.d(TAG, "Cached eventLotteryCriteria (string) length=" + (eventLotteryCriteria != null ? eventLotteryCriteria.length() : 0));
                                } else if (rawLc instanceof java.util.Map) {
                                    java.util.Map<?,?> m = (java.util.Map<?,?>) rawLc;
                                    if (m.containsKey("text")) eventLotteryCriteria = String.valueOf(m.get("text"));
                                    else if (m.containsKey("criteria")) eventLotteryCriteria = String.valueOf(m.get("criteria"));
                                    else if (m.containsKey("description")) eventLotteryCriteria = String.valueOf(m.get("description"));
                                    else {
                                        StringBuilder sb = new StringBuilder();
                                        for (java.util.Map.Entry<?,?> e : m.entrySet()) sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                                        eventLotteryCriteria = sb.toString().trim();
                                    }
                                    Log.d(TAG, "Cached eventLotteryCriteria (map) length=" + (eventLotteryCriteria != null ? eventLotteryCriteria.length() : 0));
                                } else if (rawLc instanceof java.util.List) {
                                    java.util.List<?> list = (java.util.List<?>) rawLc;
                                    StringBuilder sb = new StringBuilder();
                                    for (Object o : list) if (o != null) sb.append(o.toString()).append("\n");
                                    eventLotteryCriteria = sb.toString().trim();
                                    Log.d(TAG, "Cached eventLotteryCriteria (list) length=" + (eventLotteryCriteria != null ? eventLotteryCriteria.length() : 0));
                                } else if (rawLc != null) {
                                    eventLotteryCriteria = rawLc.toString().trim();
                                    Log.d(TAG, "Cached eventLotteryCriteria (other) length=" + (eventLotteryCriteria != null ? eventLotteryCriteria.length() : 0));
                                }
                            } catch (Exception ignore) {}
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
        textLocationSubtitle.setText("Event Location");

        // Calculate statistics
        int totalEntrants = currentEvent.getWaitlistCount();
        int spotsLeft = currentEvent.getCapacity() - currentEvent.getWaitlistCount();
        if (spotsLeft < 0) spotsLeft = 0;
        int toBeSelected = currentEvent.getCapacity();

        pillTotalEntrants.setText("Total Entrants: " + totalEntrants);
        pillSpotsLeft.setText("Spots left: " + spotsLeft);
        pillToBeSelected.setText("To be selected: " + toBeSelected);

        // Set date
        if (currentEvent.getRegistrationStartTime() != null) {
            textDate.setText("Registration Open");
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
            }
        } else {
            Log.d(TAG, "No base64 poster image available");
        }

        // Check if current user is the organizer
        checkAndSetupOrganizerView();
    }

    private void checkAndSetupOrganizerView() {
        if (currentUser != null && currentEvent != null &&
                currentEvent.getOrganizerId() != null &&
                currentEvent.getOrganizerId().equals(currentUser.getId())) {

            isOrganizer = true;
            Log.d(TAG, "Current user is the organizer - showing organizer view");

            // Show Edit button
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> openEditEvent());

            // Change buttons to organizer management buttons
            btnJoinWaitingList.setText("Manage Entrants");
            btnJoinWaitingList.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DE8C0")));
            btnJoinWaitingList.setTextColor(Color.parseColor("#003932"));
            btnJoinWaitingList.setOnClickListener(v -> openManageEntrants());

            btnLotteryCriteria.setText("Manage Lottery");
            btnLotteryCriteria.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DE8C0")));
            // Log override to help debug why click may not trigger expected behavior
            Log.d(TAG, "Organizer mode: wiring btnLotteryCriteria to openManageLottery()");
            btnLotteryCriteria.setOnClickListener(v -> openManageLottery());

            btnManageNotifications.setVisibility(View.VISIBLE);
            btnManageNotifications.setOnClickListener(v -> openManageNotifications());

        } else {
            isOrganizer = false;
            Log.d(TAG, "Current user is not the organizer - showing entrant view");

            // Hide Edit button
            btnEdit.setVisibility(View.GONE);

            // Hide Manage Notifications button
            btnManageNotifications.setVisibility(View.GONE);

            // Keep default button behavior for entrants
            // btnJoinWaitingList and btnLotteryCriteria retain their default listeners
        }
    }

    private void openEditEvent() {
        Toast.makeText(this, "Edit Event - Coming Soon", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to edit event activity
        // Intent intent = new Intent(this, EditEventActivity.class);
        // intent.putExtra(EditEventActivity.EXTRA_EVENT_ID, currentEvent.getId());
        // startActivity(intent);
    }

    private void openManageEntrants() {
        Intent intent = new Intent(this, WaitingListActivity.class);
        intent.putExtra(WaitingListActivity.EXTRA_EVENT_ID, currentEvent.getId());
        startActivity(intent);
    }

    private void openManageLottery() {
        Toast.makeText(this, "Manage Lottery - Coming Soon", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to lottery management activity
        // Intent intent = new Intent(this, ManageLotteryActivity.class);
        // intent.putExtra(ManageLotteryActivity.EXTRA_EVENT_ID, currentEvent.getId());
        // startActivity(intent);
    }

    private void openManageNotifications() {
        Toast.makeText(this, "Manage Notifications - Coming Soon", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to notification management activity
        // Intent intent = new Intent(this, ManageNotificationsActivity.class);
        // intent.putExtra(ManageNotificationsActivity.EXTRA_EVENT_ID, currentEvent.getId());
        // startActivity(intent);
    }

    private void openLotteryCriteria() {
        // Inflate a BottomSheet-style card matching existing dialog styling
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(R.layout.dialog_lottery_criteria, null, false);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(content);
        // Make background transparent so the card keeps rounded corners
        android.widget.FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) sheet.setBackgroundResource(android.R.color.transparent);
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        TextView tvCriteria = content.findViewById(R.id.tvCriteria);
        android.widget.ProgressBar progressBar = content.findViewById(R.id.progressBar);
        com.google.android.material.button.MaterialButton btnClose = content.findViewById(R.id.btnClose);
        View scrollCriteria = content.findViewById(R.id.scrollCriteria);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Show loading state while we fetch the criteria
        progressBar.setVisibility(View.VISIBLE);
        tvCriteria.setVisibility(View.GONE);
        if (scrollCriteria != null) scrollCriteria.setVisibility(View.GONE);

        final String id = currentEvent != null ? currentEvent.getId() : eventId;
        if (id == null) {
            progressBar.setVisibility(View.GONE);
            tvCriteria.setText("Event ID not available.");
            if (scrollCriteria != null) {
                // ensure container is visible so text can be read
                scrollCriteria.setVisibility(View.VISIBLE);
            } else {
                tvCriteria.setVisibility(View.VISIBLE);
            }
            dialog.show();
            return;
        }

        // Fetch the latest lotteryCriteria from Firestore for this event
        // If we already cached criteria when loading the event, show it immediately while we refresh from Firestore
        if (eventLotteryCriteria != null && !eventLotteryCriteria.trim().isEmpty()) {
            progressBar.setVisibility(View.GONE);
            if (scrollCriteria != null) {
                scrollCriteria.setVisibility(View.VISIBLE);
            } else {
                tvCriteria.setVisibility(View.VISIBLE);
            }
            tvCriteria.setText(eventLotteryCriteria);
            // still continue to fetch to get the latest server value
        }
        // First try to get the document from the server to ensure we have the latest text
        db.collection("events").document(id)
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener(documentSnapshot -> {
                    // Process server response
                    processCriteriaSnapshot(documentSnapshot, tvCriteria, scrollCriteria);
                    progressBar.setVisibility(View.GONE);
                    if (scrollCriteria != null) scrollCriteria.setVisibility(View.VISIBLE); else tvCriteria.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Server fetch failed for lotteryCriteria, falling back to default source", e);
                    // Fallback to default (cache/network) fetch
                    db.collection("events").document(id)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                processCriteriaSnapshot(documentSnapshot, tvCriteria, scrollCriteria);
                                progressBar.setVisibility(View.GONE);
                                if (scrollCriteria != null) scrollCriteria.setVisibility(View.VISIBLE); else tvCriteria.setVisibility(View.VISIBLE);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Failed to fetch event doc for lotteryCriteria", e2);
                                progressBar.setVisibility(View.GONE);
                                tvCriteria.setText("Failed to load criteria: " + e2.getMessage());
                                if (scrollCriteria != null) scrollCriteria.setVisibility(View.VISIBLE); else tvCriteria.setVisibility(View.VISIBLE);
                            });
                });

        dialog.show();
    }

    // Helper to parse and display the criteria from a DocumentSnapshot
    private void processCriteriaSnapshot(com.google.firebase.firestore.DocumentSnapshot documentSnapshot, TextView tvCriteria, View scrollCriteria) {
         try {
             if (documentSnapshot.exists()) {
                 String criteria = null;
                 Object raw = documentSnapshot.get("lotteryCriteria");
                 if (raw == null) raw = documentSnapshot.get("lottery_criteria");
                Log.d(TAG, "processCriteriaSnapshot raw value=" + String.valueOf(raw) + " type=" + (raw != null ? raw.getClass().getName() : "null"));

                 if (raw instanceof String) {
                     criteria = ((String) raw).trim();
                 } else if (raw instanceof java.util.Map) {
                    java.util.Map<?,?> m = (java.util.Map<?,?>) raw;
                    Object maybe = null;
                    if (m.containsKey("text")) maybe = m.get("text");
                    else if (m.containsKey("criteria")) maybe = m.get("criteria");
                    else if (m.containsKey("body")) maybe = m.get("body");
                    else if (m.containsKey("description")) maybe = m.get("description");
                    if (maybe instanceof String) {
                        criteria = ((String) maybe).trim();
                    } else if (maybe != null) {
                        criteria = maybe.toString();
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (java.util.Map.Entry<?,?> e : m.entrySet()) sb.append(String.valueOf(e.getKey())).append(": ").append(String.valueOf(e.getValue())).append("\n");
                        criteria = sb.toString().trim();
                    }
                 } else if (raw instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) raw;
                    StringBuilder sb = new StringBuilder();
                    for (Object o : list) if (o != null) sb.append(o.toString()).append("\n");
                    criteria = sb.toString().trim();
                 } else if (raw != null) {
                    criteria = raw.toString().trim();
                 }

                 if (criteria == null || criteria.trim().isEmpty()) {
                     try { java.util.Map<String,Object> data = documentSnapshot.getData(); if (data != null) Log.d(TAG, "Event doc keys: " + data.keySet()); } catch (Exception ignore) {}
                     tvCriteria.setText("No lottery criteria set for this event.");
                 } else {
                     // Update cached copy and show
                     eventLotteryCriteria = criteria;
                    Log.d(TAG, "processCriteriaSnapshot: extracted criteria length=" + (criteria != null ? criteria.length() : 0));
                     tvCriteria.setText(criteria);
                 }
             } else {
                 tvCriteria.setText("Event not found.");
             }
         } catch (Exception ex) {
             Log.e(TAG, "Error processing lotteryCriteria snapshot", ex);
             tvCriteria.setText("Failed to load criteria: " + ex.getMessage());
         }
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

    @Override
    protected void onResume() {
        super.onResume();
        // Re-ensure the lottery button is wired correctly in case something else overwrote the listener
        if (btnLotteryCriteria != null) {
            // If organizer view is active, it should open manage lottery
            if (isOrganizer) {
                Log.d(TAG, "onResume: organizer mode - wiring btnLotteryCriteria to openManageLottery()");
                btnLotteryCriteria.setOnClickListener(v -> {
                    Log.d(TAG, "btnLotteryCriteria clicked in organizer mode");
                    openManageLottery();
                });
            } else {
                Log.d(TAG, "onResume: entrant mode - wiring btnLotteryCriteria to openLotteryCriteria()");
                btnLotteryCriteria.setOnClickListener(v -> {
                    Log.d(TAG, "btnLotteryCriteria clicked - entrant mode listener");
                    openLotteryCriteria();
                });
            }

            // Raise elevation/translationZ to avoid being under overlays
            try {
                btnLotteryCriteria.setElevation(12f);
                btnLotteryCriteria.setTranslationZ(12f);
            } catch (Exception ex) {
                Log.w(TAG, "Could not set elevation on btnLotteryCriteria: " + ex.getMessage());
            }

            // If parent is a View, forward its clicks to the button as a fallback
            android.view.ViewParent parent = btnLotteryCriteria.getParent();
            if (parent instanceof View) {
                View parentView = (View) parent;
                parentView.setClickable(true);
                parentView.setOnClickListener(v -> {
                    Log.d(TAG, "Parent view clicked - forwarding to btnLotteryCriteria.performClick()");
                    btnLotteryCriteria.performClick();
                });
            }

            // Debug: report button status and screen bounds so we can tell if it's covered or offscreen
            try {
                btnLotteryCriteria.post(() -> {
                    try {
                        int[] loc = new int[2];
                        btnLotteryCriteria.getLocationOnScreen(loc);
                        int x = loc[0];
                        int y = loc[1];
                        String status = String.format("BtnLotteryCriteria - vis=%s enabled=%s clickable=%s x=%d y=%d",
                                btnLotteryCriteria.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT_VISIBLE",
                                btnLotteryCriteria.isEnabled(),
                                btnLotteryCriteria.isClickable(),
                                x, y);
                        Log.d(TAG, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Failed to report btnLotteryCriteria bounds: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                Log.w(TAG, "Failed to post btnLotteryCriteria debug report: " + ex.getMessage());
            }

        } else {
            Log.w(TAG, "onResume: btnLotteryCriteria is null");
        }
    }
}
