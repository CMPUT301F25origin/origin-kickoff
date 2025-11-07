/*
 * Full-screen simplified waitlist map showing entrant positions (consent-based).
 * Provides camera fitting for all markers and graceful fallback when none exist.
 */
package ca.team.originkickoff;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity presenting a full-screen map of entrants for a single event.
 */
public class WaitlistMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String EXTRA_EVENT_ID = "event_id";

    private GoogleMap map;
    private String eventId;

    /**
     * Sets up layout and requests map async; validates required extras.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waitlist_map);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Missing event id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Map fragment not found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Google Maps ready callback to initialize UI settings and load markers.
     *
     * @param googleMap map instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);

        loadMarkers();
    }

    private void loadMarkers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("waiting_list_entries")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("state", "active")
                .get()
                .addOnSuccessListener(snaps -> {
                    List<LatLng> points = new ArrayList<>();
                    for (DocumentSnapshot s : snaps.getDocuments()) {
                        Double lat = s.getDouble("lat");
                        Double lon = s.getDouble("lon");
                        Boolean consent = s.getBoolean("location_consent");
                        if (lat != null && lon != null && consent != null && consent) {
                            LatLng p = new LatLng(lat, lon);
                            points.add(p);
                            map.addMarker(new MarkerOptions()
                                    .position(p)
                                    .title("Entrant")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        }
                    }
                    fitToPoints(points);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load locations", Toast.LENGTH_SHORT).show());
    }

    private void fitToPoints(List<LatLng> points) {
        if (map == null) return;
        if (points.isEmpty()) {
            // Default to Canada center-ish if no points
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(56.1304, -106.3468), 3.5f));
            return;
        }
        LatLngBounds.Builder b = LatLngBounds.builder();
        for (LatLng p : points) b.include(p);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 100));
    }
}
