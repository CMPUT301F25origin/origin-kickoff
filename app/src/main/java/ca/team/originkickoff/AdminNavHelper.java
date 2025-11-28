package ca.team.originkickoff;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Helper to wire admin bottom navigation include (layout_admin_bottom_nav) across all admin screens.
 * Call AdminNavHelper.setup(this, Tab.X) in each admin activity after setContentView.
 */
public final class AdminNavHelper {
    private AdminNavHelper() {}

    public enum Tab { DASHBOARD, EVENTS, USERS, IMAGES, LOGS }

    public static void setup(Activity activity, Tab active) {
        View dashboard = activity.findViewById(R.id.navDashboard);
        View events = activity.findViewById(R.id.navEvents);
        View users = activity.findViewById(R.id.navUsers);
        View images = activity.findViewById(R.id.navImages);
        View logs = activity.findViewById(R.id.navLogs);
        if (dashboard == null || events == null || users == null || images == null || logs == null) {
            // Include not present, nothing to wire.
            return;
        }
        // Assign listeners.
        dashboard.setOnClickListener(v -> {
            if (active != Tab.DASHBOARD) activity.startActivity(new Intent(activity, AdminMainActivity.class));
        });
        events.setOnClickListener(v -> {
            if (active != Tab.EVENTS) activity.startActivity(new Intent(activity, AdminEventsActivity.class));
        });
        users.setOnClickListener(v -> {
            if (active != Tab.USERS) activity.startActivity(new Intent(activity, AdminUsersActivity.class));
        });
        images.setOnClickListener(v -> {
            if (active != Tab.IMAGES) activity.startActivity(new Intent(activity, AdminImagesActivity.class));
        });
        logs.setOnClickListener(v -> {
            if (active != Tab.LOGS) activity.startActivity(new Intent(activity, AdminLogsActivity.class));
        });

        // Highlight active tab.
        highlight(dashboard, active == Tab.DASHBOARD);
        highlight(events, active == Tab.EVENTS);
        highlight(users, active == Tab.USERS);
        highlight(images, active == Tab.IMAGES);
        highlight(logs, active == Tab.LOGS);
    }

    private static void highlight(View container, boolean active) {
        if (container == null) return;
        ImageView icon = null;
        TextView label = null;
        int id = container.getId();
        if (id == R.id.navDashboard) {
            icon = container.getRootView().findViewById(R.id.iconDashboard);
            label = container.getRootView().findViewById(R.id.labelDashboard);
        } else if (id == R.id.navEvents) {
            icon = container.getRootView().findViewById(R.id.iconEvents);
            label = container.getRootView().findViewById(R.id.labelEvents);
        } else if (id == R.id.navUsers) {
            icon = container.getRootView().findViewById(R.id.iconUsers);
            label = container.getRootView().findViewById(R.id.labelUsers);
        } else if (id == R.id.navImages) {
            icon = container.getRootView().findViewById(R.id.iconImages);
            label = container.getRootView().findViewById(R.id.labelImages);
        } else if (id == R.id.navLogs) {
            icon = container.getRootView().findViewById(R.id.iconLogs);
            label = container.getRootView().findViewById(R.id.labelLogs);
        }
        if (active) {
            container.setBackgroundColor(Color.parseColor("#22312E"));
            if (icon != null) icon.setColorFilter(Color.parseColor("#4AE3C1"));
            if (label != null) label.setTextColor(Color.parseColor("#4AE3C1"));
        } else {
            container.setBackgroundColor(Color.TRANSPARENT);
            if (icon != null) icon.setColorFilter(Color.WHITE);
            if (label != null) label.setTextColor(Color.WHITE);
        }
    }
}
