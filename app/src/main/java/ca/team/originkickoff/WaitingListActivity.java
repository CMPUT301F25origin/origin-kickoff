package ca.team.originkickoff;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.team.originkickoff.adapters.WaitingListAdapter;
import ca.team.originkickoff.models.WaitingListEntry;
import ca.team.originkickoff.services.WaitingListService;

public class WaitingListActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "event_id";

    private final WaitingListService service = new WaitingListService();
    private RecyclerView recyclerView;
    private WaitingListAdapter adapter;
    private ProgressBar progressBar;
    private String eventId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_list);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Missing event id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.rvWaitingList);
        progressBar = findViewById(R.id.progress);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaitingListAdapter();
        recyclerView.setAdapter(adapter);

        loadEntries();
    }

    private void loadEntries() {
        progressBar.setVisibility(View.VISIBLE);
        service.listActive(eventId)
                .addOnSuccessListener(this::showEntries)
                .addOnFailureListener(e -> {
                    Log.e("WaitingListActivity", "Failed to load waiting list", e);
                    Toast.makeText(this, "Failed to load waiting list", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void showEntries(List<WaitingListEntry> entries) {
        adapter.submit(entries);
        progressBar.setVisibility(View.GONE);
    }
}
