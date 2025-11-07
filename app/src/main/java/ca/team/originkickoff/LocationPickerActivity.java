/*
 * Interactive map picker for selecting a custom geographic location.
 * Supports reverse geocoding to provide human-readable address.
 */
package ca.team.originkickoff;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Activity allowing a user to pick a location on a map and return its coordinates.
 */
public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "LocationPickerActivity";

    private GoogleMap googleMap;
    private LatLng selectedLocation;
    private String selectedAddress;
    private Button btnConfirmLocation;

    /**
     * Sets up map fragment and confirm button UI.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Pick Location");
        }

        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnConfirmLocation.setEnabled(false);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirmLocation.setOnClickListener(v -> confirmLocation());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Map ready callback; configures default camera and tap listener.
     *
     * @param map active GoogleMap instance
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Default location (Toronto, can be changed to user's current location)
        LatLng defaultLocation = new LatLng(43.6532, -79.3832);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));

        // Set map click listener
        googleMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));

            // Reverse geocode to get address
            getAddressFromLocation(latLng);
            btnConfirmLocation.setEnabled(true);
        });
    }

    /**
     * Performs reverse geocoding or falls back to raw coordinates.
     *
     * @param latLng selected position
     */
    private void getAddressFromLocation(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                selectedAddress = address.getAddressLine(0);
            } else {
                selectedAddress = String.format(Locale.US, "%.6f, %.6f", latLng.latitude, latLng.longitude);
            }
        } catch (IOException e) {
            e.printStackTrace();
            selectedAddress = String.format(Locale.US, "%.6f, %.6f", latLng.latitude, latLng.longitude);
        }
    }

    /**
     * Returns selected location details to the calling activity.
     */
    private void confirmLocation() {
        if (selectedLocation != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("address", selectedAddress);
            resultIntent.putExtra("latitude", selectedLocation.latitude);
            resultIntent.putExtra("longitude", selectedLocation.longitude);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
        }
    }
}
