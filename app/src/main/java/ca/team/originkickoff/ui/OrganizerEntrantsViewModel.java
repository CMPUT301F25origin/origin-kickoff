// filepath: /Users/sargun/StudioProjects/origin-kickoff/app/src/main/java/ca/team/originkickoff/ui/OrganizerEntrantsViewModel.java
/**
 * ViewModel providing filtered entrant lists for organizer management.
 * Separates final enrolled and cancelled entrants from repository data.
 */
package ca.team.originkickoff.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.List;

import ca.team.originkickoff.data.repository.EntrantRepository;
import ca.team.originkickoff.models.Entrant;
import ca.team.originkickoff.models.EntrantStatus;

/**
 * ViewModel for managing and filtering entrant data in organizer views.
 */
public class OrganizerEntrantsViewModel extends ViewModel {
    private final EntrantRepository repo = new EntrantRepository();

    /** @return LiveData of all entrants */
    public LiveData<List<Entrant>> getAllEntrants() { return repo.getEntrants(); }

    /** @return LiveData of final enrolled entrants only */
    public LiveData<List<Entrant>> getFinalEnrolled() {
        return Transformations.map(getAllEntrants(), list -> {
            if (list == null) return null;
            java.util.ArrayList<Entrant> out = new java.util.ArrayList<>();
            for (Entrant e : list) if (e.getStatus() == EntrantStatus.FINAL_ENROLLED) out.add(e);
            return out;
        });
    }

    /** @return LiveData of cancelled entrants only */
    public LiveData<List<Entrant>> getCancelled() {
        return Transformations.map(getAllEntrants(), list -> {
            if (list == null) return null;
            java.util.ArrayList<Entrant> out = new java.util.ArrayList<>();
            for (Entrant e : list) if (e.getStatus() == EntrantStatus.CANCELLED) out.add(e);
            return out;
        });
    }

    /**
     * Attempts to cancel an entrant.
     * @param id entrant ID to cancel
     * @return true if successfully cancelled
     */
    public boolean cancelEntrant(String id) { return repo.cancelEntrant(id); }
}

