package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.widget.Toolbar;

import ca.team.originkickoff.util.DeviceUtils;

public class MyEventsActivity extends AppCompatActivity {
    private static final String TAG = "MyEventsActivity";
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private boolean isOrganizer = false;
    private FirebaseFirestore db;

    // Debounce for bottom-nav taps
    private long lastNavTapAtMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_events);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Events");
        }

        db = FirebaseFirestore.getInstance();
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Check if user is an organizer first, then setup tabs
        checkIfUserIsOrganizer();

        // Setup bottom navigation include
        setupBottomNav();
    }

    private void checkIfUserIsOrganizer() {
        String deviceId = DeviceUtils.getDeviceId(this);
        if (deviceId == null) {
            Log.w(TAG, "Device ID is null, defaulting to non-organizer");
            setupTabs(false);
            return;
        }

        db.collection("users")
                .whereEqualTo("device_id", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Boolean isOrganizerField = queryDocumentSnapshots.getDocuments().get(0).getBoolean("is_organizer");
                        isOrganizer = isOrganizerField != null && isOrganizerField;
                        Log.d(TAG, "User is organizer: " + isOrganizer);
                    } else {
                        Log.w(TAG, "No user found for device_id");
                        isOrganizer = false;
                    }
                    setupTabs(isOrganizer);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking organizer status", e);
                    isOrganizer = false;
                    setupTabs(false);
                });
    }

    private void setupTabs(boolean showOrganizerTab) {
        // Create adapter with dynamic tab count
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (!showOrganizerTab) {
                    // Only show Events Joined
                    return new ca.team.originkickoff.ui.fragments.EventsJoinedFragment();
                } else {
                    // Show both tabs
                    if (position == 0) {
                        return new ca.team.originkickoff.ui.fragments.EventsJoinedFragment();
                    } else {
                        return new ca.team.originkickoff.ui.fragments.EventsOrganizedFragment();
                    }
                }
            }

            @Override
            public int getItemCount() {
                return showOrganizerTab ? 2 : 1;
            }
        });

        // Setup tab names
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (!showOrganizerTab) {
                // Only one tab
                tab.setText("Events Joined");
            } else {
                // Two tabs
                if (position == 0) {
                    tab.setText("Events Joined");
                } else {
                    tab.setText("Events Organized");
                }
            }
        }).attach();

        // Wire FAB: only visible for organizers on Events Organized tab (index 1)
        FloatingActionButton fab = findViewById(R.id.fabCreate);
        if (fab != null && showOrganizerTab) {
            fab.setOnClickListener(v -> startActivity(new Intent(MyEventsActivity.this, CreateEventActivity.class)));

            // initial visibility
            fab.setVisibility(viewPager.getCurrentItem() == 1 ? View.VISIBLE : View.GONE);

            // show/hide based on page
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    if (position == 1) fab.setVisibility(View.VISIBLE);
                    else fab.setVisibility(View.GONE);
                }
            });
        } else if (fab != null) {
            // Hide FAB completely for non-organizers
            fab.setVisibility(View.GONE);
        }
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navEvents = findViewById(R.id.navEvents);
        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        if (navHome == null || navEvents == null || navNotifications == null || navProfile == null) return;

        // Highlight current tab (My Events)
        ImageView ivEvents = findViewById(R.id.ivEvents);
        TextView tvEvents = findViewById(R.id.tvEvents);
        if (ivEvents != null) ivEvents.setColorFilter(0xFF00D9C5, android.graphics.PorterDuff.Mode.SRC_IN);
        if (tvEvents != null) tvEvents.setTextColor(0xFF00D9C5);

        navHome.setOnClickListener(v -> navigateBottomTab(MainActivity.class));
        navEvents.setOnClickListener(v -> { /* already here */ });
        navNotifications.setOnClickListener(v -> navigateBottomTab(NotificationsActivity.class));
        navProfile.setOnClickListener(v -> navigateBottomTab(ProfileActivity.class));
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
