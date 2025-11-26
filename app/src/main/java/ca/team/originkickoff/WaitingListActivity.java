/*
 * Compact waiting list manager showing entrants and optional location preview map.
 * Fetches entrant data and conditionally displays a static map snapshot.
 */
package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.adapters.WaitingListAdapter;
import ca.team.originkickoff.models.WaitingListEntry;
import ca.team.originkickoff.services.WaitingListService;

/**
 * Activity displaying active waiting list entrants for an event with optional map preview.
 * Lists entrants and offers a full-screen map when geolocation is required.
 */
public class WaitingListActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "WaitingListActivity";
    public static final String EXTRA_EVENT_ID = "event_id";

    private final WaitingListService service = new WaitingListService();
    private RecyclerView recyclerView;
    private WaitingListAdapter adapter;
    private ProgressBar progressBar;
    private String eventId;

    private MapView mapView;
    private GoogleMap googleMap;
    private CardView mapPreviewCard;
    private List<WaitingListEntry> currentEntries = new ArrayList<>();
    private boolean isGeolocationRequired = true; // Default to true

    /**
     * Initializes UI, reads event ID, and begins loading entries.
     *
     * @param savedInstanceState saved instance state bundle
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_list);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Missing event id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.rvWaitingList);
        progressBar = findViewById(R.id.progress);
        mapView = findViewById(R.id.mapView);
        mapPreviewCard = findViewById(R.id.mapPreviewCard);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaitingListAdapter(entry -> showRemoveEntrantDialog(entry));
        recyclerView.setAdapter(adapter);

        // Load event details first to check geolocation requirement
        loadEventDetails();

        // Set up click listener for map expansion
        if (mapPreviewCard != null) {
            mapPreviewCard.setOnClickListener(v -> openFullScreenMap());
        }

        // Wire up View Map button if present in the layout
        int mapBtnId = getResources().getIdentifier("btnViewMap", "id", getPackageName());
        View mapBtn = mapBtnId != 0 ? findViewById(mapBtnId) : null;
        if (mapBtn != null) {
            mapBtn.setOnClickListener(v -> openFullScreenMap());
        }

        loadEntries();
    }

    /**
     * Loads event details to determine if geolocation is required and sets up the map preview accordingly.
     */
    private void loadEventDetails() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean geolocationRequired = documentSnapshot.getBoolean("geolocationRequired");
                        isGeolocationRequired = geolocationRequired != null && geolocationRequired;

                        // Show/hide map based on geolocation requirement
                        if (mapPreviewCard != null) {
                            mapPreviewCard.setVisibility(isGeolocationRequired ? View.VISIBLE : View.GONE);
                        }

                        // Initialize MapView only if geolocation is required
                        if (isGeolocationRequired && mapView != null) {
                            mapView.onCreate(null);
                            mapView.getMapAsync(this);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event details", e);
                    // Default to showing map if we can't determine
                });
    }

    /**
     * Callback fired when the preview map is ready; plots any loaded entrant markers.
     *
     * @param map google map instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);

        // Plot markers if we already have entries
        if (!currentEntries.isEmpty()) {
            plotUserLocations(currentEntries);
        }
    }

    /**
     * Resumes the map view and reloads waiting list entries.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (eventId != null) {
            loadEntries();
        }
    }

    /**
     * Pauses the map view to match activity lifecycle.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    /**
     * Destroys the map view and cleans up resources.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    /**
     * Forwards state saving to MapView.
     *
     * @param outState state bundle to populate
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    /**
     * Notifies MapView of low-memory conditions.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    /**
     * Queries the waiting list service for current entries and updates the UI.
     */
    private void loadEntries() {
        progressBar.setVisibility(View.VISIBLE);
        service.listActive(eventId)
                .addOnSuccessListener(this::showEntries)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load waiting list", e);
                    Toast.makeText(this, "Failed to load waiting list", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    /**
     * Submits the entries to the adapter and plots their locations on the map.
     *
     * @param entries list of waiting list entries (may be null)
     */
    private void showEntries(List<WaitingListEntry> entries) {
        currentEntries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
        adapter.submit(currentEntries);
        progressBar.setVisibility(View.GONE);

        // Plot locations on map
        if (googleMap != null) {
            plotUserLocations(currentEntries);
        }
    }

    /**
     * Draws markers for entrants that have shared location, and adjusts camera bounds.
     *
     * @param entries list of waiting list entries
     */
    private void plotUserLocations(List<WaitingListEntry> entries) {
        if (googleMap == null || entries == null || entries.isEmpty()) {
            return;
        }

        googleMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        int validLocationCount = 0;

        for (WaitingListEntry entry : entries) {
            if (entry.getLat() != null && entry.getLon() != null) {
                LatLng position = new LatLng(entry.getLat(), entry.getLon());
                googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title("Entrant"));
                boundsBuilder.include(position);
                validLocationCount++;
            }
        }

        // Adjust camera to show all markers
        if (validLocationCount > 0) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 100; // pixels
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                Log.e(TAG, "Error adjusting camera bounds", e);
            }
        } else {
            // No valid locations, show default view
            LatLng defaultLocation = new LatLng(43.6532, -79.3832); // Toronto
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
        }
    }

    /**
     * Launches the full-screen map activity with entrant coordinates.
     */
    private void openFullScreenMap() {
        if (currentEntries.isEmpty()) {
            Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect all coordinates with location data
        ArrayList<Double> latitudes = new ArrayList<>();
        ArrayList<Double> longitudes = new ArrayList<>();
        ArrayList<String> userIds = new ArrayList<>();
        ArrayList<String> userNames = new ArrayList<>();

        for (WaitingListEntry entry : currentEntries) {
            if (entry.getLat() != null && entry.getLon() != null) {
                latitudes.add(entry.getLat());
                longitudes.add(entry.getLon());
                userIds.add(entry.getUserId());
                userNames.add("Loading..."); // Placeholder, will be fetched in map activity
            }
        }

        if (latitudes.isEmpty()) {
            Toast.makeText(this, "No entrants have shared their location", Toast.LENGTH_SHORT).show();
            return;
        }

        // Launch full screen map activity
        Intent intent = new Intent(this, WaitingListMapActivity.class);
        intent.putExtra("event_id", eventId);
        intent.putExtra("latitudes", latitudes);
        intent.putExtra("longitudes", longitudes);
        intent.putExtra("user_ids", userIds);
        startActivity(intent);
    }

    /**
     * Shows a confirmation dialog to remove the given entrant.
     */
    private void showRemoveEntrantDialog(WaitingListEntry entry) {
        if (entry == null) return;
        // Fetch user's display name from Firestore, then show confirmation dialog with proper name.
        String userId = entry.getUserId();
        if (userId == null || userId.isEmpty()) {
            showRemoveDialogWithName("Entrant", entry);
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = extractDisplayNameFromDoc(doc);
                    if (name == null || name.isEmpty()) name = userId;
                    showRemoveDialogWithName(name, entry);
                })
                .addOnFailureListener(e -> {
                    // Fallback to userId on failure
                    showRemoveDialogWithName(userId, entry);
                });
    }

    private void showRemoveDialogWithName(String displayName, WaitingListEntry entry) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.remove_entrant_confirm_title))
                .setMessage(getString(R.string.remove_entrant_confirm_message, displayName))
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.remove_entrant_confirm_remove, (d, w) -> {
                    // Call service.leave to mark the entrant left and decrement counters
                    service.leave(eventId, entry.getUserId())
                            .addOnSuccessListener(changed -> {
                                if (changed != null && changed) {
                                    Toast.makeText(this, R.string.remove_entrant_removed, Toast.LENGTH_SHORT).show();
                                    loadEntries();
                                } else {
                                    Toast.makeText(this, R.string.remove_entrant_failed, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, R.string.remove_entrant_failed, Toast.LENGTH_SHORT).show();
                            });
                })
                .show();
    }

    private String extractDisplayNameFromDoc(com.google.firebase.firestore.DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        String[] keys = new String[]{"display_name", "displayName", "name", "username", "email"};
        for (String k : keys) {
            Object v = doc.get(k);
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }
}
