package ca.team.originkickoff;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEventActivity";

    private TextInputEditText etTitle, etDescription, etLocation, etDate, etTime,
            etPrice, etCapacity, etRegStartDate, etRegStartTime, etRegEndDate, etRegEndTime, etCriteria;
    private ImageView ivPosterPreview;
    private Button btnUploadImage, btnCreateEvent;
    private androidx.appcompat.widget.SwitchCompat switchGenerateQr;
    private ProgressBar progressBar;
    private View formContainer;

    private Uri selectedImageUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    // Hold chosen date/time in milliseconds
    private long eventDateMillis = -1;
    private long eventTimeMillis = -1;

    private long regStartMillis = -1;
    private long regEndMillis = -1;

    // Activity Result launcher for image picking
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Event");
        }

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();

        // Initialize ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedImageUri = uri;
                            ivPosterPreview.setImageURI(selectedImageUri);
                            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        attachListeners();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etPrice = findViewById(R.id.etPrice);
        etCapacity = findViewById(R.id.etCapacity);
        etRegStartDate = findViewById(R.id.etRegStartDate);
        etRegStartTime = findViewById(R.id.etRegStartTime);
        etRegEndDate = findViewById(R.id.etRegEndDate);
        etRegEndTime = findViewById(R.id.etRegEndTime);
        etCriteria = findViewById(R.id.etCriteria);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        switchGenerateQr = findViewById(R.id.switchGenerateQr);

        // Add progress bar and form container if they exist in layout
        progressBar = findViewById(R.id.progressBar);
        formContainer = findViewById(R.id.formContainer);
    }

    private void attachListeners() {
        etDate.setOnClickListener(v -> showDatePicker((millis) -> {
            eventDateMillis = millis;
            etDate.setText(android.text.format.DateFormat.getDateFormat(this).format(millis));
        }));

        etTime.setOnClickListener(v -> showTimePicker((hourOfDay, minute) -> {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            c.set(Calendar.SECOND, 0);
            eventTimeMillis = c.getTimeInMillis();
            etTime.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
        }));

        etRegStartDate.setOnClickListener(v -> showDatePicker((millis) -> {
            regStartMillis = millis;
            etRegStartDate.setText(android.text.format.DateFormat.getDateFormat(this).format(millis));
        }));

        etRegStartTime.setOnClickListener(v -> showTimePicker((hour, minute) -> {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minute);
            c.set(Calendar.SECOND, 0);
            if (regStartMillis > 0) {
                Calendar d = Calendar.getInstance();
                d.setTimeInMillis(regStartMillis);
                d.set(Calendar.HOUR_OF_DAY, hour);
                d.set(Calendar.MINUTE, minute);
                d.set(Calendar.SECOND, 0);
                regStartMillis = d.getTimeInMillis();
            } else {
                regStartMillis = c.getTimeInMillis();
            }
            etRegStartTime.setText(String.format(Locale.US, "%02d:%02d", hour, minute));
        }));

        etRegEndDate.setOnClickListener(v -> showDatePicker((millis) -> {
            regEndMillis = millis;
            etRegEndDate.setText(android.text.format.DateFormat.getDateFormat(this).format(millis));
        }));

        etRegEndTime.setOnClickListener(v -> showTimePicker((hour, minute) -> {
            if (regEndMillis > 0) {
                Calendar d = Calendar.getInstance();
                d.setTimeInMillis(regEndMillis);
                d.set(Calendar.HOUR_OF_DAY, hour);
                d.set(Calendar.MINUTE, minute);
                d.set(Calendar.SECOND, 0);
                regEndMillis = d.getTimeInMillis();
            } else {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, hour);
                c.set(Calendar.MINUTE, minute);
                c.set(Calendar.SECOND, 0);
                regEndMillis = c.getTimeInMillis();
            }
            etRegEndTime.setText(String.format(Locale.US, "%02d:%02d", hour, minute));
        }));

        btnUploadImage.setOnClickListener(v -> pickImage());

        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private interface DateChosenCallback {
        void onDateChosen(long millis);
    }

    private interface TimeChosenCallback {
        void onTimeChosen(int hourOfDay, int minute);
    }

    private void showDatePicker(DateChosenCallback cb) {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth, 0, 0, 0);
            cb.onDateChosen(chosen.getTimeInMillis());
        }, y, m, d);
        dlg.show();
    }

    private void showTimePicker(TimeChosenCallback cb) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        TimePickerDialog dlg = new TimePickerDialog(this, (view, hourOfDay, minute1) -> cb.onTimeChosen(hourOfDay, minute1), hour, minute, true);
        dlg.show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Event Image"));
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnCreateEvent.setEnabled(!loading);
        btnCreateEvent.setText(loading ? "Creating..." : "Create Event");
    }

    private void createEvent() {
        String title = getText(etTitle);
        String description = getText(etDescription);
        String location = getText(etLocation);
        String priceStr = getText(etPrice);
        String capacityStr = getText(etCapacity);
        String criteria = getText(etCriteria);
        boolean genQr = switchGenerateQr.isChecked();

        // Validation
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Event name is required");
            etTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(location)) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return;
        }

        if (eventDateMillis <= 0) {
            etDate.setError("Please choose event date");
            Toast.makeText(this, "Please choose event date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventTimeMillis <= 0) {
            etTime.setError("Please choose event time");
            Toast.makeText(this, "Please choose event time", Toast.LENGTH_SHORT).show();
            return;
        }

        long eventTimestampMillis = mergeDateAndTime(eventDateMillis, eventTimeMillis);

        // Validate event is in the future
        if (eventTimestampMillis < System.currentTimeMillis()) {
            Toast.makeText(this, "Event date and time must be in the future", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = 0;
        try {
            if (!TextUtils.isEmpty(priceStr)) {
                price = Double.parseDouble(priceStr);
                if (price < 0) {
                    etPrice.setError("Price cannot be negative");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            etPrice.setError("Invalid price format");
            return;
        }

        int capacity = 0;
        try {
            if (!TextUtils.isEmpty(capacityStr)) {
                capacity = Integer.parseInt(capacityStr);
                if (capacity <= 0) {
                    etCapacity.setError("Capacity must be greater than 0");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            etCapacity.setError("Invalid capacity");
            return;
        }

        // Show loading state
        setLoading(true);

        // Assemble event map matching Firestore schema exactly
        Map<String, Object> event = new HashMap<>();
        FirebaseUser current = auth.getCurrentUser();
        String organizerId;
        String organizerName;

        if (current != null) {
            organizerId = current.getUid();
            organizerName = current.getDisplayName() != null ? current.getDisplayName() : "Anonymous User";
        } else {
            // Create event with a default organizer for testing
            Log.w(TAG, "No user signed in, using default organizer");
            organizerId = "anonymous";
            organizerName = "Anonymous User";
        }

        // Match exact field names from Firestore sample
        event.put("name", title);
        event.put("description", description);
        event.put("organizerId", organizerId);
        event.put("organizerName", organizerName);
        event.put("location", location);
        event.put("capacity", capacity);
        event.put("price", price);
        event.put("waitlistCount", 0);
        event.put("geolocationRequired", false); // Default to false, can add UI control later
        event.put("status", "draft");
        event.put("createdAt", System.currentTimeMillis());
        event.put("eventDate", new Timestamp(new java.util.Date(eventTimestampMillis)));

        // Add registration period timestamps
        if (regStartMillis > 0) {
            event.put("registrationStartTime", new Timestamp(new java.util.Date(regStartMillis)));
        } else {
            // Default to now if not specified
            event.put("registrationStartTime", Timestamp.now());
        }

        if (regEndMillis > 0) {
            event.put("registrationEndTime", new Timestamp(new java.util.Date(regEndMillis)));
        } else {
            // Default to event date if not specified
            event.put("registrationEndTime", new Timestamp(new java.util.Date(eventTimestampMillis)));
        }

        // If an image is selected, upload first, then save event with poster url
        if (selectedImageUri != null) {
            uploadImageAndSaveEvent(selectedImageUri, event);
        } else {
            // Use empty string or placeholder URL if no image selected
            event.put("posterUrl", "");
            saveEventToFirestore(event);
        }
    }

    private void uploadImageAndSaveEvent(Uri uri, Map<String, Object> event) {
        String id = UUID.randomUUID().toString();
        StorageReference ref = storage.getReference().child("event_posters/" + id);

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully");
                    ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        event.put("posterUrl", downloadUri.toString());
                        saveEventToFirestore(event);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL", e);
                        setLoading(false);
                        Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed", e);
                    setLoading(false);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveEventToFirestore(Map<String, Object> event) {
        db.collection("events").add(event)
                .addOnSuccessListener(documentReference -> {
                    setLoading(false);
                    Toast.makeText(this, "Event created successfully!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Event saved with ID: " + documentReference.getId());

                    // Return to previous screen
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Failed to save event", e);
                    Toast.makeText(this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private long mergeDateAndTime(long dateMillis, long timeOfDayMillis) {
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTimeInMillis(dateMillis);
        Calendar timeCal = Calendar.getInstance();
        timeCal.setTimeInMillis(timeOfDayMillis);
        dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
        dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
        dateCal.set(Calendar.SECOND, 0);
        dateCal.set(Calendar.MILLISECOND, 0);
        return dateCal.getTimeInMillis();
    }

    private String getText(TextInputEditText et) {
        if (et == null) return "";
        CharSequence cs = et.getText();
        return cs == null ? "" : cs.toString().trim();
    }
}
