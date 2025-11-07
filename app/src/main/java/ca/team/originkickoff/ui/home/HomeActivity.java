/*
 * Simple host activity for home screen UI elements.
 * Serves as an entry point after authentication and setup.
 */
package ca.team.originkickoff.ui.home;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import ca.team.originkickoff.R;

/**
 * Home activity displaying the main application interface after login.
 */
public class HomeActivity extends AppCompatActivity {

    /**
     * Sets the content view for the home screen.
     *
     * @param savedInstanceState previous state bundle
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }
}
