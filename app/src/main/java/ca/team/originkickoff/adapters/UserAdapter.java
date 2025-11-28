package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.User;

/**
 * RecyclerView adapter for displaying users in AdminUsersActivity.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final List<User> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public UserAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> list) {
        users.clear();
        if (list != null) users.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User u = users.get(position);
        holder.tvName.setText(u.getDisplayName() != null ? u.getDisplayName() : holder.itemView.getContext().getString(R.string.unknown));
        holder.tvEmail.setText(u.getEmail() != null ? u.getEmail() : "");
        String role;
        if (u.isAdmin()) role = holder.itemView.getContext().getString(R.string.role_admin);
        else if (u.isOrganizer()) role = holder.itemView.getContext().getString(R.string.role_organizer);
        else role = holder.itemView.getContext().getString(R.string.role_entrant);
        holder.tvRole.setText(role);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(u);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
        }
    }
}
