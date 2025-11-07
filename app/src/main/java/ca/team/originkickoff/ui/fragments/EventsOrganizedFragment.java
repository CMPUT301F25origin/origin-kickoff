/*
 * Shows events organized by the current user with real-time Firestore queries.
 * Helps organizers review and navigate into their own events.
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.R;
import ca.team.originkickoff.adapters.OrganizedEventAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.util.DeviceUtils;

/**
 * Fragment that lists the events organized by the signed-in (device) user.
 */
public class EventsOrganizedFragment extends Fragment implements OrganizedEventAdapter.OnEventClickListener {
    private static final String TAG = "EventsOrganizedFrag";
    private FirebaseFirestore db;
    private RecyclerView rv;
    private OrganizedEventAdapter adapter;
    private final List<Event> events = new ArrayList<>();

    /**
     * Inflates the events list layout and initializes the RecyclerView and adapter.
     *
     * @param inflater  layout inflater
     * @param container parent view group
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
            v = inflater.inflate(R.layout.fragment_events_list, container, false);
        }

        rv = v.findViewById(getResources().getIdentifier("rvEvents", "id", requireContext().getPackageName()));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrganizedEventAdapter(this);
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadOrganizedEvents();

        return v;
    }

    /**
     * Resolves the user's organizer role and queries events they own.
     */
    private void loadOrganizedEvents() {
        // Get current user ID
        String deviceId = DeviceUtils.getDeviceId(requireContext());
        if (deviceId == null) {
            Toast.makeText(requireContext(), "Unable to identify user", Toast.LENGTH_SHORT).show();
            adapter.setEvents(new ArrayList<>());
            return;
        }

        db.collection("users")
                .whereEqualTo("device_id", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(userSnapshots -> {
                    if (userSnapshots.isEmpty()) {
                        Log.w(TAG, "No user found for device_id");
                        adapter.setEvents(new ArrayList<>());
                        return;
                    }

                    String userId = userSnapshots.getDocuments().get(0).getId();
                    // Support multiple field spellings
                    Boolean isOrganizer = userSnapshots.getDocuments().get(0).getBoolean("is_organizer");
                    if (isOrganizer == null) isOrganizer = userSnapshots.getDocuments().get(0).getBoolean("is_organiser");
                    if (isOrganizer == null) {
                        Object camel = userSnapshots.getDocuments().get(0).get("isOrganizer");
                        if (camel instanceof Boolean) isOrganizer = (Boolean) camel;
                    }

                    if (isOrganizer == null || !isOrganizer) {
                        // Fallback: infer organizer role if any events exist with organizerId equal to either userId or deviceId
                        List<String> possibleIds = new java.util.ArrayList<>();
                        possibleIds.add(userId);
                        if (!deviceId.equals(userId)) possibleIds.add(deviceId);
                        db.collection("events")
                                .whereIn("organizerId", possibleIds)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(eventSnap -> {
                                    if (!eventSnap.isEmpty()) {
                                        Log.d(TAG, "User inferred as organizer via existing events");
                                        loadEventsForOrganizerIds(possibleIds);
                                    } else {
                                        Log.d(TAG, "User is not an organizer (no matching events)");
                                        adapter.setEvents(new ArrayList<>());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Organizer inference query failed", e);
                                    adapter.setEvents(new ArrayList<>());
                                });
                    } else {
                        // Normal path: user explicitly marked organizer
                        List<String> organizerIds = new java.util.ArrayList<>();
                        organizerIds.add(userId);
                        if (!deviceId.equals(userId)) organizerIds.add(deviceId); // include legacy id variant
                        loadEventsForOrganizerIds(organizerIds);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user", e);
                    Toast.makeText(requireContext(), "Error loading user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads events for any of the provided organizer IDs, with a fallback path if whereIn fails.
     *
     * @param organizerIds one or more possible organizer identifiers
     */
    private void loadEventsForOrganizerIds(List<String> organizerIds) {
        // Use whereIn for up to two IDs; fallback to sequential if whereIn fails
        db.collection("events")
                .whereIn("organizerId", organizerIds)
                .get()
                .addOnSuccessListener(eventSnapshots -> {
                    events.clear();
                    for (QueryDocumentSnapshot doc : eventSnapshots) {
                        try {
                            Event e = doc.toObject(Event.class);
                            e.setId(doc.getId());
                            events.add(e);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing event", ex);
                        }
                    }
                    adapter.setEvents(events);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "whereIn failed, falling back to individual fetches: " + e.getMessage());
                    events.clear();
                    java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(organizerIds.size());
                    for (String oid : organizerIds) {
                        db.collection("events").whereEqualTo("organizerId", oid).get()
                                .addOnSuccessListener(snap -> {
                                    for (QueryDocumentSnapshot doc : snap) {
                                        try {
                                            Event ev = doc.toObject(Event.class);
                                            ev.setId(doc.getId());
                                            events.add(ev);
                                        } catch (Exception ex) {
                                            Log.e(TAG, "Parse error", ex);
                                        }
                                    }
                                    if (pending.decrementAndGet() == 0) adapter.setEvents(events);
                                })
                                .addOnFailureListener(err -> {
                                    Log.e(TAG, "Fallback organizerId query failed", err);
                                    if (pending.decrementAndGet() == 0) adapter.setEvents(events);
                                });
                    }
                });
    }

    /**
     * Handles click on an event to open its details.
     *
     * @param event selected event
     */
    @Override
    public void onEventClick(Event event) {
        startActivity(new android.content.Intent(requireContext(), ca.team.originkickoff.EventDetailActivity.class)
                .putExtra(ca.team.originkickoff.EventDetailActivity.EXTRA_EVENT_ID, event.getId()));
    }
}
