/*
 * Tabbed interface for reviewing invitation statuses (chosen, cancelled, enrolled).
 * Loads event details for context and hosts fragments via pager adapter.
 */
package ca.team.originkickoff;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.Tasks;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.team.originkickoff.adapters.InvitationsPagerAdapter;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.models.InvitationStatus;
import ca.team.originkickoff.models.User;
import ca.team.originkickoff.services.DeclineResamplingService;

/**
 * Activity for managing lottery invitations with tabs for chosen, cancelled, and enrolled users.
 */
public class ManageInvitationsActivity extends AppCompatActivity {
    private static final String TAG = "ManageInvitations";
    public static final String EXTRA_EVENT_ID = "event_id";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tvEventName;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Button btnExportEnrolled;
    private String eventId;
    private Event currentEvent;
    private FirebaseFirestore db;

    /**
     * Inflates layout, validates event ID, and initializes tabs.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_invitations);

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Begin monitoring declines for automatic resampling
        DeclineResamplingService.ensureMonitoring(eventId);

        initializeViews();
        loadEventData();
        setupTabs();
        setupExportButton();
    }

    /**
     * Inflates and binds view references (header text, tab layout, pager) and configures action bar.
     */
    private void initializeViews() {
        tvEventName = findViewById(R.id.tv_event_name);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        btnExportEnrolled = findViewById(R.id.btn_export_enrolled);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lottery Results");
        }
    }

    /**
     * Loads the event document to retrieve its name for contextual display in the header.
     * Silently logs failures without interrupting tab setup.
     */
    private void loadEventData() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentEvent = documentSnapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setId(documentSnapshot.getId());
                            tvEventName.setText(currentEvent.getName());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event", e);
                });
    }

    /**
     * Configures ViewPager2 with invitation status tabs (Chosen, Cancelled, Enrolled) and attaches TabLayout.
     * Starts on the "Chosen" tab for immediate lottery results visibility.
     */
    private void setupTabs() {
        InvitationsPagerAdapter adapter = new InvitationsPagerAdapter(this, eventId);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Chosen");
                            break;
                        case 1:
                            tab.setText("Cancelled");
                            break;
                        case 2:
                            tab.setText("Enrolled");
                            break;
                    }
                }).attach();

        // Start on Chosen tab
        viewPager.setCurrentItem(0);

        // Show export button only on Enrolled tab (position 2)
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                btnExportEnrolled.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Sets up the export button for enrolled users CSV export.
     */
    private void setupExportButton() {
        btnExportEnrolled.setOnClickListener(v -> {
            if (checkStoragePermissions()) {
                exportEnrolledUsersToCSV();
            } else {
                requestStoragePermissions();
            }
        });
    }

    /**
     * Check if storage permissions are granted (for Android < 10) or if we can write to external storage.
     */
    private boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ doesn't need storage permissions for app-specific directories
            return true;
        } else {
            int writePermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return writePermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Request storage permissions for Android < 10.
     */
    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportEnrolledUsersToCSV();
            } else {
                Toast.makeText(this, "Storage permission required to export CSV",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Export enrolled users data to CSV file.
     */
    private void exportEnrolledUsersToCSV() {
        Toast.makeText(this, "Preparing export...", Toast.LENGTH_SHORT).show();

        // Fetch enrolled users for this event
        db.collection("invitation_status")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", "enrolled")
                .get()
                .addOnSuccessListener(invitationSnapshots -> {
                    if (invitationSnapshots.isEmpty()) {
                        Toast.makeText(this, "No enrolled users to export", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<InvitationStatus> enrolledInvitations = new ArrayList<>();
                    for (DocumentSnapshot doc : invitationSnapshots.getDocuments()) {
                        InvitationStatus invitation = doc.toObject(InvitationStatus.class);
                        if (invitation != null) {
                            enrolledInvitations.add(invitation);
                        }
                    }

                    // Fetch user details for each enrolled user
                    List<com.google.android.gms.tasks.Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    for (InvitationStatus invitation : enrolledInvitations) {
                        userTasks.add(db.collection("users").document(invitation.getUserId()).get());
                    }

                    Tasks.whenAllSuccess(userTasks)
                            .addOnSuccessListener(userDocs -> {
                                List<UserExportData> exportData = new ArrayList<>();

                                for (int i = 0; i < userDocs.size(); i++) {
                                    DocumentSnapshot userDoc = (DocumentSnapshot) userDocs.get(i);
                                    User user = userDoc.toObject(User.class);
                                    if (user != null) {
                                        user.setId(userDoc.getId());
                                        InvitationStatus invitation = enrolledInvitations.get(i);
                                        exportData.add(new UserExportData(user, invitation));
                                    }
                                }

                                writeCSVFile(exportData);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching user details", e);
                                Toast.makeText(this, "Error fetching user details",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching enrolled users", e);
                    Toast.makeText(this, "Error fetching enrolled users", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Write user data to CSV file and save to Downloads directory.
     */
    private void writeCSVFile(List<UserExportData> exportData) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String eventName = currentEvent != null ? currentEvent.getName().replaceAll("[^a-zA-Z0-9]", "_") : "event";
        String fileName = "enrolled_users_" + eventName + "_" + timestamp + ".csv";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ : Use MediaStore API to save to public Downloads folder
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    throw new IOException("Failed to create MediaStore entry");
                }

                try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                    if (outputStream == null) {
                        throw new IOException("Failed to open output stream");
                    }
                    writeCSVContent(outputStream, exportData);
                }

                Toast.makeText(this, "CSV exported successfully to Downloads/" + fileName,
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "CSV exported to Downloads/" + fileName);

            } else {
                // Android 9 and below: Use legacy external storage
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    boolean created = downloadsDir.mkdirs();
                    if (!created) {
                        throw new IOException("Failed to create downloads directory");
                    }
                }

                File csvFile = new File(downloadsDir, fileName);
                FileWriter writer = new FileWriter(csvFile);
                writeCSVContentToWriter(writer, exportData);
                writer.close();

                Toast.makeText(this, "CSV exported successfully to: " + csvFile.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "CSV exported to: " + csvFile.getAbsolutePath());
            }

        } catch (IOException e) {
            Log.e(TAG, "Error writing CSV file", e);
            Toast.makeText(this, "Error writing CSV file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Write CSV content to OutputStream (for Android 10+).
     */
    private void writeCSVContent(OutputStream outputStream, List<UserExportData> exportData) throws IOException {
        StringBuilder csvContent = new StringBuilder();

        // Write CSV header
        csvContent.append("User ID,Display Name,Email,Phone,Invited At,Responded At\n");

        // Write data rows
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (UserExportData data : exportData) {
            csvContent.append(escapeCsv(data.user.getId())).append(",");
            csvContent.append(escapeCsv(data.user.getDisplayName())).append(",");
            csvContent.append(escapeCsv(data.user.getEmail())).append(",");
            csvContent.append(escapeCsv(data.user.getPhone())).append(",");

            String invitedAt = data.invitation.getInvitedAt() != null ?
                    dateFormat.format(data.invitation.getInvitedAt().toDate()) : "";
            csvContent.append(escapeCsv(invitedAt)).append(",");

            String respondedAt = data.invitation.getRespondedAt() != null ?
                    dateFormat.format(data.invitation.getRespondedAt().toDate()) : "";
            csvContent.append(escapeCsv(respondedAt)).append("\n");
        }

        outputStream.write(csvContent.toString().getBytes());
        outputStream.flush();
    }

    /**
     * Write CSV content to FileWriter (for Android 9 and below).
     */
    private void writeCSVContentToWriter(FileWriter writer, List<UserExportData> exportData) throws IOException {
        // Write CSV header
        writer.append("User ID,Display Name,Email,Phone,Invited At,Responded At\n");

        // Write data rows
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (UserExportData data : exportData) {
            writer.append(escapeCsv(data.user.getId())).append(",");
            writer.append(escapeCsv(data.user.getDisplayName())).append(",");
            writer.append(escapeCsv(data.user.getEmail())).append(",");
            writer.append(escapeCsv(data.user.getPhone())).append(",");

            String invitedAt = data.invitation.getInvitedAt() != null ?
                    dateFormat.format(data.invitation.getInvitedAt().toDate()) : "";
            writer.append(escapeCsv(invitedAt)).append(",");

            String respondedAt = data.invitation.getRespondedAt() != null ?
                    dateFormat.format(data.invitation.getRespondedAt().toDate()) : "";
            writer.append(escapeCsv(respondedAt)).append("\n");
        }

        writer.flush();
    }

    /**
     * Escape CSV values to handle commas, quotes, and newlines.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Helper class to combine user and invitation data for export.
     */
    private static class UserExportData {
        User user;
        InvitationStatus invitation;

        UserExportData(User user, InvitationStatus invitation) {
            this.user = user;
            this.invitation = invitation;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
