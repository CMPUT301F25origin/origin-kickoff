/**
 * Pager adapter supplying three invitation status fragments for a given event.
 */
package ca.team.originkickoff.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import ca.team.originkickoff.ui.fragments.InvitationListFragment;

/**
 * Adapter for ViewPager2 showing invitation status tabs (chosen, cancelled, enrolled) for a single event.
 */
public class InvitationsPagerAdapter extends FragmentStateAdapter {
    private final String eventId;

    /**
     * Constructs the pager adapter.
     * @param fragmentActivity host activity
     * @param eventId Firestore event document ID whose invitations are shown
     */
    public InvitationsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String eventId) {
        super(fragmentActivity);
        this.eventId = eventId;
    }

    /**
     * Creates a fragment for the given position (0=chosen, 1=cancelled, 2=enrolled).
     * @param position page index
     * @return new {@link InvitationListFragment} instance filtered by status
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String status = position == 0 ? "chosen" : position == 1 ? "cancelled" : "enrolled";
        return InvitationListFragment.newInstance(eventId, status);
    }

    /**
     * @return fixed page count (3 statuses)
     */
    @Override
    public int getItemCount() {
        return 3;
    }
}
