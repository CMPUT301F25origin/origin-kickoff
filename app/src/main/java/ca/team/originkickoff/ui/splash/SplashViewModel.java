/*
 * ViewModel for the splash flow handling device-based user lookup.
 * Exposes LiveData<User> after invoking processLogin.
 */
package ca.team.originkickoff.ui.splash;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import ca.team.originkickoff.data.repository.UserRepository;
import ca.team.originkickoff.models.User;
import ca.team.originkickoff.util.DeviceUtils;

/**
 * AndroidViewModel that resolves the current user from the device ID and exposes it as LiveData.
 */
public class SplashViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private LiveData<User> userLiveData;

    /**
     * Constructs the ViewModel with application context for device utilities.
     *
     * @param application application instance
     */
    public SplashViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository();
    }

    /**
     * Initiates user lookup based on device identifier, populating userLiveData.
     */
    public void processLogin() {
        String deviceId = DeviceUtils.getDeviceId(getApplication());
        userLiveData = userRepository.findUserByDeviceId(deviceId);
    }

    /**
     * Returns LiveData containing the resolved user (may be null until lookup completes).
     *
     * @return live data stream for user object
     */
    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }
}
