package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;

import ca.team.originkickoff.adapters.LocationSuggestionsAdapter;

public class LocationSearchActivity extends AppCompatActivity {
    private static final String TAG = "LocationSearchActivity";

    private EditText etSearchLocation;
    private RecyclerView rvSuggestions;
    private ProgressBar progressBar;
    private TextView tvNoResults;
    private TextView tvSelectCustom;

    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private LocationSuggestionsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_search);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Location");
        }

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();

        bindViews();
        setupRecyclerView();
        attachListeners();
    }

    private void bindViews() {
        etSearchLocation = findViewById(R.id.etSearchLocation);
        rvSuggestions = findViewById(R.id.rvSuggestions);
        progressBar = findViewById(R.id.progressBar);
        tvNoResults = findViewById(R.id.tvNoResults);
        tvSelectCustom = findViewById(R.id.tvSelectCustom);
    }

    private void setupRecyclerView() {
        adapter = new LocationSuggestionsAdapter(prediction -> {
            // When a location is selected
            fetchPlaceDetails(prediction.getPlaceId());
        });
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(adapter);
    }

    private void attachListeners() {
        etSearchLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    searchLocations(s.toString());
                } else {
                    adapter.clearSuggestions();
                    tvNoResults.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        tvSelectCustom.setOnClickListener(v -> {
            // Open map picker for custom location
            Intent intent = new Intent(this, LocationPickerActivity.class);
            startActivityForResult(intent, 100);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void searchLocations(String query) {
        progressBar.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            progressBar.setVisibility(View.GONE);
            List<AutocompletePrediction> predictions = response.getAutocompletePredictions();

            if (predictions.isEmpty()) {
                tvNoResults.setVisibility(View.VISIBLE);
            } else {
                tvNoResults.setVisibility(View.GONE);
                adapter.setSuggestions(predictions);
            }
        }).addOnFailureListener(exception -> {
            progressBar.setVisibility(View.GONE);
            tvNoResults.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Error searching locations: " + exception.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchPlaceDetails(String placeId) {
        progressBar.setVisibility(View.VISIBLE);

        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        );

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            progressBar.setVisibility(View.GONE);
            Place place = response.getPlace();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("address", place.getAddress());
            resultIntent.putExtra("placeId", place.getId());
            if (place.getLatLng() != null) {
                resultIntent.putExtra("latitude", place.getLatLng().latitude);
                resultIntent.putExtra("longitude", place.getLatLng().longitude);
            }
            setResult(RESULT_OK, resultIntent);
            finish();
        }).addOnFailureListener(exception -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error fetching location details: " + exception.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Return custom location from map picker
            setResult(RESULT_OK, data);
            finish();
        }
    }
}

