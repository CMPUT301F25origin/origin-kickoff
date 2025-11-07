package ca.team.originkickoff.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ca.team.originkickoff.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.UUID;

public class UserRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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
                            // IMPORTANT: set id explicitly from document ID (not stored as field)
                            user.setId(task.getResult().getDocuments().get(0).getId());
                        }
                        userLiveData.postValue(user);
                    } else {
                        // User not found, create a new one
                        createNewUser(deviceId, userLiveData);
                    }
                });
        return userLiveData;
    }

    private void createNewUser(String deviceId, MutableLiveData<User> userLiveData) {
        User newUser = new User();
        String userId = UUID.randomUUID().toString();
        newUser.setId(userId);
        newUser.setDeviceId(deviceId);
        newUser.setDisplayName(""); // Default empty display name
        newUser.setNotifMarketing(false);
        newUser.setNotifService(true);
        newUser.setOrganizer(false);
        newUser.setAdmin(false);
        newUser.setCreatedAt(new Date());
        newUser.setUpdatedAt(new Date());

        db.collection("users").document(userId).set(newUser)
                .addOnSuccessListener(aVoid -> userLiveData.postValue(newUser)) // On success, post the new user object
                .addOnFailureListener(e -> userLiveData.postValue(null)); // On failure, post null
    }
}
