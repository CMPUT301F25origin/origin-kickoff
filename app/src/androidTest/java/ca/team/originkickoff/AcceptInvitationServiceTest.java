package ca.team.originkickoff;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import ca.team.originkickoff.services.AcceptInvitationService;

@RunWith(AndroidJUnit4.class)
public class AcceptInvitationServiceTest {
    private FirebaseFirestore db;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
        db = FirebaseFirestore.getInstance();
        // Point Firestore to emulator if available
        try { db.useEmulator("10.0.2.2", 8080); } catch (Throwable ignored) {}
    }

    private void seedChosenInvite(String eventId, String userId) throws Exception {
        DocumentReference inviteRef = db.collection("invitation_status").document(eventId + "_" + userId);
        Map<String,Object> data = new HashMap<>();
        data.put("event_id", eventId);
        data.put("user_id", userId);
        data.put("status", "chosen");
        Tasks.await(inviteRef.set(data));
        // ensure event doc exists with enrolledCount default
        Tasks.await(db.collection("events").document(eventId).set(new HashMap<>()));
    }

    @Test
    public void confirmAttendance_happyPath_writesEnrollmentAndCount() throws Exception {
        String eventId = "evt_accept_hp";
        String userId = "userA";
        seedChosenInvite(eventId, userId);

        AcceptInvitationService svc = new AcceptInvitationService();
        Boolean ok = Tasks.await(svc.confirmAttendance(eventId, userId));
        assertTrue(ok);

        assertTrue(Tasks.await(svc.isEnrolled(eventId, userId)));
        // enrolledCount should be 1
        Long count = Tasks.await(db.collection("events").document(eventId).get()).getLong("enrolledCount");
        assertNotNull(count);
        assertEquals(1L, count.longValue());
    }

    @Test
    public void confirmAttendance_idempotent_noDuplicateEnrollmentOrIncrement() throws Exception {
        String eventId = "evt_accept_idem";
        String userId = "userB";
        seedChosenInvite(eventId, userId);

        AcceptInvitationService svc = new AcceptInvitationService();
        assertTrue(Tasks.await(svc.confirmAttendance(eventId, userId)));
        assertTrue(Tasks.await(svc.confirmAttendance(eventId, userId)));

        // enrollment doc exists
        assertTrue(Tasks.await(svc.isEnrolled(eventId, userId)));
        // enrolledCount remains 1
        Long count = Tasks.await(db.collection("events").document(eventId).get()).getLong("enrolledCount");
        assertNotNull(count);
        assertEquals(1L, count.longValue());
    }

    @Test
    public void confirmAttendance_rejectWhenNotChosen() throws Exception {
        String eventId = "evt_accept_reject";
        String userId = "userC";
        DocumentReference inviteRef = db.collection("invitation_status").document(eventId + "_" + userId);
        Map<String,Object> data = new HashMap<>();
        data.put("event_id", eventId);
        data.put("user_id", userId);
        data.put("status", "cancelled");
        Tasks.await(inviteRef.set(data));
        Tasks.await(db.collection("events").document(eventId).set(new HashMap<>()));

        AcceptInvitationService svc = new AcceptInvitationService();
        Boolean ok = Tasks.await(svc.confirmAttendance(eventId, userId));
        assertFalse(ok);
        assertFalse(Tasks.await(svc.isEnrolled(eventId, userId)));
    }
}

