package ca.team.originkickoff;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;

import ca.team.originkickoff.services.DeclineResamplingService;

@RunWith(AndroidJUnit4.class)
public class DeclineResamplingServiceTest {
    private FirebaseFirestore db;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
        db = FirebaseFirestore.getInstance();
        // Use local emulator if present
        try { db.useEmulator("10.0.2.2", 8080); } catch (Throwable ignored) {}
    }

    private void seedLottery(String eventId, List<String> winners, String method) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("winner_ids", winners);
        result.put("num_winners", winners.size());
        result.put("lottery_method", method);
        Tasks.await(db.collection("lottery_results").document(eventId).set(result));
    }

    private void seedWaitlistActive(String eventId, String userId, long joinedAtSec) throws Exception {
        DocumentReference ref = db.collection("waiting_list_entries").document(eventId + "_" + userId);
        Map<String,Object> data = new HashMap<>();
        data.put("event_id", eventId);
        data.put("user_id", userId);
        data.put("state", "active");
        data.put("joinedAt", new Timestamp(joinedAtSec, 0));
        Tasks.await(ref.set(data));
    }

    private void seedInvitationStatus(String eventId, String userId, String status) throws Exception {
        DocumentReference ref = db.collection("invitation_status").document(eventId + "_" + userId);
        Map<String,Object> data = new HashMap<>();
        data.put("event_id", eventId);
        data.put("user_id", userId);
        data.put("status", status);
        Tasks.await(ref.set(data));
    }

    @Test
    public void declineTriggersResample_replacementChosenAndInvitationCreated() throws Exception {
        String eventId = "evt_decline_hp";
        String winner = "userW";
        String replCandidate = "userX";
        seedLottery(eventId, Arrays.asList(winner), "random");
        seedInvitationStatus(eventId, winner, "chosen");
        seedWaitlistActive(eventId, replCandidate, 1000);

        DeclineResamplingService.ensureMonitoring(eventId);
        // perform decline
        Boolean changed = Tasks.await(DeclineResamplingService.getInstance().declineInvitation(eventId, winner));
        assertTrue(changed);

        // Verify lottery_results updated
        DocumentSnapshot lot = Tasks.await(db.collection("lottery_results").document(eventId).get());
        assertTrue(lot.exists());
        Object winnersObj = lot.get("winner_ids");
        assertNotNull(winnersObj);
        assertTrue(winnersObj instanceof java.util.List<?>);
        @SuppressWarnings("unchecked")
        java.util.List<String> ids = (java.util.List<String>) winnersObj;
        assertFalse(ids.contains(winner));
        assertTrue(ids.contains(replCandidate));
        // Verify invitation for replacement created (chosen)
        DocumentSnapshot inv = Tasks.await(db.collection("invitation_status").document(eventId + "_" + replCandidate).get());
        assertTrue(inv.exists());
        assertEquals("chosen", inv.getString("status"));
        // Verify notification created for replacement
        boolean notifFound = false;
        for (DocumentSnapshot n : Tasks.await(db.collection("notifications").whereEqualTo("userId", replCandidate).whereEqualTo("eventId", eventId).get()).getDocuments()) {
            notifFound = true;
            assertEquals("result_resample", n.getString("type"));
            break;
        }
        assertTrue("Expected a notification for replacement", notifFound);
    }

    @Test
    public void declineWhenNoCandidates_reducesNumWinners() throws Exception {
        String eventId = "evt_decline_nocand";
        String winner = "userW2";
        seedLottery(eventId, Arrays.asList(winner), "random");
        seedInvitationStatus(eventId, winner, "chosen");
        // no active waitlist

        DeclineResamplingService.ensureMonitoring(eventId);
        Boolean changed = Tasks.await(DeclineResamplingService.getInstance().declineInvitation(eventId, winner));
        assertTrue(changed);

        DocumentSnapshot lot = Tasks.await(db.collection("lottery_results").document(eventId).get());
        assertTrue(lot.exists());
        Object winnersObj = lot.get("winner_ids");
        assertNotNull(winnersObj);
        assertTrue(winnersObj instanceof java.util.List<?>);
        @SuppressWarnings("unchecked")
        java.util.List<String> ids = (java.util.List<String>) winnersObj;
        assertFalse(ids.contains(winner));
        Long num = lot.getLong("num_winners");
        assertNotNull(num);
        assertEquals(0L, num.longValue());
    }

    @Test
    public void duplicateDeclineEvent_processedOnce() throws Exception {
        String eventId = "evt_decline_dup";
        String winner = "userW3";
        String replCandidate = "userY";
        seedLottery(eventId, Arrays.asList(winner), "random");
        seedInvitationStatus(eventId, winner, "chosen");
        seedWaitlistActive(eventId, replCandidate, 2000);

        DeclineResamplingService.ensureMonitoring(eventId);
        assertTrue(Tasks.await(DeclineResamplingService.getInstance().declineInvitation(eventId, winner)));
        // Try to decline again (should be ignored by service state) — but declineInvitation itself returns false
        Boolean second = Tasks.await(DeclineResamplingService.getInstance().declineInvitation(eventId, winner));
        assertFalse(second);

        // Ensure only one replacement
        DocumentSnapshot lot = Tasks.await(db.collection("lottery_results").document(eventId).get());
        Object winnersObj = lot.get("winner_ids");
        assertNotNull(winnersObj);
        assertTrue(winnersObj instanceof java.util.List<?>);
        @SuppressWarnings("unchecked")
        java.util.List<String> ids = (java.util.List<String>) winnersObj;
        assertEquals(1, ids.size());
        assertTrue(ids.contains(replCandidate));
    }

    @Test
    public void excludeCancelledFromCandidates() throws Exception {
        String eventId = "evt_decline_excl";
        String winner = "userW4";
        String cancelled = "userC";
        String candidate = "userZ";
        seedLottery(eventId, Arrays.asList(winner), "random");
        seedInvitationStatus(eventId, winner, "chosen");
        seedInvitationStatus(eventId, cancelled, "cancelled");
        seedWaitlistActive(eventId, cancelled, 1000);
        seedWaitlistActive(eventId, candidate, 1001);

        DeclineResamplingService.ensureMonitoring(eventId);
        assertTrue(Tasks.await(DeclineResamplingService.getInstance().declineInvitation(eventId, winner)));

        DocumentSnapshot lot = Tasks.await(db.collection("lottery_results").document(eventId).get());
        Object winnersObj = lot.get("winner_ids");
        assertNotNull(winnersObj);
        assertTrue(winnersObj instanceof java.util.List<?>);
        @SuppressWarnings("unchecked")
        java.util.List<String> ids = (java.util.List<String>) winnersObj;
        assertTrue(ids.contains(candidate));
        assertFalse(ids.contains(cancelled));
    }
}
