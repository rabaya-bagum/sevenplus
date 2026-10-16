# Crash reporting (Sentry, opt-in)

Crash reporting is **off for everyone** until both are true:

1. **This build has a DSN.** Without one, the "Share crash reports" switch is hidden and Sentry
   never starts.
2. **The user turned it on** in Profile → Your data → Share crash reports. It is off by default,
   and "Erase all my data" turns it off again.

## Turn it on for your builds

1. Create a free account at sentry.io → **Create project** → platform **Apple iOS**.
2. Copy the project's **DSN** (Settings → Client Keys). It looks like
   `https://abc123@o456.ingest.us.sentry.io/789`.
3. In Xcode: **SevenPlus target → Info** tab → add a String key `SentryDSN` with that value.
4. In Sentry: **Project Settings → Security & Privacy** → turn on **Prevent storing of IP
   addresses**.
5. Run the app, turn on Profile → Your data → **Share crash reports**, then trigger a test crash
   (for example, temporarily add `fatalError("test")` to a button) and run again: the report
   appears in Sentry after the next launch.

The Sentry Swift package (sentry-cocoa 9.x) is already added to the iPhone target; Xcode
downloads it the first time you open the project.

## What a report contains

Crash stack trace, app version, iPhone model, iOS version and recent screen changes. The app
sets `sendDefaultPii = false` and turns off network breadcrumbs, so no names, IP address,
workout history, health data or challenge links are attached.

## App Store privacy label

Only when the DSN is set in a release build: declare **Diagnostics → Crash Data**, *not linked
to the user* and *not used for tracking*.
