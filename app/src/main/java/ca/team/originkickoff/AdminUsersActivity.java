package ca.team.originkickoff;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.adapters.UsersAdapter;
import ca.team.originkickoff.models.User;

public class AdminUsersActivity extends AppCompatActivity implements UsersAdapter.OnUserClickListener {
    private RecyclerView recyclerUsers;
    private UsersAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<User> allUsers = new ArrayList<>();
    private TextInputEditText inputSearch;
    private MaterialButton btnRole;
    private MaterialButton btnStatus; // Stub for later
    private RoleFilter roleFilter = RoleFilter.ALL;

    private enum RoleFilter { ALL, ORGANIZER, ADMIN, ENTRANT }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);
        AdminNavHelper.setup(this, AdminNavHelper.Tab.USERS);

        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

        recyclerUsers = findViewById(R.id.recyclerUsers);
        inputSearch = findViewById(R.id.inputSearch);
        btnRole = findViewById(R.id.btnRole);
        btnStatus = findViewById(R.id.btnStatus);

        adapter = new UsersAdapter(this);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(adapter);

        setupSearch();
        setupRoleFilter();

        loadAllUsers();
    }

    private void setupSearch() {
        if (inputSearch == null) return;
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRoleFilter() {
        if (btnRole == null) return;
        btnRole.setOnClickListener(v -> cycleRoleFilter());
        updateRoleButtonLabel();
    }

    private void cycleRoleFilter() {
        switch (roleFilter) {
            case ALL: roleFilter = RoleFilter.ORGANIZER; break;
            case ORGANIZER: roleFilter = RoleFilter.ADMIN; break;
            case ADMIN: roleFilter = RoleFilter.ENTRANT; break;
            case ENTRANT: roleFilter = RoleFilter.ALL; break;
        }
        updateRoleButtonLabel();
        applyFilters();
    }

    private void updateRoleButtonLabel() {
        if (btnRole == null) return;
        String label;
        switch (roleFilter) {
            case ORGANIZER: label = "Organizer"; break;
            case ADMIN: label = "Admin"; break;
            case ENTRANT: label = "Entrant"; break;
            default: label = "All Roles"; break;
        }
        btnRole.setText(label);
    }

    private void loadAllUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(snapshots -> {
                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.setId(doc.getId());
                            allUsers.add(u);
                        }
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_to_load_users), Toast.LENGTH_SHORT).show());
    }

    private void applyFilters() {
        String query = (inputSearch != null && inputSearch.getText() != null) ? inputSearch.getText().toString().trim().toLowerCase() : "";
        List<User> filtered = new ArrayList<>();
        for (User u : allUsers) {
            if (u == null) continue;
            if (!query.isEmpty()) {
                String name = u.getDisplayName() != null ? u.getDisplayName().toLowerCase() : "";
                String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                if (!name.contains(query) && !email.contains(query)) {
                    continue;
                }
            }
            boolean include;
            switch (roleFilter) {
                case ORGANIZER: include = u.isOrganizer(); break;
                case ADMIN: include = u.isAdmin(); break;
                case ENTRANT: include = !u.isAdmin() && !u.isOrganizer(); break;
                default: include = true; break;
            }
            if (include) filtered.add(u);
        }
        adapter.setUsers(filtered);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload users to reflect any deletions/changes performed in detail screens
        loadAllUsers();
    }

    @Override
    public void onUserClick(User user) {
        // Open profile screen
        if (user == null || user.getId() == null) {
            Toast.makeText(this, getString(R.string.failed_to_load_users), Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.Intent intent = new android.content.Intent(this, AdminUserProfileActivity.class);
        intent.putExtra(AdminUserProfileActivity.EXTRA_USER_ID, user.getId());
        startActivity(intent);
    }
}
