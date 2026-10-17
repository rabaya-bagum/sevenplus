# SevenPlus for Android

Kotlin + Jetpack Compose port of the iOS app (same features except the Apple Watch app).

| Module | What |
| --- | --- |
| `core/` | Pure Kotlin domain logic shared by everything: exercise library, workout builder, timer engine, adaptive rules, stats, free-tier rules, calories, challenges, export format, rep counter. Unit-tested on the JVM. |
| `app/` | The Android app. `contracts/` defines the interfaces between areas; `data/`, `health/`, `reminders/` (storage and system), `billing/`, `challenges/`, `crash/`, `reps/` (services), `ui/` (screens). |

The 3D exercise clips are not copied: `app` reads `../SevenPlus/Resources/ExerciseClips` (shared with iOS) as its assets.

## Build

Android Studio (latest), or from the command line with JDK 21:

```bash
cd android
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
```

Optional services use the same keys as iOS, set in `gradle.properties` (or `~/.gradle/gradle.properties`):
`sevenplus.supabaseUrl`, `sevenplus.supabaseAnonKey`, `sevenplus.sentryDsn`, `sevenplus.privacyPolicyUrl`.
