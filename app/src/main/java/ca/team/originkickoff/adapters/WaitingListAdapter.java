package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.WaitingListEntry;

public class WaitingListAdapter extends RecyclerView.Adapter<WaitingListAdapter.VH> {
    private final List<WaitingListEntry> items = new ArrayList<>();

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_waiting_list_entry, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        WaitingListEntry e = items.get(position);
        holder.userId.setText(e.getUserId());
        holder.source.setText(e.getSource());
        String when = e.getJoinedAt() != null ?
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(e.getJoinedAt().getSeconds()*1000)) :
                "";
        holder.joinedAt.setText(when);
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void submit(List<WaitingListEntry> in) {
        items.clear();
        if (in != null) items.addAll(in);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView userId, joinedAt, source;
        VH(@NonNull View itemView) {
            super(itemView);
            userId = itemView.findViewById(R.id.tvUserId);
            joinedAt = itemView.findViewById(R.id.tvJoinedAt);
            source = itemView.findViewById(R.id.tvSource);
        }
    }
}

