// filepath: /Users/sargun/StudioProjects/origin-kickoff/app/src/main/java/ca/team/originkickoff/adapters/EntrantAdapter.java
package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.Entrant;
import ca.team.originkickoff.models.EntrantStatus;

public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.VH> {
    private List<Entrant> items = new ArrayList<>();
    private final OnCancelListener onCancel;

    public interface OnCancelListener { void onCancel(Entrant entrant); }

    public EntrantAdapter(OnCancelListener onCancel) { this.onCancel = onCancel; }

    public void submitList(List<Entrant> list) {
        items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        TextView name, email, status;
        Button cancel;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvEntrantName);
            email = itemView.findViewById(R.id.tvEntrantEmail);
            status = itemView.findViewById(R.id.tvEntrantStatus);
            cancel = itemView.findViewById(R.id.btnCancelEntrant);
        }

        void bind(Entrant e) {
            name.setText(e.getName());
            email.setText(e.getEmail());
            status.setText(e.getStatus().name());
            cancel.setVisibility(e.getStatus() == EntrantStatus.PENDING ? View.VISIBLE : View.GONE);
            cancel.setOnClickListener(v -> onCancel.onCancel(e));
        }
    }
}

