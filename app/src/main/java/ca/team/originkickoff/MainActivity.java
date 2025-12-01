/*
 * Main feed displaying upcoming events with filtering by category, date, and location.
 * Hosts navigation bar and entry points for scanning and event creation.
 */
package ca.team.originkickoff;

import android.app.AlertDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import ca.team.originkickoff.adapters.EventAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.ui.fragments.EventDetailFragment;

/**
 * Launcher activity showing the event feed and filters.
 */
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
    private String selectedLocation = null;
    private View loadingView;
    private View btnSwitchToAdmin;
    private boolean isAdminUser = false;

    private long lastNavTapAtMs = 0L;

    /**
     * Sets up UI, applies insets, initializes Firestore and loads events.
     *
     * @param savedInstanceState previously saved state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // If we are in forced user mode, do not auto-show switch-to-admin until we resolve admin
        // (will show a "Switch to Admin" button allowing return)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        loadingView = findViewById(R.id.loadingView);
        if (loadingView != null) loadingView.setVisibility(View.GONE); // hide overlay initially
        btnSwitchToAdmin = findViewById(R.id.btnSwitchToAdmin);
        if (btnSwitchToAdmin != null) btnSwitchToAdmin.setOnClickListener(v -> {
            // Leaving user mode and going back to admin dashboard
            SessionManager.setForceUserMode(false);
            Intent i = new Intent(MainActivity.this, AdminMainActivity.class);
            startActivity(i);
        });

        setupRecyclerView();
        setupClickListeners();
        checkAdminAndToggleSwitch();
        loadEventsFromFirestore();
    }

    /**
     * Reloads events when activity resumes to keep the feed current.
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkAdminAndToggleSwitch();
        loadEventsFromFirestore();
    }

    /**
     * Configures the RecyclerView and its adapter for the event list.
     */
    private void setupRecyclerView() {
        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(event -> {
            Intent intent = new Intent(MainActivity.this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
            Log.d(TAG, "Event clicked: " + event.getName() + " (ID: " + event.getId() + ")");
        });

        rvEvents.setAdapter(eventAdapter);
    }

    /**
     * Fetches events from Firestore, removes conducted events, and applies filters.
     */
    private void loadEventsFromFirestore() {
        Log.d(TAG, "Loading events from Firestore...");

        loadingView.setVisibility(View.VISIBLE);

        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEvents.clear();
                    long now = System.currentTimeMillis();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            event.setId(document.getId());

                            String lotteryStatus = document.getString("lotteryStatus");
                            if ("conducted".equals(lotteryStatus)) {
                                continue;
                            }

                            java.util.Date start = event.getRegistrationStartTime();
                            java.util.Date end = event.getRegistrationEndTime();
                            if (start == null || end == null) {
                                // Hide events without both bounds to avoid showing closed ones unintentionally
                                continue;
                            }
                            if (now < start.getTime() || now > end.getTime()) {
                                // Registration window not open
                                continue;
                            }

                            allEvents.add(event);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event document: " + document.getId(), e);
                        }
                    }
                    filterEvents();

                    loadingView.setVisibility(View.GONE);
                    Log.d(TAG, "Loaded " + allEvents.size() + " open events from Firestore");

                    if (allEvents.isEmpty()) {
                        Toast.makeText(this, "No open events right now", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events from Firestore", e);

                    loadingView.setVisibility(View.GONE);

                    Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sets up listeners for search, filters, bottom nav, and quick actions.
     */
    private void setupClickListeners() {
        searchInput = findViewById(R.id.searchInput);
        categoryFilter = findViewById(R.id.categoryFilter);
        dateFilter = findViewById(R.id.dateFilter);
        locationFilter = findViewById(R.id.locationFilter);

        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navEvents = findViewById(R.id.navEvents);
        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        ImageView ivHome = findViewById(R.id.ivHome);
        TextView tvHome = findViewById(R.id.tvHome);
        if (ivHome != null) ivHome.setColorFilter(0xFF00D9C5, android.graphics.PorterDuff.Mode.SRC_IN);
        if (tvHome != null) tvHome.setTextColor(0xFF00D9C5);

        navHome.setOnClickListener(v -> { });
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

    /**
     * Navigates to a bottom navigation destination activity using a debounce and no animations.
     * Skips if target is the current class.
     *
     * @param targetActivity destination activity class
     */
    private void navigateBottomTab(Class<?> targetActivity) {
        if (targetActivity == null) return;
        if (getClass().equals(targetActivity)) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastNavTapAtMs < 300) return;
        lastNavTapAtMs = now;
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    /**
     * Handles event clicks from the adapter and shows the detail fragment.
     *
     * @param event event model that was clicked
     */
    @Override
    public void onEventClick(Event event) {
        if (event == null) return;
        Intent intent = new Intent(MainActivity.this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    /**
     * Shows a dialog listing event categories for filtering.
     */
    private void showCategoryFilterDialog() {
        List<String> categories = allEvents.stream()
                .map(Event::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        categories.add(0, "All Categories");

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

    /**
     * Opens a date picker and filters events on the chosen day.
     */
    private void showDatePickerDialog() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
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
            local.set(java.util.Calendar.HOUR_OF_DAY, 0);
            local.set(java.util.Calendar.MINUTE, 0);
            local.set(java.util.Calendar.SECOND, 0);
            local.set(java.util.Calendar.MILLISECOND, 0);
            selectedDate = local.getTimeInMillis();
            filterEvents();
        });
        picker.show(getSupportFragmentManager(), "main_date_filter");
    }

    /**
     * Displays a dialog to filter events by location substring or pick from list.
     */
    private void showLocationFilterDialog() {
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
                .setPositiveButton("Apply", (d, w) -> {
                    selectedLocation = search.getText().toString().trim();
                    if (selectedLocation.isEmpty()) selectedLocation = null;
                    filterEvents();
                })
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String choice = adapter.getItem(position);
            selectedLocation = choice;
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

    /**
     * Applies in-memory filters (search text, category, date, location) to the current open events list
     * and submits the filtered list to the adapter.
     */
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

    /**
     * Checks if the current user/device is an admin and shows/hides the admin switch button.
     */
    private void checkAdminAndToggleSwitch() {
        if (SessionManager.isForceUserMode()) {
            // In forced user mode: treat as normal user (hide admin switch button label maybe?)
            if (btnSwitchToAdmin != null) btnSwitchToAdmin.setVisibility(View.VISIBLE); // show to allow switching back
            isAdminUser = false; // disable admin behaviors like special filtering
            return;
        }
        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        db.collection("users")
                .whereEqualTo("device_id", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean admin = false;
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        Boolean flag = doc.getBoolean("is_admin");
                        admin = flag != null && flag;
                    }
                    isAdminUser = admin;
                    if (btnSwitchToAdmin != null) btnSwitchToAdmin.setVisibility(isAdminUser ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    isAdminUser = false;
                    if (btnSwitchToAdmin != null) btnSwitchToAdmin.setVisibility(View.GONE);
                });
    }
}
