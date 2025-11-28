package ca.team.originkickoff.services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.models.User;

/**
 * Firestore-backed helper that streams the contents of the "users" collection for admin tools.
 */
public class FirebaseUserService {
    private static final String TAG = "FirebaseUserService";
    private static final String COLLECTION = "users";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration registration;

    /**
     * Subscribes to live updates for all users.
     */
    public void subscribeToUsers(@NonNull UsersCallback callback) {
        cancelSubscription();
        registration = db.collection(COLLECTION)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading users", error);
                        callback.onError("Failed to load users: " + error.getMessage());
                        return;
                    }
                    if (snapshot == null) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            User user = doc.toObject(User.class);
                            if (user == null) continue;
                            user.setId(doc.getId());
                            users.add(user);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse user " + doc.getId(), e);
                        }
                    }
                    callback.onSuccess(users);
                });
    }

    /**
     * Removes the active Firestore listener if present.
     */
    public void cancelSubscription() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    /**
     * Deletes a user document by id.
     */
    public void deleteUser(@NonNull String userId, @Nullable CompletionCallback callback) {
        db.collection(COLLECTION)
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onComplete(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onComplete(e);
                });
    }

    public interface UsersCallback {
        void onSuccess(List<User> users);
        void onError(String errorMessage);
    }

    public interface CompletionCallback {
        void onComplete(@Nullable Exception error);
    }
}

