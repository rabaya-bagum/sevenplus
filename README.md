# SevenPlus

Classic 7-minute workout with a voice coach, background-safe timer, quiet mode, streaks and history.
Requires **Xcode 16+**, iOS 17+.

## Open in Xcode

1. Open `SevenPlus.xcodeproj`.
2. Target **SevenPlus → Signing & Capabilities**: pick your **Team** (and change the bundle ID
   `com.sevenplus.app` if it's taken).
3. Run tests: **⌘U**. Run the app: **⌘R** on a simulator or your iPhone.

The `SevenPlus/` and `SevenPlusTests/` folders are synced groups, so new files you add there
join their target automatically. Background audio (keeps the voice coach going when the screen
locks) is set in `Config/SevenPlus-Info.plist`.

## What's here

| File | Purpose |
| --- | --- |
| `Models/Models.swift` | Exercise, Workout, Session, Difficulty |
| `Models/ExerciseLibrary.swift` | 18 exercises + 4 quiet-mode swaps, classic circuit |
| `Models/ExerciseTraits.swift` | Body area and joint load per exercise |
| `Models/Profile.swift` | Goal, level (intervals), length, limitations |
| `Engine/WorkoutBuilder.swift` | Builds the daily 5–20 min workout from the profile |
| `Views/OnboardingView.swift` | 4-step onboarding |
| `Engine/WorkoutEngine.swift` | Pure `Timeline` (elapsed → phase) + wall-clock engine: pause, resume, skip |
| `Engine/Coach.swift` | Voice cues via AVSpeechSynthesizer, ducks music |
| `Models/PersistentModels.swift` | SwiftData models (sessions, profile) and the container |
| `Engine/SessionStore.swift` | Data facade over SwiftData, streaks, one-time import of Phase 0 data |
| `Engine/Stats.swift` | Daily minutes, totals, best streak |
| `Views/RootTabView.swift` | Today · Workouts · Progress · Profile tabs |
| `Views/TodayView.swift` | Daily workout with length picker and streak |
| `Views/WorkoutsView.swift` | Classic circuit + searchable exercise library by body area |
| `Views/ExerciseDetailView.swift` | Cue, area, impact, joint load, quiet alternative |
| `Views/ProgressTabView.swift` | Streaks, totals, 7-day chart, history |
| `Views/ProfileView.swift` | Goal, level, default length, limitations |
| `Views/PlayerView.swift` | Timer ring, cues, next-up, pause/skip/end |
| `Views/SummaryView.swift` | Time, streak, difficulty rating |

## Design notes

- The timer is computed from wall-clock time, not by counting ticks, so it stays correct after
  backgrounding or a phone call.
- `Timeline` has no clock inside it, so every phase boundary is unit-tested.
- SF Symbols stand in for exercise videos until you record or license them.

## Next steps (Phase 1)

1. SwiftData store + onboarding (goal, level, limitations)
2. Exercise detail screen with video loops
3. Custom workouts and 5/10/15/20-minute lengths
4. Adaptive rules using the difficulty rating
5. HealthKit write + Apple Watch app
