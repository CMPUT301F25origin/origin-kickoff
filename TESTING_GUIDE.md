# Testing Guide

## Problem: Tests Taking Too Long

Your project has two types of tests that behave very differently:

### 1. Unit Tests (Fast - runs in seconds)
- Located in: `app/src/test/`
- Run on the JVM (your computer)
- Examples: `LotteryMethodTest`, `UserTest`, `EventTest`
- **These are FAST** ✅

### 2. Instrumented Tests (Slow - requires emulator)
- Located in: `app/src/androidTest/`
- Require an Android emulator or device to run
- Connect to Firebase/Firestore
- Examples: `DeclineResamplingServiceTest`, `AcceptInvitationServiceTest`, `MainActivitySmokeTest`
- **These are SLOW** ⚠️ (7+ minutes if emulator not ready)

## Solutions

### Quick Fix: Run Only Unit Tests
```bash
./gradlew test
```
This runs ONLY the fast unit tests in `src/test/`. Should complete in under a minute.

### Run Instrumented Tests (Requires Setup)
```bash
./gradlew connectedAndroidTest
```

**Requirements:**
1. Start an Android emulator first, OR
2. Connect a physical Android device
3. (Optional) Start Firebase emulator if tests need it:
   ```bash
   firebase emulators:start --only firestore
   ```

### Run Specific Test Class
```bash
# Unit test (fast)
./gradlew test --tests ca.team.originkickoff.models.LotteryMethodTest

# Instrumented test (requires emulator)
./gradlew connectedAndroidTest --tests ca.team.originkickoff.AcceptInvitationServiceTest
```

## Why Were Tests Hanging?

Your instrumented tests in `androidTest/` were:
1. Waiting for an Android emulator (that wasn't running)
2. Trying to connect to Firebase/Firestore emulator at `10.0.2.2:8080` (not running)
3. Eventually timing out after several minutes

## Recommendations

1. **Use `./gradlew test` for quick development** - Only runs unit tests
2. **Use instrumented tests sparingly** - They're slow and require setup
3. **Consider mocking Firebase in unit tests** - Most logic can be tested without real Firebase
4. **Run instrumented tests in CI/CD** - Better suited for automated testing with proper setup

## Performance Tips

Add these to `gradle.properties` for faster builds:
```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

