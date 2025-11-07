package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.appcompat.widget.Toolbar;

public class MyEventsActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_events);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Events");
        }

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return new ca.team.originkickoff.ui.fragments.EventsJoinedFragment();
                    case 1:
                    default:
                        return new ca.team.originkickoff.ui.fragments.EventsOrganizedFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("Events Joined");
            else tab.setText("Events Organized");
        }).attach();

        // Wire FAB: only visible on Events Organized (index 1)
        FloatingActionButton fab = findViewById(R.id.fabCreate);
        if (fab != null) {
            fab.setOnClickListener(v -> startActivity(new Intent(MyEventsActivity.this, CreateEventActivity.class)));

            // initial visibility
            fab.setVisibility(viewPager.getCurrentItem() == 1 ? View.VISIBLE : View.GONE);

            // show/hide based on page
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    if (position == 1) fab.setVisibility(View.VISIBLE);
                    else fab.setVisibility(View.GONE);
                }
            });
        }

        // Setup bottom navigation include
        setupBottomNav();
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navEvents = findViewById(R.id.navEvents);
        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        if (navHome == null || navEvents == null || navNotifications == null || navProfile == null) return;

        // Highlight current tab (My Events)
        ImageView ivEvents = findViewById(R.id.ivEvents);
        TextView tvEvents = findViewById(R.id.tvEvents);
        if (ivEvents != null) ivEvents.setColorFilter(0xFF00D9C5, android.graphics.PorterDuff.Mode.SRC_IN);
        if (tvEvents != null) tvEvents.setTextColor(0xFF00D9C5);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(MyEventsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        navEvents.setOnClickListener(v -> { /* already here */ });
        navNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(MyEventsActivity.this, NotificationsActivity.class);
            startActivity(intent);
        });
        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MyEventsActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
