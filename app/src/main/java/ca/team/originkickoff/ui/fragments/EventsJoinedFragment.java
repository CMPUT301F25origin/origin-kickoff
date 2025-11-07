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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.R;
import ca.team.originkickoff.adapters.EventAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.util.DeviceUtils;

public class EventsJoinedFragment extends Fragment implements EventAdapter.OnEventClickListener {
    private static final String TAG = "EventsJoinedFragment";
    private FirebaseFirestore db;
    private RecyclerView rv;
    private EventAdapter adapter;
    private final List<Event> events = new ArrayList<>();
    private final Map<String, String> eventStatusMap = new HashMap<>();

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

        loadJoinedEvents();

        return v;
    }

    private void loadJoinedEvents() {
        Log.d(TAG, "Loading joined events...");

        // Get current user ID
        String deviceId = DeviceUtils.getDeviceId(requireContext());
        if (deviceId == null) {
            Toast.makeText(requireContext(), "Unable to identify user", Toast.LENGTH_SHORT).show();
            return;
        }

        // First, get the user ID from device_id
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

                    // Query waiting_list_entries for this user
                    db.collection("waiting_list_entries")
                            .whereEqualTo("user_id", userId)
                            .whereEqualTo("state", "active")
                            .get()
                            .addOnSuccessListener(entrySnapshots -> {
                                if (entrySnapshots.isEmpty()) {
                                    Log.d(TAG, "No waiting list entries found");
                                    adapter.setEvents(new ArrayList<>());
                                    return;
                                }

                                // Get all event IDs
                                List<String> eventIds = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : entrySnapshots) {
                                    String eventId = doc.getString("event_id");
                                    if (eventId != null) {
                                        eventIds.add(eventId);
                                    }
                                }

                                if (eventIds.isEmpty()) {
                                    adapter.setEvents(new ArrayList<>());
                                    return;
                                }

                                // Load events and check their lottery status
                                loadEventsWithStatus(userId, eventIds);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading waiting list entries", e);
                                Toast.makeText(requireContext(), "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user", e);
                    Toast.makeText(requireContext(), "Error loading user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadEventsWithStatus(String userId, List<String> eventIds) {
        events.clear();
        eventStatusMap.clear();

        // Load each event and check lottery status
        for (String eventId : eventIds) {
            db.collection("events").document(eventId).get()
                    .addOnSuccessListener(eventDoc -> {
                        if (eventDoc.exists()) {
                            try {
                                Event event = eventDoc.toObject(Event.class);
                                event.setId(eventDoc.getId());

                                // Check lottery status
                                String lotteryStatus = eventDoc.getString("lotteryStatus");

                                if ("conducted".equals(lotteryStatus)) {
                                    // Check invitation_status to get user's result
                                    db.collection("invitation_status")
                                            .whereEqualTo("event_id", eventId)
                                            .whereEqualTo("user_id", userId)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener(invitationSnapshots -> {
                                                String status = "YET TO DRAW";
                                                if (!invitationSnapshots.isEmpty()) {
                                                    String invitationStatus = invitationSnapshots.getDocuments().get(0).getString("status");
                                                    if ("chosen".equals(invitationStatus) || "enrolled".equals(invitationStatus)) {
                                                        status = "YOU WERE SELECTED";
                                                    } else {
                                                        status = "YOU WERE NOT SELECTED";
                                                    }
                                                }
                                                eventStatusMap.put(eventId, status);
                                                events.add(event);
                                                adapter.setEventsWithStatus(events, eventStatusMap);
                                            })
                                            .addOnFailureListener(e -> {
                                                eventStatusMap.put(eventId, "YET TO DRAW");
                                                events.add(event);
                                                adapter.setEventsWithStatus(events, eventStatusMap);
                                            });
                                } else {
                                    // Lottery not conducted yet
                                    eventStatusMap.put(eventId, "YET TO DRAW");
                                    events.add(event);
                                    adapter.setEventsWithStatus(events, eventStatusMap);
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error parsing event", ex);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error loading event: " + eventId, e));
        }
    }

    @Override
    public void onEventClick(Event event) {
        // Reuse EventDetailActivity
        startActivity(new android.content.Intent(requireContext(), ca.team.originkickoff.EventDetailActivity.class)
                .putExtra(ca.team.originkickoff.EventDetailActivity.EXTRA_EVENT_ID, event.getId()));
    }
}
