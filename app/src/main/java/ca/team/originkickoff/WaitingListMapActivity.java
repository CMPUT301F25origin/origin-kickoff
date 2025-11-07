/*
 * Full-screen map view of entrant locations and event marker.
 * Supports immersive UI toggle and fetches entrant names for labels.
 */
package ca.team.originkickoff;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.models.User;

/**
 * Displays entrant locations on a Google Map for a specific event.
 * Fetches entrant names and plots markers alongside the event location.
 */
public class WaitingListMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "WaitingListMapActivity";

    private GoogleMap googleMap;
    private ArrayList<Double> latitudes;
    private ArrayList<Double> longitudes;
    private ArrayList<String> userIds;
    private String eventId;
    private FirebaseFirestore db;
    private Map<String, String> userNamesCache = new HashMap<>();
    private Event event;
    private int userNamesFetched = 0;
    private boolean isImmersive = false;
    private ImageButton btnClose;
    private ImageButton btnFullscreen;

    /**
     * Initializes UI, parses intent extras, and kicks off map/data loading.
     *
     * @param savedInstanceState previous state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start non-immersive; allow toggling to full screen
        setSystemUiImmersive(false);

        setContentView(R.layout.activity_waiting_list_map);

        db = FirebaseFirestore.getInstance();

        // Get data from intent
        eventId = getIntent().getStringExtra("event_id");
        @SuppressWarnings("unchecked")
        ArrayList<Double> lats = (ArrayList<Double>) getIntent().getSerializableExtra("latitudes");
        @SuppressWarnings("unchecked")
        ArrayList<Double> lngs = (ArrayList<Double>) getIntent().getSerializableExtra("longitudes");
        @SuppressWarnings("unchecked")
        ArrayList<String> uIds = (ArrayList<String>) getIntent().getSerializableExtra("user_ids");

        latitudes = lats;
        longitudes = lngs;
        userIds = uIds;

        // If no entrant locations, keep going so event marker can still render
        if (latitudes == null || longitudes == null || latitudes.isEmpty()) {
            Toast.makeText(this, "No entrant locations", Toast.LENGTH_SHORT).show();
            latitudes = new ArrayList<>();
            longitudes = new ArrayList<>();
            userIds = new ArrayList<>();
        }

        // Set up close button
        btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        // Fullscreen toggle button (if present)
        btnFullscreen = findViewById(R.id.btnFullscreenToggle);
        if (btnFullscreen != null) {
            btnFullscreen.setOnClickListener(v -> setSystemUiImmersive(!isImmersive));
        }

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Fetch event details for event location
        if (eventId != null) {
            fetchEventDetails();
        }

        // Fetch user names
        if (userIds != null && !userIds.isEmpty()) {
            fetchUserNames();
        }
    }

    /**
     * Fetches event details from Firestore to obtain event location and name.
     */
    private void fetchEventDetails() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            event.setId(documentSnapshot.getId());
                            // Refresh map to add event location marker
                            if (googleMap != null) {
                                plotMarkers();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch event details", e);
                });
    }

    /**
     * Resolves display names for user IDs so map markers can show readable titles.
     */
    private void fetchUserNames() {
        Log.d(TAG, "Fetching user names for " + userIds.size() + " users");
        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            final int index = i;

            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null && user.getDisplayName() != null) {
                                userNamesCache.put(userId, user.getDisplayName());
                                Log.d(TAG, "Fetched user name: " + user.getDisplayName() + " for ID: " + userId);
                            } else {
                                userNamesCache.put(userId, "Entrant " + (index + 1));
                                Log.d(TAG, "User has no display name, using fallback for ID: " + userId);
                            }
                        } else {
                            userNamesCache.put(userId, "Entrant " + (index + 1));
                            Log.w(TAG, "User document does not exist for ID: " + userId);
                        }

                        userNamesFetched++;
                        Log.d(TAG, "Fetched " + userNamesFetched + " of " + userIds.size() + " user names");
                        // Once all names are fetched, refresh the map
                        if (userNamesFetched == userIds.size() && googleMap != null) {
                            Log.d(TAG, "All user names fetched, refreshing map markers");
                            plotMarkers();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user name for " + userId, e);
                        userNamesCache.put(userId, "Entrant " + (index + 1));
                        userNamesFetched++;

                        if (userNamesFetched == userIds.size() && googleMap != null) {
                            Log.d(TAG, "All user names fetched (with errors), refreshing map markers");
                            plotMarkers();
                        }
                    });
        }
    }

    /**
     * Handles action bar item selections.
     * @param item the pressed menu item
     * @return true if handled
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
     * Called when Google Map is ready. Configures UI settings and plots markers.
     * @param map GoogleMap instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        googleMap.getUiSettings().setTiltGesturesEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // Tap anywhere on the map to toggle immersive fullscreen
        googleMap.setOnMapClickListener(latLng -> setSystemUiImmersive(!isImmersive));

        plotMarkers();
    }

    /**
     * Ensures the activity finishes on back press.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * Adds entrant and event markers to the map and adjusts camera bounds.
     */
    private void plotMarkers() {
        if (googleMap == null || latitudes == null || longitudes == null) {
            return;
        }

        googleMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        int markerCount = 0;

        // Add entrant markers (red) with entrant names as labels
        for (int i = 0; i < latitudes.size() && i < longitudes.size(); i++) {
            Double lat = latitudes.get(i);
            Double lng = longitudes.get(i);

            if (lat != null && lng != null) {
                LatLng position = new LatLng(lat, lng);

                // Get user name from cache or use placeholder
                String userName = "Entrant " + (i + 1);
                if (userIds != null && i < userIds.size()) {
                    String userId = userIds.get(i);
                    if (userNamesCache.containsKey(userId)) {
                        userName = userNamesCache.get(userId);
                    }
                }

                googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(userName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                boundsBuilder.include(position);
                markerCount++;
            }
        }

        // Add event location marker (blue, larger)
        if (event != null && event.getLocationLatitude() != 0 && event.getLocationLongitude() != 0) {
            LatLng eventLocation = new LatLng(event.getLocationLatitude(), event.getLocationLongitude());

            // Use a larger, blue vector-based icon for the event
            // Fallback to default blue marker if vector conversion fails
            com.google.android.gms.maps.model.BitmapDescriptor eventIcon = null;
            try {
                eventIcon = MapMarkerUtil.bitmapDescriptorFromVector(this, R.drawable.ic_location, 48 /* dp size */, MapMarkerUtil.BLUE_TINT);
            } catch (Exception ignored) {
            }

            MarkerOptions eventMarker = new MarkerOptions()
                    .position(eventLocation)
                    .title("Event Location: " + (event.getName() != null ? event.getName() : "Event"))
                    .snippet(event.getLocation());

            if (eventIcon != null) {
                eventMarker.icon(eventIcon);
            } else {
                eventMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            }

            googleMap.addMarker(eventMarker);
            boundsBuilder.include(eventLocation);
            markerCount++;
        }

        // Adjust camera to show all markers
        if (markerCount > 0) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 150; // pixels
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                Log.e(TAG, "Error adjusting camera bounds", e);
                // Fallback to first marker
                if (!latitudes.isEmpty() && !longitudes.isEmpty()) {
                    LatLng firstPosition = new LatLng(latitudes.get(0), longitudes.get(0));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 12));
                }
            }
        }
    }

    /**
     * Toggles immersive full-screen UI and updates overlay control visibility.
     * @param immersive true to enable immersive mode
     */
    private void setSystemUiImmersive(boolean immersive) {
        isImmersive = immersive;
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (immersive) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(flags);

        // Hide overlay controls in immersive for true full-screen feel
        if (btnClose != null) btnClose.setVisibility(immersive ? View.GONE : View.VISIBLE);
        if (btnFullscreen != null) btnFullscreen.setImageResource(immersive ? android.R.drawable.ic_menu_revert : android.R.drawable.ic_menu_view);
    }
}
