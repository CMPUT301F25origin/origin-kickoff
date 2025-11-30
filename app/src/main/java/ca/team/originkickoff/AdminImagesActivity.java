package ca.team.originkickoff;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import android.util.Base64;
import android.util.Log;

import ca.team.originkickoff.adapters.AdminImageAdapter;

public class AdminImagesActivity extends AppCompatActivity implements AdminImageAdapter.Listener {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private RecyclerView rv;
    private AdminImageAdapter adapter;
    private TextInputEditText etSearch;
    private MaterialButton btnEvents;
    private MaterialButton btnUsers;

    private final List<AdminImageAdapter.Item> all = new ArrayList<>();
    private final java.util.Set<String> addedPaths = new java.util.HashSet<>();
    private final java.util.Set<String> addedUrls = new java.util.HashSet<>();
    private boolean showEvents = true;
    private boolean showUsers = true;

    private static final String TAG = "AdminImages";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_images);
        AdminNavHelper.setup(this, AdminNavHelper.Tab.IMAGES);

        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvImages);
        etSearch = findViewById(R.id.etSearch);
        btnEvents = findViewById(R.id.btnFilterEvents);
        btnUsers = findViewById(R.id.btnFilterUsers);
        adapter = new AdminImageAdapter(this);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(adapter);

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (btnEvents != null) btnEvents.setOnClickListener(v -> { showEvents = !showEvents; styleFilters(); applyFilter(); });
        if (btnUsers != null) btnUsers.setOnClickListener(v -> { showUsers = !showUsers; styleFilters(); applyFilter(); });

        styleFilters();
        loadImages();
    }

    /**
     * Updates filter button alpha styling to reflect active/inactive state selections.
     */
    private void styleFilters() {
        if (btnEvents != null) btnEvents.setAlpha(showEvents ? 1f : 0.5f);
        if (btnUsers != null) btnUsers.setAlpha(showUsers ? 1f : 0.5f);
    }

    /**
     * Loads image references from Firestore (events/users) then supplements with raw Storage listings.
     * Clears previous cached lists before aggregation.
     */
    private void loadImages() {
        all.clear();
        addedPaths.clear();
        addedUrls.clear();
        // Firestore-sourced images
        db.collection("events").get().addOnSuccessListener(snap -> {
            for (QueryDocumentSnapshot d : snap) {
                String posterUrl = d.getString("posterUrl");
                String name = d.getString("name");
                String b64 = d.getString("posterBase64");
                if (posterUrl != null && !posterUrl.isEmpty()) {
                    String path = inferStoragePathFromUrl(posterUrl);
                    if (path != null && !path.isEmpty()) {
                        // Validate path exists by asking Storage for a fresh download URL
                        storage.getReference().child(path).getDownloadUrl().addOnSuccessListener(uri -> {
                            AdminImageAdapter.Item it = new AdminImageAdapter.Item(d.getId(), name != null ? name : "Event", uri.toString(), path, "event");
                            addItemUnique(it);
                            adapter.setItems(filterNow());
                        }).addOnFailureListener(e -> Log.d(TAG, "Skip invalid event poster: " + path));
                    }
                } else if (b64 != null && !b64.isEmpty()) {
                    try {
                        byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                        AdminImageAdapter.Item it = new AdminImageAdapter.Item(d.getId(), name != null ? name : "Event", null, null, "event", bytes);
                        addItemUnique(it);
                        adapter.setItems(filterNow());
                    } catch (Exception ignored) {}
                }
            }
            db.collection("users").get().addOnSuccessListener(users -> {
                for (QueryDocumentSnapshot u : users) {
                    String photoUrl = u.getString("photoUrl");
                    String display = u.getString("display_name");
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        String path = inferStoragePathFromUrl(photoUrl);
                        if (path != null && !path.isEmpty()) {
                            storage.getReference().child(path).getDownloadUrl().addOnSuccessListener(uri -> {
                                AdminImageAdapter.Item it = new AdminImageAdapter.Item(u.getId(), display != null ? display : "User", uri.toString(), path, "user");
                                addItemUnique(it);
                                adapter.setItems(filterNow());
                            }).addOnFailureListener(e -> Log.d(TAG, "Skip invalid user photo: " + path));
                        }
                    } else {
                        String conventional = "profile_pictures/" + u.getId() + ".jpg";
                        StorageReference ref = storage.getReference().child(conventional);
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            AdminImageAdapter.Item it = new AdminImageAdapter.Item(u.getId(), display != null ? display : "User", uri.toString(), conventional, "user");
                            addItemUnique(it);
                            adapter.setItems(filterNow());
                        }).addOnFailureListener(e -> { /* ignore missing */ });
                    }
                }
                adapter.setItems(filterNow());
                // After Firestore images, also list from Storage folders to catch any not referenced in Firestore
                loadFromStorage();
            }).addOnFailureListener(e -> { adapter.setItems(filterNow()); loadFromStorage(); });
        }).addOnFailureListener(e -> { adapter.setItems(filterNow()); loadFromStorage(); });
    }

    /**
     * Adds an image item to the master list ensuring uniqueness by storage path or URL; skips invalid items.
     *
     * @param it candidate image adapter item
     */
    private void addItemUnique(AdminImageAdapter.Item it) {
        if (it == null) return;
        boolean hasUrl = it.url != null && !it.url.isEmpty() && (it.url.startsWith("http://") || it.url.startsWith("https://"));
        boolean hasBytes = it.bytes != null && it.bytes.length > 0;
        if (!hasUrl && !hasBytes) {
            Log.d(TAG, "Skip item without image data: id=" + it.id + " kind=" + it.kind);
            return;
        }
        if (it.storagePath != null && !it.storagePath.isEmpty()) {
            String norm = it.storagePath.startsWith("/") ? it.storagePath.substring(1) : it.storagePath;
            if (!addedPaths.add(norm)) {
                Log.d(TAG, "Duplicate by path: " + norm);
                return;
            }
        } else if (hasUrl) {
            if (!addedUrls.add(it.url)) {
                Log.d(TAG, "Duplicate by url: " + it.url);
                return;
            }
        }
        all.add(it);
        Log.d(TAG, "Added image: kind=" + it.kind + " id=" + it.id + (hasUrl ? " url" : " bytes"));
    }

    /**
     * Kicks off listing of common storage folders to find images not referenced in Firestore.
     */
    private void loadFromStorage() {
        // Try common folders: event_posters, profile_pictures, and generic events/*
        StorageReference root = storage.getReference();
        listFlat(root.child("event_posters"), "event");
        listFlat(root.child("profile_pictures"), "user");
        listRecursive(root.child("events"), "event");
    }

    /**
     * Lists all direct child items of a storage directory and appends them as image items.
     *
     * @param dir  storage reference directory
     * @param kind semantic kind (event/user)
     */
    private void listFlat(StorageReference dir, String kind) {
        dir.listAll().addOnSuccessListener((ListResult res) -> {
            for (StorageReference item : res.getItems()) {
                item.getDownloadUrl().addOnSuccessListener(uri -> {
                    String path = stripLeadingSlash(item.getPath());
                    addItemUnique(new AdminImageAdapter.Item("", "", uri.toString(), path, kind));
                    adapter.setItems(filterNow());
                });
            }
        }).addOnFailureListener(e -> {});
    }

    /**
     * Recursively walks a storage directory adding any file items encountered.
     *
     * @param dir  starting storage reference
     * @param kind semantic kind (event/user)
     */
    private void listRecursive(StorageReference dir, String kind) {
        dir.listAll().addOnSuccessListener((ListResult res) -> {
            for (StorageReference item : res.getItems()) {
                item.getDownloadUrl().addOnSuccessListener(uri -> {
                    String path = stripLeadingSlash(item.getPath());
                    addItemUnique(new AdminImageAdapter.Item("", "", uri.toString(), path, kind));
                    adapter.setItems(filterNow());
                });
            }
            for (StorageReference sub : res.getPrefixes()) {
                listRecursive(sub, kind);
            }
        }).addOnFailureListener(e -> {});
    }

    /**
     * Parses a Firebase Storage download URL into its underlying object path.
     *
     * @param url public download URL
     * @return decoded storage path or null if parsing fails
     */
    private String inferStoragePathFromUrl(String url) {
        try {
            // Download URL format contains "/o/<bucketPath>?"
            int idx = url.indexOf("/o/");
            if (idx >= 0) {
                String rest = url.substring(idx + 3); // after /o/
                int q = rest.indexOf('?');
                String encoded = q >= 0 ? rest.substring(0, q) : rest;
                // URL paths are URL-encoded; decode %2F to '/'
                return java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8.name());
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Applies current text + kind filters and refreshes adapter items.
     */
    private void applyFilter() {
        adapter.setItems(filterNow());
    }

    /**
     * Produces a filtered list snapshot based on search query and kind toggles.
     *
     * @return filtered live list of items
     */
    private List<AdminImageAdapter.Item> filterNow() {
        String q = etSearch != null && etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        List<AdminImageAdapter.Item> base = new ArrayList<>();
        for (AdminImageAdapter.Item it : all) {
            if ("event".equals(it.kind) && !showEvents) continue;
            if ("user".equals(it.kind) && !showUsers) continue;
            boolean hasUrl = it.url != null && !it.url.isEmpty() && (it.url.startsWith("http://") || it.url.startsWith("https://"));
            boolean hasBytes = it.bytes != null && it.bytes.length > 0;
            if (!hasUrl && !hasBytes) {
                Log.d(TAG, "Filter skip (no image): id=" + it.id + " kind=" + it.kind);
                continue;
            }
            base.add(it);
        }
        if (q.isEmpty()) return base;
        List<AdminImageAdapter.Item> res = new ArrayList<>();
        for (AdminImageAdapter.Item it : base) {
            String title = it.title != null ? it.title.toLowerCase() : "";
            if (title.contains(q)) res.add(it);
        }
        return res;
    }

    /**
     * Deletes the backing storage object or clears Firestore fields for base64-only posters, then updates UI.
     *
     * @param item selected image item to remove
     */
    @Override
    public void onDelete(AdminImageAdapter.Item item) {
        if (item == null) return;
        String path = item.storagePath;
        if ((path == null || path.isEmpty()) && item.url != null && !item.url.isEmpty()) {
            path = inferStoragePathFromUrl(item.url);
        }
        // If we still don't have a storage path and it's an event base64 image, clear field
        if ((path == null || path.isEmpty()) && "event".equals(item.kind)) {
            // Attempt to clear base64 poster if present
            if (item.id != null && !item.id.isEmpty()) {
                db.collection("events").document(item.id).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String b64 = doc.getString("posterBase64");
                        if (b64 != null && !b64.isEmpty()) {
                            doc.getReference().update("posterBase64", null, "posterUrl", null)
                                    .addOnSuccessListener(unused -> {
                                        removeItemByUrlOrId(item);
                                        Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            return;
                        }
                    }
                    Toast.makeText(this, "Missing storage path", Toast.LENGTH_SHORT).show();
                });
            } else {
                Toast.makeText(this, "Missing storage path", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (path == null || path.isEmpty()) {
            Toast.makeText(this, "Missing storage path", Toast.LENGTH_SHORT).show();
            return;
        }
        final String finalPath = path;
        storage.getReference().child(finalPath).delete()
                .addOnSuccessListener(unused -> {
                    if ("event".equals(item.kind)) {
                        if (item.id != null && !item.id.isEmpty()) {
                            db.collection("events").document(item.id).update("posterUrl", null, "posterBase64", null);
                        }
                    } else if ("user".equals(item.kind)) {
                        if (item.id != null && !item.id.isEmpty()) {
                            db.collection("users").document(item.id).update("photoUrl", null);
                        }
                    }
                    removeItemByUrlOrId(item);
                    Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Removes an item from the master list by matching URL or ID and refreshes adapter.
     *
     * @param item item descriptor to remove
     */
    private void removeItemByUrlOrId(AdminImageAdapter.Item item) {
        // Remove from master list
        for (int i = 0; i < all.size(); i++) {
            AdminImageAdapter.Item it = all.get(i);
            boolean match = false;
            if (item.url != null && it.url != null) match = item.url.equals(it.url);
            if (!match && item.id != null && !item.id.isEmpty() && it.id != null) match = item.id.equals(it.id);
            if (match) { all.remove(i); break; }
        }
        adapter.setItems(filterNow());
    }

    /**
     * Removes a leading '/' from a storage path if present.
     *
     * @param p raw path
     * @return normalized path
     */
    private String stripLeadingSlash(String p) { return p != null && p.startsWith("/") ? p.substring(1) : p; }
}
