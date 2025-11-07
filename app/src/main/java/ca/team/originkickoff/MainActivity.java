package ca.team.originkickoff;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import ca.team.originkickoff.adapters.EventAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.ui.fragments.EventDetailFragment;

public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {
    private static final String TAG = "MainActivity";
    private FirebaseFirestore db;
    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;
    private EditText searchInput;
    private LinearLayout categoryFilter, dateFilter, locationFilter;

    private final List<Event> allEvents = new ArrayList<>();
    private String selectedCategory = null;
    private Long selectedDate = null;
    private String selectedLocation = null; // treated as a query (substring match)
    private View loadingView;

    // Debounce for bottom-nav taps
    private long lastNavTapAtMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // Initialize loading view
        loadingView = findViewById(R.id.loadingView);

        // Set up RecyclerView
        setupRecyclerView();
        setupClickListeners();
        loadEventsFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsFromFirestore();
    }

    private void setupRecyclerView() {
        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(event -> {
            // Handle event click - open event details
            Intent intent = new Intent(MainActivity.this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
            Log.d(TAG, "Event clicked: " + event.getName() + " (ID: " + event.getId() + ")");
        });

        rvEvents.setAdapter(eventAdapter);
    }

    private void loadEventsFromFirestore() {
        Log.d(TAG, "Loading events from Firestore...");

        // Show loading screen
        loadingView.setVisibility(View.VISIBLE);

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            event.setId(document.getId());
                            allEvents.add(event);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event document: " + document.getId(), e);
                        }
                    }
                    // Update adapter based on filters
                    filterEvents();

                    // Hide loading and log
                    loadingView.setVisibility(View.GONE);
                    Log.d(TAG, "Loaded " + allEvents.size() + " events from Firestore");

                    if (allEvents.isEmpty()) {
                        Toast.makeText(this, "No events available", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events from Firestore", e);

                    // Hide loading screen even on error
                    loadingView.setVisibility(View.GONE);

                    Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        searchInput = findViewById(R.id.searchInput);
        categoryFilter = findViewById(R.id.categoryFilter);
        dateFilter = findViewById(R.id.dateFilter);
        locationFilter = findViewById(R.id.locationFilter);

        // Bottom nav
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navEvents = findViewById(R.id.navEvents);
        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> { /* already here */ });
        navEvents.setOnClickListener(v -> navigateBottomTab(MyEventsActivity.class));
        navNotifications.setOnClickListener(v -> navigateBottomTab(NotificationsActivity.class));
        navProfile.setOnClickListener(v -> navigateBottomTab(ProfileActivity.class));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterEvents(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        categoryFilter.setOnClickListener(v -> showCategoryFilterDialog());
        dateFilter.setOnClickListener(v -> showDatePickerDialog());
        locationFilter.setOnClickListener(v -> showLocationFilterDialog());

        ImageView ivAddEvent = findViewById(R.id.ivAddEvent);
        ivAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });

        Button btnScanQR = findViewById(R.id.btnScanQR);
        btnScanQR.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            startActivity(intent);
        });
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

    @Override
    public void onEventClick(Event event) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, EventDetailFragment.newInstance(event.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void showCategoryFilterDialog() {
        List<String> categories = allEvents.stream()
                .map(Event::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        // Add an "All" option at the top for quick clear
        categories.add(0, "All Categories");

        // Build a simple searchable dialog with an EditText + ListView
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        EditText search = new EditText(this);
        search.setHint("Search category");
        root.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView listView = new ListView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Filter by Category")
                .setView(root)
                .setNegativeButton("Clear", (d, w) -> { selectedCategory = null; filterEvents(); })
                .setPositiveButton("Close", null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String choice = adapter.getItem(position);
            selectedCategory = ("All Categories".equalsIgnoreCase(choice)) ? null : choice;
            filterEvents();
            dialog.dismiss();
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { adapter.getFilter().filter(s); }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private void showDatePickerDialog() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDate = selectedCal.getTimeInMillis();
            filterEvents();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showLocationFilterDialog() {
        // Distinct list of locations for suggestions
        List<String> locations = allEvents.stream()
                .map(Event::getLocation)
                .filter(l -> l != null && !l.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        EditText search = new EditText(this);
        search.setHint("Search location");
        if (selectedLocation != null) search.setText(selectedLocation);
        root.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView listView = new ListView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, locations);
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Filter by Location")
            .setView(root)
            .setNegativeButton("Clear", (d, w) -> { selectedLocation = null; filterEvents(); })
            .setPositiveButton("Apply", (d, w) -> { selectedLocation = search.getText().toString().trim(); if (selectedLocation.isEmpty()) selectedLocation = null; filterEvents(); })
            .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String choice = adapter.getItem(position);
            selectedLocation = choice; // selecting suggestion
            filterEvents();
            dialog.dismiss();
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { adapter.getFilter().filter(s); }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private void filterEvents() {
        List<Event> tempFiltered = new ArrayList<>(allEvents);
        String query = searchInput.getText() != null ? searchInput.getText().toString().toLowerCase() : "";

        if (!query.isEmpty()) {
            String q = query;
            tempFiltered = tempFiltered.stream()
                    .filter(event -> (event.getName() != null && event.getName().toLowerCase().contains(q)) ||
                            (event.getDescription() != null && event.getDescription().toLowerCase().contains(q)) ||
                            (event.getLocation() != null && event.getLocation().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        if (selectedCategory != null) {
            String cat = selectedCategory.toLowerCase();
            tempFiltered = tempFiltered.stream()
                    .filter(event -> event.getCategory() != null && event.getCategory().toLowerCase().equals(cat))
                    .collect(Collectors.toList());
        }

        if (selectedDate != null) {
            Long picked = selectedDate;
            tempFiltered = tempFiltered.stream()
                    .filter(event -> {
                        if (event.getEventDate() == null) return false;
                        Calendar eventCal = Calendar.getInstance();
                        eventCal.setTime(event.getEventDate());
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.setTimeInMillis(picked);
                        return eventCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
                                && eventCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR);
                    })
                    .collect(Collectors.toList());
        }

        if (selectedLocation != null) {
            String loc = selectedLocation.toLowerCase();
            tempFiltered = tempFiltered.stream()
                    .filter(event -> event.getLocation() != null && event.getLocation().toLowerCase().contains(loc))
                    .collect(Collectors.toList());
        }

        eventAdapter.setEvents(tempFiltered);
    }
}
