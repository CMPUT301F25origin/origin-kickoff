package ca.team.originkickoff;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ca.team.originkickoff.models.EventLocation;
import ca.team.originkickoff.models.User;
import ca.team.originkickoff.util.DeviceUtils;
import ca.team.originkickoff.utils.QRCodeGenerator;

public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEventActivity";
    private static final int LOCATION_REQUEST_CODE = 101;

    private EditText etEventName, etDescription, etLocation, etDate, etTime,
            etRegStartDate, etRegStartTime, etRegEndDate, etRegEndTime;
    // Optional fields that may not be in the layout
    private EditText etCategory, etPrice, etCapacity, etCriteria;
    private ImageView ivPosterPreview, btnClose;
    private LinearLayout layoutUploadImage;
    private Button btnCreateEvent;
    private androidx.appcompat.widget.SwitchCompat switchGenerateQr;
    private ProgressBar progressBar;
    private View formContainer;

    private Uri selectedImageUri;
    private FirebaseFirestore db;

    // Location data
    private EventLocation selectedLocation;

    // Hold chosen date/time in milliseconds
    private long eventDateMillis = -1;
    private long eventTimeMillis = -1;

    private long regStartMillis = -1;
    private long regEndMillis = -1;

    // Activity Result launcher for image picking
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // Current user from Firestore
    private User currentUser;

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

        // Get current user from Firestore
        getCurrentUserFromFirestore();

        bindViews();

        // Initialize ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedImageUri = uri;
                            if (ivPosterPreview != null) {
                                ivPosterPreview.setImageURI(selectedImageUri);
                            }
                            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        attachListeners();
    }

    private void getCurrentUserFromFirestore() {
        // Get device ID and query Firestore for the user
        String deviceId = DeviceUtils.getDeviceId(this);
        if (deviceId != null) {
            db.collection("users")
                    .whereEqualTo("device_id", deviceId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            currentUser = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                            if (currentUser != null) {
                                Log.d(TAG, "Current user loaded: " + currentUser.getId());
                            }
                        } else {
                            Log.w(TAG, "No user found with device_id: " + deviceId);
                            currentUser = null;
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user", e);
                        currentUser = null;
                    });
        } else {
            Log.w(TAG, "Device ID is null, cannot fetch user");
            currentUser = null;
        }
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
        etEventName = findViewById(R.id.etEventName);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etRegStartDate = findViewById(R.id.etRegStartDate);
        etRegStartTime = findViewById(R.id.etRegStartTime);
        etRegEndDate = findViewById(R.id.etRegEndDate);
        etRegEndTime = findViewById(R.id.etRegEndTime);
        etCapacity = findViewById(R.id.etCapacity);
        etCriteria = findViewById(R.id.etCriteria);
        btnClose = findViewById(R.id.btnClose);
        layoutUploadImage = findViewById(R.id.layoutUploadImage);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);

        // Optional views that don't exist in the new layout - leaving them null
        // ivPosterPreview = findViewById(R.id.ivPosterPreview);
        // switchGenerateQr = findViewById(R.id.switchGenerateQr);
        // progressBar = findViewById(R.id.progressBar);
    }

    private void attachListeners() {
        // Make location field clickable but not editable via keyboard
        etLocation.setFocusable(false);
        etLocation.setFocusableInTouchMode(false);
        etLocation.setClickable(true);
        etLocation.setOnClickListener(v -> openLocationSearch());

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

        btnClose.setOnClickListener(v -> finish());

        layoutUploadImage.setOnClickListener(v -> pickImage());

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
        String title = getText(etEventName);
        String description = getText(etDescription);
        String location = getText(etLocation);
        String capacityStr = getText(etCapacity);
        String criteria = getText(etCriteria);

        // Validation - ALL FIELDS ARE MANDATORY
        if (TextUtils.isEmpty(title)) {
            etEventName.setError("Event name is required");
            etEventName.requestFocus();
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

        if (regStartMillis <= 0) {
            etRegStartDate.setError("Please choose registration start date");
            Toast.makeText(this, "Please choose registration start date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (regEndMillis <= 0) {
            etRegEndDate.setError("Please choose registration end date");
            Toast.makeText(this, "Please choose registration end date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(capacityStr)) {
            etCapacity.setError("Capacity is required");
            etCapacity.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(criteria)) {
            etCriteria.setError("Lottery criteria is required");
            etCriteria.requestFocus();
            return;
        }

        // Image is now optional - removed validation

        long eventTimestampMillis = mergeDateAndTime(eventDateMillis, eventTimeMillis);

        // Validate event is in the future
        if (eventTimestampMillis < System.currentTimeMillis()) {
            Toast.makeText(this, "Event date and time must be in the future", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate registration period is before event date
        if (regEndMillis > eventTimestampMillis) {
            Toast.makeText(this, "Registration must end before event date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate registration start is before end
        if (regStartMillis >= regEndMillis) {
            Toast.makeText(this, "Registration start must be before registration end", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity = 0;
        try {
            capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) {
                etCapacity.setError("Capacity must be greater than 0");
                etCapacity.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etCapacity.setError("Invalid capacity");
            etCapacity.requestFocus();
            return;
        }

        // Show loading state
        setLoading(true);

        // Get organizer information from currentUser
        String organizerId;
        String organizerName;

        if (currentUser != null) {
            organizerId = currentUser.getId();
            organizerName = currentUser.getDisplayName();

            // Use a fallback if display name is empty
            if (TextUtils.isEmpty(organizerName)) {
                organizerName = "User";
            }

            Log.d(TAG, "Creating event with organizer ID: " + organizerId);
        } else {
            // Fallback: if user not loaded yet, get device ID directly
            String deviceId = DeviceUtils.getDeviceId(this);
            Log.w(TAG, "Current user not loaded, using device ID as fallback: " + deviceId);
            organizerId = deviceId != null ? deviceId : "unknown";
            organizerName = "User";
        }

        // Assemble event map matching Firestore schema exactly
        Map<String, Object> event = new HashMap<>();

        // Match exact field names from Firestore sample
        event.put("name", title);
        event.put("description", description);
        event.put("organizerId", organizerId);
        event.put("organizerName", organizerName);
        event.put("location", location);

        // Add location coordinates if available
        if (selectedLocation != null) {
            event.put("locationLatitude", selectedLocation.getLatitude());
            event.put("locationLongitude", selectedLocation.getLongitude());
            if (selectedLocation.getPlaceId() != null) {
                event.put("locationPlaceId", selectedLocation.getPlaceId());
            }
        }

        event.put("category", "General");
        event.put("capacity", capacity);
        event.put("lotteryCriteria", criteria);
        event.put("price", 0);
        event.put("waitlistCount", 0);
        event.put("geolocationRequired", false);
        event.put("status", "draft");
        event.put("createdAt", System.currentTimeMillis());
        event.put("eventDate", new Timestamp(new java.util.Date(eventTimestampMillis)));
        event.put("registrationStartTime", new Timestamp(new java.util.Date(regStartMillis)));
        event.put("registrationEndTime", new Timestamp(new java.util.Date(regEndMillis)));

        // Check if image was selected
        if (selectedImageUri != null) {
            // Convert image to Base64 and save event
            uploadImageAndSaveEvent(selectedImageUri, event);
        } else {
            // No image selected, save event directly without poster
            saveEventToFirestore(event);
        }
    }

    private void uploadImageAndSaveEvent(Uri uri, Map<String, Object> event) {
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

        // Convert image to base64 and attach to event
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize bitmap if too large to avoid base64 string being too big
            int maxWidth = 800;
            int maxHeight = 800;
            if (bitmap != null && (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight)) {
                float scale = Math.min(
                        (float) maxWidth / bitmap.getWidth(),
                        (float) maxHeight / bitmap.getHeight()
                );
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            if (bitmap != null) {
                // Convert to base64 (no wrap to save bytes)
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();
                String posterBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                // Save base64 to event
                event.put("posterBase64", posterBase64);
            } else {
                Log.w(TAG, "Bitmap decoding returned null; skipping posterBase64");
            }

            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to base64", e);
        }

        // Directly save event to Firestore without using Firebase Storage
        saveEventToFirestore(event);
    }

    private void saveEventToFirestore(Map<String, Object> event) {
        db.collection("events").add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();
                    Log.d(TAG, "Event saved with ID: " + eventId);

                    generateAndSaveQRCodeAsBase64(eventId, documentReference);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Failed to save event", e);
                    Toast.makeText(this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Generates a QR code for the event ID, Base64 encodes it, and saves it to the event document.
     */
    private void generateAndSaveQRCodeAsBase64(String eventId, com.google.firebase.firestore.DocumentReference eventRef) {
        Log.d(TAG, "Generating QR code for event: " + eventId);

        Bitmap qrCodeBitmap = QRCodeGenerator.generateQRCode(eventId);

        if (qrCodeBitmap == null) {
            Log.e(TAG, "Failed to generate QR code bitmap");
            Toast.makeText(this, "Event created but QR code generation failed.", Toast.LENGTH_LONG).show();
            navigateToEventDetail(eventId); // Navigate anyway
            return;
        }

        byte[] qrCodeBytes = QRCodeGenerator.bitmapToByteArray(qrCodeBitmap);

        if (qrCodeBytes == null) {
            Log.e(TAG, "Failed to convert QR code bitmap to byte array");
            Toast.makeText(this, "Event created but QR code processing failed.", Toast.LENGTH_LONG).show();
            navigateToEventDetail(eventId); // Navigate anyway
            return;
        }

        // Base64 encode the byte array to a string
        String qrCodeBase64 = Base64.encodeToString(qrCodeBytes, Base64.NO_WRAP);

        // Update the event document with the Base64 string
        eventRef.update("qrCodeBase64", qrCodeBase64)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Event created with QR code!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Event updated with Base64 QR code.");
                    navigateToEventDetail(eventId);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Failed to update event with Base64 QR code", e);
                    Toast.makeText(this, "Event created but failed to save QR code.", Toast.LENGTH_LONG).show();
                    navigateToEventDetail(eventId); // Navigate anyway, event exists
                });
    }


    private void navigateToEventDetail(String eventId) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
        finish();
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

    private String getText(EditText et) {
        if (et == null) return "";
        CharSequence cs = et.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private void openLocationSearch() {
        Intent intent = new Intent(this, LocationSearchActivity.class);
        startActivityForResult(intent, LOCATION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String address = data.getStringExtra("address");
            double latitude = data.getDoubleExtra("latitude", 0.0);
            double longitude = data.getDoubleExtra("longitude", 0.0);
            String placeId = data.getStringExtra("placeId");

            if (address != null && !address.isEmpty()) {
                selectedLocation = new EventLocation(address, latitude, longitude, placeId);
                etLocation.setText(address);
                etLocation.setError(null); // Clear any previous error
                Log.d(TAG, "Location selected: " + address + " (lat: " + latitude + ", lng: " + longitude + ")");
            } else {
                Toast.makeText(this, "Invalid location data received", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Received empty or null address from LocationSearchActivity");
            }
        } else if (requestCode == LOCATION_REQUEST_CODE && resultCode != RESULT_OK) {
            Log.d(TAG, "Location selection cancelled or failed");
        }
    }
}
