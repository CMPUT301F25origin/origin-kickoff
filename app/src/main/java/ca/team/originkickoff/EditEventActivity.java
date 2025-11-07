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

import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.models.EventLocation;

public class EditEventActivity extends AppCompatActivity {
    private static final String TAG = "EditEventActivity";
    public static final String EXTRA_EVENT_ID = "event_id";
    private static final int LOCATION_REQUEST_CODE = 101;

    private EditText etEventName, etDescription, etLocation, etDate, etTime,
            etRegStartDate, etRegStartTime, etRegEndDate, etRegEndTime;
    private EditText etCapacity, etCriteria;
    private EditText etSelectionSize, etWaitlistLimit;
    private ImageView ivPosterPreview, btnClose;
    private LinearLayout layoutUploadImage;
    private Button btnCreateEvent;
    private ProgressBar progressBar;
    private androidx.appcompat.widget.SwitchCompat switchGeoRequired, switchLimitWaitlist;

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

    // Event being edited
    private String eventId;
    private Event currentEvent;
    private boolean imageChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Event");
        }

        db = FirebaseFirestore.getInstance();

        // Get event ID from intent
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();

        // Initialize ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedImageUri = uri;
                            imageChanged = true;
                            if (ivPosterPreview != null) {
                                ivPosterPreview.setImageURI(selectedImageUri);
                            }
                            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        attachListeners();

        // Load the event data
        loadEventData();
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
        etSelectionSize = findViewById(R.id.etSelectionSize);
        etWaitlistLimit = findViewById(R.id.etWaitlistLimit);
        btnClose = findViewById(R.id.btnClose);
        layoutUploadImage = findViewById(R.id.layoutUploadImage);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        switchGeoRequired = findViewById(R.id.switchGeoRequired);
        switchLimitWaitlist = findViewById(R.id.switchLimitWaitlist);

        // Change button text to "Update Event"
        btnCreateEvent.setText("Update Event");
    }

    private void loadEventData() {
        setLoading(true);
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists()) {
                        currentEvent = documentSnapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setId(documentSnapshot.getId());
                            populateFields();
                        } else {
                            Toast.makeText(this, "Error loading event data", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Error loading event", e);
                    Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateFields() {
        // Populate basic fields
        etEventName.setText(currentEvent.getName());
        etDescription.setText(currentEvent.getDescription());
        etLocation.setText(currentEvent.getLocation());
        etCapacity.setText(String.valueOf(currentEvent.getCapacity()));

        if (currentEvent.getLotteryCriteria() != null) {
            etCriteria.setText(currentEvent.getLotteryCriteria());
        }

        // Store location data
        if (currentEvent.getLocation() != null) {
            selectedLocation = new EventLocation(
                    currentEvent.getLocation(),
                    currentEvent.getLocationLatitude(),
                    currentEvent.getLocationLongitude(),
                    currentEvent.getLocationPlaceId()
            );
        }

        // Populate event date and time
        if (currentEvent.getEventDate() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentEvent.getEventDate());

            eventDateMillis = cal.getTimeInMillis();
            etDate.setText(android.text.format.DateFormat.getDateFormat(this).format(currentEvent.getEventDate()));

            eventTimeMillis = cal.getTimeInMillis();
            etTime.setText(String.format(Locale.US, "%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE)));
        }

        // Populate registration start date and time
        if (currentEvent.getRegistrationStartTime() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentEvent.getRegistrationStartTime());

            regStartMillis = cal.getTimeInMillis();
            etRegStartDate.setText(android.text.format.DateFormat.getDateFormat(this).format(currentEvent.getRegistrationStartTime()));
            etRegStartTime.setText(String.format(Locale.US, "%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE)));
        }

        // Populate registration end date and time
        if (currentEvent.getRegistrationEndTime() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentEvent.getRegistrationEndTime());

            regEndMillis = cal.getTimeInMillis();
            etRegEndDate.setText(android.text.format.DateFormat.getDateFormat(this).format(currentEvent.getRegistrationEndTime()));
            etRegEndTime.setText(String.format(Locale.US, "%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE)));
        }

        // Load existing poster if available
        if (ivPosterPreview != null && currentEvent.getPosterBase64() != null && !currentEvent.getPosterBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(currentEvent.getPosterBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivPosterPreview.setImageBitmap(decodedByte);
                ivPosterPreview.setVisibility(View.VISIBLE); // Show preview
            } catch (Exception e) {
                Log.e(TAG, "Error decoding poster image", e);
            }
        }

        // Set switches
        switchGeoRequired.setChecked(currentEvent.isGeolocationRequired());
        switchLimitWaitlist.setChecked(currentEvent.getWaitlistLimit() > 0);

        // Show and populate waitlist limit if it exists
        if (currentEvent.getWaitlistLimit() > 0) {
            etWaitlistLimit.setText(String.valueOf(currentEvent.getWaitlistLimit()));
            etWaitlistLimit.setVisibility(View.VISIBLE);
        }
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

        btnCreateEvent.setOnClickListener(v -> updateEvent());

        // Toggle listener for waitlist limit
        switchLimitWaitlist.setOnCheckedChangeListener((btn, checked) -> {
            if (etWaitlistLimit != null) {
                etWaitlistLimit.setVisibility(checked ? View.VISIBLE : View.GONE);
                if (!checked) {
                    etWaitlistLimit.setText("");
                }
            }
        });
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
        btnCreateEvent.setText(loading ? "Updating..." : "Update Event");
    }

    private void updateEvent() {
        String title = getText(etEventName);
        String description = getText(etDescription);
        String location = getText(etLocation);
        String capacityStr = getText(etCapacity);
        String criteria = getText(etCriteria);
        String selectionSizeStr = getText(etSelectionSize);
        String waitlistLimitStr = getText(etWaitlistLimit);

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

        // Validate selection size and waitlist limit if applicable
        boolean limitWaitlist = switchLimitWaitlist.isChecked();
        boolean geoRequired = switchGeoRequired.isChecked();

        int waitlistLimit = -1;
        if (limitWaitlist) {
            if (TextUtils.isEmpty(waitlistLimitStr)) {
                etWaitlistLimit.setError("Waitlist limit is required");
                etWaitlistLimit.requestFocus();
                return;
            }
            try {
                waitlistLimit = Integer.parseInt(waitlistLimitStr);
                if (waitlistLimit < 0) {
                    etWaitlistLimit.setError("Waitlist limit cannot be negative");
                    etWaitlistLimit.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etWaitlistLimit.setError("Invalid waitlist limit");
                etWaitlistLimit.requestFocus();
                return;
            }
        }

        int selectionSize = -1;
        if (TextUtils.isEmpty(selectionSizeStr)) {
            etSelectionSize.setError("Selection size is required");
            etSelectionSize.requestFocus();
            return;
        }
        try {
            selectionSize = Integer.parseInt(selectionSizeStr);
            if (selectionSize <= 0) {
                etSelectionSize.setError("Selection size must be greater than 0");
                etSelectionSize.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etSelectionSize.setError("Invalid selection size");
            etSelectionSize.requestFocus();
            return;
        }

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

        // Assemble event update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", title);
        updates.put("description", description);
        updates.put("location", location);
        updates.put("capacity", capacity);
        updates.put("lotteryCriteria", criteria);
        updates.put("eventDate", new Timestamp(new java.util.Date(eventTimestampMillis)));
        updates.put("registrationStartTime", new Timestamp(new java.util.Date(regStartMillis)));
        updates.put("registrationEndTime", new Timestamp(new java.util.Date(regEndMillis)));
        updates.put("geolocationRequired", geoRequired);
        updates.put("waitlistLimit", limitWaitlist ? waitlistLimit : 0);
        updates.put("selectionSize", selectionSize);

        // Add location coordinates if available
        if (selectedLocation != null) {
            updates.put("locationLatitude", selectedLocation.getLatitude());
            updates.put("locationLongitude", selectedLocation.getLongitude());
            if (selectedLocation.getPlaceId() != null) {
                updates.put("locationPlaceId", selectedLocation.getPlaceId());
            }
        }

        // Check if image was changed
        if (imageChanged && selectedImageUri != null) {
            // Convert new image to Base64 and update event
            uploadImageAndUpdateEvent(selectedImageUri, updates);
        } else {
            // No image change, update event directly
            updateEventInFirestore(updates);
        }
    }

    private void uploadImageAndUpdateEvent(Uri uri, Map<String, Object> updates) {
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize bitmap if too large
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
                updates.put("posterBase64", posterBase64);
            }

            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to base64", e);
        }

        updateEventInFirestore(updates);
    }

    private void updateEventInFirestore(Map<String, Object> updates) {
        db.collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Event updated successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Event updated: " + eventId);
                    navigateToEventDetail(eventId);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Failed to update event", e);
                    Toast.makeText(this, "Failed to update event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToEventDetail(String eventId) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

