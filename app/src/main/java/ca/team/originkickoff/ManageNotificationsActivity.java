package ca.team.originkickoff;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.services.NotificationService;
import ca.team.originkickoff.services.WaitingListService;

/**
 * Organizer screen for broadcasting notifications to entrant groups (waiting list, chosen, enrolled, cancelled).
 */
public class ManageNotificationsActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "event_id";

    private String eventId;
    private Event currentEvent;
    private TextView tvEventName;
    private TextView tvStats;
    private ProgressBar progressBar;
    private MaterialButton btnNotifyWaitlist;
    private MaterialButton btnNotifyChosen;
    private MaterialButton btnNotifyEnrolled;
    private MaterialButton btnNotifyCancelled;

    private NotificationService notificationService;
    private WaitingListService waitingListService;

    private enum Group { WAITLIST, CHOSEN, ENROLLED, CANCELLED }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_notifications);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeServices();
        initializeViews();
        loadEventAndStats();
        setupButtons();
    }

    private void initializeServices() {
        notificationService = new NotificationService();
        waitingListService = new WaitingListService();
    }

    private void initializeViews() {
        tvEventName = findViewById(R.id.tv_event_name);
        tvStats = findViewById(R.id.tv_stats);
        progressBar = findViewById(R.id.progress_bar);
        btnNotifyWaitlist = findViewById(R.id.btn_notify_waitlist);
        btnNotifyChosen = findViewById(R.id.btn_notify_chosen);
        btnNotifyEnrolled = findViewById(R.id.btn_notify_enrolled);
        btnNotifyCancelled = findViewById(R.id.btn_notify_cancelled);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.manage_notifications_title);
        }
    }

    private void loadEventAndStats() {
        showLoading(true);
        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentEvent = doc.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setId(doc.getId());
                            tvEventName.setText(currentEvent.getName());
                        }
                        loadStats();
                    } else {
                        Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
                        showLoading(false);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void loadStats() {
        waitingListService.countActive(eventId)
                .addOnSuccessListener(waitlistCount -> FirebaseFirestore.getInstance()
                        .collection("invitation_status")
                        .whereEqualTo("event_id", eventId)
                        .get()
                        .addOnSuccessListener(snaps -> {
                            int chosen = 0, enrolled = 0, cancelled = 0;
                            for (QueryDocumentSnapshot d : snaps) {
                                String status = d.getString("status");
                                if ("chosen".equals(status)) chosen++;
                                else if ("enrolled".equals(status)) enrolled++;
                                else if ("cancelled".equals(status)) cancelled++;
                            }
                            tvStats.setText(getString(R.string.stats_format, waitlistCount, chosen, enrolled, cancelled));
                            showLoading(false);
                        })
                        .addOnFailureListener(e -> {
                            tvStats.setText(R.string.stats_unavailable);
                            showLoading(false);
                        }))
                .addOnFailureListener(e -> {
                    tvStats.setText(R.string.stats_unavailable);
                    showLoading(false);
                });
    }

    private void setupButtons() {
        btnNotifyWaitlist.setOnClickListener(v -> fetchGroupAndPromptBroadcast(Group.WAITLIST));
        btnNotifyChosen.setOnClickListener(v -> fetchGroupAndPromptBroadcast(Group.CHOSEN));
        btnNotifyEnrolled.setOnClickListener(v -> fetchGroupAndPromptBroadcast(Group.ENROLLED));
        btnNotifyCancelled.setOnClickListener(v -> fetchGroupAndPromptBroadcast(Group.CANCELLED));
    }

    private void fetchGroupAndPromptBroadcast(Group group) {
        String eventName = currentEvent != null ? currentEvent.getName() : getString(R.string.event_name_placeholder);
        showLoading(true);
        if (group == Group.WAITLIST) {
            waitingListService.getAllActiveUserIds(eventId)
                    .addOnSuccessListener(ids -> { showLoading(false); promptBroadcast(ids, eventName); })
                    .addOnFailureListener(e -> { showLoading(false); Toast.makeText(this, R.string.broadcast_failed, Toast.LENGTH_SHORT).show(); });
            return;
        }
        String status = mapGroupToStatus(group);
        FirebaseFirestore.getInstance().collection("invitation_status")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<String> ids = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snaps) {
                        String uid = d.getString("user_id");
                        if (uid != null && !uid.isEmpty()) ids.add(uid);
                    }
                    showLoading(false);
                    promptBroadcast(ids, eventName);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, R.string.broadcast_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private String mapGroupToStatus(Group g) {
        switch (g) {
            case CHOSEN: return "chosen";
            case ENROLLED: return "enrolled";
            case CANCELLED: return "cancelled";
            default: return "active";
        }
    }

    private void promptBroadcast(List<String> userIds, String eventName) {
        if (userIds.isEmpty()) {
            Toast.makeText(this, R.string.no_notifications, Toast.LENGTH_SHORT).show();
            return;
        }
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        android.widget.EditText titleInput = new android.widget.EditText(this);
        titleInput.setHint(getString(R.string.broadcast_title_hint));
        android.widget.EditText messageInput = new android.widget.EditText(this);
        messageInput.setHint(getString(R.string.broadcast_message_hint));
        messageInput.setMinLines(3);
        messageInput.setGravity(Gravity.TOP | Gravity.START);
        container.addView(titleInput);
        container.addView(messageInput);

        new AlertDialog.Builder(this)
                .setTitle(R.string.broadcast_dialog_title)
                .setMessage(getString(R.string.broadcast_dialog_message, userIds.size()))
                .setView(container)
                .setPositiveButton(R.string.broadcast_send, (dialog, which) -> {
                    String title = titleInput.getText() != null ? titleInput.getText().toString() : null;
                    String message = messageInput.getText() != null ? messageInput.getText().toString() : null;
                    if (message == null || message.trim().isEmpty()) {
                        Toast.makeText(this, R.string.broadcast_message_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    notificationService.notifyWaitingListEntrants(userIds, eventId, eventName, title, message)
                            .addOnSuccessListener(v -> Toast.makeText(this, R.string.broadcast_sent, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, R.string.broadcast_failed, Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(R.string.broadcast_cancel, (d, w) -> d.dismiss())
                .show();
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
