package ca.team.originkickoff.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ca.team.originkickoff.R;
import ca.team.originkickoff.CreateEventActivity;
import ca.team.originkickoff.adapters.EventAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.services.FirebaseEventService;

public class EventListFragment extends Fragment implements EventAdapter.OnEventClickListener {
    private static final String TAG = "EventListFragment";
    private RecyclerView eventsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyState;
    private LinearLayout loadingState;
    private EditText searchInput;
    private ImageButton filterButton;

    private EventAdapter adapter;
    private FirebaseEventService firebaseEventService;
    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupSearchAndFilter();

        // Initialize the add button (in the Activity layout) and set click to open CreateEventActivity
        try {
            ImageView ivAddEvent = requireActivity().findViewById(R.id.ivAddEvent);
            if (ivAddEvent != null) {
                ivAddEvent.setOnClickListener(v -> {
                    Log.d(TAG, "ivAddEvent clicked - launching CreateEventActivity");
                    Intent i = new Intent(requireActivity(), CreateEventActivity.class);
                    startActivity(i);
                });
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not bind ivAddEvent: " + e.getMessage());
        }

        firebaseEventService = new FirebaseEventService();
        loadEvents();
    }

    private void initializeViews(View view) {
        eventsRecyclerView = view.findViewById(R.id.eventsRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyState = view.findViewById(R.id.emptyState);
        loadingState = view.findViewById(R.id.loadingState);
        searchInput = view.findViewById(R.id.searchInput);
        filterButton = view.findViewById(R.id.filterButton);
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(filteredEvents, this);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsRecyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadEvents);
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.teal_700));
    }

    private void setupSearchAndFilter() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        filterButton.setOnClickListener(v -> {
            // TODO: Implement advanced filtering (by date, price, location, etc.)
            Toast.makeText(getContext(), "Filter feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadEvents() {
        Log.d(TAG, "loadEvents() called");
        showLoadingState(true);

        // Changed to getAllEvents to fetch all events from the database
        firebaseEventService.getAllEvents(new FirebaseEventService.EventsCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                Log.d(TAG, "onSuccess called with " + events.size() + " events");
                allEvents.clear();
                allEvents.addAll(events);
                filteredEvents.clear();
                filteredEvents.addAll(events);
                Log.d(TAG, "Updated adapter with " + filteredEvents.size() + " events");
                adapter.updateEvents(filteredEvents);
                updateUIState();
                swipeRefreshLayout.setRefreshing(false);
                showLoadingState(false);

                if (events.isEmpty()) {
                    Toast.makeText(getContext(), "No events found", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Loaded " + events.size() + " events", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "onError called: " + errorMessage);
                swipeRefreshLayout.setRefreshing(false);
                showLoadingState(false);
                Toast.makeText(getContext(), "Error loading events: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterEvents(String query) {
        if (query.isEmpty()) {
            filteredEvents.clear();
            filteredEvents.addAll(allEvents);
        } else {
            String queryLower = query.toLowerCase();
            filteredEvents.clear();
            filteredEvents.addAll(
                    allEvents.stream()
                            .filter(event -> event.getName().toLowerCase().contains(queryLower) ||
                                    event.getDescription().toLowerCase().contains(queryLower) ||
                                    event.getLocation().toLowerCase().contains(queryLower) ||
                                    event.getOrganizerName().toLowerCase().contains(queryLower))
                            .collect(Collectors.toList())
            );
        }
        adapter.updateEvents(filteredEvents);
        updateUIState();
    }

    private void updateUIState() {
        if (filteredEvents.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            eventsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            eventsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoadingState(boolean isLoading) {
        if (isLoading && filteredEvents.isEmpty()) {
            loadingState.setVisibility(View.VISIBLE);
            eventsRecyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            loadingState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEventClick(Event event) {
        // Navigate to event detail fragment
        EventDetailFragment detailFragment = EventDetailFragment.newInstance(event.getId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main, detailFragment)
                .addToBackStack(null)
                .commit();
    }
}
