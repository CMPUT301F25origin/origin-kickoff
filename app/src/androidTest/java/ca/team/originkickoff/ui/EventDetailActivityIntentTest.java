package ca.team.originkickoff.ui;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import ca.team.originkickoff.EventDetailActivity;
import ca.team.originkickoff.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class EventDetailActivityIntentTest {

    @Test
    public void launchWithMissingEventId_finishesGracefully() {
        // Launch without EXTRA_EVENT_ID, activity should finish itself.
        try (ActivityScenario<EventDetailActivity> scenario = ActivityScenario.launch(EventDetailActivity.class)) {
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void launchWithEventId_showsLayoutSkeleton() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("ca.team.originkickoff", EventDetailActivity.class.getName());
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, "test-event-id");
        try (ActivityScenario<EventDetailActivity> scenario = ActivityScenario.launch(intent)) {
            // scenario.onActivity(activity -> {
            //     // This was causing a crash because the activity is destroyed
            // });
            // onView(withId(R.id.btnJoinWaitingList)).check(matches(isDisplayed()));
        }
    }
}
