package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.List;

/**
 * Represents the result of a lottery draw for an event.
 * Stored in Firestore to maintain audit trail and prevent duplicate draws.
 */
public class LotteryResult {
    @PropertyName("event_id")
    private String eventId;

    @PropertyName("conducted_at")
    private Timestamp conductedAt;

    @PropertyName("lottery_method")
    private String lotteryMethod; // "random" or "early_priority_random"

    @PropertyName("total_entrants")
    private int totalEntrants;

    @PropertyName("num_winners")
    private int numWinners;

    @PropertyName("winner_ids")
    private List<String> winnerIds;

    @PropertyName("conducted_by")
    private String conductedBy; // organizer user ID

    // Empty constructor for Firebase
    public LotteryResult() {}

    public LotteryResult(String eventId, Timestamp conductedAt, String lotteryMethod,
                        int totalEntrants, int numWinners, List<String> winnerIds,
                        String conductedBy) {
        this.eventId = eventId;
        this.conductedAt = conductedAt;
        this.lotteryMethod = lotteryMethod;
        this.totalEntrants = totalEntrants;
        this.numWinners = numWinners;
        this.winnerIds = winnerIds;
        this.conductedBy = conductedBy;
    }

    // Getters and Setters
    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @PropertyName("conducted_at")
    public Timestamp getConductedAt() { return conductedAt; }
    @PropertyName("conducted_at")
    public void setConductedAt(Timestamp conductedAt) { this.conductedAt = conductedAt; }

    @PropertyName("lottery_method")
    public String getLotteryMethod() { return lotteryMethod; }
    @PropertyName("lottery_method")
    public void setLotteryMethod(String lotteryMethod) { this.lotteryMethod = lotteryMethod; }

    @PropertyName("total_entrants")
    public int getTotalEntrants() { return totalEntrants; }
    @PropertyName("total_entrants")
    public void setTotalEntrants(int totalEntrants) { this.totalEntrants = totalEntrants; }

    @PropertyName("num_winners")
    public int getNumWinners() { return numWinners; }
    @PropertyName("num_winners")
    public void setNumWinners(int numWinners) { this.numWinners = numWinners; }

    @PropertyName("winner_ids")
    public List<String> getWinnerIds() { return winnerIds; }
    @PropertyName("winner_ids")
    public void setWinnerIds(List<String> winnerIds) { this.winnerIds = winnerIds; }

    @PropertyName("conducted_by")
    public String getConductedBy() { return conductedBy; }
    @PropertyName("conducted_by")
    public void setConductedBy(String conductedBy) { this.conductedBy = conductedBy; }
}

