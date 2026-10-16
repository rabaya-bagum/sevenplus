# Apple Watch app — one-time Xcode setup (≈ 2 minutes)

The Watch code is in `SevenPlusWatch/`. Xcode needs a Watch target to build it.
Creating a target by hand-editing the project file is error-prone, so do it once in Xcode:

1. **File → New → Target… → watchOS → App** → Next.
   - Product Name: `SevenPlusWatch`
   - Watch App for: **Existing iOS App** → `SevenPlus`
   - Interface: SwiftUI · uncheck tests (optional) → Finish. Activate the scheme if asked.
2. Xcode creates a group/folder like `SevenPlusWatch Watch App` with `ContentView.swift` and an
   app file. **Delete both generated Swift files** (Move to Trash).
3. Right-click that group → **Add Files to "SevenPlus"…** → select the three files in
   `SevenPlusWatch/` (`SevenPlusWatchApp.swift`, `WatchModel.swift`, `WatchPlayerView.swift`).
   Untick "Copy items", tick only the **watch target** → Add.
4. Select `SevenPlus/Engine/WatchProtocol.swift` → **File inspector → Target Membership** →
   also tick the **watch target** (it is shared by both apps).
5. Watch target → **Signing & Capabilities**:
   - pick your Team
   - **+ Capability → HealthKit**
   - **+ Capability → Background Modes** → tick **Workout Processing**
6. Watch target → **Info** tab → add
   `Privacy - Health Share Usage Description` = "SevenPlus reads your heart rate during workouts."
7. Run the **SevenPlusWatch** scheme on a paired iPhone + Watch simulator. Start a workout on the
   phone: the watch shows the exercise, countdown, next move and heart rate, and its Pause/Skip
   buttons control the phone.

How it works: the phone sends the player state on every stage change (application context +
live message); the watch counts down locally in between. While a workout is active the watch
runs a HealthKit workout session for live heart rate and sends it to the phone. The phone
saves the finished workout to Health, so the watch discards its own copy (no duplicates).
