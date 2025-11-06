package ca.team.originkickoff.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import ca.team.originkickoff.MainActivity;
import ca.team.originkickoff.R;

public class SplashActivity extends AppCompatActivity {

    private SplashViewModel splashViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);

        splashViewModel.processLogin();

        splashViewModel.getUserLiveData().observe(this, user -> {
            if (user == null) {
                // Show an error message if user creation fails
                Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_LONG).show();
            } else {
                // Always navigate to MainActivity on success
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        });
    }
}
