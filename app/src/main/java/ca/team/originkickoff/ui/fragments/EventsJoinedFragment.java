/*
 * Shows events the current user has joined or is waitlisted for, with real-time status.
 * Merges event details with personalized invitation/lottery outcome statuses.
 */
package ca.team.originkickoff.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.R;
import ca.team.originkickoff.adapters.EventAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.util.DeviceUtils;

/**
 * Fragment listing events the user has joined, including dynamic lottery/invitation status.
 */
public class EventsJoinedFragment extends Fragment implements EventAdapter.OnEventClickListener {
    private static final String TAG = "EventsJoinedFragment";
    private FirebaseFirestore db;
    private RecyclerView rv;
    private EventAdapter adapter;
    private final List<Event> events = new ArrayList<>();
    private final Map<String, String> eventStatusMap = new HashMap<>();

    private ListenerRegistration waitlistListener; // real-time updates
    private String currentUserId; // cached user id

    /**
     * Inflates the list layout and configures RecyclerView with the adapter.
     *
     * @param inflater  layout inflater
     * @param container parent container
     * @param savedInstanceState state bundle
     * @return inflated root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutId = getResources().getIdentifier("fragment_events_list", "layout", requireContext().getPackageName());
        View v;
        if (layoutId != 0) {
            v = inflater.inflate(layoutId, container, false);
        } else {
            // fallback to R if identifier lookup fails in this environment
            v = inflater.inflate(R.layout.fragment_events_list, container, false);
        }

        rv = v.findViewById(getResources().getIdentifier("rvEvents", "id", requireContext().getPackageName()));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EventAdapter(this);
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Replace one-off load with real-time listener
        resolveUserAndListen();

        return v;
    }

    /**
     * Resolves the current user and attaches the real-time waitlist listener.
     */
    private void resolveUserAndListen() {
        String deviceId = DeviceUtils.getDeviceId(requireContext());
        if (deviceId == null) {
            Log.e(TAG, "Device ID null - cannot load joined events");
            adapter.setEvents(new ArrayList<>());
            return;
        }
        db.collection("users")
                .whereEqualTo("device_id", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) {
                        Log.w(TAG, "No user found for device_id");
                        adapter.setEvents(new ArrayList<>());
                        return;
                    }
                    currentUserId = q.getDocuments().get(0).getId();
                    attachWaitlistListener();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to resolve user", e));
    }

    /**
     * Subscribes to active waitlist entries to keep the joined list in sync.
     */
    private void attachWaitlistListener() {
        if (currentUserId == null) return;
        if (waitlistListener != null) {
            waitlistListener.remove();
        }
        Log.d(TAG, "Attaching waitlist listener for user " + currentUserId);
        waitlistListener = db.collection("waiting_list_entries")
                .whereEqualTo("user_id", currentUserId)
                .whereEqualTo("state", "active")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Log.e(TAG, "Waitlist listener error", err);
                        return;
                    }
                    if (snap == null) {
                        adapter.setEvents(new ArrayList<>());
                        return;
                    }
                    List<String> eventIds = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String eid = d.getString("event_id");
                        if (eid != null) eventIds.add(eid);
                    }
                    Log.d(TAG, "Waitlist listener received " + eventIds.size() + " event IDs");
                    if (eventIds.isEmpty()) {
                        events.clear();
                        eventStatusMap.clear();
                        adapter.setEvents(new ArrayList<>());
                    } else {
                        fetchEventsInBatches(eventIds);
                    }
                });
    }

    /**
     * Fetches event documents referenced by waitlist entries and updates the list.
     *
     * @param allEventIds event IDs from the user's waitlist entries
     */
    private void fetchEventsInBatches(List<String> allEventIds) {
        events.clear();
        eventStatusMap.clear();
        // Fetch each event doc individually; simpler and avoids whereIn and batch completion race
        for (String eid : allEventIds) {
            db.collection("events").document(eid).get()
                    .addOnSuccessListener(this::handleEventDoc)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to load event doc " + eid, e));
        }
    }

    /**
     * Adds or replaces an event in the aggregated list and refreshes the adapter.
     *
     * @param event event to upsert in the list
     */
    private void addOrReplaceEvent(Event event) {
        int idx = -1;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(event.getId())) { idx = i; break; }
        }
        if (idx >= 0) {
            events.set(idx, event);
        } else {
            events.add(event);
        }
        adapter.setEventsWithStatus(new ArrayList<>(events), new HashMap<>(eventStatusMap));
    }

    /**
     * Parses an event document, determines personalized status, and updates the list.
     *
     * @param eventDoc Firestore document for the event
     */
    private void handleEventDoc(DocumentSnapshot eventDoc) {
        if (eventDoc == null || !eventDoc.exists()) return;
        try {
            Event event = eventDoc.toObject(Event.class);
            if (event == null) return;
            event.setId(eventDoc.getId());
            String lotteryStatus = eventDoc.getString("lotteryStatus");
            if ("conducted".equals(lotteryStatus)) {
                // Determine user-specific status (invitation_status collection)
                db.collection("invitation_status")
                        .whereEqualTo("event_id", event.getId())
                        .whereEqualTo("user_id", currentUserId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(invSnap -> {
                            String status;
                            if (!invSnap.isEmpty()) {
                                String invitationStatus = invSnap.getDocuments().get(0).getString("status");
                                if ("chosen".equals(invitationStatus) || "enrolled".equals(invitationStatus)) {
                                    status = "YOU WERE SELECTED";
                                } else {
                                    status = "YOU WERE NOT SELECTED";
                                }
                            } else {
                                status = "YOU WERE NOT SELECTED";
                            }
                            eventStatusMap.put(event.getId(), status);
                            addOrReplaceEvent(event);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Invitation status check failed", e);
                            eventStatusMap.put(event.getId(), "YOU WERE NOT SELECTED");
                            addOrReplaceEvent(event);
                        });
            } else {
                eventStatusMap.put(event.getId(), "YET TO DRAW");
                addOrReplaceEvent(event);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error parsing event doc", ex);
        }
    }

    /**
     * Cleans up listeners on view destruction.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (waitlistListener != null) {
            waitlistListener.remove();
            waitlistListener = null;
        }
    }

    /**
     * Handles click on an event to show details.
     *
     * @param event selected event
     */
    @Override
    public void onEventClick(Event event) {
        // Reuse EventDetailActivity
        startActivity(new android.content.Intent(requireContext(), ca.team.originkickoff.EventDetailActivity.class)
                .putExtra(ca.team.originkickoff.EventDetailActivity.EXTRA_EVENT_ID, event.getId()));
    }
}
