/**
 * Adapter displaying Google Places autocomplete predictions for location selection.
 * Provides click callbacks when a suggestion is chosen.
 */
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

/**
 * RecyclerView adapter binding {@link com.google.android.libraries.places.api.model.AutocompletePrediction} entries.
 */
public class LocationSuggestionsAdapter extends RecyclerView.Adapter<LocationSuggestionsAdapter.ViewHolder> {

    private List<AutocompletePrediction> suggestions = new ArrayList<>();
    private OnLocationSelectedListener listener;

    /**
     * Callback for when a prediction is selected by the user.
     */
    public interface OnLocationSelectedListener {
        /**
         * Invoked when the user taps a location suggestion.
         * @param prediction the selected autocomplete prediction
         */
        void onLocationSelected(AutocompletePrediction prediction);
    }

    /**
     * Constructs the adapter with a selection listener.
     * @param listener callback invoked on item clicks (nullable)
     */
    public LocationSuggestionsAdapter(OnLocationSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the current suggestions list.
     * @param suggestions new predictions list
     */
    public void setSuggestions(List<AutocompletePrediction> suggestions) {
        this.suggestions = suggestions;
        notifyDataSetChanged();
    }

    /**
     * Clears all suggestions.
     */
    public void clearSuggestions() {
        this.suggestions.clear();
        notifyDataSetChanged();
    }

    /**
     * Inflates a suggestion item view.
     * @param parent parent view group
     * @param viewType unused view type
     * @return a new view holder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_suggestion, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the prediction at the given position.
     * @param holder holder to bind
     * @param position adapter position
     */
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

    /**
     * @return number of predictions currently displayed
     */
    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    /**
     * ViewHolder displaying primary and secondary texts for a prediction.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPrimaryText;
        TextView tvSecondaryText;

        /**
         * Creates the holder and binds view references.
         * @param itemView inflated item view
         */
        ViewHolder(View itemView) {
            super(itemView);
            tvPrimaryText = itemView.findViewById(R.id.tvPrimaryText);
            tvSecondaryText = itemView.findViewById(R.id.tvSecondaryText);
        }
    }
}
