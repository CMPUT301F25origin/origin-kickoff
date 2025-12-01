package ca.team.originkickoff.ui.splash;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;

import ca.team.originkickoff.models.User;

import static org.junit.Assert.*;

/**
 * Unit test for SplashViewModel avoiding Firebase by overriding processLogin.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class SplashViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    @Test
    public void processLogin_populatesUserLiveData() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        // Mock FirebaseFirestore.getInstance to prevent FirebaseApp initialization.
        try (MockedStatic<FirebaseFirestore> firestoreMock = Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(Mockito.mock(FirebaseFirestore.class));
            SplashViewModel vm = new SplashViewModel(app);
            // Inject stub repository that returns deterministic user.
            class StubUserRepository extends ca.team.originkickoff.data.repository.UserRepository {
                private final MutableLiveData<User> live = new MutableLiveData<>();
                @Override
                public androidx.lifecycle.LiveData<User> findUserByDeviceId(String deviceId) {
                    User u = new User();
                    u.setId("test-id");
                    u.setDeviceId(deviceId);
                    u.setDisplayName("TestUser");
                    live.setValue(u);
                    return live;
                }
            }
            java.lang.reflect.Field repoField = SplashViewModel.class.getDeclaredField("userRepository");
            repoField.setAccessible(true);
            repoField.set(vm, new StubUserRepository());
            // Prepare a device id for DeviceUtils
            android.provider.Settings.Secure.putString(app.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID, "device-x");
            assertNull(vm.getUserLiveData()); // before login
            vm.processLogin();
            User value = vm.getUserLiveData().getValue();
            assertNotNull(value);
            assertEquals("test-id", value.getId());
            assertEquals("device-x", value.getDeviceId());
            assertEquals("TestUser", value.getDisplayName());
        }
    }
}
