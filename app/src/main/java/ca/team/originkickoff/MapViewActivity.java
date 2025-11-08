/*
 * Read-only map viewer for a single event location with option to open external Google Maps.
 * Provides marker placement and graceful fallback for missing data.
 */
package ca.team.originkickoff;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

/**
 * Activity showing a map centered on provided coordinates.
 */
public class MapViewActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MapViewActivity";

    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_LOCATION_NAME = "location_name";

    private GoogleMap googleMap;
    private double latitude;
    private double longitude;
    private String locationName;
    private TextView tvLocationName;
    private Button btnOpenInGoogleMaps;

    /**
     * Initializes views, validates required extras, and requests map.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Location");
        }

        // Get location data from intent
        latitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0.0);
        longitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0.0);
        locationName = getIntent().getStringExtra(EXTRA_LOCATION_NAME);

        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Location coordinates not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvLocationName = findViewById(R.id.tvLocationName);
        btnOpenInGoogleMaps = findViewById(R.id.btnOpenInGoogleMaps);

        if (locationName != null && !locationName.isEmpty()) {
            tvLocationName.setText(locationName);
        } else {
            tvLocationName.setText(String.format(Locale.US, "%.6f, %.6f", latitude, longitude));
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnOpenInGoogleMaps.setOnClickListener(v -> openInGoogleMaps());
    }

    /**
     * Handles navigation when the back button in the action bar is pressed.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Places marker and configures map UI when ready.
     *
     * @param map GoogleMap instance
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        LatLng location = new LatLng(latitude, longitude);

        // Add marker at location
        googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(locationName != null ? locationName : "Event Location"));

        // Move camera to location with zoom
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));

        // Enable map controls
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    /**
     * Launches external maps app or browser fallback for the location.
     */
    private void openInGoogleMaps() {
        // Create URI for Google Maps
        String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                latitude, longitude, latitude, longitude,
                locationName != null ? locationName : "Event Location");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // If Google Maps not installed, open in browser
            String browserUri = String.format(Locale.US,
                    "https://www.google.com/maps/search/?api=1&query=%f,%f",
                    latitude, longitude);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri));
            startActivity(browserIntent);
        }
    }
}
