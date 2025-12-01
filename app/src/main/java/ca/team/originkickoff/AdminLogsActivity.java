package ca.team.originkickoff;

import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.team.originkickoff.adapters.NotificationLogAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.models.NotificationLog;
import ca.team.originkickoff.models.User;

/**
 * Admin Logs screen for reviewing notifications sent by organizers.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Admin-only access enforced via a Firestore lookup on the current device's user (is_admin).</li>
 *   <li>Pulls paginated notification data from the "notifications" collection ordered by createdAt.</li>
 *   <li>Augments each page with event and user display names using batched whereIn queries.</li>
 *   <li>Client-side text filter by event name/id; server-side date range filtering.</li>
 *   <li>Infinite scroll pagination with startAfter cursors.</li>
 * </ul>
 */
public class AdminLogsActivity extends AppCompatActivity {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private RecyclerView rv;
    private View tvEmpty;
    private NotificationLogAdapter adapter;

    private TextInputEditText etEventSearch;
    private TextView btnDateFrom, btnDateTo, btnClearFilters;

    private Date fromDate = null;
    private Date toDate = null;
    private String eventQuery = "";

    // Pagination state
    private static final int PAGE_SIZE = 20;
    private DocumentSnapshot lastSnapshot = null;
    private boolean isLoading = false;
    private boolean hasMore = true;

