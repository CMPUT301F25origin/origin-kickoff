package ca.team.originkickoff;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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

        setupBottomNav();
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navEvents = findViewById(R.id.navEvents);
        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        LinearLayout navProfile = findViewById(R.id.navProfile);
        if (navHome == null) return; // bottom bar not present

        // Highlight Notifications
        ImageView iv = findViewById(R.id.ivNotifications);
        TextView tv = findViewById(R.id.tvNotifications);
        if (iv != null) iv.setColorFilter(0xFF00D9C5, android.graphics.PorterDuff.Mode.SRC_IN);
        if (tv != null) tv.setTextColor(0xFF00D9C5);

        navHome.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });
        navEvents.setOnClickListener(v -> {
            startActivity(new Intent(this, MyEventsActivity.class));
            finish();
        });
        navNotifications.setOnClickListener(v -> { /* already here */ });
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void updateFcmSubscription(boolean enabled) {
        // If using Firebase Messaging, you can subscribe/unsubscribe to a topic.
        // FirebaseMessaging.getInstance().subscribeToTopic("general") / unsubscribeFromTopic("general")
        // Left as a stub to avoid introducing runtime calls during build/test.
    }
}
