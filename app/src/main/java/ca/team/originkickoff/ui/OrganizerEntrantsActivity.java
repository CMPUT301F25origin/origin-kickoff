// filepath: /Users/sargun/StudioProjects/origin-kickoff/app/src/main/java/ca/team/originkickoff/ui/OrganizerEntrantsActivity.java
/**
 * Organizer view for managing entrants (enrolled and cancelled).
 * Allows organizers to review and cancel entrant registrations.
 */
package ca.team.originkickoff.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ca.team.originkickoff.R;
import ca.team.originkickoff.adapters.EntrantAdapter;
import ca.team.originkickoff.models.Entrant;

public class OrganizerEntrantsActivity extends ComponentActivity {
    private final OrganizerEntrantsViewModel vm = new OrganizerEntrantsViewModel();
    private EntrantAdapter enrolledAdapter;
    private EntrantAdapter cancelledAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_entrants);

        RecyclerView rvEnrolled = findViewById(R.id.recycler_final_enrolled);
        RecyclerView rvCancelled = findViewById(R.id.recycler_cancelled);

        enrolledAdapter = new EntrantAdapter(e -> attemptCancel(e));
        cancelledAdapter = new EntrantAdapter(e -> attemptCancel(e));

        rvEnrolled.setLayoutManager(new LinearLayoutManager(this));
        rvEnrolled.setAdapter(enrolledAdapter);

        rvCancelled.setLayoutManager(new LinearLayoutManager(this));
        rvCancelled.setAdapter(cancelledAdapter);

        vm.getFinalEnrolled().observe(this, list -> enrolledAdapter.submitList(list));
        vm.getCancelled().observe(this, list -> cancelledAdapter.submitList(list));
    }

    private void attemptCancel(Entrant e) {
        boolean ok = vm.cancelEntrant(e.getId());
        String msg = ok ? "Entrant cancelled" : "Cannot cancel — only pending entrants can be cancelled";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
