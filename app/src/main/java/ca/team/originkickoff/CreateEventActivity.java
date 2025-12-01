/**
 * Event creation workflow allowing organizers to set metadata, registration windows, poster, and limits.
 * Persists a new event document in Firestore with optional waitlist constraints.
 */
package ca.team.originkickoff;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
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
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
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

/**
 * Activity for creating a new event and saving it to Firestore.
 */
public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEventActivity";
    private static final int LOCATION_REQUEST_CODE = 101;

    private EditText etEventName, etDescription, etLocation, etDate, etTime,
            etRegStartDate, etRegStartTime, etRegEndDate, etRegEndTime;
    private EditText etCategory, etPrice, etCapacity, etCriteria;
    private EditText etSelectionSize, etWaitlistLimit;
    private ImageView ivPosterPreview, btnClose;
    private LinearLayout layoutUploadImage;
    private Button btnCreateEvent;
    private SwitchCompat switchGenerateQr, switchGeoRequired, switchLimitWaitlist;
    private ProgressBar progressBar;
    private View formContainer;

    private Uri selectedImageUri;
    private FirebaseFirestore db;

    private EventLocation selectedLocation;

    private long eventDateMillis = -1;
    private long eventTimeMillis = -1;

    private long regStartMillis = -1;
    private long regEndMillis = -1;

    private ActivityResultLauncher<Intent> pickImageLauncher;

    private User currentUser;

    /**
     * Initializes views, loads current user, and sets listeners.
     *
     * @param savedInstanceState previous instance state if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Event");
        }

        db = FirebaseFirestore.getInstance();

        getCurrentUserFromFirestore();

        bindViews();

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedImageUri = uri;
                            if (ivPosterPreview != null) {
                                ivPosterPreview.setImageURI(selectedImageUri);
                                ivPosterPreview.setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        attachListeners();
    }

    /**
     * Fetches the current user document from Firestore using the device ID.
     * Populates organizer metadata on success.
     */
    private void getCurrentUserFromFirestore() {
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
                                currentUser.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
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

    /**
     * Handles action bar item clicks (e.g. back navigation).
     *
     * @param item menu item selected
     * @return true if handled, otherwise passes to super
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Binds all required views from the layout to local fields.
     */
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
        etSelectionSize = findViewById(R.id.etSelectionSize);
        etWaitlistLimit = findViewById(R.id.etWaitlistLimit);
        switchGeoRequired = findViewById(R.id.switchGeoRequired);
        switchLimitWaitlist = findViewById(R.id.switchLimitWaitlist);
        etCriteria = findViewById(R.id.etCriteria);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnClose = findViewById(R.id.btnClose);
        layoutUploadImage = findViewById(R.id.layoutUploadImage);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
    }

    /**
     * Attaches click, picker, and toggle listeners for form fields.
     */
    private void attachListeners() {
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

        switchLimitWaitlist.setOnCheckedChangeListener((btn, checked) -> {
            if (etWaitlistLimit != null) {
                etWaitlistLimit.setVisibility(checked ? View.VISIBLE : View.GONE);
                if (!checked) {
                    etWaitlistLimit.setText("");
                }
            }
        });
    }

    /**
     * Callback for date picker selection.
     */
    private interface DateChosenCallback {
        /**
         * Called when the user picks a date.
         *
         * @param millis selected date in milliseconds
         */
        void onDateChosen(long millis);
    }

    /**
     * Callback for time picker selection.
     */
    private interface TimeChosenCallback {
        /**
         * Called when the user picks a time.
         *
         * @param hourOfDay hour of day selected
         * @param minute    minute selected
         */
        void onTimeChosen(int hourOfDay, int minute);
    }

    /**
     * Shows a date picker and sends the chosen date to the provided callback.
     *
     * @param cb callback to receive the chosen date in millis
     */
    private void showDatePicker(DateChosenCallback cb) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setTheme(R.style.ThemeOverlay_KickOff_DatePicker)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            java.util.Calendar utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            java.util.Calendar local = java.util.Calendar.getInstance();
            local.set(java.util.Calendar.YEAR, utc.get(java.util.Calendar.YEAR));
            local.set(java.util.Calendar.MONTH, utc.get(java.util.Calendar.MONTH));
            local.set(java.util.Calendar.DAY_OF_MONTH, utc.get(java.util.Calendar.DAY_OF_MONTH));
            local.set(java.util.Calendar.HOUR_OF_DAY, 0);
            local.set(java.util.Calendar.MINUTE, 0);
            local.set(java.util.Calendar.SECOND, 0);
            local.set(java.util.Calendar.MILLISECOND, 0);
            cb.onDateChosen(local.getTimeInMillis());
        });
        picker.show(getSupportFragmentManager(), "create_event_date_picker");
    }

    /**
     * Shows a time picker and sends the chosen time to the provided callback.
     *
     * @param cb callback to receive chosen hour and minute
     */
    private void showTimePicker(TimeChosenCallback cb) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        TimePickerDialog dlg = new TimePickerDialog(this, (view, hourOfDay, minute1) -> cb.onTimeChosen(hourOfDay, minute1), hour, minute, true);
        dlg.show();
        // Ensure dialog action buttons (OK/Cancel) are visible against light backgrounds
        try {
            Button positive = dlg.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negative = dlg.getButton(DialogInterface.BUTTON_NEGATIVE);
            if (positive != null) positive.setTextColor(0xFFFFFFFF);
            if (negative != null) negative.setTextColor(0xFFFFFFFF);
        } catch (Exception ignored) {
        }
    }

    /**
     * Launches an image picker for poster selection.
     */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Event Image"));
    }

    /**
     * Toggles progress visibility and disables/enables the create button.
     *
     * @param loading true to show loading, false to restore normal state
     */
    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnCreateEvent.setEnabled(!loading);
        btnCreateEvent.setText(loading ? "Creating..." : "Create Event");
    }

    /**
     * Validates input fields, assembles the event map, and decides how to save it.
     * Ensures required timings and capacity are correct before persisting.
     */
    private void createEvent() {
        String title = getText(etEventName);
        String description = getText(etDescription);
        String location = getText(etLocation);
        String capacityStr = getText(etCapacity);
        String selectionSizeStr = getText(etSelectionSize);
        String waitlistLimitStr = getText(etWaitlistLimit);
        String criteria = getText(etCriteria);

        boolean geoRequired = switchGeoRequired != null && switchGeoRequired.isChecked();
        boolean limitWaitlist = switchLimitWaitlist != null && switchLimitWaitlist.isChecked();

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

        if (android.text.TextUtils.isEmpty(capacityStr)) {
            etCapacity.setError("Capacity is required");
            etCapacity.requestFocus();
            return;
        }

        int capacity;
        int selectionSize;
        try {
            capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) {
                etCapacity.setError("Capacity must be > 0");
                return;
            }
            selectionSize = capacity;
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Invalid number input", Toast.LENGTH_SHORT).show();
            return;
        }

        int waitlistLimit = 0;
        if (limitWaitlist) {
            if (android.text.TextUtils.isEmpty(waitlistLimitStr)) {
                etWaitlistLimit.setError("Specify waitlist limit");
                etWaitlistLimit.requestFocus();
                return;
            }
            try {
                waitlistLimit = Integer.parseInt(waitlistLimitStr);
                if (waitlistLimit <= capacity) {
                    etWaitlistLimit.setError("Must be greater than event capacity");
                    return;
                }
            } catch (NumberFormatException ex) {
                etWaitlistLimit.setError("Invalid waitlist limit");
                return;
            }
        }

        long eventTimestampMillis = mergeDateAndTime(eventDateMillis, eventTimeMillis);
        if (eventTimestampMillis < System.currentTimeMillis()) {
            Toast.makeText(this, "Event date and time must be in the future", Toast.LENGTH_SHORT).show();
            return;
        }

        if (regEndMillis > eventTimestampMillis) {
            Toast.makeText(this, "Registration must end before event date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (regStartMillis >= regEndMillis) {
            Toast.makeText(this, "Registration start must be before registration end", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(this, "User not loaded yet. Please wait a moment and try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        String organizerId = currentUser.getId();
        String organizerName = currentUser.getDisplayName();
        if (android.text.TextUtils.isEmpty(organizerName)) organizerName = "User";
        Log.d(TAG, "Creating event with organizer ID: " + organizerId);

        Map<String, Object> event = new HashMap<>();

        event.put("name", title);
        event.put("description", description);
        event.put("organizerId", organizerId);
        event.put("organizerName", organizerName);
        event.put("location", location);

        if (selectedLocation != null) {
            event.put("locationLatitude", selectedLocation.getLatitude());
            event.put("locationLongitude", selectedLocation.getLongitude());
            if (selectedLocation.getPlaceId() != null) {
                event.put("locationPlaceId", selectedLocation.getPlaceId());
            }
        }

        event.put("category", "General");
        event.put("capacity", capacity);
        event.put("selectionSize", selectionSize);
        event.put("limitWaitlist", limitWaitlist);
        if (limitWaitlist) event.put("waitlistLimit", waitlistLimit);
        event.put("lotteryCriteria", criteria);
        event.put("price", 0);
        event.put("waitlistCount", 0);
        event.put("geolocationRequired", geoRequired);
        event.put("status", "draft");
        event.put("createdAt", System.currentTimeMillis());
        event.put("eventDate", new Timestamp(new java.util.Date(eventTimestampMillis)));
        event.put("registrationStartTime", new Timestamp(new java.util.Date(regStartMillis)));
        event.put("registrationEndTime", new Timestamp(new java.util.Date(regEndMillis)));

        if (selectedImageUri != null) {
            uploadImageAndSaveEvent(selectedImageUri, event);
        } else {
            saveEventToFirestore(event);
        }
    }

    /**
     * Converts the selected image to Base64, attaches it to the event map, and then saves.
     *
     * @param uri   image URI selected from storage
     * @param event event map being prepared for Firestore
     */
    private void uploadImageAndSaveEvent(Uri uri, Map<String, Object> event) {
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

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
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();
                String posterBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

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

        saveEventToFirestore(event);
    }

    /**
     * Saves the event to the Firestore "events" collection and continues with post-create steps.
     *
     * @param event event data to persist
     */
    private void saveEventToFirestore(Map<String, Object> event) {
        db.collection("events").add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();
                    Log.d(TAG, "Event saved with ID: " + eventId);

                    updateUserToOrganizer();

                    generateAndSaveQRCodeAsBase64(eventId, documentReference);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Failed to save event", e);
                    Toast.makeText(this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Ensures the current user document is marked as organizer after they create an event.
     */
    private void updateUserToOrganizer() {
        if (currentUser == null) {
            Log.w(TAG, "Current user is null, cannot update isOrganizer");
            return;
        }

        if (currentUser.isOrganizer()) {
            Log.d(TAG, "User is already an organizer");
            return;
        }

        db.collection("users")
                .document(currentUser.getId())
                .update("is_organizer", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User updated to organizer");
                    currentUser.setOrganizer(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user to organizer", e);
                });
    }

    /**
     * Generates a QR code for the event and stores it as a Base64 string on the same document.
     *
     * @param eventId   Firestore event ID used to generate QR
     * @param eventRef  Firestore document reference to update with QR code
     */
    private void generateAndSaveQRCodeAsBase64(String eventId, com.google.firebase.firestore.DocumentReference eventRef) {
        Log.d(TAG, "Generating QR code for event: " + eventId);

        Bitmap qrCodeBitmap = QRCodeGenerator.generateQRCode(eventId);

        if (qrCodeBitmap == null) {
            Log.e(TAG, "Failed to generate QR code bitmap");
            Toast.makeText(this, "Event created but QR code generation failed.", Toast.LENGTH_LONG).show();
            navigateToEventDetail(eventId);
            return;
        }

        byte[] qrCodeBytes = QRCodeGenerator.bitmapToByteArray(qrCodeBitmap);

        if (qrCodeBytes == null) {
            Log.e(TAG, "Failed to convert QR code bitmap to byte array");
            Toast.makeText(this, "Event created but QR code processing failed.", Toast.LENGTH_LONG).show();
            navigateToEventDetail(eventId);
            return;
        }

        String qrCodeBase64 = Base64.encodeToString(qrCodeBytes, Base64.NO_WRAP);

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
                    navigateToEventDetail(eventId);
                });
    }

    /**
     * Navigates to the event detail screen for the newly created event.
     *
     * @param eventId id of the created event
     */
    private void navigateToEventDetail(String eventId) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
        finish();
    }

    /**
     * Merges a date (millis) and a time-of-day (millis) into a single timestamp (millis).
     *
     * @param dateMillis     date in milliseconds
     * @param timeOfDayMillis time of day in milliseconds
     * @return combined timestamp in milliseconds
     */
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

    /**
     * Safely extracts text from an EditText, trimming whitespace.
     *
     * @param et edit text to read
     * @return trimmed string value or empty string if null
     */
    private String getText(EditText et) {
        if (et == null) return "";
        CharSequence cs = et.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    /**
     * Opens the location search activity to allow the user to pick a location.
     */
    private void openLocationSearch() {
        Intent intent = new Intent(this, LocationSearchActivity.class);
        startActivityForResult(intent, LOCATION_REQUEST_CODE);
    }

    /**
     * Receives the selected location from the LocationSearchActivity and updates UI fields.
     *
     * @param requestCode request code sent
     * @param resultCode  result code returned
     * @param data        intent with location data
     */
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
                etLocation.setError(null);
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
