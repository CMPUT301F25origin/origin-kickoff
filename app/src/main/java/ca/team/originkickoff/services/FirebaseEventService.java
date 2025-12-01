/*
 * Firestore-backed service for reading Event data used throughout the app.
 * Centralizes event queries and parsing to keep UI layers simple.
 */
package ca.team.originkickoff.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import ca.team.originkickoff.models.Event;

/**
 * Provides methods to fetch events and individual event details from Firestore.
 */
public class FirebaseEventService {
    private static final String TAG = "FirebaseEventService";
    private static final String EVENTS_COLLECTION = "events";
    private FirebaseFirestore db;

    /**
     * Creates a new instance backed by the default FirebaseFirestore.
     */
    public FirebaseEventService() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetches events currently within their registration window among those marked as published.
     * Falls back to a debug fetch of all events if the filtered query fails (e.g., missing index).
     *
     * @param callback callback invoked with the filtered list or an error message
     */
    public void getEventsWithOpenRegistration(@NonNull EventsCallback callback) {
        Log.d(TAG, "Starting event fetch - looking for published events");
        db.collection(EVENTS_COLLECTION)
                .whereEqualTo("status", "published")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching events with status filter: " + error.getMessage(), error);
                        Log.d(TAG, "Trying fallback: fetching all events without status filter");
                        getAllEventsDebug(callback);
                        return;
                    }

                    if (snapshot != null) {
                        Log.d(TAG, "Firestore snapshot received. Total documents: " + snapshot.size());
                        java.util.List<Event> events = new java.util.ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                Event event = doc.toObject(Event.class);
                                Log.d(TAG, "Processing document: " + doc.getId());

                                if (event != null) {
                                    event.setId(doc.getId());
                                    Log.d(TAG, "Event loaded: " + event.getName() + ", Status: " + (event.getRegistrationStartTime() != null ? "has dates" : "no dates"));

                                    long currentTime = System.currentTimeMillis();
                                    java.util.Date start = event.getRegistrationStartTime();
                                    java.util.Date end = event.getRegistrationEndTime();

                                    boolean hasBoth = start != null && end != null;
                                    boolean withinWindow = hasBoth && currentTime >= start.getTime() && currentTime <= end.getTime();

                                    if (withinWindow) {
                                        events.add(event);
                                        Log.d(TAG, "✓ Added open event: " + event.getName());
                                    } else {
                                        Log.d(TAG, "✗ Event not in registration window: " + event.getName());
                                    }
                                } else {
                                    Log.d(TAG, "Event object is null for document: " + doc.getId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing event: ", e);
                            }
                        }

                        events.sort((e1, e2) -> {
                            if (e1.getRegistrationEndTime() == null || e2.getRegistrationEndTime() == null) {
                                return 0;
                            }
                            return Long.compare(e1.getRegistrationEndTime().getTime(),
                                              e2.getRegistrationEndTime().getTime());
                        });

                        Log.d(TAG, "Final result: " + events.size() + " events with open registration");
                        callback.onSuccess(events);
                    } else {
                        Log.d(TAG, "Snapshot is null");
                        callback.onSuccess(new java.util.ArrayList<>());
                    }
                });
    }

    /**
     * Fallback helper that fetches all events for diagnostic purposes.
     *
     * @param callback callback invoked with any events found or error message
     */
    private void getAllEventsDebug(@NonNull EventsCallback callback) {
        Log.d(TAG, "Debug fetch: Getting ALL events");
        db.collection(EVENTS_COLLECTION)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error in debug fetch: " + error.getMessage(), error);
                        callback.onError("Debug fetch failed: " + error.getMessage());
                        return;
                    }

                    if (snapshot != null) {
                        Log.d(TAG, "Debug: Found " + snapshot.size() + " total events in collection");
                        java.util.List<Event> events = new java.util.ArrayList<>();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                Event event = doc.toObject(Event.class);
                                if (event != null) {
                                    event.setId(doc.getId());
                                    Log.d(TAG, "Debug Event: " + event.getName() + ", Status: " + (event.getRegistrationStartTime() != null ? "has times" : "NO TIMES"));

                                    long currentTime = System.currentTimeMillis();
                                    java.util.Date start = event.getRegistrationStartTime();
                                    java.util.Date end = event.getRegistrationEndTime();

                                    boolean hasBoth = start != null && end != null;
                                    boolean withinWindow = hasBoth && currentTime >= start.getTime() && currentTime <= end.getTime();

                                    if (withinWindow) {
                                        events.add(event);
                                        Log.d(TAG, "✓ Debug added: " + event.getName());
                                    } else {
                                        Log.d(TAG, "✗ Debug out of window: " + event.getName());
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error in debug parse: ", e);
                            }
                        }

                        Log.d(TAG, "Debug result: " + events.size() + " events");
                        callback.onSuccess(events);
                    }
                });
    }

    /**
     * Retrieves all events without filtering.
     *
     * @param callback callback invoked with the full list or error message
     */
    public void getAllEvents(@NonNull EventsCallback callback) {
        Log.d(TAG, "Fetching ALL events from Firestore.");
        db.collection(EVENTS_COLLECTION)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching all events: " + error.getMessage(), error);
                        callback.onError("Error fetching all events: " + error.getMessage());
                        return;
                    }

                    if (snapshot != null) {
                        Log.d(TAG, "Firestore snapshot received. Total documents: " + snapshot.size());
                        java.util.List<Event> events = new java.util.ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                Event event = doc.toObject(Event.class);
                                if (event != null) {
                                    event.setId(doc.getId());
                                    events.add(event);
                                    Log.d(TAG, "✓ Added event: " + event.getName());
                                } else {
                                    Log.d(TAG, "Event object is null for document: " + doc.getId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing event: ", e);
                            }
                        }
                        Log.d(TAG, "Final result: " + events.size() + " total events found.");
                        callback.onSuccess(events);
                    } else {
                        Log.d(TAG, "Snapshot is null");
                        callback.onSuccess(new java.util.ArrayList<>());
                    }
                });
    }

    /**
     * Subscribes to a single event document and returns its current value.
     *
     * @param eventId  Firestore document ID of the event
     * @param callback callback invoked with the event or an error message
     */
    public void getEventById(@NonNull String eventId, @NonNull SingleEventCallback callback) {
        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching event: ", error);
                        callback.onError(error.getMessage());
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Event event = snapshot.toObject(Event.class);
                        if (event != null) {
                            event.setId(snapshot.getId());
                            callback.onSuccess(event);
                        }
                    } else {
                        callback.onError("Event not found");
                    }
                });
    }

    /**
     * Callback interface for event list queries.
     */
    public interface EventsCallback {
        /**
         * Called when events are fetched successfully.
         *
         * @param events list of events returned from Firestore
         */
        void onSuccess(java.util.List<Event> events);

        /**
         * Called when an error occurs.
         *
         * @param errorMessage human-readable error message
         */
        void onError(String errorMessage);
    }

    /**
     * Callback interface for single event queries.
     */
    public interface SingleEventCallback {
        /**
         * Called when the event is fetched successfully.
         *
         * @param event the loaded event
         */
        void onSuccess(Event event);

        /**
         * Called when an error occurs.
         *
         * @param errorMessage human-readable error message
         */
        void onError(String errorMessage);
    }
}
