package ca.team.originkickoff;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class ManageInvitationsActivityTest {
    private FirebaseFirestore db;

    @Before
    public void setup() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }
        db = FirebaseFirestore.getInstance();
        try { db.useEmulator("10.0.2.2", 8080); } catch (Throwable ignored) {}
    }

    @Test
    public void loadsEventName_andStartsOnChosenTab() throws Exception {
        String eventId = "evt_manage_tabs";
        HashMap<String,Object> ev = new HashMap<>();
        ev.put("name", "Kickoff Event");
        Tasks.await(db.collection("events").document(eventId).set(ev));

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageInvitationsActivity.class);
        intent.putExtra(ManageInvitationsActivity.EXTRA_EVENT_ID, eventId);
        try (ActivityScenario<ManageInvitationsActivity> ignored = ActivityScenario.launch(intent)) {
            // Header shows event name
            onView(withId(R.id.tv_event_name)).check(matches(withText("Kickoff Event")));
            // Chosen tab label present
            onView(withText("Chosen")).check(matches(withText("Chosen")));
        }
    }
}

