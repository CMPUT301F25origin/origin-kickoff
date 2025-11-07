/* Organizer interface for executing and finalizing a lottery for an event. Enables method selection, persistence, and notification dispatch. */
package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
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
 * Activity enabling organizers to conduct a lottery, send notifications, and view results.
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

    /**
     * Lifecycle entry point: inflates layout, validates intent extras, initializes services and loads event data.
     * @param savedInstanceState prior state bundle
     */
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

    /**
     * Binds UI components and configures action bar/title and click listeners.
     */
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

    /**
     * Instantiates orchestrator and waiting list service dependencies.
     */
    private void initializeServices() {
        lotteryOrchestrator = new LotteryOrchestrator();
        waitingListService = new WaitingListService();
    }

    /**
     * Loads the event document then triggers waitlist count retrieval and lottery status check.
     */
    private void loadEventData() {
        showLoading(true);
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

    /**
     * Retrieves active waiting list entrant count and updates UI.
     */
    private void loadWaitlistCount() {
        waitingListService.countActive(eventId).addOnSuccessListener(count -> {
            entrantsCount = count;
            tvEntrantsCount.setText("Entrants: " + count);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load waitlist count", e);
            tvEntrantsCount.setText("Entrants: Unknown");
        });
    }

    /**
     * Determines whether the lottery has already been conducted and navigates or shows status accordingly.
     */
    private void checkLotteryStatus() {
        lotteryOrchestrator.hasLotteryBeenConducted(eventId).addOnSuccessListener(conducted -> {
            if (conducted) {
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

    /**
     * Updates UI to reflect that the lottery has not been run yet.
     */
    private void showNotConductedView() {
        layoutNotConducted.setVisibility(View.VISIBLE);
        tvLotteryStatus.setText("Status: Not Conducted");
        tvLotteryStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        showLoading(false);
    }

    /**
     * Presents a bottom sheet allowing the organizer to select which lottery method to use.
     */
    private void showLotteryMethodDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_lottery_method, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        android.widget.FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) sheet.setBackgroundResource(android.R.color.transparent);
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        RadioButton radioRandom = dialogView.findViewById(R.id.radio_random);
        RadioButton radioEarlyPriority = dialogView.findViewById(R.id.radio_early_priority);
        CardView cardRandom = dialogView.findViewById(R.id.card_random);
        CardView cardEarlyPriority = dialogView.findViewById(R.id.card_early_priority);
        updateCardSelection(cardRandom, cardEarlyPriority, true);
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
        radioRandom.setOnClickListener(v -> {
            radioEarlyPriority.setChecked(false);
            updateCardSelection(cardRandom, cardEarlyPriority, true);
        });
        radioEarlyPriority.setOnClickListener(v -> {
            radioRandom.setChecked(false);
            updateCardSelection(cardRandom, cardEarlyPriority, false);
        });
        dialogView.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            LotteryMethod method = radioRandom.isChecked() ? LotteryMethod.RANDOM : LotteryMethod.EARLY_PRIORITY_RANDOM;
            dialog.dismiss();
            conductLottery(method);
        });
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Adjusts card stroke styling to reflect which method is selected.
     * @param cardRandom random method card view
     * @param cardEarlyPriority early priority method card view
     * @param randomSelected true if random is chosen
     */
    private void updateCardSelection(CardView cardRandom, CardView cardEarlyPriority, boolean randomSelected) {
        com.google.android.material.card.MaterialCardView randomCard = (com.google.android.material.card.MaterialCardView) cardRandom;
        com.google.android.material.card.MaterialCardView earlyCard = (com.google.android.material.card.MaterialCardView) cardEarlyPriority;
        if (randomSelected) {
            randomCard.setStrokeColor(android.graphics.Color.parseColor("#68F0C9"));
            randomCard.setStrokeWidth(dpToPx(2));
            earlyCard.setStrokeColor(android.graphics.Color.parseColor("#2A3A38"));
            earlyCard.setStrokeWidth(dpToPx(2));
        } else {
            randomCard.setStrokeColor(android.graphics.Color.parseColor("#2A3A38"));
            randomCard.setStrokeWidth(dpToPx(2));
            earlyCard.setStrokeColor(android.graphics.Color.parseColor("#68F0C9"));
            earlyCard.setStrokeWidth(dpToPx(2));
        }
    }

    /**
     * Converts density-independent pixels to raw pixel units.
     * @param dp value in density-independent pixels
     * @return corresponding pixel value
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Executes the lottery workflow: selection, invitation status creation, notifications, and status update.
     * @param method chosen lottery selection method
     */
    private void conductLottery(LotteryMethod method) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network settings.", Toast.LENGTH_LONG).show();
            showLoading(false);
            btnConductLottery.setEnabled(true);
            return;
        }
        showLoading(true);
        btnConductLottery.setEnabled(false);
        String organizerId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown";
        int capacity = currentEvent.getCapacity();
        int numWinners = Math.min(entrantsCount, capacity);
        android.os.Handler timeoutHandler = new android.os.Handler();
        Runnable timeoutRunnable = () -> {
            showLoading(false);
            btnConductLottery.setEnabled(true);
            Toast.makeText(this, "Request timeout. Please check your internet connection and try again.", Toast.LENGTH_LONG).show();
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000);
        lotteryOrchestrator.conductLottery(eventId, organizerId, numWinners, method)
                .addOnSuccessListener(result -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    createInvitationStatuses(result.getWinnerIds())
                            .addOnSuccessListener(aVoid -> sendLotteryNotifications(result.getWinnerIds(), result.getAllEntrantIds())
                                    .addOnSuccessListener(aVoid1 -> {
                                        markLotteryAsConducted();
                                        Toast.makeText(this, "Lottery conducted successfully! " + result.getNumWinners() + " winners selected.", Toast.LENGTH_LONG).show();
                                        navigateToResults();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to send lottery notifications", e);
                                        Toast.makeText(this, "Lottery completed but failed to send notifications: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        showLoading(false);
                                        btnConductLottery.setEnabled(true);
                                    }))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to create invitation statuses", e);
                                Toast.makeText(this, "Lottery completed but failed to create invitations: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                showLoading(false);
                                btnConductLottery.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
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

    /**
     * Checks whether device currently has an active network connection.
     * @return true if connected, false otherwise
     */
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Creates invitation status documents for all selected winners.
     * @param winnerIds list of user IDs selected by the lottery
     * @return commit task representing batch write completion
     */
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

    /**
     * Sends notifications to winners and non-winners reflecting lottery results.
     * @param winnerIds list of user IDs that won
     * @param allEntrantIds list of all entrant user IDs for the lottery
     * @return task for notification batch commit
     */
    private com.google.android.gms.tasks.Task<Void> sendLotteryNotifications(List<String> winnerIds, List<String> allEntrantIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();
        String eventName = currentEvent != null ? currentEvent.getName() : "Event";
        for (String userId : winnerIds) {
            String notificationId = db.collection("notifications").document().getId();
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", userId);
            notification.put("eventId", eventId);
            notification.put("type", "result");
            notification.put("title", "\uD83C\uDF89 Lottery Result - You Won!");
            notification.put("message", "Congratulations! You were selected in the lottery for " + eventName);
            notification.put("createdAt", now);
            notification.put("read", false);
            batch.set(db.collection("notifications").document(notificationId), notification);
        }
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

    /**
     * Updates the event document to reflect that its lottery has been conducted.
     */
    private void markLotteryAsConducted() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .update("lotteryStatus", "conducted")
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update lottery status", e));
    }

    /**
     * Navigates to the invitation management screen for this event and finishes current activity.
     */
    private void navigateToResults() {
        Intent intent = new Intent(this, ManageInvitationsActivity.class);
        intent.putExtra(ManageInvitationsActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
        finish();
    }

    /**
     * Toggles visibility of the progress indicator.
     * @param show true to show loading spinner, false to hide
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Handles action bar up navigation by finishing this activity.
     * @return true once handled
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
