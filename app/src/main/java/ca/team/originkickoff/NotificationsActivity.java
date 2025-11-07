package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ca.team.originkickoff.adapters.NotificationAdapter;
import ca.team.originkickoff.models.NotificationItem;
import ca.team.originkickoff.services.NotificationService;
import ca.team.originkickoff.util.DeviceUtils;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    // Debounce for bottom-nav taps
    private long lastNavTapAtMs = 0L;

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private NotificationService notificationService;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        // Apply window insets to avoid overlapping system bars and keep bottom bar pinned
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notificationsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        notificationService = new NotificationService();

        setupTopBar();
        setupBottomNav();
        setupRecyclerView();
        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload notifications when returning to activity
        loadNotifications();
    }

    private void setupTopBar() {
        ImageButton back = findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerNotifications);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new NotificationAdapter(this::onNotificationClick);
            recyclerView.setAdapter(adapter);
        }
    }

    private void loadNotifications() {
        // Use device ID to get the user, just like other parts of the app
        String deviceId = DeviceUtils.getDeviceId(this);

        Log.d(TAG, "=== LOADING NOTIFICATIONS ===");
        Log.d(TAG, "Device ID: " + deviceId);

        if (deviceId == null) {
            Log.e(TAG, "Device ID is null");
            showEmptyState();
            return;
        }

        showLoading(true);

        // First, get the user document ID from device_id
        db.collection("users")
                .whereEqualTo("device_id", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(userSnapshots -> {
                    if (userSnapshots.isEmpty()) {
                        Log.w(TAG, "No user found for device_id: " + deviceId);
                        showLoading(false);
                        showEmptyState();
                        return;
                    }

                    // Get the user's document ID (this is what's stored in notifications)
                    String userId = userSnapshots.getDocuments().get(0).getId();
                    Log.d(TAG, "Found user ID: " + userId);
                    Log.d(TAG, "Now querying notifications for this user...");

                    // Now fetch notifications for this user
                    notificationService.getNotificationsForUser(userId)
                            .addOnSuccessListener(notifications -> {
                                showLoading(false);
                                Log.d(TAG, "Query completed. Found " + notifications.size() + " notifications");

                                // Format timestamps for display
                                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                                for (NotificationItem item : notifications) {
                                    if (item.getCreatedAt() != null) {
                                        Date date = item.getCreatedAt().toDate();
                                        item.setTimestamp(dateFormat.format(date));
                                    } else {
                                        item.setTimestamp("Recent");
                                    }
                                }

                                if (notifications.isEmpty()) {
                                    Log.d(TAG, "No notifications found for user - showing empty state");
                                    showEmptyState();
                                } else {
                                    Log.d(TAG, "Displaying " + notifications.size() + " notifications");
                                    adapter.setItems(notifications);
                                    recyclerView.setVisibility(View.VISIBLE);
                                    tvEmptyState.setVisibility(View.GONE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to load notifications: " + e.getMessage(), e);
                                Log.e(TAG, "Exception class: " + e.getClass().getName());

                                // Check if it's a missing index error
                                if (e.getMessage() != null && e.getMessage().contains("index")) {
                                    Log.e(TAG, "*** FIRESTORE INDEX REQUIRED ***");
                                    Log.e(TAG, "You need to create a composite index in Firestore.");
                                    Log.e(TAG, "Check the full error message above for a link to create the index.");
                                }

                                showLoading(false);
                                showEmptyState();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user: " + e.getMessage(), e);
                    showLoading(false);
                    showEmptyState();
                });
    }

    private void onNotificationClick(NotificationItem notification) {
        // Mark as read
        if (notification.getId() != null && !notification.isRead()) {
            notificationService.markAsRead(notification.getId())
                    .addOnSuccessListener(aVoid -> {
                        notification.setRead(true);
                        adapter.notifyDataSetChanged();
                    });
        }

        // Show dialog if it's a result type
        if ("result".equals(notification.getType()) && notification.getEventId() != null) {
            showResultDialog(notification);
        }
    }

    private void showResultDialog(NotificationItem notification) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_lottery_result, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        MaterialButton btnGoToEvent = dialogView.findViewById(R.id.btnGoToEvent);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

        tvTitle.setText(notification.getTitle());
        tvMessage.setText(notification.getMessage());

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        btnGoToEvent.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToEvent(notification.getEventId());
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void navigateToEvent(String eventId) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No notifications yet");
        }
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navEvents = findViewById(R.id.navEvents);
        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        if (navHome == null || navEvents == null || navNotifications == null || navProfile == null) return;

        // Highlight current tab (Notifications)
        ImageView iv = findViewById(R.id.ivNotifications);
        TextView tv = findViewById(R.id.tvNotifications);
        if (iv != null) iv.setColorFilter(0xFF00D9C5, android.graphics.PorterDuff.Mode.SRC_IN);
        if (tv != null) tv.setTextColor(0xFF00D9C5);

        navHome.setOnClickListener(v -> navigateBottomTab(MainActivity.class));
        navEvents.setOnClickListener(v -> navigateBottomTab(MyEventsActivity.class));
        navNotifications.setOnClickListener(v -> { /* already here */ });
        navProfile.setOnClickListener(v -> navigateBottomTab(ProfileActivity.class));
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
}