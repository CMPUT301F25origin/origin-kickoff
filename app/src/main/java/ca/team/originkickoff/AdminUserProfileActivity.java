package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.adapters.EventAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.models.User;

/**
 * Admin User Profile screen showing a user's info, organized events, and joined events.
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Loads user details from Firestore and binds display name, email, and device id.</li>
 *   <li>Lists organized events (events.organizerId == userId) and joined events from waiting_list_entries.</li>
 *   <li>Allows deleting the profile, also removing any waiting list entries, then finishes the screen.</li>
 *   <li>Bottom admin navigation is wired via {@link AdminNavHelper} with the Users tab active.</li>
 * </ul>
 */
public class AdminUserProfileActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "extra_user_id";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ImageView btnBack, btnEdit;
    private TextView txtName, txtEmail, txtDeviceId;
    private RecyclerView rvOrganized, rvJoined;
    private EventAdapter organizedAdapter, joinedAdapter;

    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_profile);
        AdminNavHelper.setup(this, AdminNavHelper.Tab.USERS);

        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtDeviceId = findViewById(R.id.txtDeviceId);
        rvOrganized = findViewById(R.id.rvOrganizedEvents);
        rvJoined = findViewById(R.id.rvJoinedEvents);

        rvOrganized.setLayoutManager(new LinearLayoutManager(this));
        rvJoined.setLayoutManager(new LinearLayoutManager(this));
        organizedAdapter = new EventAdapter(null);
        joinedAdapter = new EventAdapter(null);
        rvOrganized.setAdapter(organizedAdapter);
        rvJoined.setAdapter(joinedAdapter);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnEdit != null) btnEdit.setOnClickListener(v -> Toast.makeText(this, R.string.edit_profile, Toast.LENGTH_SHORT).show());

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, R.string.failed_to_load_users, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUser(userId);
        loadOrganizedEvents(userId);
        loadJoinedEvents(userId);

        View btnDelete = findViewById(R.id.btnDeleteProfile);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> deleteProfile(userId));
        }
    }

    /**
     * Loads the Firestore user document by id and binds its fields to UI.
     *
     * @param userId Firestore document id of the user
     */
    private void loadUser(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(this::bindUser)
                .addOnFailureListener(e -> Toast.makeText(this, R.string.failed_to_load_users, Toast.LENGTH_SHORT).show());
    }

    /**
     * Binds a user snapshot to on-screen text views, applying fallbacks where necessary.
     * Finishes the activity if the snapshot is invalid.
     *
     * @param snapshot Firestore document snapshot of the user
     */
    private void bindUser(DocumentSnapshot snapshot) {
        if (!snapshot.exists()) {
            Toast.makeText(this, R.string.failed_to_load_users, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        User u = snapshot.toObject(User.class);
        if (u == null) {
            Toast.makeText(this, R.string.failed_to_load_users, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        u.setId(snapshot.getId());
        String name = u.getDisplayName() != null && !u.getDisplayName().isEmpty() ? u.getDisplayName() : getString(R.string.unknown);
        String email = u.getEmail() != null ? u.getEmail() : "";
        String device = u.getDeviceId() != null ? u.getDeviceId() : "—";
        if (txtName != null) txtName.setText(name);
        if (txtEmail != null) txtEmail.setText(email);
        if (txtDeviceId != null) txtDeviceId.setText(getString(R.string.device_id, device));
    }

    /**
     * Queries events where this user is the organizer and sends them to adapter.
     *
     * @param userId user id to match organizerId field
     */
    private void loadOrganizedEvents(String userId) {
        db.collection("events").whereEqualTo("organizerId", userId).get()
                .addOnSuccessListener(this::bindOrganizedEvents)
                .addOnFailureListener(e -> organizedAdapter.setEvents(new ArrayList<>()));
    }

    /**
     * Converts query snapshot of organized events into model list and updates adapter.
     *
     * @param snaps query snapshot of event documents
     */
    private void bindOrganizedEvents(QuerySnapshot snaps) {
        List<Event> out = new ArrayList<>();
        if (snaps != null) {
            for (DocumentSnapshot s : snaps.getDocuments()) {
                Event e = s.toObject(Event.class);
                if (e != null) {
                    e.setId(s.getId());
                    out.add(e);
                }
            }
        }
        organizedAdapter.setEvents(out);
    }

    /**
     * Loads events the user has actively joined by mining waiting list entries then fetching event docs in chunks.
     *
     * @param userId user document id used in waiting_list_entries user_id field
     */
    private void loadJoinedEvents(String userId) {
        // joined events => events where waiting_list_entries has state==active for this user
        db.collection("waiting_list_entries")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("state", "active")
                .get()
                .addOnSuccessListener(waitlist -> {
                    List<String> eventIds = new ArrayList<>();
                    if (waitlist != null) {
                        for (DocumentSnapshot s : waitlist.getDocuments()) {
                            String eventId = s.getString("event_id");
                            if (eventId != null) eventIds.add(eventId);
                        }
                    }
                    if (eventIds.isEmpty()) {
                        joinedAdapter.setEvents(new ArrayList<>());
                        return;
                    }
                    // Fetch all events by document id (whereIn supports up to 10; chunk if needed)
                    List<Event> result = new ArrayList<>();
                    List<List<String>> chunks = chunk(eventIds, 10);
                    List<com.google.android.gms.tasks.Task<QuerySnapshot>> tasks = new ArrayList<>();
                    for (List<String> chunk : chunks) {
                        tasks.add(db.collection("events").whereIn(FieldPath.documentId(), chunk).get());
                    }
                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(list -> {
                        for (Object o : list) {
                            if (o instanceof QuerySnapshot) {
                                for (DocumentSnapshot doc : ((QuerySnapshot) o).getDocuments()) {
                                    Event e = doc.toObject(Event.class);
                                    if (e != null) {
                                        e.setId(doc.getId());
                                        result.add(e);
                                    }
                                }
                            }
                        }
                        joinedAdapter.setEvents(result);
                    }).addOnFailureListener(e -> joinedAdapter.setEvents(new ArrayList<>()));
                })
                .addOnFailureListener(e -> joinedAdapter.setEvents(new ArrayList<>()));
    }

    /**
     * Utility to split a list into fixed-size sublists.
     *
     * @param list source list
     * @param size max chunk size (>0)
     * @param <T>  element type
     * @return list of chunks preserving order
     */
    private static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return chunks;
    }

    /**
     * Deletes the user profile and associated waiting list entries, then finishes activity.
     *
     * @param userId Firestore user document id to delete
     */
    private void deleteProfile(String userId) {
        // Delete user document and related waitlist entries
        db.collection("users").document(userId).delete()
                .addOnSuccessListener(v -> {
                    // also delete their waitlist entries
                    db.collection("waiting_list_entries").whereEqualTo("user_id", userId).get()
                            .addOnSuccessListener(snaps -> {
                                List<com.google.android.gms.tasks.Task<Void>> deletions = new ArrayList<>();
                                if (snaps != null) {
                                    for (DocumentSnapshot s : snaps.getDocuments()) {
                                        deletions.add(s.getReference().delete());
                                    }
                                }
                                Tasks.whenAll(deletions).addOnCompleteListener(t -> {
                                    Toast.makeText(this, R.string.profile_deleted, Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, R.string.profile_deleted, Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.failed_to_load_users, Toast.LENGTH_SHORT).show());
    }
}
