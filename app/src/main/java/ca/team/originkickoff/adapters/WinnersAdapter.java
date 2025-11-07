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
 * Adapter for displaying lottery winners in a RecyclerView.
 */
public class WinnersAdapter extends RecyclerView.Adapter<WinnersAdapter.WinnerViewHolder> {
    private final List<String> winnerIds;

    public WinnersAdapter(List<String> winnerIds) {
        this.winnerIds = winnerIds;
    }

    @NonNull
    @Override
    public WinnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_winner, parent, false);
        return new WinnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WinnerViewHolder holder, int position) {
        String winnerId = winnerIds.get(position);
        holder.bind(winnerId, position + 1);
    }

    @Override
    public int getItemCount() {
        return winnerIds.size();
    }

    static class WinnerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPosition;
        private final TextView tvUserId;

        public WinnerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.tv_winner_position);
            tvUserId = itemView.findViewById(R.id.tv_winner_user_id);
        }

        public void bind(String userId, int position) {
            tvPosition.setText("#" + position);
            // Display user ID (you can enhance this to load and display user names)
            tvUserId.setText(userId);
        }
    }
}

