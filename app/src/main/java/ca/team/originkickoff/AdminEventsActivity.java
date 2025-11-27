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

        int layoutId = getResources().getIdentifier("activity_admin_events", "layout", getPackageName());
        if (layoutId != 0) {
            setContentView(layoutId);
        } else {
            // Fallback: create a simple RecyclerView-only layout programmatically
            RecyclerView rv = new RecyclerView(this);
            rv.setId(View.generateViewId());
            setContentView(rv);
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new EventAdapter(this);
            rv.setAdapter(adapter);
            rvAdminEvents = rv;
            loadAllEvents();
            return;
        }

        int rvId = getResources().getIdentifier("rvAdminEvents", "id", getPackageName());
        int progressId = getResources().getIdentifier("progress", "id", getPackageName());
        int emptyId = getResources().getIdentifier("tvEmpty", "id", getPackageName());
        int backId = getResources().getIdentifier("btnBack", "id", getPackageName());

        rvAdminEvents = findViewById(rvId);
        progress = findViewById(progressId);
        tvEmpty = findViewById(emptyId);
        View back = findViewById(backId);
        if (back != null && back instanceof ImageView) {
            back.setOnClickListener(v -> finish());
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
                adapter.setEvents(events);
                if (tvEmpty != null) tvEmpty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String errorMessage) {
                if (progress != null) progress.setVisibility(View.GONE);
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Failed to load events");
                }
                Log.e(TAG, "Error: " + errorMessage);
                Toast.makeText(AdminEventsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEventClick(Event event) {
        Toast.makeText(this, event.getName(), Toast.LENGTH_SHORT).show();
    }
}