    private final List<NotificationLog> accumulated = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_logs);
        AdminNavHelper.setup(this, AdminNavHelper.Tab.LOGS);

        // Enforce admin access before wiring the rest
        enforceAdminAccessAndInit();
    }

    /**
     * Verifies admin access based on the device-bound user document and initializes the UI on success.
     */
    private void enforceAdminAccessAndInit() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        db.collection("users")
                .whereEqualTo("device_id", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean isAdmin = false;
                    if (snapshot != null && !snapshot.isEmpty()) {
                        Boolean flag = snapshot.getDocuments().get(0).getBoolean("is_admin");
                        isAdmin = flag != null && flag;
                    }
                    if (!isAdmin) {
                        Toast.makeText(this, getString(R.string.admin_access_only), Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    // Proceed with normal init
                    initViewsAndData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.admin_access_only), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    /**
     * Wires views, sets up filters and pagination listeners, and triggers the initial load.
     */
    private void initViewsAndData() {
        rv = findViewById(R.id.rvLogs);
        tvEmpty = findViewById(R.id.tvEmpty);
        etEventSearch = findViewById(R.id.etEventSearch);
        btnDateFrom = findViewById(R.id.btnDateFrom);
        btnDateTo = findViewById(R.id.btnDateTo);
        btnClearFilters = findViewById(R.id.btnClearFilters);

        adapter = new NotificationLogAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Filters
        if (etEventSearch != null) {
            etEventSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    eventQuery = s != null ? s.toString().trim().toLowerCase() : "";
                    resetAndLoad();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (btnDateFrom != null) btnDateFrom.setOnClickListener(v -> pickDate(true));
        if (btnDateTo != null) btnDateTo.setOnClickListener(v -> pickDate(false));
        if (btnClearFilters != null) btnClearFilters.setOnClickListener(v -> {
            fromDate = null; toDate = null; eventQuery = "";
            if (etEventSearch != null) etEventSearch.setText("");
            resetAndLoad();
        });

        // Pagination scroll listener
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) return;
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;
                int visible = lm.getChildCount();
                int total = lm.getItemCount();
                int first = lm.findFirstVisibleItemPosition();
                if (!isLoading && hasMore && (visible + first) >= (total - 4)) {
                    loadNextPage();
                }
            }
        });

        resetAndLoad();
    }

    /**
     * Opens a date picker and applies either the From or To filter, then reloads data.
     *
     * @param isFrom true to set the lower bound (From), false for upper bound (To).
     */
    private void pickDate(boolean isFrom) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isFrom ? "From date" : "To date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setTheme(R.style.ThemeOverlay_KickOff_DatePicker)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            java.util.Calendar utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            java.util.Calendar local = java.util.Calendar.getInstance();
            local.set(java.util.Calendar.YEAR, utc.get(java.util.Calendar.YEAR));
            local.set(java.util.Calendar.MONTH, utc.get(java.util.Calendar.MONTH));
            local.set(java.util.Calendar.DAY_OF_MONTH, utc.get(java.util.Calendar.DAY_OF_MONTH));
            if (isFrom) {
                local.set(java.util.Calendar.HOUR_OF_DAY, 0);
                local.set(java.util.Calendar.MINUTE, 0);
                local.set(java.util.Calendar.SECOND, 0);
                local.set(java.util.Calendar.MILLISECOND, 0);
                fromDate = local.getTime();
            } else {
                local.set(java.util.Calendar.HOUR_OF_DAY, 23);
                local.set(java.util.Calendar.MINUTE, 59);
                local.set(java.util.Calendar.SECOND, 59);
                local.set(java.util.Calendar.MILLISECOND, 999);
                toDate = local.getTime();
            }
            resetAndLoad();
        });
        picker.show(getSupportFragmentManager(), isFrom ? "admin_logs_from" : "admin_logs_to");
    }

    /**
     * Resets pagination state and loads the first page according to current filters.
     */
    private void resetAndLoad() {
        lastSnapshot = null;
        hasMore = true;
        accumulated.clear();
        adapter.setItems(new ArrayList<>());
        loadNextPage();
    }

    /**
     * Builds the base Firestore query against the notifications collection with date constraints.
     *
     * @return configured query limited by {@link #PAGE_SIZE}
     */
    private Query buildBaseQuery() {
        // Pull from user notifications collection; organizers trigger these writes
        Query q = db.collection("notifications").orderBy("createdAt", Query.Direction.DESCENDING);
        if (fromDate != null) q = q.whereGreaterThanOrEqualTo("createdAt", new Timestamp(fromDate));
        if (toDate != null) q = q.whereLessThanOrEqualTo("createdAt", new Timestamp(toDate));
        return q.limit(PAGE_SIZE);
    }

    /**
     * Loads the next page of results using the current cursor and appends them to the adapter.
     */
    private void loadNextPage() {
        if (!hasMore || isLoading) return;
        isLoading = true;
        Query q = buildBaseQuery();
        if (lastSnapshot != null) q = q.startAfter(lastSnapshot);
        q.get().addOnCompleteListener(task -> {
            isLoading = false;
            if (!task.isSuccessful() || task.getResult() == null) {
                showEmptyIfNeeded();
                return;
            }
            QuerySnapshot snap = task.getResult();
            int fetchedCount = snap.size();
            if (fetchedCount == 0) {
                hasMore = false;
                showEmptyIfNeeded();
                return;
            }
            List<QueryDocumentSnapshot> docs = new ArrayList<>();
            DocumentSnapshot last = null;
            for (QueryDocumentSnapshot d : snap) {
                docs.add(d);
                last = d;
            }
            if (last != null) lastSnapshot = last;
            if (fetchedCount < PAGE_SIZE) hasMore = false;

            augmentAndAppend(docs);
        });
    }

    /**
     * Enriches raw notification documents with event and user names via batched lookups,
     * applies client-side text filter, and appends the transformed items to the list.
     *
     * @param docs the page of notification documents to augment
     */
    private void augmentAndAppend(List<QueryDocumentSnapshot> docs) {
        // Collect unique ids to batch fetch events and users
        Set<String> eventIds = new HashSet<>();
        Set<String> userIds = new HashSet<>();
        for (QueryDocumentSnapshot d : docs) {
            String eId = d.getString("eventId");
            if (eId != null && !eId.isEmpty()) eventIds.add(eId);
            String uId = d.getString("userId");
            if (uId != null && !uId.isEmpty()) userIds.add(uId);
        }

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        // Batch events
        List<List<String>> eventChunks = chunk(new ArrayList<>(eventIds), 10);
        for (List<String> chunk : eventChunks) {
            if (!chunk.isEmpty()) tasks.add(db.collection("events").whereIn(FieldPath.documentId(), chunk).get());
        }
        // Batch users
        List<List<String>> userChunks = chunk(new ArrayList<>(userIds), 10);
        for (List<String> chunk : userChunks) {
            if (!chunk.isEmpty()) tasks.add(db.collection("users").whereIn(FieldPath.documentId(), chunk).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            Map<String, Event> eventMap = new HashMap<>();
            Map<String, User> userMap = new HashMap<>();
            for (Object o : results) {
                if (o instanceof QuerySnapshot) {
                    for (DocumentSnapshot doc : ((QuerySnapshot) o).getDocuments()) {
                        if ("events".equals(doc.getReference().getParent().getId())) {
                            Event e = doc.toObject(Event.class);
                            if (e != null) { e.setId(doc.getId()); eventMap.put(doc.getId(), e); }
                        } else if ("users".equals(doc.getReference().getParent().getId())) {
                            User u = doc.toObject(User.class);
                            if (u != null) { u.setId(doc.getId()); userMap.put(doc.getId(), u); }
                        }
                    }
                }
            }

            List<NotificationLog> page = new ArrayList<>();
            for (QueryDocumentSnapshot d : docs) {
                NotificationLog log = new NotificationLog();
                String eventId = d.getString("eventId");
                String recipientId = d.getString("userId");
                log.setEventId(eventId);
                log.setRecipientId(recipientId);
                log.setType(d.getString("type"));
                log.setCreatedAt(d.getTimestamp("createdAt"));
                log.setId(d.getId());

                Event ev = eventId != null ? eventMap.get(eventId) : null;
                if (ev != null) {
                    log.setEventName(ev.getName());
                    log.setSenderId(ev.getOrganizerId());
                    log.setSenderName(ev.getOrganizerName());
                }
                User recip = recipientId != null ? userMap.get(recipientId) : null;
                if (recip != null) {
                    log.setRecipientName(recip.getDisplayName());
                }

                // In-memory event filter (by name or id)
                if (!eventQuery.isEmpty()) {
                    String name = log.getEventName() != null ? log.getEventName().toLowerCase() : "";
                    String id = eventId != null ? eventId.toLowerCase() : "";
                    if (!name.contains(eventQuery) && !id.contains(eventQuery)) {
                        continue;
                    }
                }
                page.add(log);
            }

            accumulated.addAll(page);
            adapter.setItems(accumulated);
            showEmptyIfNeeded();
        }).addOnFailureListener(e -> {
            // Fallback: append minimally without augmentation
            List<NotificationLog> page = new ArrayList<>();
            for (QueryDocumentSnapshot d : docs) {
                NotificationLog log = new NotificationLog();
                log.setId(d.getId());
                log.setEventId(d.getString("eventId"));
                log.setRecipientId(d.getString("userId"));
                log.setType(d.getString("type"));
                log.setCreatedAt(d.getTimestamp("createdAt"));
                page.add(log);
            }
            accumulated.addAll(page);
            adapter.setItems(accumulated);
            showEmptyIfNeeded();
        });
    }

    /**
     * Utility: splits a list into fixed-size chunks to respect Firestore whereIn limits.
     *
     * @param list source list
     * @param size chunk size (max 10 for Firestore whereIn)
     * @param <T> element type
     * @return list of sublists of at most {@code size}
     */
    private static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return chunks;
    }

    /**
     * Toggles empty-state label visibility based on adapter item count.
     */
    private void showEmptyIfNeeded() {
        if (tvEmpty != null) tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}
