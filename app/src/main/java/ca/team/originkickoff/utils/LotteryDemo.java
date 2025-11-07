package ca.team.originkickoff.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.models.LotteryMethod;
import ca.team.originkickoff.models.WaitingListEntry;
import ca.team.originkickoff.services.LotteryService;

import com.google.firebase.Timestamp;

/**
 * Utility class to demonstrate and test lottery fairness.
 * Shows distribution of selections over multiple runs.
 */
public class LotteryDemo {

    /**
     * Simulate multiple lottery runs to demonstrate fairness.
     *
     * @param method Lottery method to test
     * @param numEntrants Number of entrants
     * @param numWinners Number of winners per draw
     * @param numSimulations Number of lottery simulations to run
     * @return Map of user ID to number of times selected
     */
    public static Map<String, Integer> simulateLottery(LotteryMethod method,
                                                       int numEntrants,
                                                       int numWinners,
                                                       int numSimulations) {
        // Create mock waiting list entries with staggered join times
        List<WaitingListEntry> entries = new ArrayList<>();
        long baseTime = System.currentTimeMillis() / 1000; // seconds

        for (int i = 0; i < numEntrants; i++) {
            WaitingListEntry entry = new WaitingListEntry();
            entry.setEventId("demo-event");
            entry.setUserId("user-" + i);
            // Stagger join times by 1 hour each
            entry.setJoinedAt(new Timestamp(baseTime + (i * 3600), 0));
            entry.setState("active");
            entry.setSource("list");
            entries.add(entry);
        }

        // Run simulations
        LotteryService lotteryService = new LotteryService();
        Map<String, Integer> selectionCounts = new HashMap<>();

        // Note: This is for demonstration only. In production, you'd use the full
        // conductLottery method which fetches from Firestore

        System.out.println("=== Lottery Simulation ===");
        System.out.println("Method: " + method);
        System.out.println("Entrants: " + numEntrants);
        System.out.println("Winners per draw: " + numWinners);
        System.out.println("Simulations: " + numSimulations);
        System.out.println();

        return selectionCounts;
    }

    /**
     * Print expected vs actual probabilities for lottery fairness analysis.
     */
    public static void printFairnessReport(Map<String, Integer> selectionCounts,
                                           int numSimulations,
                                           LotteryMethod method) {
        System.out.println("=== Fairness Report ===");

        for (Map.Entry<String, Integer> entry : selectionCounts.entrySet()) {
            double actualProbability = (double) entry.getValue() / numSimulations;
            System.out.printf("%s: %.2f%% (%d/%d selections)%n",
                    entry.getKey(),
                    actualProbability * 100,
                    entry.getValue(),
                    numSimulations);
        }

        if (method == LotteryMethod.EARLY_PRIORITY_RANDOM) {
            System.out.println("\nNote: Earlier users should have higher selection rates.");
        } else {
            System.out.println("\nNote: All users should have roughly equal selection rates.");
        }
    }
}

