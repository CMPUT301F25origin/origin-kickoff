# Lottery System Implementation

## Overview
The lottery system provides two fair methods for selecting winners from event waiting lists:

1. **RANDOM** - Pure random selection using cryptographically secure randomness
2. **EARLY_PRIORITY_RANDOM** - Weighted selection favoring earlier entrants

## Architecture

### Core Components

#### 1. LotteryMethod (Enum)
- `RANDOM`: Equal probability for all entrants
- `EARLY_PRIORITY_RANDOM`: Earlier entrants get exponentially higher weights

#### 2. LotteryService
Implements the core lottery algorithms:

**Random Selection:**
- Uses `SecureRandom` for cryptographic security
- Fisher-Yates shuffle algorithm ensures uniform distribution
- All entrants have exactly equal probability

**Early Priority Random:**
- Weight formula: `w = e^(-0.5 * normalizedTime)`
  - normalizedTime ∈ [0, 1]: 0 = earliest, 1 = latest
  - Earliest entrant has weight = 1.0
  - Latest entrant has weight ≈ 0.6
- Uses Efraimidis-Spirakis algorithm for weighted sampling without replacement
- Provably fair and efficient O(n log n)

#### 3. LotteryOrchestrator
Manages the complete lottery workflow:
- Validates lottery hasn't been conducted yet
- Retrieves active waiting list entries
- Conducts the draw
- Saves results to Firestore
- Provides winner lookup methods

#### 4. LotteryResult (Model)
Stores lottery results in Firestore for audit trail:
- Event ID
- Timestamp
- Method used
- Total entrants
- Number of winners
- Winner IDs
- Organizer who conducted it

## Usage

### Conducting a Lottery

```java
LotteryOrchestrator orchestrator = new LotteryOrchestrator();

// For pure random
orchestrator.conductLottery(
    eventId,
    organizerId,
    numWinners,
    LotteryMethod.RANDOM
).addOnSuccessListener(result -> {
    // Lottery completed
    List<String> winners = result.getWinnerIds();
    // Notify winners...
}).addOnFailureListener(e -> {
    // Handle error
});

// For early priority random
orchestrator.conductLottery(
    eventId,
    organizerId,
    numWinners,
    LotteryMethod.EARLY_PRIORITY_RANDOM
).addOnSuccessListener(result -> {
    // Process winners
});
```

### Checking Lottery Status

```java
// Check if lottery conducted
orchestrator.hasLotteryBeenConducted(eventId)
    .addOnSuccessListener(conducted -> {
        if (conducted) {
            // Lottery already done
        }
    });

// Check if user is a winner
orchestrator.isWinner(eventId, userId)
    .addOnSuccessListener(isWinner -> {
        if (isWinner) {
            // Show winner status
        }
    });

// Get full lottery result
orchestrator.getLotteryResult(eventId)
    .addOnSuccessListener(result -> {
        // Display results
    });
```

## Fairness & Security

### Random Method
- **Cryptographically Secure**: Uses `java.security.SecureRandom`
- **Uniform Distribution**: Fisher-Yates shuffle guarantees equal probability
- **Unbiased**: No preference based on any attribute

### Early Priority Random Method
- **Predictable Advantage**: Earlier entrants have quantifiable higher chance
- **Still Fair**: All entrants have non-zero probability
- **Mathematically Sound**: Uses proven Efraimidis-Spirakis algorithm
- **Transparent Formula**: Weight decay is documented and auditable

### Audit Trail
- All lottery results stored in Firestore
- Timestamp of when conducted
- Who conducted it (organizer ID)
- Method used
- Complete winner list
- Cannot be re-conducted once completed

## Algorithm Details

### Fisher-Yates Shuffle (Random Method)
```
for i from n-1 down to 1:
    j = random(0, i+1)
    swap array[i] with array[j]
return first k elements
```
Time: O(n), Space: O(n)

### Efraimidis-Spirakis (Early Priority Method)
```
for each item with weight w:
    score = random^(1/w)
    
sort by score descending
return top k items
```
Time: O(n log n), Space: O(n)

Why this works: Items with higher weight have higher expected scores.

## Weight Distribution Example

For 10 entrants joining 1 hour apart (Early Priority Random):

| Position | Time (hrs) | Normalized | Weight | Relative Probability |
|----------|-----------|------------|--------|---------------------|
| 1st      | 0         | 0.00       | 1.00   | 1.00x              |
| 2nd      | 1         | 0.11       | 0.95   | 0.95x              |
| 3rd      | 2         | 0.22       | 0.90   | 0.90x              |
| 5th      | 4         | 0.44       | 0.81   | 0.81x              |
| 10th     | 9         | 1.00       | 0.61   | 0.61x              |

The 1st entrant is 1.64x more likely to be selected than the 10th entrant.

## Firestore Collections

### lottery_results
Document ID: `{eventId}`
```json
{
  "event_id": "string",
  "conducted_at": "timestamp",
  "lottery_method": "random | early_priority_random",
  "total_entrants": "number",
  "num_winners": "number",
  "winner_ids": ["string array"],
  "conducted_by": "string (organizer ID)"
}
```

## Error Handling

The system handles:
- ✓ Lottery already conducted (prevents duplicate draws)
- ✓ No active entrants
- ✓ Invalid number of winners
- ✓ Firestore failures
- ✓ Task failures propagated to caller

## Future Enhancements

Potential improvements:
1. **Commit-Reveal**: Multi-party randomness for increased trust
2. **Blockchain Verification**: Store hash on public ledger
3. **Custom Weights**: Allow organizers to set decay factor
4. **Location-based Priority**: Weight by geolocation if required
5. **Notification Integration**: Auto-notify winners via push notifications
6. **Appeal System**: Allow users to contest results within time window

## Testing

To test lottery fairness, use the `LotteryDemo` utility:
```java
Map<String, Integer> results = LotteryDemo.simulateLottery(
    LotteryMethod.RANDOM,
    100,  // entrants
    10,   // winners
    1000  // simulations
);
LotteryDemo.printFairnessReport(results, 1000, LotteryMethod.RANDOM);
```

Expected results:
- **Random**: All entrants ~10% selection rate (10/100)
- **Early Priority**: Earlier entrants have higher selection rates

## Integration Checklist

To integrate into UI:
- [ ] Add lottery button to organizer event detail view
- [ ] Create lottery configuration dialog (select method)
- [ ] Display lottery results screen
- [ ] Show winner/loser status to entrants
- [ ] Send notifications to winners
- [ ] Update event status after lottery
- [ ] Add lottery history view for organizers

