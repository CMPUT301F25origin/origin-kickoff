package ca.team.originkickoff.ui.fragments;

import android.os.Bundle;
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

    public static EventDetailFragment newInstance(String eventId) {
        EventDetailFragment fragment = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        firebaseEventService = new FirebaseEventService();
        loadEventDetails();
    }

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

        // Poster Image
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

