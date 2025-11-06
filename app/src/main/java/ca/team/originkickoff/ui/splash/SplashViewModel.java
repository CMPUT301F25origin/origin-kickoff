package ca.team.originkickoff.ui.splash;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import ca.team.originkickoff.data.repository.UserRepository;
import ca.team.originkickoff.models.User;
import ca.team.originkickoff.util.DeviceUtils;

public class SplashViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private LiveData<User> userLiveData;

    public SplashViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository();
    }

    public void processLogin() {
        String deviceId = DeviceUtils.getDeviceId(getApplication());
        userLiveData = userRepository.findUserByDeviceId(deviceId);
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }
}
