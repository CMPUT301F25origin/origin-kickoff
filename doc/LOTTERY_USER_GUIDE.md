# Using the Lottery System in the App

## Quick Start for Organizers

### Step 1: Navigate to Your Event
1. Open the app and go to "My Events"
2. Select an event you've organized
3. You'll see the "Manage Lottery" button (it replaced the lottery criteria button for organizers)

### Step 2: Open Lottery Management
- Tap the **"Manage Lottery"** button
- This opens the ManageLotteryActivity screen

### Step 3: Conduct the Lottery

#### First Time (Lottery Not Conducted)
You'll see:
- Event name and capacity
- Total number of entrants
- "Conduct Lottery" button

**To Run the Lottery:**
1. Tap **"Conduct Lottery"**
2. A dialog appears asking you to choose a method:
   - **Pure Random**: All entrants have equal probability
   - **Early Priority Random**: Earlier entrants get ~1.6x higher weight
3. Select your preferred method
4. Tap **"Conduct Lottery"** in the dialog
5. Confirm your choice in the confirmation dialog
6. The lottery will run and select winners automatically!

#### After Lottery is Conducted
You'll see:
- Lottery status: "Completed" ✅
- Method used (Pure Random or Early Priority Random)
- Timestamp of when it was conducted
- Number of winners selected
- "Show Winners" button to view the list

### Step 4: View Winners
- Tap **"Show Winners"** to expand the winners list
- Each winner shows their position (#1, #2, etc.) and User ID
- Tap again to collapse the list

## Important Notes

⚠️ **The lottery can only be conducted once per event** - This prevents manipulation and ensures fairness

✅ **Results are permanently stored** - All lottery results are saved in Firestore with an audit trail including:
- Who conducted it (organizer ID)
- When it was conducted
- Method used
- Complete list of winners

🔒 **Cryptographically secure** - Uses SecureRandom for unpredictable, fair selection

## Lottery Methods Explained

### Pure Random
- **Best for**: Equal opportunity events
- **How it works**: Fisher-Yates shuffle algorithm
- **Probability**: Everyone has exactly the same chance
- **Example**: 10 spots, 100 entrants = everyone has 10% chance

### Early Priority Random
- **Best for**: Rewarding early sign-ups
- **How it works**: Exponential weight decay based on join time
- **Probability**: Earlier entrants get higher weights
- **Formula**: `weight = e^(-0.5 × normalizedTime)`
- **Example**: 
  - 1st entrant: 100% weight (1.0x)
  - 5th entrant: 81% weight (0.81x)
  - 10th entrant: 61% weight (0.61x)
- **Fairness**: First entrant is ~1.6x more likely than last, but everyone still has a chance

## Integration Status

### ✅ Completed
- [x] Lottery algorithm implementation (Random + Early Priority)
- [x] LotteryOrchestrator service
- [x] ManageLotteryActivity UI
- [x] Lottery method selection dialog
- [x] Winners display
- [x] Result persistence in Firestore
- [x] Integration with "Manage Lottery" button
- [x] One-time lottery enforcement
- [x] Android Manifest registration

### 🔄 To Build
To see the UI working, you need to rebuild the project so Android Studio generates the R.java file with the layout resource IDs:

**In Android Studio:**
1. Click **Build** → **Clean Project**
2. Then **Build** → **Rebuild Project**
3. Wait for the build to complete

**Or via command line:**
```bash
cd /Users/gurmannnpreet/AndroidStudioProjects/origin-kickoff
./gradlew clean assembleDebug
```

### 📋 Future Enhancements (Optional)
- [ ] Send push notifications to winners
- [ ] Send notifications to non-winners
- [ ] Display winner names (instead of just IDs)
- [ ] Export lottery results to CSV
- [ ] Show lottery history for organizers
- [ ] Allow entrants to see if they won
- [ ] Add winner acceptance/decline flow

## Testing the Lottery

### Manual Testing Steps
1. Create a test event as an organizer
2. Join the waitlist with multiple test accounts (or have friends join)
3. Go to event details as the organizer
4. Tap "Manage Lottery"
5. Conduct lottery with one of the methods
6. Verify winners are selected correctly
7. Try to conduct lottery again - should see error preventing duplicate draw

### Verifying Fairness
Run the lottery multiple times with different test events to verify:
- **Pure Random**: Winners are different each time, no pattern
- **Early Priority**: Earlier entrants appear more frequently as winners

## Firestore Data Structure

### Collection: `lottery_results`
Document ID: `{eventId}`
```json
{
  "event_id": "abc123",
  "conducted_at": Timestamp,
  "lottery_method": "random" | "early_priority_random",
  "total_entrants": 50,
  "num_winners": 10,
  "winner_ids": ["userId1", "userId2", ...],
  "conducted_by": "organizerUserId"
}
```

## Troubleshooting

### "Cannot resolve symbol 'activity_manage_lottery'" error
**Solution**: Rebuild the project to generate R.java
- Build → Clean Project
- Build → Rebuild Project

### "Lottery has already been conducted" error
**Expected behavior** - This prevents multiple draws for fairness
**If you need to test again**: Create a new test event or delete the lottery_results document from Firestore

### No entrants in waiting list
**Error message**: "No active entrants in waiting list"
**Solution**: Have users join the waiting list before conducting lottery

### Winners list is empty
Check that:
- Event capacity > 0
- Entrants actually joined the waiting list
- Lottery was conducted successfully (check Firestore lottery_results collection)

## Code Files Created

### Core Lottery Logic
- `models/LotteryMethod.java` - Enum for lottery methods
- `services/LotteryService.java` - Core algorithms
- `services/LotteryOrchestrator.java` - Workflow orchestration
- `models/LotteryResult.java` - Result model

### UI Components
- `ManageLotteryActivity.java` - Main lottery management screen
- `adapters/WinnersAdapter.java` - RecyclerView adapter for winners
- `layout/activity_manage_lottery.xml` - Main activity layout
- `layout/dialog_lottery_method.xml` - Method selection dialog
- `layout/item_winner.xml` - Winner list item

### Documentation
- `doc/lottery-implementation.md` - Technical documentation
- `doc/LOTTERY_USER_GUIDE.md` - This guide

## Support

For technical details about algorithms and implementation, see:
`doc/lottery-implementation.md`

