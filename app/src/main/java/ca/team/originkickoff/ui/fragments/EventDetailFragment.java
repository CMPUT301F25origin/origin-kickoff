/*
 * Displays detailed information for a single event, including poster, metadata, and registration info.
 * Fetches the event from Firestore via FirebaseEventService by ID passed in arguments.
 */
package ca.team.originkickoff.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Locale;

import ca.team.originkickoff.R;
import ca.team.originkickoff.models.Event;
import ca.team.originkickoff.services.FirebaseEventService;

/**
 * Fragment for presenting an event's full details with image and formatted fields.
 */
public class EventDetailFragment extends Fragment {
    private static final String ARG_EVENT_ID = "event_id";
    private String eventId;

    private ImageView detailEventPoster;
    private TextView detailEventName;
    private TextView detailOrganizerName;
    private TextView detailEventDate;
    private TextView detailEventLocation;
    private TextView detailEventPrice;
    private TextView detailCapacityInfo;
    private TextView detailWaitlistInfo;
    private TextView detailRegistrationPeriod;
    private TextView detailEventDescription;

    private FirebaseEventService firebaseEventService;

    /**
     * Factory method to create a new fragment instance for a specific event.
     *
     * @param eventId Firestore document ID of the event
     * @return configured fragment instance
     */
    public static EventDetailFragment newInstance(String eventId) {
        EventDetailFragment fragment = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Reads arguments to capture the target event ID.
     *
     * @param savedInstanceState previously saved state bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    /**
     * Inflates the detail layout.
     *
     * @param inflater  layout inflater
     * @param container parent view group
     * @param savedInstanceState previous state bundle
     * @return inflated view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    /**
     * Initializes views and kicks off event loading.
     *
     * @param view root view
     * @param savedInstanceState previous state bundle
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        firebaseEventService = new FirebaseEventService();
        loadEventDetails();
    }

    /**
     * Binds the view references for later population.
     *
     * @param view root view
     */
    private void initializeViews(View view) {
        detailEventPoster = view.findViewById(R.id.detailEventPoster);
        detailEventName = view.findViewById(R.id.detailEventName);
        detailOrganizerName = view.findViewById(R.id.detailOrganizerName);
        detailEventDate = view.findViewById(R.id.detailEventDate);
        detailEventLocation = view.findViewById(R.id.detailEventLocation);
        detailEventPrice = view.findViewById(R.id.detailEventPrice);
        detailCapacityInfo = view.findViewById(R.id.detailCapacityInfo);
        detailWaitlistInfo = view.findViewById(R.id.detailWaitlistInfo);
        detailRegistrationPeriod = view.findViewById(R.id.detailRegistrationPeriod);
        detailEventDescription = view.findViewById(R.id.detailEventDescription);
    }

    /**
     * Fetches event data for the stored event ID and updates the UI on success.
     */
    private void loadEventDetails() {
        firebaseEventService.getEventById(eventId, new FirebaseEventService.SingleEventCallback() {
            @Override
            public void onSuccess(Event event) {
                displayEventDetails(event);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(), "Error loading event: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Populates the UI with event fields and resolves the poster image from base64 or URL.
     *
     * @param event model with details to display
     */
    private void displayEventDetails(Event event) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        detailEventName.setText(event.getName());
        detailOrganizerName.setText(event.getOrganizerName());
        detailEventDescription.setText(event.getDescription());

        // Date and Time
        if (event.getEventDate() != null) {
            detailEventDate.setText(dateTimeFormat.format(event.getEventDate()));
        }

        // Location
        detailEventLocation.setText(event.getLocation());

        // Price
        if (event.getPrice() > 0) {
            detailEventPrice.setText(String.format(Locale.getDefault(), "$%.2f", event.getPrice()));
        } else {
            detailEventPrice.setText(R.string.free_label);
        }

        // Capacity Info
        int availableSpots = event.getCapacity() - event.getWaitlistCount();
        detailCapacityInfo.setText(String.format(Locale.getDefault(),
                "Capacity: %d participants", event.getCapacity()));

        // Waitlist Info
        detailWaitlistInfo.setText(String.format(Locale.getDefault(),
                "%d on waitlist • %d spots available",
                event.getWaitlistCount(), availableSpots));

        // Registration Period
        if (event.getRegistrationStartTime() != null && event.getRegistrationEndTime() != null) {
            String registrationPeriod = String.format(Locale.getDefault(),
                    "Opens: %s\nCloses: %s",
                    dateTimeFormat.format(event.getRegistrationStartTime()),
                    dateTimeFormat.format(event.getRegistrationEndTime()));
            detailRegistrationPeriod.setText(registrationPeriod);
        }

        // Poster Image preference: Base64 first, then URL fallback
        boolean loadedPoster = false;
        if (event.getPosterBase64() != null && !event.getPosterBase64().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(event.getPosterBase64(), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    detailEventPoster.setImageBitmap(bmp);
                    loadedPoster = true;
                }
            } catch (Exception ignored) { }
        }
        if (!loadedPoster) {
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(this)
                        .load(event.getPosterUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(detailEventPoster);
            } else {
                detailEventPoster.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }
}
