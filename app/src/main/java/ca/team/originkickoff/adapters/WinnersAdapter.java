/**
 * Adapter listing lottery winners with ranking display.
 */
package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.team.originkickoff.R;

/**
 * RecyclerView adapter listing lottery winners for an event.
 */
public class WinnersAdapter extends RecyclerView.Adapter<WinnersAdapter.WinnerViewHolder> {
    private final List<String> winnerIds;

    /**
     * Constructs the adapter with a list of winner IDs.
     * @param winnerIds ordered list of winners (1st at index 0)
     */
    public WinnersAdapter(List<String> winnerIds) {
        this.winnerIds = winnerIds;
    }

    /**
     * Inflates the winner row layout.
     * @param parent parent view group
     * @param viewType unused view type
     * @return new view holder
     */
    @NonNull
    @Override
    public WinnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_winner, parent, false);
        return new WinnerViewHolder(view);
    }

    /**
     * Binds the winner entry at the given position.
     * @param holder target holder
     * @param position 0-based ranking index
     */
    @Override
    public void onBindViewHolder(@NonNull WinnerViewHolder holder, int position) {
        String winnerId = winnerIds.get(position);
        holder.bind(winnerId, position + 1);
    }

    /**
     * @return number of winners displayed
     */
    @Override
    public int getItemCount() {
        return winnerIds.size();
    }

    /**
     * ViewHolder showing ranking position and user identifier.
     */
    static class WinnerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPosition;
        private final TextView tvUserId;

        /**
         * Constructs the holder and binds view references.
         * @param itemView inflated item view
         */
        public WinnerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.tv_winner_position);
            tvUserId = itemView.findViewById(R.id.tv_winner_user_id);
        }

        /**
         * Populates the row with rank number and the winner's user id.
         * @param userId Firestore users/{id} of the winner
         * @param position 1-based ranking position
         */
        public void bind(String userId, int position) {
            tvPosition.setText("#" + position);
            tvUserId.setText(userId);
        }
    }
}
