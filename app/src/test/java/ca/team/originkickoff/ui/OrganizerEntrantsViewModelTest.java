package ca.team.originkickoff.ui;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import ca.team.originkickoff.data.repository.EntrantRepository;
import ca.team.originkickoff.models.Entrant;
import ca.team.originkickoff.models.EntrantStatus;

import static org.junit.Assert.*;

/**
 * Unit tests for OrganizerEntrantsViewModel filtering logic using a stubbed EntrantRepository.
 */
public class OrganizerEntrantsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    /** Stub repository providing controllable entrant list and cancel behavior. */
    static class StubEntrantRepository extends EntrantRepository {
        private final MutableLiveData<List<Entrant>> live = new MutableLiveData<>();
        private final java.util.Map<String, Entrant> map = new java.util.HashMap<>();
        @Override
        public LiveData<List<Entrant>> getEntrants() { return live; }
        @Override
        public boolean cancelEntrant(String id) {
            Entrant e = map.get(id);
            if (e == null) return false;
            e.setStatus(EntrantStatus.CANCELLED);
            live.setValue(Arrays.asList(map.values().toArray(new Entrant[0])));
            return true;
        }
        void setEntrants(List<Entrant> entrants) {
            map.clear();
            for (Entrant e : entrants) map.put(e.getId(), e);
            live.setValue(entrants);
        }
    }

    private OrganizerEntrantsViewModel createViewModelWithStub(StubEntrantRepository stub) throws Exception {
        OrganizerEntrantsViewModel vm = new OrganizerEntrantsViewModel();
        Field f = OrganizerEntrantsViewModel.class.getDeclaredField("repo");
        f.setAccessible(true);
        f.set(vm, stub);
        return vm;
    }

    private Entrant makeEntrant(String id, EntrantStatus status) {
        Entrant e = new Entrant();
        e.setId(id);
        e.setStatus(status);
        return e;
    }

    @Test
    public void getFinalEnrolled_filtersCorrectly() throws Exception {
        StubEntrantRepository stub = new StubEntrantRepository();
        OrganizerEntrantsViewModel vm = createViewModelWithStub(stub);
        LiveData<List<Entrant>> enrolledLive = vm.getFinalEnrolled();
        enrolledLive.observeForever(l -> {}); // activate transformation
        stub.setEntrants(Arrays.asList(
                makeEntrant("1", EntrantStatus.PENDING),
                makeEntrant("2", EntrantStatus.FINAL_ENROLLED),
                makeEntrant("3", EntrantStatus.FINAL_ENROLLED),
                makeEntrant("4", EntrantStatus.CANCELLED)));
        List<Entrant> enrolled = enrolledLive.getValue();
        assertNotNull(enrolled);
        assertEquals(2, enrolled.size());
        assertTrue(enrolled.stream().allMatch(e -> e.getStatus() == EntrantStatus.FINAL_ENROLLED));
    }

    @Test
    public void getCancelled_filtersCorrectlyAndCancelEntrantUpdates() throws Exception {
        StubEntrantRepository stub = new StubEntrantRepository();
        OrganizerEntrantsViewModel vm = createViewModelWithStub(stub);
        LiveData<List<Entrant>> cancelledLive = vm.getCancelled();
        cancelledLive.observeForever(l -> {});
        stub.setEntrants(Arrays.asList(
                makeEntrant("1", EntrantStatus.PENDING),
                makeEntrant("2", EntrantStatus.FINAL_ENROLLED),
                makeEntrant("3", EntrantStatus.PENDING)));
        List<Entrant> cancelledInitial = cancelledLive.getValue();
        assertNotNull(cancelledInitial);
        assertEquals(0, cancelledInitial.size());
        assertTrue(vm.cancelEntrant("3"));
        List<Entrant> cancelledAfter = cancelledLive.getValue();
        assertNotNull(cancelledAfter);
        assertEquals(1, cancelledAfter.size());
        assertEquals("3", cancelledAfter.get(0).getId());
        assertEquals(EntrantStatus.CANCELLED, cancelledAfter.get(0).getStatus());
    }
}
