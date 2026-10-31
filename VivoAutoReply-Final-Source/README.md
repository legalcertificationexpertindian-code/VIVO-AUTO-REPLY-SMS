# Vivo Auto SMS Reply — Advanced source project

Android Studio Kotlin project targeted at Vivo T4 Pro / Android 16 / OriginOS 6. **Source only; not compiled or tested on the phone.**

Features: master ON/OFF, seven independent day switches with start/end times and editable messages, Friday lunch override, one-date festival/holiday override, SIM subscription ID selection, and 4-hour duplicate-number suppression. Priority: holiday > Friday lunch > daily schedule. Defaults: Sunday enabled, all other daily schedules disabled, Friday lunch enabled, holiday disabled.

## Build
Open this folder in Android Studio, install SDK 35, sync Gradle, and Build > Build APK(s). This repository does not include a Gradle wrapper or a prebuilt APK. Install the debug APK manually after building. Grant phone state, call log and SMS permissions. On OriginOS, review background/autostart/battery restrictions.

## Critical limitations
Android 16 may restrict access to call logs/SMS permissions; Google Play distribution is subject to restricted-permission policy. PHONE_STATE_CHANGED is not a guaranteed immediate or persistent missed-call trigger. Some Vivo firmware may suppress broadcasts or defer call-log writes. This implementation checks the call log 1.8 seconds after an unanswered ringing-to-idle transition and sends only when the call-log entry is marked MISSED. Rejected calls may be classified differently. If the call-log entry is not yet available, the reply will not send. No guaranteed instant delivery. SMS is sent using the handset SIM, subject to carrier costs, anti-spam controls, and signal. SMS sent-result tracking and robust history UI are not implemented. Avoid testing with real customers until verified on-device. The app has no server and no cloud dependency.

## Known engineering follow-ups
Test on Vivo T4 Pro. Consider retrying call-log lookup via scheduled work, add sent/delivery PendingIntents, add proper subscription picker, test multi-SIM broadcasts, improve call state handling across process death, and validate SMS sending limits. Verify Kotlin/Gradle compatibility during Android Studio sync.


## Final-source revision (not a compiled APK)
- Supports saved and unsaved callers equally; no contacts filtering.
- Tries reading the updated missed-call log at approximately 0.7, 1.7, 3.2 and 5.2 seconds after IDLE. Android may delay or suppress broadcasts and background execution.
- Daily toggles, message and HH:mm window; holiday override; Friday lunch override; master toggle.
- Last SMS submission status is visible in the app; **submission is not delivery confirmation**.
- Same-number cooldown is four hours.
- Call-log restrictions mean this app may not work reliably on Android 16 without device-specific testing and potentially changing the architecture/permissions.
- Do not assume Play Store approval for restricted SMS and Call Log permissions.
- This environment does not have an Android SDK or Gradle installation; compilation, signing and testing remain necessary.
