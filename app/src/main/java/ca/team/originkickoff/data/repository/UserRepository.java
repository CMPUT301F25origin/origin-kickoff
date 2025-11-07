/**
 * Repository providing user lookup and creation operations backed by Firestore.
 * Ensures a user exists for a given device identifier (creating one if absent).
 */
package ca.team.originkickoff.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ca.team.originkickoff.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

/**
 * Data access layer for User entities stored in the Firestore 'users' collection.
 */
public class UserRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Retrieves a user by device id, creating a new persisted user document if none exists.
     * @param deviceId the device identifier used as a unique key
     * @return LiveData emitting the found or newly created User (or null on creation failure)
     */
    public LiveData<User> findUserByDeviceId(String deviceId) {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        db.collection("users")
                .whereEqualTo("device_id", deviceId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        User user = task.getResult().getDocuments().get(0).toObject(User.class);
                        if (user != null) {
                            user.setId(task.getResult().getDocuments().get(0).getId());
                        }
                        userLiveData.postValue(user);
                    } else {
                        createNewUser(deviceId, userLiveData);
                    }
                });
        return userLiveData;
    }

    /**
     * Creates and persists a new user document initialized with default settings.
     * @param deviceId device identifier used as both document id and internal id
     * @param userLiveData target LiveData to publish result (user or null on failure)
     */
    private void createNewUser(String deviceId, MutableLiveData<User> userLiveData) {
        User newUser = new User();
        String userId = deviceId;
        newUser.setId(userId);
        newUser.setDeviceId(deviceId);
        newUser.setDisplayName("");
        newUser.setNotifMarketing(false);
        newUser.setNotifService(true);
        newUser.setOrganizer(false);
        newUser.setAdmin(false);
        newUser.setCreatedAt(new Date());
        newUser.setUpdatedAt(new Date());

        db.collection("users").document(userId).set(newUser)
                .addOnSuccessListener(aVoid -> userLiveData.postValue(newUser))
                .addOnFailureListener(e -> userLiveData.postValue(null));
    }
}
