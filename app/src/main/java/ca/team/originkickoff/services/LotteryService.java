/*
 * Lottery selection service encapsulating algorithms for choosing winners.
 * Supports pure random and early-priority weighted random selection strategies.
 */
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
 * Provides algorithms to fairly select winners from waiting list entries based on
 * configured {@link LotteryMethod}. Implements pure random and weighted early priority selection.
 */
public class LotteryService {
    private final SecureRandom secureRandom;
    private final WaitingListService waitingListService;
    private static final double EARLY_PRIORITY_DECAY = 0.5;

    /**
     * Constructs a lottery service with a cryptographically strong random source.
     */
    public LotteryService() {
        this(new WaitingListService(), new SecureRandom());
    }

    @VisibleForTesting
    LotteryService(WaitingListService waitingListService, SecureRandom secureRandom) {
        this.waitingListService = waitingListService;
        this.secureRandom = secureRandom;
    }

    /**
     * Conduct a lottery draw for an event using the specified method.
     *
     * @param eventId    event identifier
     * @param method     selection method to apply
     * @param numWinners requested number of winners (clamped to entrant count)
     * @return Task resolving with list of winner user IDs
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
     * Pure random selection where all entrants have equal probability.
     * Implements Fisher-Yates shuffle for unbiased ordering.
     *
     * @param entries list of waiting list entries
     * @param numWinners number of winners to select
     * @return winner user IDs
     */
    private List<String> selectPureRandom(List<WaitingListEntry> entries, int numWinners) {
        List<WaitingListEntry> shuffled = new ArrayList<>(entries);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            Collections.swap(shuffled, i, j);
        }
        List<String> winners = new ArrayList<>();
        for (int i = 0; i < numWinners && i < shuffled.size(); i++) {
            winners.add(shuffled.get(i).getUserId());
        }
        return winners;
    }

    /**
     * Early priority selection favoring earlier entrants using exponential decay weighting.
     * Normalizes join times to [0,1] before computing decayed weights.
     *
     * @param entries sorted waiting list entries (ascending by joinedAt)
     * @param numWinners number of winners to select
     * @return winner user IDs
     */
    private List<String> selectEarlyPriorityRandom(List<WaitingListEntry> entries, int numWinners) {
        if (entries.isEmpty()) {
            return new ArrayList<>();
        }
        List<WeightedEntry> weightedEntries = new ArrayList<>();
        long earliestTime = entries.get(0).getJoinedAt().getSeconds();
        long latestTime = entries.get(entries.size() - 1).getJoinedAt().getSeconds();
        long timeRange = latestTime - earliestTime;
        for (WaitingListEntry entry : entries) {
            double normalizedTime = timeRange == 0 ? 0 : (double) (entry.getJoinedAt().getSeconds() - earliestTime) / timeRange;
            double weight = Math.exp(-EARLY_PRIORITY_DECAY * normalizedTime);
            weightedEntries.add(new WeightedEntry(entry.getUserId(), weight));
        }
        return weightedRandomSample(weightedEntries, numWinners);
    }

    /**
     * Weighted random sampling without replacement using Efraimidis-Spirakis algorithm.
     * Generates a score per entry and selects highest scores.
     *
     * @param weightedEntries entries with precomputed weights
     * @param numSamples number of samples to draw
     * @return selected user IDs
     */
    private List<String> weightedRandomSample(List<WeightedEntry> weightedEntries, int numSamples) {
        List<ScoredEntry> scoredEntries = new ArrayList<>();
        for (WeightedEntry entry : weightedEntries) {
            double random = secureRandom.nextDouble();
            if (random < 1e-10) random = 1e-10;
            double score = Math.exp(Math.log(random) / entry.weight);
            scoredEntries.add(new ScoredEntry(entry.userId, score));
        }
        Collections.sort(scoredEntries, (a, b) -> Double.compare(b.score, a.score));
        List<String> winners = new ArrayList<>();
        for (int i = 0; i < numSamples && i < scoredEntries.size(); i++) {
            winners.add(scoredEntries.get(i).userId);
        }
        return winners;
    }

    /**
     * Container for a weighted entrant.
     */
    private static class WeightedEntry {
        final String userId;
        final double weight;
        WeightedEntry(String userId, double weight) { this.userId = userId; this.weight = weight; }
    }

    /**
     * Container for a scored entrant during sampling.
     */
    private static class ScoredEntry {
        final String userId;
        final double score;
        ScoredEntry(String userId, double score) { this.userId = userId; this.score = score; }
    }
}
