package ca.team.originkickoff;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class NotificationsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "notifications_prefs";
    private static final String KEY_NOTIFS_ENABLED = "notifications_enabled";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        ImageButton back = findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_NOTIFS_ENABLED, true);

        SwitchCompat sw = findViewById(R.id.switch_notifications);
        if (sw != null) {
            sw.setChecked(enabled);
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_NOTIFS_ENABLED, isChecked).apply();
                Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
                updateFcmSubscription(isChecked);
            });
        }
    }

    private void updateFcmSubscription(boolean enabled) {
        // If using Firebase Messaging, you can subscribe/unsubscribe to a topic.
        // FirebaseMessaging.getInstance().subscribeToTopic("general") / unsubscribeFromTopic("general")
        // Left as a stub to avoid introducing runtime calls during build/test.
    }
}
