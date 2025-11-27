// filepath: /Users/sargun/StudioProjects/origin-kickoff/app/src/main/java/ca/team/originkickoff/ui/OrganizerEntrantsViewModel.java
package ca.team.originkickoff.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.List;

import ca.team.originkickoff.data.repository.EntrantRepository;
import ca.team.originkickoff.models.Entrant;
import ca.team.originkickoff.models.EntrantStatus;

public class OrganizerEntrantsViewModel extends ViewModel {
    private final EntrantRepository repo = new EntrantRepository();

    public LiveData<List<Entrant>> getAllEntrants() { return repo.getEntrants(); }

    public LiveData<List<Entrant>> getFinalEnrolled() {
        return Transformations.map(getAllEntrants(), list -> {
            if (list == null) return null;
            java.util.ArrayList<Entrant> out = new java.util.ArrayList<>();
            for (Entrant e : list) if (e.getStatus() == EntrantStatus.FINAL_ENROLLED) out.add(e);
            return out;
        });
    }

    public LiveData<List<Entrant>> getCancelled() {
        return Transformations.map(getAllEntrants(), list -> {
            if (list == null) return null;
            java.util.ArrayList<Entrant> out = new java.util.ArrayList<>();
            for (Entrant e : list) if (e.getStatus() == EntrantStatus.CANCELLED) out.add(e);
            return out;
        });
    }

    public boolean cancelEntrant(String id) { return repo.cancelEntrant(id); }
}

