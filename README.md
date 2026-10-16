# SevenPlus — Phase 0 prototype

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
| `Models/ExerciseLibrary.swift` | 12 classic exercises + quiet-mode swaps |
| `Engine/WorkoutEngine.swift` | Pure `Timeline` (elapsed → phase) + wall-clock engine: pause, resume, skip |
| `Engine/Coach.swift` | Voice cues via AVSpeechSynthesizer, ducks music |
| `Engine/SessionStore.swift` | Local JSON history, streak calculation, quiet-mode setting |
| `Views/HomeView.swift` | Today card, quiet toggle, streak, exercise list, history |
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
