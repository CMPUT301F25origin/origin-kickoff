package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;

/**
 * Admin entry activity with bottom bar: Dashboard, Events, Users, Images, Logs.
 */
public class AdminMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);
        // Ensure we are NOT in forced user mode when landing on admin dashboard
        SessionManager.setForceUserMode(false);
        AdminNavHelper.setup(this, AdminNavHelper.Tab.DASHBOARD);

        // Wire switch-to-user button
        View switchBtn = findViewById(R.id.btnSwitchToUser);
        if (switchBtn != null) {
            switchBtn.setOnClickListener(v -> {
                // Enter forced user mode so admin behaves like normal entrant/organizer
                SessionManager.setForceUserMode(true);
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                // Do not finish; allow back to admin
            });
        }

        View cardEvents = findViewById(R.id.itemManageEvents);
        View cardUsers = findViewById(R.id.itemUserManagement);
        View cardImages = findViewById(R.id.itemImageManagement);
        View cardLogs = findViewById(R.id.itemNotificationLogs);

        bindCard(cardEvents, "Manage Events", "Browse & oversee events", R.drawable.ic_search, () -> openEvents());
        bindCard(cardUsers, "User Management", "Browse user profiles", R.drawable.ic_search, () -> openUsers());
        bindCard(cardImages, "Images", "Review uploaded images", R.drawable.ic_search, () -> openImages());
        bindCard(cardLogs, "Notification Logs", "View broadcast logs", R.drawable.ic_search, () -> openLogs());
    }

    private interface Action { void run(); }

    /**
     * Binds title, subtitle, icon, and click action to a dashboard card view.
     *
     * @param card     root view of the card
     * @param title    display title text
     * @param subtitle secondary description text
     * @param iconRes  drawable resource id for icon
     * @param action   action invoked when card is tapped
     */
    private void bindCard(View card, String title, String subtitle, int iconRes, Action action) {
        if (card == null) return;
        TextView tvTitle = card.findViewById(R.id.title);
        TextView tvSubtitle = card.findViewById(R.id.subtitle);
        ImageView icon = card.findViewById(R.id.icon);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        if (icon != null) icon.setImageResource(iconRes);
        card.setOnClickListener(v -> action.run());
    }

    /**
     * Opens the admin events management screen.
     */
    private void openEvents() {
        startActivity(new Intent(this, AdminEventsActivity.class));
    }

    /**
     * Opens the admin user management screen.
     */
    private void openUsers() {
        startActivity(new Intent(this, AdminUsersActivity.class));
    }

    /**
     * Opens the admin images review screen.
     */
    private void openImages() {
        startActivity(new Intent(this, AdminImagesActivity.class));
    }

    /**
     * Opens the admin notification logs screen.
     */
    private void openLogs() {
        startActivity(new Intent(this, AdminLogsActivity.class));
    }
}
