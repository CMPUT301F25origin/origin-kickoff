// filepath: /Users/sargun/StudioProjects/origin-kickoff/app/src/main/java/ca/team/originkickoff/data/repository/EntrantRepository.java
package ca.team.originkickoff.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.models.Entrant;
import ca.team.originkickoff.models.EntrantStatus;

/**
 * Simple in-memory repository for entrants. Can be replaced by Firestore-backed implementation.
 */
public class EntrantRepository {
    private final MutableLiveData<List<Entrant>> entrants = new MutableLiveData<>();

    public EntrantRepository() {
        // seed sample data
        List<Entrant> sample = new ArrayList<>();
        sample.add(new Entrant("1", "u1", "Alice", "alice@example.com", EntrantStatus.PENDING, "e1"));
        sample.add(new Entrant("2", "u2", "Bob", "bob@example.com", EntrantStatus.FINAL_ENROLLED, "e1"));
        sample.add(new Entrant("3", "u3", "Carol", "carol@example.com", EntrantStatus.CANCELLED, "e1"));
        sample.add(new Entrant("4", "u4", "Dan", "dan@example.com", EntrantStatus.ACCEPTED, "e1"));
        sample.add(new Entrant("5", "u5", "Eve", "eve@example.com", EntrantStatus.PENDING, "e1"));
        entrants.setValue(sample);
    }

    public LiveData<List<Entrant>> getEntrants() { return entrants; }

    /**
     * Attempts to cancel an entrant by id. Only succeeds if current status is PENDING.
     * @param id entrant id
     * @return true if cancelled
     */
    public boolean cancelEntrant(String id) {
        List<Entrant> list = entrants.getValue();
        if (list == null) return false;
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) { idx = i; break; }
        }
        if (idx == -1) return false;
        Entrant e = list.get(idx);
        if (e.getStatus() != EntrantStatus.PENDING) return false;
        List<Entrant> copy = new ArrayList<>(list);
        copy.set(idx, new Entrant(e.getId(), e.getUserId(), e.getName(), e.getEmail(), EntrantStatus.CANCELLED, e.getEventId()));
        entrants.setValue(copy);
        return true;
    }
}

