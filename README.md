# ROBO NEON CONTROL — Complete Action Panel (Android source)

Native Kotlin Android Studio project for the ROBO ESP32-WROOM-32 firmware over Bluetooth Classic SPP.

## Open and build
1. Open this folder in Android Studio.
2. Allow Gradle sync (Android Gradle Plugin / Kotlin plugin from the project files; compile SDK 35).
3. Pair the ESP32 Bluetooth device named `ROBO` in Android Bluetooth settings.
4. Run the app and grant Nearby Devices / Bluetooth permission.
5. Tap CONNECT, select the paired ROBO device, then use the control panels.

This is an Android Studio source project, not a prebuilt APK.

## App panels and supported actions

- **Quick Dashboard:** Bluetooth, Manual, Auto, Object Follow, Obstacle Avoid, Dance, Salute, Head Control, Relax, Game Center, Voice Mode, Chat Mode, Singing Mode, Safe / Stop.
- **Manual Drive & Servos:** Forward `F`, Back `B`, Left `L`, Right `R`, Stop `S`; left hand up/down `H`/`h`; right hand up/down `J`/`j`; both hands up/down `K`/`k`; head left/center/right `Q`/`C`/`E`.
- **Game Center:** all 16 game tiles. The app sends `N` to enter Game Center, waits 900 ms, then sends the selected game command: `! @ # $ % ^ & * ( ) _ + = ? ~ :`.
- **Game controls:** `2` up, `4` left, `6` right, `8` down, `5` action/fire. Tic-Tac-Toe keeps its 1–9 board. `S` pauses/resumes; `X` exits to Safe Mode.
- **Conversation:** select Chat Mode first, then use phrase keys `1–9`, `0`, `A–J` for tracks 0064–0083 and `K–M` for Dad/Mom/Friend greetings (0084–0086). Track 0087 is the Chat Mode activation announcement.
- **Voice Mode action panel:** select Voice Mode first; `S` salute, `D` dance, `L`/`R` head left/right, `C` center, `F` forward, `B` backward.
- **Audio & Dance:** Singing Mode `Z` uses random songs 0098–0107. Dance Mode `D` uses random songs 0098–0107 and the robot's GPIO19 microphone sound pulses to trigger dance movements. The songs are not Game Center background music.
- **Feedback:** the app listens for text status messages sent by the firmware over Bluetooth and displays the latest message in the connection status area.
- **Emergency:** `X` enters Safe Mode.

## Bluetooth command mapping

| Action | Command |
|---|---|
| Bluetooth / Manual / Auto / Follow / Avoid | `W` / `M` / `A` / `O` / `P` |
| Dance / Salute / Head / Relax / Game / Voice / Chat / Singing / Safe | `D` / `T` / `G` / `I` / `N` / `Y` / `U` / `Z` / `X` |
| Drive forward / back / left / right / stop | `F` / `B` / `L` / `R` / `S` |
| Left hand up/down; right hand up/down; both up/down | `H`/`h`; `J`/`j`; `K`/`k` |
| Head left / center / right | `Q` / `C` / `E` |
| Game selection | `N` followed by one of `! @ # $ % ^ & * ( ) _ + = ? ~ :` |
| Directional game controls / action | `2` / `4` / `6` / `8` / `5` |
| Tic-Tac-Toe board cell | `1`–`9` |
| Pause/resume / exit to Safe Mode | `S` / `X` |
| Conversation phrase bank | `1`–`9`, `0`, `A`–`M` while Chat Mode is active |

## Compatibility notes

- This app is matched to `ROBO_FINAL_LOCKED_16_GAMES_DANCE_AUDIO_0001_0108.ino` supplied in this work session.
- Game selection is routed directly into the firmware's dedicated 16-game Game Center; it no longer uses the old Relax Mode handshake.
- The app only exposes commands supported by that firmware. It does not add a camera screen because the camera idea was set aside.
- Bluetooth status display depends on the firmware sending `SerialBT.println(...)` status messages. It shows the latest received message, not continuous sensor telemetry.
- Movement/servo actions are sent as discrete commands. The firmware remains responsible for motor timing, autonomous modes, sensor logic, dance timing, game rendering and safety.
- Verify the app on an Android phone and test the robot with wheels lifted before motor-control testing.

## GitHub Actions APK build

This project includes `.github/workflows/android.yml`.

1. Create a GitHub repository and push the contents of this project.
2. Push to `main` or `master`, or run the workflow manually from the **Actions** tab.
3. Open **Actions** and select **Build ROBO NEON CONTROL APK**.
4. After a successful run, download the artifact named **ROBO-NEON-CONTROL-debug-apk**.
5. The artifact contains `app-debug.apk`.

This workflow creates a debug APK, not a signed release APK.


## JVM target compatibility fix

The app module sets both Java `sourceCompatibility`/`targetCompatibility` and Kotlin `jvmTarget` to 17 in `app/build.gradle.kts`. Keep these targets aligned if you change the JDK or compiler configuration. The GitHub Actions workflow uses JDK 17 and Gradle 8.7.
