package ca.team.originkickoff;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import ca.team.originkickoff.R;

public class NotificationsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_NOTIFS_ENABLED = "notifications_enabled";

    // Debounce for bottom-nav taps
    private long lastNavTapAtMs = 0L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        // Apply window insets to avoid overlapping system bars and keep bottom bar pinned
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notificationsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupTopBar();
        setupBottomNav();
        setupSwitch();
    }

    private void setupTopBar() {
        ImageButton back = findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> finish());
    }

    private void setupSwitch() {
        SwitchCompat sw = findViewById(R.id.switch_notifications);
        if (sw == null) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_NOTIFS_ENABLED, true);
        sw.setChecked(enabled);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFS_ENABLED, isChecked).apply();
            updateFcmSubscription(isChecked);
        });
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navEvents = findViewById(R.id.navEvents);
        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        if (navHome == null || navEvents == null || navNotifications == null || navProfile == null) return;

        // Highlight current tab (Notifications)
        ImageView iv = findViewById(R.id.ivNotifications);
        TextView tv = findViewById(R.id.tvNotifications);
        if (iv != null) iv.setColorFilter(0xFF00D9C5, android.graphics.PorterDuff.Mode.SRC_IN);
        if (tv != null) tv.setTextColor(0xFF00D9C5);

        navHome.setOnClickListener(v -> navigateBottomTab(MainActivity.class));
        navEvents.setOnClickListener(v -> navigateBottomTab(MyEventsActivity.class));
        navNotifications.setOnClickListener(v -> { /* already here */ });
        navProfile.setOnClickListener(v -> navigateBottomTab(ProfileActivity.class));
    }

    // Helper to navigate between bottom-bar destinations smoothly with no transition animation
    private void navigateBottomTab(Class<?> targetActivity) {
        if (targetActivity == null) return;
        if (getClass().equals(targetActivity)) return; // already on this tab
        long now = SystemClock.elapsedRealtime();
        if (now - lastNavTapAtMs < 300) return; // debounce rapid taps
        lastNavTapAtMs = now;
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void updateFcmSubscription(boolean enabled) {
        // If using Firebase Messaging, you can subscribe/unsubscribe to a topic.
        // Example:
        // FirebaseMessaging.getInstance().subscribeToTopic("general");
        // FirebaseMessaging.getInstance().unsubscribeFromTopic("general");
        // Left as a stub to avoid introducing runtime calls during build/test.
    }
}