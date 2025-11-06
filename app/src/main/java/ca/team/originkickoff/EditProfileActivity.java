package ca.team.originkickoff;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editProfileRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        // Load existing values
        String name = getSharedPreferences("profile", MODE_PRIVATE).getString("name", getString(R.string.sample_user_name));
        String email = getSharedPreferences("profile", MODE_PRIVATE).getString("email", getString(R.string.sample_user_email));
        String phone = getSharedPreferences("profile", MODE_PRIVATE).getString("phone", "");
        etName.setText(name);
        etEmail.setText(email);
        etPhone.setText(phone);

        View btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());

        MaterialButton btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> onSave());

        // Optional: edit picture button placeholder
        findViewById(R.id.btnEditPicture).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.edit_profile_picture), Toast.LENGTH_SHORT).show());
    }

    private void onSave() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etName.setError("Required");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Required");
            etEmail.requestFocus();
            return;
        }

        getSharedPreferences("profile", MODE_PRIVATE)
                .edit()
                .putString("name", name)
                .putString("email", email)
                .putString("phone", phone)
                .apply();

        Toast.makeText(this, getString(R.string.save_changes), Toast.LENGTH_SHORT).show();
        finish();
    }
}

