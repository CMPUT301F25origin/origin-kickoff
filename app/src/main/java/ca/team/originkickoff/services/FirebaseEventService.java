package ca.team.originkickoff.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import ca.team.originkickoff.models.Event;

public class FirebaseEventService {
    private static final String TAG = "FirebaseEventService";
    private static final String EVENTS_COLLECTION = "events";
    private FirebaseFirestore db;

    public FirebaseEventService() {
        this.db = FirebaseFirestore.getInstance();
    }

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
                        long currentTime = System.currentTimeMillis();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                Event event = doc.toObject(Event.class);
                                Log.d(TAG, "Processing document: " + doc.getId());

                                if (event != null) {
                                    event.setId(doc.getId());
                                    Log.d(TAG, "Event loaded: " + event.getName() + ", Status: " + (event.getRegistrationStartTime() != null ? "has dates" : "no dates"));

                                    if (event.getRegistrationStartTime() != null &&
                                        event.getRegistrationEndTime() != null) {
                                        long startTime = event.getRegistrationStartTime().getTime();
                                        long endTime = event.getRegistrationEndTime().getTime();

                                        Log.d(TAG, "Current time: " + currentTime + ", Start: " + startTime + ", End: " + endTime);

                                        if (currentTime >= startTime && currentTime <= endTime) {
                                            events.add(event);
                                            Log.d(TAG, "✓ Added open event: " + event.getName());
                                        } else {
                                            Log.d(TAG, "✗ Event not in registration window: " + event.getName());
                                        }
                                    } else {
                                        Log.d(TAG, "✗ Event missing registration times: " + event.getName());
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
                        long currentTime = System.currentTimeMillis();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                Event event = doc.toObject(Event.class);
                                if (event != null) {
                                    event.setId(doc.getId());
                                    Log.d(TAG, "Debug Event: " + event.getName() + ", Status: " + (event.getRegistrationStartTime() != null ? "has times" : "NO TIMES"));

                                    if (event.getRegistrationStartTime() != null &&
                                        event.getRegistrationEndTime() != null) {
                                        long startTime = event.getRegistrationStartTime().getTime();
                                        long endTime = event.getRegistrationEndTime().getTime();

                                        if (currentTime >= startTime && currentTime <= endTime) {
                                            events.add(event);
                                            Log.d(TAG, "✓ Debug added: " + event.getName());
                                        } else {
                                            Log.d(TAG, "✗ Debug out of window: " + event.getName());
                                        }
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

    public interface EventsCallback {
        void onSuccess(java.util.List<Event> events);
        void onError(String errorMessage);
    }

    public interface SingleEventCallback {
        void onSuccess(Event event);
        void onError(String errorMessage);
    }
}
