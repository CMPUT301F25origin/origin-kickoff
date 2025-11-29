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
        AdminNavHelper.setup(this, AdminNavHelper.Tab.DASHBOARD);

        // Wire switch-to-user button
        View switchBtn = findViewById(R.id.btnSwitchToUser);
        if (switchBtn != null) {
            switchBtn.setOnClickListener(v -> {
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

    private void openEvents() {
        startActivity(new Intent(this, AdminEventsActivity.class));
    }

    private void openUsers() {
        startActivity(new Intent(this, AdminUsersActivity.class));
    }

    private void openImages() {
        startActivity(new Intent(this, AdminImagesActivity.class));
    }

    private void openLogs() {
        startActivity(new Intent(this, AdminLogsActivity.class));
    }
}
