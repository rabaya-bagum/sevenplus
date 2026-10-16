# Apple Watch app

The Watch app is the `SevenPlusWatch` target in `SevenPlus.xcodeproj`; its code is in
`SevenPlusWatch/`. It is embedded in the iPhone app, so installing the iPhone app on a phone
with a paired Watch installs it too.

- **Requirements:** the watchOS platform in Xcode (Xcode → Settings → Components). Without it
  neither the iPhone nor the Watch scheme builds.
- **Shared code:** `SevenPlus/Engine/WatchProtocol.swift` is compiled into both apps (an
  exception on the `SevenPlus` synced folder adds it to the Watch target).
- **Config:** `Config/SevenPlusWatch-Info.plist` (heart-rate usage text, Workout Processing
  background mode) and `Config/SevenPlusWatch.entitlements` (HealthKit).
- **Signing:** pick your Team on the `SevenPlusWatch` target too. Its bundle ID must stay
  `<iPhone bundle ID>.watchkitapp`, and `WKCompanionAppBundleIdentifier` must match the
  iPhone bundle ID, so change both if you change `com.sevenplus.app`.
- **Run:** pick the **SevenPlusWatch** scheme and a Watch simulator paired with an iPhone
  simulator. Start a workout on the phone: the watch shows the exercise, countdown, next move
  and heart rate, and its Pause/Skip buttons control the phone.

How it works: the phone sends the player state on every stage change (application context +
live message); the watch counts down locally in between. While a workout is active the watch
runs a HealthKit workout session for live heart rate and sends it to the phone. The phone
saves the finished workout to Health, so the watch discards its own copy (no duplicates).
