package ca.team.originkickoff.ui;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import ca.team.originkickoff.MainActivity;
import ca.team.originkickoff.R;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {

    @Test
    public void launchMainActivity_andRecyclerVisible() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Use the scenario so it's not considered unused
            scenario.onActivity(activity -> {
                // no-op; just ensuring activity is accessible
            });
            Espresso.onView(withId(R.id.rvEvents)).check(matches(isDisplayed()));
        }
    }
}
