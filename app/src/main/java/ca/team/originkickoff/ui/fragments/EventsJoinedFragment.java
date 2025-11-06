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
import ca.team.originkickoff.adapters.EventAdapter;
import ca.team.originkickoff.models.Event;

public class EventsJoinedFragment extends Fragment implements EventAdapter.OnEventClickListener {
    private static final String TAG = "EventsJoinedFragment";
    private FirebaseFirestore db;
    private RecyclerView rv;
    private EventAdapter adapter;
    private final List<Event> events = new ArrayList<>();

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
        // Try to load a registration/attendance collection; if not present, fallback to showing all events
        Log.d(TAG, "Loading joined events...");
        db.collection("events").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    events.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
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
                    Log.e(TAG, "Error loading events", e);
                    Toast.makeText(requireContext(), "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onEventClick(Event event) {
        // Reuse EventDetailActivity
        startActivity(new android.content.Intent(requireContext(), ca.team.originkickoff.EventDetailActivity.class)
                .putExtra(ca.team.originkickoff.EventDetailActivity.EXTRA_EVENT_ID, event.getId()));
    }
}
