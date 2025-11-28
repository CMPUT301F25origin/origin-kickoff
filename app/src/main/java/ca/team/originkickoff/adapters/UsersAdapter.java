package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.User;

/**
 * Adapter rendering all users for admin browsing.
 */
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {
    private final List<User> users = new ArrayList<>();
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UsersAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> list) {
        users.clear();
        if (list != null) users.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class VH extends RecyclerView.ViewHolder {
        private final ImageView ivProfile;
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvRole;

        VH(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
        }

        void bind(User u) {
            String name = (u.getDisplayName() != null && !u.getDisplayName().isEmpty()) ? u.getDisplayName() : "Unnamed";
            tvName.setText(name);
            tvEmail.setText(u.getEmail() != null ? u.getEmail() : "");
            tvRole.setText(resolveRole(u));
            itemView.setOnClickListener(v -> { if (listener != null) listener.onUserClick(u); });
        }

        private String resolveRole(User u) {
            if (u.isAdmin()) return itemView.getContext().getString(R.string.role_admin);
            if (u.isOrganizer()) return itemView.getContext().getString(R.string.role_organizer);
            return itemView.getContext().getString(R.string.role_entrant);
        }
    }
}

