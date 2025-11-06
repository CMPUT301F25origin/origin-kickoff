package ca.team.originkickoff;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ProfileActivity extends AppCompatActivity {

    private SwitchMaterial switchWon;
    private SwitchMaterial switchLost;
    private TextView tvDeviceId;
    private TextView tvUserName;
    private TextView tvUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupTopBar();
        setupToggles();
        setupButtons();
        setupBottomBar();
        setupDeviceId();
        updateProfileHeader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileHeader();
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        View btnEdit = findViewById(R.id.btnEditProfile);
        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, EditProfileActivity.class);
            startActivity(i);
        });
    }

    private void setupToggles() {
        switchWon = findViewById(R.id.switchWon);
        switchLost = findViewById(R.id.switchLost);

        // simple local persistence using SharedPreferences
        boolean won = getSharedPreferences("profile", MODE_PRIVATE).getBoolean("won_updates", true);
        boolean lost = getSharedPreferences("profile", MODE_PRIVATE).getBoolean("lost_updates", true);
        switchWon.setChecked(won);
        switchLost.setChecked(lost);

        switchWon.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences("profile", MODE_PRIVATE).edit().putBoolean("won_updates", isChecked).apply());
        switchLost.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences("profile", MODE_PRIVATE).edit().putBoolean("lost_updates", isChecked).apply());
    }

    private void setupButtons() {
        MaterialButton btnDelete = findViewById(R.id.btnDelete);

        btnDelete.setOnClickListener(v -> showDeleteProfileSheet());
    }

    private void showDeleteProfileSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_delete_profile, null);
        dialog.setContentView(view);

        View btnCancel = view.findViewById(R.id.btnCancel);
        View btnDelete = view.findViewById(R.id.btnDelete);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            getSharedPreferences("profile", MODE_PRIVATE).edit().clear().apply();
            setupToggles();
            updateProfileHeader();
            Toast.makeText(this, getString(R.string.delete_profile), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupBottomBar() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navEvents = findViewById(R.id.navEvents);
        LinearLayout navNotifications = findViewById(R.id.navNotifications);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        });
        navEvents.setOnClickListener(v -> Toast.makeText(this, "My Events coming soon", Toast.LENGTH_SHORT).show());
        navNotifications.setOnClickListener(v -> Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show());
        navProfile.setOnClickListener(v -> {}); // already here
    }

    private void setupDeviceId() {
        tvDeviceId = findViewById(R.id.tvDeviceId);
        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        tvDeviceId.setText(getString(R.string.device_id, id != null ? id : "-"));
    }

    private void updateProfileHeader() {
        if (tvUserName == null) tvUserName = findViewById(R.id.tvUserName);
        if (tvUserEmail == null) tvUserEmail = findViewById(R.id.tvUserEmail);
        String name = getSharedPreferences("profile", MODE_PRIVATE).getString("name", getString(R.string.sample_user_name));
        String email = getSharedPreferences("profile", MODE_PRIVATE).getString("email", getString(R.string.sample_user_email));
        tvUserName.setText(name);
        tvUserEmail.setText(email);
    }
}
