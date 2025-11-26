/*
 * Detailed event screen showing metadata, poster, lottery status, and actions.
 * Handles waitlist join/leave, location preview, and organizer-specific controls.
 */
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

import java.util.List;

import ca.team.originkickoff.data.repository.UserRepository;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.models.User;
import ca.team.originkickoff.services.WaitingListService;

/**
 * Activity presenting full event details with interactive join and organizer features.
 */
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
    private LinearLayout actionButtonsContainer;
    private CardView lotteryResultCard;
    private TextView tvLotteryResult;

    private FirebaseFirestore db;
    private String eventId;
    private Event currentEvent;

    private final WaitingListService waitingListService = new WaitingListService();
    private final UserRepository userRepository = new UserRepository();
    private User currentUser;
    private boolean isOrganizer = false;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isShowingLotteryResult = false;

    private long lastNavTapAtMs = 0L;

    /**
     * Initializes the detail screen, binds UI, resolves user, and loads event data.
     *
     * @param savedInstanceState previous instance state if available
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Error: Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        resolveCurrentUser();
        loadEventData();
    }

    /**
     * Refreshes lottery or join state when the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (currentEvent != null && currentUser != null) {
            if (isOrganizer) {
                checkLotteryStatusAndUpdateButton();
            } else {
                checkLotteryStatusForEntrant();
            }
        }
    }

    /**
     * Binds view references from the layout.
     */
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
        actionButtonsContainer = findViewById(R.id.actionButtonsContainer);
        lotteryResultCard = findViewById(R.id.lotteryResultCard);
        tvLotteryResult = findViewById(R.id.tvLotteryResult);
    }

    /**
     * Attaches click listeners for navigation, joining, and auxiliary actions.
     */
    private void setupListeners() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

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

        posterImage.setOnClickListener(v -> {
            if (currentEvent != null && currentEvent.getPosterBase64() != null && !currentEvent.getPosterBase64().isEmpty()) {
                openImageViewer(currentEvent.getPosterBase64());
            }
        });

        LinearLayout navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> navigateBottomTab(MainActivity.class));

        LinearLayout navEvents = findViewById(R.id.navEvents);
        navEvents.setOnClickListener(v -> navigateBottomTab(MyEventsActivity.class));

        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        navNotifications.setOnClickListener(v -> navigateBottomTab(NotificationsActivity.class));

        LinearLayout navProfile = findViewById(R.id.navProfile);
        navProfile.setOnClickListener(v -> navigateBottomTab(ProfileActivity.class));

        locationCard.setOnClickListener(v -> {
            if (currentEvent != null && currentEvent.getLocation() != null) {
                openMapPreview();
            }
        });
    }

    /**
     * Navigates to a bottom tab activity with basic debounce.
     *
     * @param targetActivity activity class to open
     */
    private void navigateBottomTab(Class<?> targetActivity) {
        if (targetActivity == null) return;
        if (getClass().equals(targetActivity)) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastNavTapAtMs < 300) return;
        lastNavTapAtMs = now;
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    /**
     * Resolves the current user from the local device identifier and refreshes UI.
     */
    private void resolveCurrentUser() {
        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        userRepository.findUserByDeviceId(deviceId).observe(this, user -> {
            currentUser = user;
            if (currentUser != null) {
                Log.d(TAG, "Current user loaded: " + currentUser.getId());
                if (currentEvent != null) {
                    checkAndSetupOrganizerView();
                }
            }
            if (currentEvent != null && !"conducted".equals(currentEvent.getLotteryStatus())) {
                refreshJoinButton();
            }
        });
    }

    /**
     * Updates the join button state for entrants based on registration and waitlist state.
     */
    private void refreshJoinButton() {
        if (currentEvent == null || currentUser == null) return;
        if (isOrganizer) {
            Log.d(TAG, "User is organizer, skipping join button refresh");
            return;
        }
        if (isShowingLotteryResult) {
            Log.d(TAG, "Showing lottery result, skipping join button refresh");
            return;
        }
        waitingListService.isOnWaitlist(currentEvent.getId(), currentUser.getId())
                .addOnSuccessListener(this::updateJoinButtonConsideringState);
    }

    /**
     * Applies button text/enabled state based on whether user is on the list and window status.
     *
     * @param isOnList true if user is currently on the waiting list
     */
    private void updateJoinButtonConsideringState(boolean isOnList) {
        if (currentEvent == null) return;
        long now = System.currentTimeMillis();
        Long start = currentEvent.getRegistrationStartTime() != null ? currentEvent.getRegistrationStartTime().getTime() : null;
        Long end = currentEvent.getRegistrationEndTime() != null ? currentEvent.getRegistrationEndTime().getTime() : null;

        boolean beforeStart = start != null && now < start;
        boolean afterEnd = end != null && now > end;
        boolean withinWindow = (start == null || now >= start) && (end == null || now <= end);

        boolean waitlistFull = currentEvent.isLimitWaitlist() && currentEvent.getWaitlistLimit() > 0 &&
                currentEvent.getWaitlistCount() >= currentEvent.getWaitlistLimit();

        if (!isOnList) {
            if (beforeStart) {
                setDisabledJoinButton("Registration opening soon");
                return;
            }
            if (afterEnd) {
                setDisabledJoinButton("Registration closed");
                return;
            }
            if (withinWindow && waitlistFull) {
                setDisabledJoinButton("Waiting list full");
                return;
            }
        }

        if (isShowingLotteryResult) {
            return;
        }

        if (isOnList) {
            btnJoinWaitingList.setText("Leave Waiting List");
            btnJoinWaitingList.setEnabled(true);
            btnJoinWaitingList.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF3B30")));
            btnJoinWaitingList.setTextColor(Color.WHITE);
        } else {
            btnJoinWaitingList.setText("Join Waiting List");
            btnJoinWaitingList.setEnabled(true);
            btnJoinWaitingList.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DE8C0")));
            btnJoinWaitingList.setTextColor(Color.parseColor("#003932"));
        }
    }

    /**
     * Sets a disabled style and label for the join button.
     *
     * @param label text to show on the disabled button
     */
    private void setDisabledJoinButton(String label) {
        btnJoinWaitingList.setText(label);
        btnJoinWaitingList.setEnabled(false);
        btnJoinWaitingList.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7A7A7A")));
        btnJoinWaitingList.setTextColor(Color.WHITE);
    }

    /**
     * Toggles current user between joining and leaving the waiting list.
     */
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
                                    refreshJoinButton();
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

    /**
     * Shows a confirmation bottom sheet before joining the waiting list.
     *
     * @param eventId  event identifier
     * @param userId   user identifier
     * @param location optional location to attach to the join entry
     */
    private void showJoinConfirmationDialog(String eventId, String userId, Location location) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(R.layout.dialog_join_waitlist, null, false);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(content);
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

    /**
     * Performs the join action via waiting list service.
     *
     * @param eventId  event identifier
     * @param userId   user identifier
     * @param location optional location data to store
     */
    private void doJoin(String eventId, String userId, Location location) {
        Double latitude = location != null ? location.getLatitude() : null;
        Double longitude = location != null ? location.getLongitude() : null;
        boolean locationConsent = location != null;

        waitingListService.join(eventId, userId, locationConsent, latitude, longitude, null, "list")
                .addOnSuccessListener(changed -> {
                    if (changed) Toast.makeText(this, "Joined waiting list", Toast.LENGTH_SHORT).show();
                    currentEvent.setWaitlistCount(currentEvent.getWaitlistCount() + (changed ? 1 : 0));
                    updateUI();
                    refreshJoinButton();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Requests location permission and proceeds to join if granted.
     */
    private void requestLocationAndJoin() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationAndJoin();
        }
    }

    /**
     * Handles permission result for location-based join.
     *
     * @param requestCode  permission request code
     * @param permissions  permissions requested
     * @param grantResults grant results for requested permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndJoin();
            } else {
                Toast.makeText(this, "Location permission denied. Joining without location.", Toast.LENGTH_SHORT).show();
                showJoinConfirmationDialog(currentEvent.getId(), currentUser.getId(), null);
            }
        }
    }

    /**
     * Obtains the last known location and prompts joining with it.
     */
    private void getLocationAndJoin() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                showJoinConfirmationDialog(currentEvent.getId(), currentUser.getId(), location);
            } else {
                Toast.makeText(this, "Could not retrieve location. Joining without location.", Toast.LENGTH_SHORT).show();
                showJoinConfirmationDialog(currentEvent.getId(), currentUser.getId(), null);
            }
        });
    }

    /**
     * Fetches event document from Firestore and updates the UI model.
     */
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

                            Long capacity = documentSnapshot.getLong("capacity");
                            currentEvent.setCapacity(capacity != null ? capacity.intValue() : 0);

                            Long waitlistCount = documentSnapshot.getLong("waitlistCount");
                            currentEvent.setWaitlistCount(waitlistCount != null ? waitlistCount.intValue() : 0);

                            Long selectionSize = documentSnapshot.getLong("selectionSize");
                            if (selectionSize != null) currentEvent.setSelectionSize(selectionSize.intValue());
                            Boolean limitWaitlist = documentSnapshot.getBoolean("limitWaitlist");
                            if (limitWaitlist != null) currentEvent.setLimitWaitlist(limitWaitlist);
                            Long waitlistLimit = documentSnapshot.getLong("waitlistLimit");
                            if (waitlistLimit != null) currentEvent.setWaitlistLimit(waitlistLimit.intValue());

                            Double price = documentSnapshot.getDouble("price");
                            currentEvent.setPrice(price != null ? price : 0.0);

                            Long createdAt = documentSnapshot.getLong("createdAt");
                            currentEvent.setCreatedAt(createdAt != null ? createdAt : 0L);

                            Boolean geolocationRequired = documentSnapshot.getBoolean("geolocationRequired");
                            currentEvent.setGeolocationRequired(geolocationRequired != null && geolocationRequired);

                            String lotteryStatus = documentSnapshot.getString("lotteryStatus");
                            if (lotteryStatus != null) {
                                currentEvent.setLotteryStatus(lotteryStatus);
                            }

                            String lotteryCriteria = documentSnapshot.getString("lotteryCriteria");
                            if (lotteryCriteria != null) {
                                currentEvent.setLotteryCriteria(lotteryCriteria);
                            }

                            com.google.firebase.Timestamp regStart = documentSnapshot.getTimestamp("registrationStartTime");
                            if (regStart != null) {
                                currentEvent.setRegistrationStartTime(regStart.toDate());
                            }

                            com.google.firebase.Timestamp regEnd = documentSnapshot.getTimestamp("registrationEndTime");
                            if (regEnd != null) {
                                currentEvent.setRegistrationEndTime(regEnd.toDate());
                            }

                            updateUI();

                            if (!"conducted".equals(currentEvent.getLotteryStatus())) {
                                refreshJoinButton();
                            }

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

    /**
     * Applies current event values to the UI elements.
     */
    private void updateUI() {
        if (currentEvent == null) return;

        textTitle.setText(currentEvent.getName());
        textOrganizer.setText("Organized by: " + currentEvent.getOrganizerName());

        textLocationTitle.setText(currentEvent.getLocation());
        textLocationSubtitle.setText("Event Location");

        int eventCapacity = currentEvent.getCapacity();
        int totalEntrants = currentEvent.getWaitlistCount();
        int toBeSelected = currentEvent.getSelectionSize() > 0 ? currentEvent.getSelectionSize() : currentEvent.getCapacity();

        pillTotalEntrants.setText("Event Capacity: " + eventCapacity);
        pillSpotsLeft.setText("Entrants in Waitlist: " + totalEntrants);

        if (currentEvent.isLimitWaitlist() && currentEvent.getWaitlistLimit() > 0) {
            int waitlistLimit = currentEvent.getWaitlistLimit();
            int spotsLeftOnWaitlist = waitlistLimit - totalEntrants;
            if (spotsLeftOnWaitlist < 0) spotsLeftOnWaitlist = 0;
            pillToBeSelected.setText("Spots left on Waitlist: " + spotsLeftOnWaitlist);
            pillToBeSelected.setVisibility(View.VISIBLE);
        } else {
            pillToBeSelected.setVisibility(View.GONE);
        }

        if (currentEvent.getRegistrationStartTime() != null) {
            textDate.setText("Registration Open");
        }

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

        checkAndSetupOrganizerView();
    }

    /**
     * Configures the screen for organizer or entrant view based on current user.
     */
    private void checkAndSetupOrganizerView() {
        if (currentUser == null || currentEvent == null) {
            isOrganizer = false;
            return;
        }
        String organizerId = currentEvent.getOrganizerId();
        String userId = currentUser.getId();
        boolean organizerMatch = organizerId != null && organizerId.equals(userId);
        boolean userHasOrganizerFlag = currentUser.isOrganizer();
        boolean userIsAdmin = currentUser.isAdmin();
        boolean displayNameMatches = false;
        if (currentUser.getDisplayName() != null && currentEvent.getOrganizerName() != null) {
            displayNameMatches = currentUser.getDisplayName().trim().equalsIgnoreCase(currentEvent.getOrganizerName().trim());
        }

        // Treat the user as organizer if their id matches the event organizer, their profile has the organizer flag,
        // they are an admin, or their display name matches the event organizer name.
        if (organizerMatch || userHasOrganizerFlag || userIsAdmin || displayNameMatches) {
            isOrganizer = true;
            Log.d(TAG, "Organizer recognized (userId match) - showing organizer view");
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> openEditEvent());
            btnJoinWaitingList.setText("Manage Entrants");
            btnJoinWaitingList.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DE8C0")));
            btnJoinWaitingList.setTextColor(Color.parseColor("#003932"));
            btnJoinWaitingList.setOnClickListener(v -> openManageEntrants());

            checkLotteryStatusAndUpdateButton();

            btnManageNotifications.setVisibility(View.VISIBLE);
            btnManageNotifications.setOnClickListener(v -> openManageNotifications());
        } else {
            // Not recognized locally — attempt to resolve organizer doc's device id as a fallback
            isOrganizer = false;
            Log.d(TAG, "Current user not recognized as organizer locally. Attempting organizer doc fallback.");
            if (organizerId != null && !organizerId.isEmpty()) {
                db.collection("users").document(organizerId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc != null && doc.exists()) {
                                String organizerDeviceId = doc.getString("device_id");
                                String organizerDisplayName = doc.getString("display_name");
                                String organizerEmail = doc.getString("email");
                                String currentDeviceId = currentUser.getDeviceId();
                                String currentDisplayName = currentUser.getDisplayName();
                                String currentEmail = currentUser.getEmail();

                                boolean matchesDevice = organizerDeviceId != null && currentDeviceId != null && organizerDeviceId.equals(currentDeviceId);
                                boolean matchesDisplayName = organizerDisplayName != null && currentDisplayName != null && organizerDisplayName.trim().equalsIgnoreCase(currentDisplayName.trim());
                                boolean matchesEmail = organizerEmail != null && currentEmail != null && organizerEmail.trim().equalsIgnoreCase(currentEmail.trim());

                                if (matchesDevice || matchesDisplayName || matchesEmail) {
                                    // The organizer document appears to refer to this user/device -> treat as organizer
                                    isOrganizer = true;
                                    Log.d(TAG, "Organizer doc appears to match current user -> enabling organizer view");
                                    btnEdit.setVisibility(View.VISIBLE);
                                    btnEdit.setOnClickListener(v -> openEditEvent());
                                    btnJoinWaitingList.setText(getString(R.string.manage_entrants));
                                    btnJoinWaitingList.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DE8C0")));
                                    btnJoinWaitingList.setTextColor(Color.parseColor("#003932"));
                                    btnJoinWaitingList.setOnClickListener(v -> openManageEntrants());
                                    checkLotteryStatusAndUpdateButton();
                                    btnManageNotifications.setVisibility(View.VISIBLE);
                                    btnManageNotifications.setOnClickListener(v -> openManageNotifications());
                                    return;
                                }
                            }
                            // Fallback still not matched -> entrant view
                            Log.d(TAG, "Organizer fallback did not match; showing entrant view");
                            btnEdit.setVisibility(View.GONE);
                            btnManageNotifications.setVisibility(View.GONE);
                            checkLotteryStatusForEntrant();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to load organizer user doc for fallback check", e);
                            btnEdit.setVisibility(View.GONE);
                            btnManageNotifications.setVisibility(View.GONE);
                            checkLotteryStatusForEntrant();
                        });
            } else {
                btnEdit.setVisibility(View.GONE);
                btnManageNotifications.setVisibility(View.GONE);
                checkLotteryStatusForEntrant();
            }
        }
    }

    /**
     * For organizers, checks lottery results and updates the action button text.
     */
    private void checkLotteryStatusAndUpdateButton() {
        db.collection("lottery_results")
                .document(currentEvent.getId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        btnLotteryCriteria.setText("Lottery Results");
                    } else {
                        btnLotteryCriteria.setText("Manage Lottery");
                    }
                    btnLotteryCriteria.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DE8C0")));
                    btnLotteryCriteria.setOnClickListener(v -> openManageLottery());
                })
                .addOnFailureListener(e -> {
                    btnLotteryCriteria.setText("Manage Lottery");
                    btnLotteryCriteria.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DE8C0")));
                    btnLotteryCriteria.setOnClickListener(v -> openManageLottery());
                });
    }

    /**
     * For entrants, checks whether a lottery was conducted and shows appropriate UI.
     */
    private void checkLotteryStatusForEntrant() {
        String lotteryStatus = currentEvent.getLotteryStatus();
        Log.d(TAG, "Checking lottery status for entrant. Status: " + lotteryStatus);

        if ("conducted".equals(lotteryStatus)) {
            Log.d(TAG, "Lottery status is 'conducted', checking user's lottery result");
            checkUserLotteryResult();
        } else {
            Log.d(TAG, "Lottery not conducted, showing normal buttons");
            actionButtonsContainer.setVisibility(View.VISIBLE);
            lotteryResultCard.setVisibility(View.GONE);
        }
    }

    /**
     * Looks up whether the current user was selected or not in the conducted lottery.
     */
    private void checkUserLotteryResult() {
        if (currentUser == null) return;

        String userId = currentUser.getId();
        String eventId = currentEvent.getId();

        Log.d(TAG, "Checking lottery result for user: " + userId + " in event: " + eventId);

        db.collection("invitation_status")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String status = snapshots.getDocuments().get(0).getString("status");
                        Log.d(TAG, "Found invitation_status: " + status);
                        showLotteryResult(status);
                    } else {
                        Log.d(TAG, "No invitation_status found, checking if user was in waiting list");
                        checkIfUserWasInOriginalWaitingList(eventId, userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user lottery result", e);
                    actionButtonsContainer.setVisibility(View.VISIBLE);
                    lotteryResultCard.setVisibility(View.GONE);
                });
    }

    /**
     * Determines whether user ever joined the waiting list if no invitation_status is found.
     *
     * @param eventId event identifier
     * @param userId  user identifier
     */
    private void checkIfUserWasInOriginalWaitingList(String eventId, String userId) {
        db.collection("waiting_list_entries")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Log.d(TAG, "User was in waiting list but not selected");
                        showLotteryResult("not_selected");
                    } else {
                        Log.d(TAG, "User was never in the waiting list");
                        actionButtonsContainer.setVisibility(View.VISIBLE);
                        lotteryResultCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking waiting list", e);
                    actionButtonsContainer.setVisibility(View.VISIBLE);
                    lotteryResultCard.setVisibility(View.GONE);
                });
    }

    /**
     * Displays the lottery result card for the entrant with the given status.
     *
     * @param status invitation or selection status string
     */
    private void showLotteryResult(String status) {
        Log.d(TAG, "showLotteryResult called with status: " + status);

        isShowingLotteryResult = true;

        actionButtonsContainer.setVisibility(View.GONE);
        btnLotteryCriteria.setVisibility(View.GONE);

        lotteryResultCard.setVisibility(View.VISIBLE);

        if ("chosen".equals(status) || "enrolled".equals(status)) {
            tvLotteryResult.setText("🎉 Congratulations! You were selected in the lottery!");
            tvLotteryResult.setTextColor(Color.parseColor("#4DE8C0"));
        } else if ("cancelled".equals(status)) {
            tvLotteryResult.setText("You were selected but cancelled your enrollment");
            tvLotteryResult.setTextColor(Color.parseColor("#FFD60A"));
        } else {
            tvLotteryResult.setText("Unfortunately, you were not selected in the lottery");
            tvLotteryResult.setTextColor(Color.parseColor("#FF3B30"));
        }

        Log.d(TAG, "Lottery result card visibility set to VISIBLE, action buttons set to GONE");
    }

    /**
     * Opens the event editing screen for the current event.
     */
    private void openEditEvent() {
        Intent intent = new Intent(this, EditEventActivity.class);
        intent.putExtra(EditEventActivity.EXTRA_EVENT_ID, currentEvent.getId());
        startActivity(intent);
    }

    /**
     * Opens the entrant management screen for organizers.
     */
    private void openManageEntrants() {
        Intent intent = new Intent(this, WaitingListActivity.class);
        intent.putExtra(WaitingListActivity.EXTRA_EVENT_ID, currentEvent.getId());
        startActivity(intent);
    }

    /**
     * Opens the lottery management screen for organizers.
     */
    private void openManageLottery() {
        Intent intent = new Intent(this, ManageLotteryActivity.class);
        intent.putExtra(ManageLotteryActivity.EXTRA_EVENT_ID, currentEvent.getId());
        startActivity(intent);
    }

    /**
     * Opens the organizer notifications management screen.
     */
    private void openManageNotifications() {
        if (currentEvent == null) {
            Toast.makeText(this, "Event not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ManageNotificationsActivity.class);
        intent.putExtra(ManageNotificationsActivity.EXTRA_EVENT_ID, currentEvent.getId());
        startActivity(intent);
    }

    /**
     * Shows a bottom sheet with lottery criteria text for entrants.
     */
    private void openLotteryCriteria() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(R.layout.dialog_lottery_criteria, null, false);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(content);

        android.widget.FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) sheet.setBackgroundResource(android.R.color.transparent);

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        TextView tvLotteryCriteria = content.findViewById(R.id.tvLotteryCriteria);
        String criteria = currentEvent.getLotteryCriteria();

        if (criteria != null && !criteria.trim().isEmpty()) {
            tvLotteryCriteria.setText(criteria);
        } else {
            tvLotteryCriteria.setText("No lottery criteria specified for this event.");
        }

        content.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Opens a map or external app to preview the event location.
     */
    private void openMapPreview() {
        if (currentEvent.getLocationLatitude() != 0.0 && currentEvent.getLocationLongitude() != 0.0) {
            Intent intent = new Intent(this, MapViewActivity.class);
            intent.putExtra(MapViewActivity.EXTRA_LATITUDE, currentEvent.getLocationLatitude());
            intent.putExtra(MapViewActivity.EXTRA_LONGITUDE, currentEvent.getLocationLongitude());
            intent.putExtra(MapViewActivity.EXTRA_LOCATION_NAME, currentEvent.getLocation());
            startActivity(intent);
        } else {
            String uri = "geo:0,0?q=" + Uri.encode(currentEvent.getLocation());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                String browserUri = "https://www.google.com/maps/search/?api=1&query=" +
                        Uri.encode(currentEvent.getLocation());
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri));
                startActivity(browserIntent);
            }
        }
    }

    /**
     * Opens an image viewer activity to display an event poster.
     *
     * @param base64Image base64 encoded image string
     */
    private void openImageViewer(String base64Image) {
        try {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            Intent intent = new Intent(this, ImageViewerActivity.class);
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_BITMAP, decodedByte);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding Base64 image", e);
            Toast.makeText(this, "Error opening image", Toast.LENGTH_SHORT).show();
        }
    }
}

