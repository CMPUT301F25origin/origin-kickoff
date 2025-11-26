/*
 * Displays invitations for an event filtered by status (chosen/cancelled/enrolled).
 * Provides a simple list UI backed by a Firestore snapshot listener.
 */
package ca.team.originkickoff.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.R;
import ca.team.originkickoff.adapters.InvitationAdapter;
import ca.team.originkickoff.models.InvitationStatus;

/**
 * Fragment for displaying a list of invitations filtered by a specific status.
 */
public class InvitationListFragment extends Fragment implements InvitationAdapter.OnRemoveEntrantListener {
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
        adapter = new InvitationAdapter(new ArrayList<>(), this, status);
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
                            invitation.setId(doc.getId()); // Capture the document ID for later use
                            invitations.add(invitation);
                        }
                        adapter.updateData(invitations);
                        tvEmpty.setVisibility(View.GONE);
                    } else {
                        adapter.updateData(new ArrayList<>()); // Clear the list on empty results
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

    @Override
    public void onRemoveEntrant(InvitationStatus invitation) {
        new AlertDialog.Builder(getContext())
                .setTitle("Remove Entrant")
                .setMessage("Are you sure you want to remove this entrant from the lottery?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    removeEntrantFromLottery(invitation);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeEntrantFromLottery(InvitationStatus invitation) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lottery_results")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String lotteryResultDocId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Create a batch write to perform both updates atomically
                        WriteBatch batch = db.batch();

                        // Action 1: Remove user from the 'winner_ids' array in lottery_results
                        DocumentReference lotteryResultRef = db.collection("lottery_results").document(lotteryResultDocId);
                        batch.update(lotteryResultRef, "winner_ids", FieldValue.arrayRemove(invitation.getUserId()));

                        // Action 2: Update the user's status to 'cancelled' in their invitation document
                        DocumentReference invitationStatusRef = db.collection("invitation_status").document(invitation.getId());
                        batch.update(invitationStatusRef, "status", "cancelled");

                        // Commit the atomic batch
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Entrant has been removed.", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                    // The UI will now update automatically via the snapshot listener
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to remove entrant.", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Failed to commit batch remove", e);
                                    progressBar.setVisibility(View.GONE);
                                });
                    } else {
                        Toast.makeText(getContext(), "Lottery results document not found.", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to find lottery results: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
}
