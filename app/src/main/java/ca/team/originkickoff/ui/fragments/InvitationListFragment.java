package ca.team.originkickoff.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.R;
import ca.team.originkickoff.adapters.InvitationAdapter;
import ca.team.originkickoff.models.InvitationStatus;

/**
 * Fragment for displaying list of invitations by status (chosen/cancelled/enrolled)
 */
public class InvitationListFragment extends Fragment {
    private static final String TAG = "InvitationListFragment";
    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_STATUS = "status";

    private String eventId;
    private String status;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private InvitationAdapter adapter;

    public static InvitationListFragment newInstance(String eventId, String status) {
        InvitationListFragment fragment = new InvitationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            status = getArguments().getString(ARG_STATUS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invitation_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InvitationAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadInvitations();

        return view;
    }

    private void loadInvitations() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("invitation_status")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", status)
                .addSnapshotListener((snapshots, error) -> {
                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e(TAG, "Error loading invitations", error);
                        showEmpty();
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<InvitationStatus> invitations = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            InvitationStatus invitation = doc.toObject(InvitationStatus.class);
                            invitations.add(invitation);
                        }
                        adapter.updateData(invitations);
                        tvEmpty.setVisibility(View.GONE);
                    } else {
                        showEmpty();
                    }
                });
    }

    private void showEmpty() {
        tvEmpty.setVisibility(View.VISIBLE);
        String message;
        switch (status) {
            case "chosen":
                message = "No users selected yet";
                break;
            case "cancelled":
                message = "No cancellations";
                break;
            case "enrolled":
                message = "No enrollments yet";
                break;
            default:
                message = "No results";
        }
        tvEmpty.setText(message);
    }
}

