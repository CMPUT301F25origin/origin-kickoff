package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.TextView;
import android.widget.ImageView;

/**
 * Admin entry activity with bottom bar: Dashboard, Events, Users, Images, Logs.
 */
public class AdminMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        View cardEvents = findViewById(R.id.itemManageEvents);
        View cardUsers = findViewById(R.id.itemUserManagement);
        View cardImages = findViewById(R.id.itemImageManagement);
        View cardLogs = findViewById(R.id.itemNotificationLogs);

        bindCard(cardEvents, "Manage Events", "Browse & oversee events", R.drawable.ic_search, () -> openEvents());
        bindCard(cardUsers, "User Management", "Browse user profiles", R.drawable.ic_search, () -> openUsers());
        bindCard(cardImages, "Images", "Review uploaded images", R.drawable.ic_search, () -> openImages());
        bindCard(cardLogs, "Notification Logs", "View broadcast logs", R.drawable.ic_search, () -> openLogs());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_dashboard) return true;
                if (id == R.id.nav_events) { openEvents(); return true; }
                if (id == R.id.nav_users) { openUsers(); return true; }
                if (id == R.id.nav_images) { openImages(); return true; }
                if (id == R.id.nav_logs) { openLogs(); return true; }
                return false;
            });
        }
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
