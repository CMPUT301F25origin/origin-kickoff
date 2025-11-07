package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.models.LotteryMethod;
import ca.team.originkickoff.services.LotteryOrchestrator;
import ca.team.originkickoff.services.WaitingListService;

/**
 * Activity for organizers to manage lottery for their events.
 * Allows conducting lottery with different methods and viewing results.
 */
public class ManageLotteryActivity extends AppCompatActivity {
    private static final String TAG = "ManageLotteryActivity";
    public static final String EXTRA_EVENT_ID = "event_id";

    private TextView tvEventName;
    private TextView tvEntrantsCount;
    private TextView tvCapacity;
    private TextView tvLotteryStatus;
    private Button btnConductLottery;
    private ProgressBar progressBar;
    private View layoutNotConducted;

    private String eventId;
    private Event currentEvent;
    private LotteryOrchestrator lotteryOrchestrator;
    private WaitingListService waitingListService;
    private int entrantsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_lottery);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        initializeServices();
        loadEventData();
    }

    private void initializeViews() {
        tvEventName = findViewById(R.id.tv_event_name);
        tvEntrantsCount = findViewById(R.id.tv_entrants_count);
        tvCapacity = findViewById(R.id.tv_capacity);
        tvLotteryStatus = findViewById(R.id.tv_lottery_status);
        btnConductLottery = findViewById(R.id.btn_conduct_lottery);
        progressBar = findViewById(R.id.progress_bar);
        layoutNotConducted = findViewById(R.id.layout_not_conducted);

        btnConductLottery.setOnClickListener(v -> showLotteryMethodDialog());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Lottery");
        }
    }

    private void initializeServices() {
        lotteryOrchestrator = new LotteryOrchestrator();
        waitingListService = new WaitingListService();
    }

    private void loadEventData() {
        showLoading(true);

        // Load event details directly from Firestore
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentEvent = documentSnapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setId(documentSnapshot.getId());
                            tvEventName.setText(currentEvent.getName());
                            tvCapacity.setText("Capacity: " + currentEvent.getCapacity());
                            loadWaitlistCount();
                            checkLotteryStatus();
                        } else {
                            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event", e);
                    Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void loadWaitlistCount() {
        waitingListService.countActive(eventId).addOnSuccessListener(count -> {
            entrantsCount = count;
            tvEntrantsCount.setText("Entrants: " + count);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load waitlist count", e);
            tvEntrantsCount.setText("Entrants: Unknown");
        });
    }

    private void checkLotteryStatus() {
        lotteryOrchestrator.hasLotteryBeenConducted(eventId).addOnSuccessListener(conducted -> {
            if (conducted) {
                // Redirect to results page
                navigateToResults();
            } else {
                showNotConductedView();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to check lottery status", e);
            Toast.makeText(this, "Failed to check lottery status", Toast.LENGTH_SHORT).show();
            showLoading(false);
        });
    }

    private void showNotConductedView() {
        layoutNotConducted.setVisibility(View.VISIBLE);
        tvLotteryStatus.setText("Status: Not Conducted");
        tvLotteryStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        showLoading(false);
    }

    private void showLotteryMethodDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_lottery_method, null);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);

        // Make background transparent
        android.widget.FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) sheet.setBackgroundResource(android.R.color.transparent);

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        RadioButton radioRandom = dialogView.findViewById(R.id.radio_random);
        RadioButton radioEarlyPriority = dialogView.findViewById(R.id.radio_early_priority);
        CardView cardRandom = dialogView.findViewById(R.id.card_random);
        CardView cardEarlyPriority = dialogView.findViewById(R.id.card_early_priority);

        // Update card styling based on selection
        updateCardSelection(cardRandom, cardEarlyPriority, true);

        // Make cards clickable to select radio buttons
        cardRandom.setOnClickListener(v -> {
            radioRandom.setChecked(true);
            radioEarlyPriority.setChecked(false);
            updateCardSelection(cardRandom, cardEarlyPriority, true);
        });

        cardEarlyPriority.setOnClickListener(v -> {
            radioEarlyPriority.setChecked(true);
            radioRandom.setChecked(false);
            updateCardSelection(cardRandom, cardEarlyPriority, false);
        });

        // Also handle radio button clicks
        radioRandom.setOnClickListener(v -> {
            radioEarlyPriority.setChecked(false);
            updateCardSelection(cardRandom, cardEarlyPriority, true);
        });

        radioEarlyPriority.setOnClickListener(v -> {
            radioRandom.setChecked(false);
            updateCardSelection(cardRandom, cardEarlyPriority, false);
        });

        dialogView.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            LotteryMethod method = radioRandom.isChecked()
                    ? LotteryMethod.RANDOM
                    : LotteryMethod.EARLY_PRIORITY_RANDOM;
            dialog.dismiss();
            conductLottery(method);
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateCardSelection(CardView cardRandom, CardView cardEarlyPriority, boolean randomSelected) {
        // Cast to MaterialCardView to access stroke methods
        com.google.android.material.card.MaterialCardView randomCard =
                (com.google.android.material.card.MaterialCardView) cardRandom;
        com.google.android.material.card.MaterialCardView earlyCard =
                (com.google.android.material.card.MaterialCardView) cardEarlyPriority;

        if (randomSelected) {
            // Highlight random card (selected)
            randomCard.setStrokeColor(android.graphics.Color.parseColor("#68F0C9"));
            randomCard.setStrokeWidth(dpToPx(2));

            // Dim early priority card (not selected)
            earlyCard.setStrokeColor(android.graphics.Color.parseColor("#2A3A38"));
            earlyCard.setStrokeWidth(dpToPx(2));
        } else {
            // Dim random card (not selected)
            randomCard.setStrokeColor(android.graphics.Color.parseColor("#2A3A38"));
            randomCard.setStrokeWidth(dpToPx(2));

            // Highlight early priority card (selected)
            earlyCard.setStrokeColor(android.graphics.Color.parseColor("#68F0C9"));
            earlyCard.setStrokeWidth(dpToPx(2));
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void conductLottery(LotteryMethod method) {
        // Check for network connectivity first
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network settings.",
                    Toast.LENGTH_LONG).show();
            showLoading(false);
            btnConductLottery.setEnabled(true);
            return;
        }

        showLoading(true);
        btnConductLottery.setEnabled(false);

        String organizerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "unknown";

        int capacity = currentEvent.getCapacity();

        // If entrants <= capacity, select everyone
        int numWinners = Math.min(entrantsCount, capacity);

        // Add timeout handler (30 seconds)
        android.os.Handler timeoutHandler = new android.os.Handler();
        Runnable timeoutRunnable = () -> {
            showLoading(false);
            btnConductLottery.setEnabled(true);
            Toast.makeText(this, "Request timeout. Please check your internet connection and try again.",
                    Toast.LENGTH_LONG).show();
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000); // 30 second timeout

        lotteryOrchestrator.conductLottery(eventId, organizerId, numWinners, method)
                .addOnSuccessListener(result -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable); // Cancel timeout
                    // Create invitation statuses for all winners
                    createInvitationStatuses(result.getWinnerIds())
                            .addOnSuccessListener(aVoid -> {
                                // Send notifications to winners and losers
                                sendLotteryNotifications(result.getWinnerIds(), result.getAllEntrantIds())
                                        .addOnSuccessListener(aVoid1 -> {
                                            // Mark event as lottery conducted
                                            markLotteryAsConducted();

                                            Toast.makeText(this, "Lottery conducted successfully! " +
                                                    result.getNumWinners() + " winners selected.", Toast.LENGTH_LONG).show();

                                            // Navigate to invitations page
                                            navigateToResults();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to send lottery notifications", e);
                                            Toast.makeText(this, "Lottery completed but failed to send notifications: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                            showLoading(false);
                                            btnConductLottery.setEnabled(true);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to create invitation statuses", e);
                                Toast.makeText(this, "Lottery completed but failed to create invitations: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                showLoading(false);
                                btnConductLottery.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable); // Cancel timeout
                    Log.e(TAG, "Failed to conduct lottery", e);

                    String errorMessage = "Failed to conduct lottery";
                    if (e.getMessage() != null && e.getMessage().contains("UNAVAILABLE")) {
                        errorMessage = "Network error. Please check your internet connection and try again.";
                    } else if (e.getMessage() != null) {
                        errorMessage = "Failed to conduct lottery: " + e.getMessage();
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    showLoading(false);
                    btnConductLottery.setEnabled(true);
                });
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager =
                (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private com.google.android.gms.tasks.Task<Void> createInvitationStatuses(List<String> winnerIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();

        for (String userId : winnerIds) {
            String docId = eventId + "_" + userId;
            Map<String, Object> data = new HashMap<>();
            data.put("event_id", eventId);
            data.put("user_id", userId);
            data.put("status", "chosen");
            data.put("invited_at", now);

            batch.set(db.collection("invitation_status").document(docId), data);
        }

        return batch.commit();
    }

    private com.google.android.gms.tasks.Task<Void> sendLotteryNotifications(List<String> winnerIds, List<String> allEntrantIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();

        // Get event name for notification
        String eventName = currentEvent != null ? currentEvent.getName() : "Event";

        // Send notifications to winners
        for (String userId : winnerIds) {
            String notificationId = db.collection("notifications").document().getId();
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", userId);
            notification.put("eventId", eventId);
            notification.put("type", "result");
            notification.put("title", "🎉 Lottery Result - You Won!");
            notification.put("message", "Congratulations! You were selected in the lottery for " + eventName);
            notification.put("createdAt", now);
            notification.put("read", false);

            batch.set(db.collection("notifications").document(notificationId), notification);
        }

        // Send notifications to losers (entrants who didn't win)
        for (String userId : allEntrantIds) {
            if (!winnerIds.contains(userId)) {
                String notificationId = db.collection("notifications").document().getId();
                Map<String, Object> notification = new HashMap<>();
                notification.put("userId", userId);
                notification.put("eventId", eventId);
                notification.put("type", "result");
                notification.put("title", "Lottery Result");
                notification.put("message", "Unfortunately, you were not selected in the lottery for " + eventName);
                notification.put("createdAt", now);
                notification.put("read", false);

                batch.set(db.collection("notifications").document(notificationId), notification);
            }
        }

        return batch.commit();
    }

    private void markLotteryAsConducted() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .update("lotteryStatus", "conducted")
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update lottery status", e));
    }

    private void navigateToResults() {
        Intent intent = new Intent(this, ManageInvitationsActivity.class);
        intent.putExtra(ManageInvitationsActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
