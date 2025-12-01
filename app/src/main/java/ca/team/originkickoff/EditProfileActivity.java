/**
 * Profile editing screen for updating user display info and avatar.
 * Supports image upload (Base64) with size validation and Firestore persistence.
 */
package ca.team.originkickoff;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Activity for editing a user's profile details and image.
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private EditText etName, etEmail, etPhone;
    private ImageView ivProfile;
    private FirebaseFirestore db;
    private String deviceId;
    private String userDocId;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadImage(imageUri);
                    }
                }
            });

    /**
     * Initializes the profile editor, loads current user data, and sets up UI listeners.
     *
     * @param savedInstanceState previously saved state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editProfileRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        ivProfile = findViewById(R.id.ivProfile);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        if (TextUtils.isEmpty(deviceId)) {
            Toast.makeText(this, "Cannot get device ID. Profile cannot be loaded or saved.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadUserData();

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> onSave());
        findViewById(R.id.btnEditPicture).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }

    /**
     * Loads the Firestore user document for this device and populates the profile form.
     */
    private void loadUserData() {
        db.collection("users").whereEqualTo("device_id", deviceId).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        userDocId = documentSnapshot.getId();

                        etName.setText(documentSnapshot.getString("display_name"));
                        etEmail.setText(documentSnapshot.getString("email"));
                        etPhone.setText(documentSnapshot.getString("phone"));

                        String imageId = documentSnapshot.getString("profile_image_id");
                        if (imageId != null && !imageId.isEmpty()) {
                            loadAndSetProfileImage(imageId);
                        } else {
                            showPlaceholderImage();
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fetches the stored Base64 avatar image from the images collection and displays it.
     *
     * @param imageId Firestore document ID of the stored profile image
     */
    private void loadAndSetProfileImage(String imageId) {
        db.collection("images").document(imageId).get().addOnSuccessListener(imageDoc -> {
            if (imageDoc.exists()) {
                String base64Image = imageDoc.getString("storage_path");
                if (base64Image != null && !base64Image.isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        ivProfile.setImageTintList(null);
                        Glide.with(this)
                                .load(decodedString)
                                .apply(RequestOptions.circleCropTransform())
                                .into(ivProfile);
                    } catch (Exception e) {
                        showPlaceholderImage();
                    }
                } else {
                    showPlaceholderImage();
                }
            } else {
                showPlaceholderImage();
            }
        }).addOnFailureListener(e -> showPlaceholderImage());
    }

    /**
     * Displays a default placeholder avatar when no profile image is available.
     */
    private void showPlaceholderImage() {
        ivProfile.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ko_teal)));
        Glide.with(this).load(R.drawable.ic_person).apply(RequestOptions.circleCropTransform()).into(ivProfile);
    }

    /**
     * Validates user input and merges updated profile fields into Firestore.
     */
    private void onSave() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            if (TextUtils.isEmpty(name)) etName.setError("Required");
            if (TextUtils.isEmpty(email)) etEmail.setError("Required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            return;
        }

        if (userDocId == null) {
            Toast.makeText(this, "Error: Could not find your profile to update.", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("display_name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("updated_at", FieldValue.serverTimestamp());

        db.collection("users").document(userDocId).set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(EditProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Processes a picked image URI, ensures it's within size limits, stores it in Firestore,
     * and links the image document to the current user.
     *
     * @param imageUri content URI of the selected image from gallery
     */
    private void uploadImage(Uri imageUri) {
        if (userDocId == null) {
            Toast.makeText(this, "Cannot upload image, user profile not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = uriToBitmap(imageUri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            if (byteArray.length > 1048576) {
                Toast.makeText(this, "Image is too large. Please select an image under 1MB.", Toast.LENGTH_LONG).show();
                return;
            }

            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);
            String imageId = UUID.randomUUID().toString();

            Map<String, Object> image = new HashMap<>();
            image.put("id", imageId);
            image.put("owner_user_id", userDocId);
            image.put("event_id", null);
            image.put("storage_path", base64Image);
            image.put("mime_type", getContentResolver().getType(imageUri));
            image.put("width_px", bitmap.getWidth());
            image.put("height_px", bitmap.getHeight());
            image.put("moderation_status", "pending");
            image.put("created_at", FieldValue.serverTimestamp());

            db.collection("images").document(imageId).set(image)
                    .addOnSuccessListener(aVoid -> db.collection("users").document(userDocId)
                            .update("profile_image_id", imageId)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(EditProfileActivity.this, "Profile picture updated.", Toast.LENGTH_SHORT).show();
                                loadAndSetProfileImage(imageId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to link image to profile.", e);
                                Toast.makeText(EditProfileActivity.this, "Error linking profile image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error uploading image to Firestore", e);
                        Toast.makeText(EditProfileActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (IOException e) {
            Log.e(TAG, "Error processing image for upload", e);
            Toast.makeText(this, "Failed to read or process image.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Decodes the given image URI into a Bitmap, using newer or legacy APIs depending on OS version.
     *
     * @param uri image content URI to decode
     * @return decoded bitmap for the given URI
     * @throws IOException if reading or decoding the image fails
     */
    private Bitmap uriToBitmap(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
        } else {
            return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        }
    }
}
