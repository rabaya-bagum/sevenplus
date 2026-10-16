# TestFlight beta checklist

Goal: 10 people using SevenPlus for a week (Phase 0's last item). Tick items off as you go.

## 1. Accounts and app record (once)

- [ ] Join the **Apple Developer Program** (US$99/year) with your Apple ID.
- [ ] Choose the final **bundle ID** (e.g. `com.<yourname>.sevenplus`). If you change it from
      `com.sevenplus.app`, also change the Watch app (`<id>.watchkitapp`) and
      `WKCompanionAppBundleIdentifier` (see `docs/WATCH_SETUP.md`).
- [ ] In Xcode, pick your **Team** on the SevenPlus and SevenPlusWatch targets; let Xcode
      register the IDs and the **HealthKit** capability.
- [ ] **App Store Connect → My Apps → +  New App**: platform iOS, name "SevenPlus" (or your
      final name), primary language, bundle ID, SKU (any text, e.g. `sevenplus-001`).

## 2. Subscriptions

App Store Connect → your app → **Monetization → Subscriptions**:

- [ ] Subscription group **Premium**.
- [ ] `com.sevenplus.premium.monthly`: 1 month, e.g. CA$9.99
- [ ] `com.sevenplus.premium.yearly`: 1 year, e.g. CA$59.99, **introductory offer:
      1-week free trial**
- [ ] Display name and description for each, plus a review screenshot of the paywall.
- [ ] Sign the **Paid Apps agreement** and add banking/tax info (Business section). Purchases
      won't load on TestFlight until this is done.

The product IDs must match `PremiumStore.productIDs` exactly. On TestFlight, purchases are free
sandbox purchases, so testers are never charged.

## 3. Privacy

- [ ] Host the privacy policy (`docs/privacy-policy.md`, with the brackets filled in) at a public
      HTTPS address. GitHub Pages, Notion or your own site all work.
- [ ] Put that address in **App Store Connect → App Information → Privacy Policy URL** and in
      `PrivacyPolicyURL` in `Config/SevenPlus-Info.plist` (shows the link on the paywall and in
      Your data).
- [ ] **App Privacy** questionnaire. Pick the row that matches your build:

| Your build | Answer |
| --- | --- |
| No `SentryDSN` and no `SupabaseURL` (default) | **Data Not Collected** |
| `SentryDSN` set | Diagnostics → **Crash Data**: *not linked to the user, not used for tracking*, purpose *App Functionality* |
| `SupabaseURL` set | Contact Info → **Name** (display name), Identifiers → **User ID** (random id), Health & Fitness → **Fitness** (workout count): each *linked to the user, not used for tracking*, purpose *App Functionality* |

Apple Health data, camera frames and everything stored on the iPhone are **not "collected"** in
Apple's sense, because they never leave the device.

## 4. Upload a build

- [ ] Bump **Version** (e.g. 0.1) and **Build** (1, 2, 3… unique per upload) on the SevenPlus target.
- [ ] Scheme destination **Any iOS Device** → **Product → Archive**.
- [ ] Organizer → **Distribute App → App Store Connect → Upload**.
- [ ] Encryption questions are already answered (`ITSAppUsesNonExemptEncryption = NO`, since the
      app only uses standard HTTPS).
- [ ] Wait for the email "has completed processing" (10–30 min).

## 5. Invite testers

- [ ] **TestFlight → Test Information**: beta description (what to test, see
      `docs/BETA_TESTERS.md`), feedback email, privacy policy URL.
- [ ] **External Testing → + group** "Friends", add the build. The first external build goes
      through a short **Beta App Review** (usually under a day).
- [ ] Turn on the group's **Public Link** and send it with the tester guide. Testers install the
      TestFlight app, open the link, done.
- [ ] (Faster for 1–2 people you trust: add them as App Store Connect users and use **Internal
      Testing**, which needs no review.)

## 6. During the week

- [ ] Day 1: check **TestFlight → Crashes** and screenshot feedback daily.
- [ ] Day 3: short check-in message to testers.
- [ ] Day 7: send the feedback form (`docs/BETA_TESTERS.md`), then collect results.

## Success targets from the spec

- Day-1 return rate above 40%
- 3 or more workouts per tester in the week
- No crash reported twice
- At least 7 of 10 rate the workouts "just right" most of the time
