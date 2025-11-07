package ca.team.originkickoff.services;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.team.originkickoff.models.LotteryMethod;
import ca.team.originkickoff.models.WaitingListEntry;

/**
 * Service for conducting fair lottery selection from waiting list entries.
 * Supports two methods:
 * 1. RANDOM: Pure random selection using SecureRandom
 * 2. EARLY_PRIORITY_RANDOM: Weighted selection favoring earlier entrants
 */
public class LotteryService {
    private final SecureRandom secureRandom;
    private final WaitingListService waitingListService;

    // Decay factor for early priority weighting (higher = more advantage to early entrants)
    // With 0.5, weight halves every normalized time unit
    private static final double EARLY_PRIORITY_DECAY = 0.5;

    public LotteryService() {
        this(new WaitingListService(), new SecureRandom());
    }

    @VisibleForTesting
    LotteryService(WaitingListService waitingListService, SecureRandom secureRandom) {
        this.waitingListService = waitingListService;
        this.secureRandom = secureRandom;
    }

    /**
     * Conduct a lottery draw for an event.
     *
     * @param eventId The event ID
     * @param method The lottery method to use
     * @param numWinners Number of winners to select
     * @return Task containing list of selected winner user IDs
     */
    public Task<List<String>> conductLottery(@NonNull String eventId,
                                              @NonNull LotteryMethod method,
                                              int numWinners) {
        if (numWinners <= 0) {
            return Tasks.forResult(new ArrayList<>());
        }

        return waitingListService.listActive(eventId)
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        throw new Exception("Failed to retrieve waiting list entries");
                    }

                    List<WaitingListEntry> entries = task.getResult();
                    if (entries.isEmpty()) {
                        return new ArrayList<>();
                    }

                    // Can't select more winners than available entrants
                    int actualWinners = Math.min(numWinners, entries.size());

                    switch (method) {
                        case EARLY_PRIORITY_RANDOM:
                            return selectEarlyPriorityRandom(entries, actualWinners);
                        case RANDOM:
                        default:
                            return selectPureRandom(entries, actualWinners);
                    }
                });
    }

    /**
     * Pure random selection - all entrants have equal probability.
     * Uses Fisher-Yates shuffle with SecureRandom for cryptographic security.
     *
     * @param entries List of waiting list entries
     * @param numWinners Number of winners to select
     * @return List of selected user IDs
     */
    private List<String> selectPureRandom(List<WaitingListEntry> entries, int numWinners) {
        // Create a copy to avoid modifying original list
        List<WaitingListEntry> shuffled = new ArrayList<>(entries);

        // Fisher-Yates shuffle using SecureRandom
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            Collections.swap(shuffled, i, j);
        }

        // Take first numWinners entries
        List<String> winners = new ArrayList<>();
        for (int i = 0; i < numWinners && i < shuffled.size(); i++) {
            winners.add(shuffled.get(i).getUserId());
        }

        return winners;
    }

    /**
     * Early priority random selection - earlier entrants get higher weight.
     * Uses exponential decay: weight = e^(-decay * normalizedTime)
     * where normalizedTime ∈ [0, 1], 0 = earliest, 1 = latest
     *
     * Algorithm: Weighted reservoir sampling with exponential weights
     *
     * @param entries List of waiting list entries (assumed sorted by joinedAt ascending)
     * @param numWinners Number of winners to select
     * @return List of selected user IDs
     */
    private List<String> selectEarlyPriorityRandom(List<WaitingListEntry> entries, int numWinners) {
        if (entries.isEmpty()) {
            return new ArrayList<>();
        }

        // Calculate weights based on join time
        List<WeightedEntry> weightedEntries = new ArrayList<>();

        long earliestTime = entries.get(0).getJoinedAt().getSeconds();
        long latestTime = entries.get(entries.size() - 1).getJoinedAt().getSeconds();
        long timeRange = latestTime - earliestTime;

        for (WaitingListEntry entry : entries) {
            double normalizedTime;
            if (timeRange == 0) {
                // All joined at same time - equal weights
                normalizedTime = 0;
            } else {
                // Normalize to [0, 1] where 0 = earliest
                normalizedTime = (double) (entry.getJoinedAt().getSeconds() - earliestTime) / timeRange;
            }

            // Exponential decay weight: earlier = higher weight
            // e^(-decay * 0) = 1.0 (earliest)
            // e^(-decay * 1) ≈ 0.6 (latest, with decay=0.5)
            double weight = Math.exp(-EARLY_PRIORITY_DECAY * normalizedTime);

            weightedEntries.add(new WeightedEntry(entry.getUserId(), weight));
        }

        // Use weighted random sampling without replacement
        return weightedRandomSample(weightedEntries, numWinners);
    }

    /**
     * Weighted random sampling without replacement using the "Efraimidis-Spirakis" algorithm.
     * For each item, generate key = random^(1/weight), then select items with largest keys.
     * This is efficient and provably fair.
     *
     * @param weightedEntries List of entries with weights
     * @param numSamples Number of samples to draw
     * @return List of selected user IDs
     */
    private List<String> weightedRandomSample(List<WeightedEntry> weightedEntries, int numSamples) {
        List<ScoredEntry> scoredEntries = new ArrayList<>();

        for (WeightedEntry entry : weightedEntries) {
            // Generate uniform random [0, 1)
            double random = secureRandom.nextDouble();

            // Avoid log(0) by using a tiny minimum value
            if (random < 1e-10) random = 1e-10;

            // Calculate score: random^(1/weight) = exp(log(random) / weight)
            double score = Math.exp(Math.log(random) / entry.weight);

            scoredEntries.add(new ScoredEntry(entry.userId, score));
        }

        // Sort by score descending (highest scores win)
        Collections.sort(scoredEntries, (a, b) -> Double.compare(b.score, a.score));

        // Take top numSamples
        List<String> winners = new ArrayList<>();
        for (int i = 0; i < numSamples && i < scoredEntries.size(); i++) {
            winners.add(scoredEntries.get(i).userId);
        }

        return winners;
    }

    /**
     * Helper class for weighted entries
     */
    private static class WeightedEntry {
        final String userId;
        final double weight;

        WeightedEntry(String userId, double weight) {
            this.userId = userId;
            this.weight = weight;
        }
    }

    /**
     * Helper class for scored entries (used in weighted sampling)
     */
    private static class ScoredEntry {
        final String userId;
        final double score;

        ScoredEntry(String userId, double score) {
            this.userId = userId;
            this.score = score;
        }
    }
}
