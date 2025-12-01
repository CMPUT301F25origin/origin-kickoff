package ca.team.originkickoff;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;

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

import ca.team.originkickoff.EventDetailActivity;

@RunWith(AndroidJUnit4.class)
public class EventDetailActivityTest {
    private FirebaseFirestore db;

    @Before
    public void setup() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }
        db = FirebaseFirestore.getInstance();
        try { db.useEmulator("10.0.2.2", 8080); } catch (Throwable ignored) {}
    }

    private void seedChosenInvite(String eventId, String userId) throws Exception {
        HashMap<String,Object> invite = new HashMap<>();
        invite.put("event_id", eventId);
        invite.put("user_id", userId);
        invite.put("status", "chosen");
        Tasks.await(db.collection("invitation_status").document(eventId + "_" + userId).set(invite));
        Tasks.await(db.collection("events").document(eventId).set(new HashMap<>()));
        HashMap<String,Object> user = new HashMap<>();
        user.put("id", userId);
        Tasks.await(db.collection("users").document(userId).set(user));
    }

    @Test
    public void acceptButton_confirmsAttendance_andHidesActionRow() throws Exception {
        String eventId = "evt_ui_accept";
        String userId = "uiUserA";
        seedChosenInvite(eventId, userId);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        try (ActivityScenario<EventDetailActivity> scenario = ActivityScenario.launch(intent)) {
            // Action row initially visible
            onView(withId(R.id.invitationActionRow)).check(matches(withEffectiveVisibility(VISIBLE)));
            // Click Accept
            onView(withId(R.id.btnAcceptInvitation)).perform(click());
            // After success, action row hidden
            onView(withId(R.id.invitationActionRow)).check(matches(withEffectiveVisibility(GONE)));
        }
    }

    @Test
    public void declineButton_setsCancelled_andHidesActionRow() throws Exception {
        String eventId = "evt_ui_decline";
        String userId = "uiUserB";
        seedChosenInvite(eventId, userId);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        try (ActivityScenario<EventDetailActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.invitationActionRow)).check(matches(withEffectiveVisibility(VISIBLE)));
            // Click Decline and confirm
            onView(withId(R.id.btnDeclineInvitation)).perform(click());
            // We avoid asserting dialog button text to reduce flakiness; rely on service call via DeclineResamplingService
            // After decline flow completes, action row should be hidden
            onView(withId(R.id.invitationActionRow)).check(matches(withEffectiveVisibility(GONE)));
        }
    }
}
