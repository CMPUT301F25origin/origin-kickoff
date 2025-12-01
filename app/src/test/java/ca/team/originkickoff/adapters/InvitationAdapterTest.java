package ca.team.originkickoff.adapters;

import com.google.firebase.firestore.FirebaseFirestore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ca.team.originkickoff.models.InvitationStatus;

import static org.junit.Assert.*;

/**
 * JVM/Robolectric-safe tests for InvitationAdapter focusing on dataset size and cache clearing.
 * Avoids constructor (which initializes Firebase) by Unsafe allocation and manual field init.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class InvitationAdapterTest {

    private InvitationAdapter newAdapter(List<InvitationStatus> initial) {
        try (MockedStatic<FirebaseFirestore> firestoreMock = Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(Mockito.mock(FirebaseFirestore.class));
            // Construct using normal constructor so RecyclerView.Adapter internals initialize
            return new InvitationAdapter(initial == null ? new ArrayList<>() : initial);
        }
    }

    @Test
    public void getItemCount_initialEmpty() {
        InvitationAdapter adapter = newAdapter(new ArrayList<>());
        assertNotNull(adapter);
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void updateData_changesSize_andClearsCaches() throws Exception {
        InvitationAdapter adapter = newAdapter(new ArrayList<>());
        assertNotNull(adapter);
        // Populate caches via reflection
        Field nameCacheField = InvitationAdapter.class.getDeclaredField("nameCache");
        Field imageCacheField = InvitationAdapter.class.getDeclaredField("imageCache");
        nameCacheField.setAccessible(true);
        imageCacheField.setAccessible(true);
        @SuppressWarnings("unchecked") HashMap<String,String> nameCache = (HashMap<String,String>) nameCacheField.get(adapter);
        @SuppressWarnings("unchecked") HashMap<String,String> imageCache = (HashMap<String,String>) imageCacheField.get(adapter);
        nameCache.put("uid1", "User One");
        imageCache.put("uid1", "img1");
        assertEquals(1, nameCache.size());
        assertEquals(1, imageCache.size());
        // New invitations list
        List<InvitationStatus> newInvites = new ArrayList<>();
        InvitationStatus s = new InvitationStatus();
        s.setUserId("u2");
        newInvites.add(s);
        // Mock Firestore again for updateData call (constructor only mocked earlier)
        try (MockedStatic<FirebaseFirestore> firestoreMock = Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(Mockito.mock(FirebaseFirestore.class));
            adapter.updateData(newInvites);
        }
        assertEquals(1, adapter.getItemCount());
        assertTrue(nameCache.isEmpty());
        assertTrue(imageCache.isEmpty());
    }
}
