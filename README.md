# origin-kickoff
## Project Part 1 

## Team Members

| Student Name            | CCID       |
| ----------------------- | ---------  |
| `Gurmanpreet Tiwana`    | `gtiwana`  |
| `Karan Brar`            | `karan4`   |
| `Sargundeep Singh`      | `sargunde` |
| `Ranbir Singh`          | `ranbirsi` |
| `Amitoj Singh`          | `amitoj3`  |
| `Anhad Singh Sarna`     | `assarna`  |

---

## Profile Screen (new)

- Activity: `ca.team.originkickoff.ProfileActivity`
- Layout: `res/layout/activity_profile.xml`
- Strings/Colors: added in `res/values/strings.xml` and `res/values/colors.xml`
- Registered in `AndroidManifest.xml` and wired from Main screen bottom bar (Profile icon).

Features:
- Avatar stub, name/email text
- Won/Lost Lottery Update toggles with SharedPreferences persistence
- Event history sample cards
- Delete Profile (clears local profile prefs) and Log Out placeholder
- Shows device ID at bottom

### Build notes
This project requires JDK 11+ for the Android Gradle Plugin.

Windows (cmd.exe) quick build:
```
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew.bat clean assembleDebug
```

Run and open the app, then tap the Profile icon in the bottom bar to see the screen.
