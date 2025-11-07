/*
 * Tabbed interface for reviewing invitation statuses (chosen, cancelled, enrolled).
 * Loads event details for context and hosts fragments via pager adapter.
 */
package ca.team.originkickoff;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

import ca.team.originkickoff.adapters.InvitationsPagerAdapter;
import ca.team.originkickoff.models.Event;

/**
 * Activity for managing lottery invitations with tabs for chosen, cancelled, and enrolled users.
 */
public class ManageInvitationsActivity extends AppCompatActivity {
    private static final String TAG = "ManageInvitations";
    public static final String EXTRA_EVENT_ID = "event_id";

    private TextView tvEventName;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private String eventId;
    private Event currentEvent;

    /**
     * Inflates layout, validates event ID, and initializes tabs.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_invitations);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadEventData();
        setupTabs();
    }

    private void initializeViews() {
        tvEventName = findViewById(R.id.tv_event_name);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lottery Results");
        }
    }

    /**
     * Fetches event data to show its name in the header.
     */
    private void loadEventData() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentEvent = documentSnapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setId(documentSnapshot.getId());
                            tvEventName.setText(currentEvent.getName());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event", e);
                });
    }

    /**
     * Sets up the ViewPager2 and TabLayout with invitation status tabs.
     */
    private void setupTabs() {
        InvitationsPagerAdapter adapter = new InvitationsPagerAdapter(this, eventId);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Chosen");
                            break;
                        case 1:
                            tab.setText("Cancelled");
                            break;
                        case 2:
                            tab.setText("Enrolled");
                            break;
                    }
                }).attach();

        // Start on Chosen tab
        viewPager.setCurrentItem(0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
