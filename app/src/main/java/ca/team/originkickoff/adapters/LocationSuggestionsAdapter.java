package ca.team.originkickoff.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.R;

public class LocationSuggestionsAdapter extends RecyclerView.Adapter<LocationSuggestionsAdapter.ViewHolder> {

    private List<AutocompletePrediction> suggestions = new ArrayList<>();
    private OnLocationSelectedListener listener;

    public interface OnLocationSelectedListener {
        void onLocationSelected(AutocompletePrediction prediction);
    }

    public LocationSuggestionsAdapter(OnLocationSelectedListener listener) {
        this.listener = listener;
    }

    public void setSuggestions(List<AutocompletePrediction> suggestions) {
        this.suggestions = suggestions;
        notifyDataSetChanged();
    }

    public void clearSuggestions() {
        this.suggestions.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AutocompletePrediction prediction = suggestions.get(position);
        holder.tvPrimaryText.setText(prediction.getPrimaryText(null));
        holder.tvSecondaryText.setText(prediction.getSecondaryText(null));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLocationSelected(prediction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPrimaryText;
        TextView tvSecondaryText;

        ViewHolder(View itemView) {
            super(itemView);
            tvPrimaryText = itemView.findViewById(R.id.tvPrimaryText);
            tvSecondaryText = itemView.findViewById(R.id.tvSecondaryText);
        }
    }
}

