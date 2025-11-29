package ca.team.originkickoff;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.adapters.EventAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.services.FirebaseEventService;

public class AdminEventsActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {
    private static final String TAG = "AdminEventsActivity";

    private RecyclerView rvAdminEvents;
    private View progress;
    private TextView tvEmpty;
    private EventAdapter adapter;
    private final FirebaseEventService eventService = new FirebaseEventService();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layoutId = R.layout.activity_admin_events;
        setContentView(layoutId);
        AdminNavHelper.setup(this, AdminNavHelper.Tab.EVENTS);
        rvAdminEvents = findViewById(R.id.rvAdminEvents);
        progress = findViewById(R.id.progress);
        tvEmpty = findViewById(R.id.tvEmpty);
        View back = findViewById(R.id.btnBack);
        if (back instanceof ImageView) back.setOnClickListener(v -> finish());
        if (rvAdminEvents == null) {
            Toast.makeText(this, R.string.failed_to_load_users, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        adapter = new EventAdapter(this);
        rvAdminEvents.setLayoutManager(new LinearLayoutManager(this));
        rvAdminEvents.setAdapter(adapter);
        loadAllEvents();
    }

    private void loadAllEvents() {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        eventService.getAllEvents(new FirebaseEventService.EventsCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (progress != null) progress.setVisibility(View.GONE);
                if (events == null) events = new ArrayList<>();
                if (adapter != null) adapter.setEvents(events);
                if (tvEmpty != null) tvEmpty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onError(String errorMessage) {
                if (progress != null) progress.setVisibility(View.GONE);
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.no_events);
                }
                Log.e(TAG, "Error: " + errorMessage);
                Toast.makeText(AdminEventsActivity.this, getString(R.string.failed) + " " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEventClick(Event event) {
        // Admin can only view/browse events - open in read-only mode
        Toast.makeText(this, "Viewing event: " + event.getName(), Toast.LENGTH_SHORT).show();

        // Open event detail activity to view event information (use correct extra key)
        android.content.Intent intent = new android.content.Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId()); // FIX: was "event_id"
        intent.putExtra("admin_view_only", true); // Flag to indicate admin read-only mode
        startActivity(intent);
    }
}
