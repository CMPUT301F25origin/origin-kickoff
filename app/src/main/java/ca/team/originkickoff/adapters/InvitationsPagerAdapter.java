package ca.team.originkickoff.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import ca.team.originkickoff.ui.fragments.InvitationListFragment;

/**
 * Adapter for ViewPager2 showing invitation status tabs
 */
public class InvitationsPagerAdapter extends FragmentStateAdapter {
    private final String eventId;

    public InvitationsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String eventId) {
        super(fragmentActivity);
        this.eventId = eventId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 0 = Chosen, 1 = Cancelled, 2 = Enrolled
        String status = position == 0 ? "chosen" : position == 1 ? "cancelled" : "enrolled";
        return InvitationListFragment.newInstance(eventId, status);
    }

    @Override
    public int getItemCount() {
        return 3; // Chosen, Cancelled, Enrolled
    }
}

