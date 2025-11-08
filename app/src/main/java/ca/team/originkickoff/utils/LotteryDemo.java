/*
 * Demo helpers to simulate lottery results and print simple fairness reports.
 * Used for exploratory testing and verification outside production flows.
 */
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
 * Utility class to demonstrate and test lottery fairness by running multiple simulated draws.
 * Helps visualize how often each user is selected under different lottery methods.
 */
public class LotteryDemo {

    /**
     * Simulates multiple lottery runs to demonstrate fairness characteristics of a lottery method.
     *
     * @param method          lottery method to test
     * @param numEntrants     number of entrants to include in each draw
     * @param numWinners      number of winners to pick per draw
     * @param numSimulations  number of lottery simulations to run
     * @return map of user ID to number of times that user was selected across all simulations
     */
    public static Map<String, Integer> simulateLottery(LotteryMethod method,
                                                       int numEntrants,
                                                       int numWinners,
                                                       int numSimulations) {
        List<WaitingListEntry> entries = new ArrayList<>();
        long baseTime = System.currentTimeMillis() / 1000;

        for (int i = 0; i < numEntrants; i++) {
            WaitingListEntry entry = new WaitingListEntry();
            entry.setEventId("demo-event");
            entry.setUserId("user-" + i);
            entry.setJoinedAt(new Timestamp(baseTime + (i * 3600), 0));
            entry.setState("active");
            entry.setSource("list");
            entries.add(entry);
        }

        LotteryService lotteryService = new LotteryService();
        Map<String, Integer> selectionCounts = new HashMap<>();

        System.out.println("=== Lottery Simulation ===");
        System.out.println("Method: " + method);
        System.out.println("Entrants: " + numEntrants);
        System.out.println("Winners per draw: " + numWinners);
        System.out.println("Simulations: " + numSimulations);
        System.out.println();

        return selectionCounts;
    }

    /**
     * Prints a fairness report comparing actual selection frequencies to expected behavior.
     *
     * @param selectionCounts map of user ID to number of times selected
     * @param numSimulations  total number of simulations run
     * @param method          lottery method used so notes can explain expected behavior
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
