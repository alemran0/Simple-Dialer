# Simple Dialer — Debugging & Known Issues

This document catalogues bugs and feature gaps identified from Android logcat analysis and code
inspection.  **No runtime behaviour is changed here**; this file exists solely to aid future
contributors in reproducing and fixing each issue.

---

## Table of Contents

1. [Skipped Frames / Davey Duration Jank](#1-skipped-frames--davey-duration-jank)
2. [RecyclerView "No adapter attached; skipping layout"](#2-recyclerview-no-adapter-attached-skipping-layout)
3. [SQLite Connection Leak — local_contacts.db](#3-sqlite-connection-leak--local_contactsdb)
4. [Glide: Missing GeneratedAppGlideModule / Empty-Model Load Failures](#4-glide-missing-generatedappglidemodule--empty-model-load-failures)
5. [SIP Registration Timeout then Success — Race / Timeout Handling](#5-sip-registration-timeout-then-success--race--timeout-handling)
6. [InputEventReceiver Disposed Warnings](#6-inputeventreceiver-disposed-warnings)
7. [Hidden-API Reflection Warnings](#7-hidden-api-reflection-warnings)
8. [AudioTrack / ToneGenerator Deprecated Stream Types](#8-audiotrack--tonegenerator-deprecated-stream-types)
9. [SELinux `avc: denied` Entries](#9-selinux-avc-denied-entries)
10. [ActivityThread "no activity for token"](#10-activitythread-no-activity-for-token)
11. [SIP Dialing Does Not Show Proper In-Call UI](#11-sip-dialing-does-not-show-proper-in-call-ui)
12. [Dialpad Missing Third Button for IP / SIP Dialing](#12-dialpad-missing-third-button-for-ip--sip-dialing)

---

## 1. Skipped Frames / Davey Duration Jank

### Log signature
```
Choreographer: Skipped NN frames! The application may be doing too much work on its main thread.
RenderThread: Davey! duration=NNNms
```

### Root cause (code reference)
`DialpadActivity.dialpadValueChanged()` (`app/src/main/kotlin/…/activities/DialpadActivity.kt`,
method `dialpadValueChanged`) performs **full contact filtering** synchronously on the main thread
every keystroke:

```kotlin
val filtered = allContacts.filter { … }.sortedWith(…).toMutableList() as ArrayList<Contact>
```

On devices with large contact lists this can easily consume many milliseconds per frame,
exceeding the 16 ms budget and producing the "Davey" jank reports.

### Reproduction hint
1. Import ≥ 500 contacts.
2. Open the Dialpad screen.
3. Type several characters quickly — observe frame-skip warnings in logcat.

---

## 2. RecyclerView "No adapter attached; skipping layout"

### Log signature
```
RecyclerView: No adapter attached; skipping layout
```

### Root cause (code reference)
`DialpadActivity.onCreate()` attaches the `RecyclerView` (`dialpad_list`) through the XML layout
before any adapter is set.  The adapter is only created inside the `dialpadValueChanged()` callback
which is called from `gotContacts()`, itself delivered asynchronously from `ContactsHelper`.  During
the gap between `setContentView` and the first `gotContacts` delivery the `RecyclerView` will
attempt a layout pass with no adapter, emitting the warning.

Relevant code path:

```
onCreate()
  └─ ContactsHelper.getContacts() (background thread)
       └─ gotContacts() → dialpadValueChanged() → adapter set
```

The `dialpad_list` in `activity_dialpad.xml` already declares
`app:layoutManager="…MyLinearLayoutManager"`, which triggers layout before the adapter arrives.

### Reproduction hint
Open `DialpadActivity` on a device with a moderate number of contacts and watch logcat immediately
after the activity starts.

---

## 3. SQLite Connection Leak — local_contacts.db

### Log signature
```
SQLiteConnectionPool: The connection pool for database 'local_contacts.db' has been unable to grant
a connection to thread ... A connection leak might be occurring ...
```

### Root cause (code reference)
`DialpadActivity.onCreate()` calls:

```kotlin
privateCursor = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
```

The `Cursor` stored in `privateCursor` is opened from the `MyContactsContentProvider` which
accesses `local_contacts.db`.  The activity never explicitly calls `privateCursor?.close()` in
`onDestroy()` (or any lifecycle method), leaving the underlying `SQLiteConnection` unclosed when
the activity is destroyed.

### Reproduction hint
Open and close `DialpadActivity` several times in quick succession, then check logcat for the
`SQLiteConnectionPool` warning.

---

## 4. Glide: Missing GeneratedAppGlideModule / Empty-Model Load Failures

### Log signatures
```
Glide: Failed to find GeneratedAppGlideModule. …
Glide: Load failed for null model
Glide: class com.bumptech.glide.load.engine.GlideException: Failed to load resource
  There is no more source to fetch or decode from
```

### Root cause (code reference)
`App.kt` does **not** implement `AppGlideModule`:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
    }
}
```

Without an `@GlideModule`-annotated `AppGlideModule` subclass, Glide's annotation processor
cannot generate the `GeneratedAppGlideModule` that `Glide.get(context)` expects, so it falls
back to reflection and warns on every `Glide.with()` call.

Additionally, contact avatars loaded through `CallContactAvatarHelper`
(`app/src/main/kotlin/…/helpers/CallContactAvatarHelper.kt`) can pass a `null` URI when no
avatar is available, producing the `"Load failed for null model"` error.

### Reproduction hint
1. Open the Contacts or Recents tab with at least one contact that has no avatar photo.
2. Observe Glide log warnings in logcat.

---

## 5. SIP Registration Timeout then Success — Race / Timeout Handling

### Log signatures
```
SIP_REG: Registration timed out after 30s — transitioning to ERROR
SIP_REG: onRegistered: SIP registration succeeded
```
(The second line may appear after the first when the network is slow.)

### Root cause (code reference)
`SipManagerWrapper` (`app/src/main/kotlin/…/helpers/SipManagerWrapper.kt`) posts a 30-second
timeout runnable:

```kotlin
private val registrationTimeoutRunnable = Runnable {
    if (registrationState == SipRegistrationState.CONNECTING) {
        Log.w(TAG, "Registration timed out after …")
        notifyStateChanged(SipRegistrationState.ERROR)
    }
}
```

If the SIP server responds *after* 30 seconds (e.g., on a slow mobile network), both the
timeout runnable **and** the `onRegistered()` callback fire.  The timeout transitions the state
to `ERROR` first; when `onRegistered()` subsequently fires, it calls `cancelRegistrationTimeout()`
(which is now a no-op) and then transitions the state back to `REGISTERED`.  The UI may briefly
flash an error state before correcting itself, or — if the two runnables execute on different
threads — the final state may be non-deterministic.

`SipRegistrationService` also calls `stopSelf()` when the immediate `register()` call returns
`false`, but the actual SIP engine startup is asynchronous; if `SipRegistrationService` is
destroyed before the async `onRegistered()` callback arrives the service will miss the
registration success and leave the notification stale.

### Reproduction hint
Configure SIP credentials for a server with a response time > 30 seconds, or artificially
throttle the network, then trigger registration via Settings.

---

## 6. InputEventReceiver Disposed Warnings

### Log signature
```
InputEventReceiver: Attempted to finish an input event but the input event receiver has already
been disposed.
```

### Root cause (code reference)
`DialpadActivity.setupCharClick()` attaches a `MotionEvent.ACTION_UP` handler via
`setOnTouchListener`.  If the user lifts their finger after the activity begins to finish (e.g.,
the user presses Back while touching a dialpad key), the touch event may be dispatched to a
`View` whose window has already been detached, causing the `InputEventReceiver` warning.

### Reproduction hint
Start pressing a dialpad key, then immediately press the Back button before lifting the finger.

---

## 7. Hidden-API Reflection Warnings

### Log signature
```
Accessing hidden field / method … (greylist, reflection)
W/System: ClassLoader referenced unknown path: …
```

### Root cause (code reference)
The bundled sipdroid library (`org.sipdroid.*`) uses Java reflection to access internal Android
telephony APIs (e.g., `ITelephony` stubs) that are on the hidden-API grey/dark list.  These
warnings appear at runtime on Android 9+ whenever `Receiver.engine(context)` initialises the SIP
stack (`SipManagerWrapper.register()` and `SipManagerWrapper.placeCall()`).

### Reproduction hint
Enable SIP in Settings and observe logcat during the `StartEngine()` call.

---

## 8. AudioTrack / ToneGenerator Deprecated Stream Types

### Log signature
```
AudioTrack: AUDIO_OUTPUT_FLAG_FAST denied by client; transfer 4, track 0x…
ToneGenerator: startTone() failed, … stream type deprecated
```

### Root cause (code reference)
`ToneGeneratorHelper` (`app/src/main/kotlin/…/helpers/ToneGeneratorHelper.kt`) creates a
`ToneGenerator` using `AudioManager.STREAM_DTMF`:

```kotlin
const val DIAL_TONE_STREAM_TYPE = STREAM_DTMF

private val toneGenerator = ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME)
```

`STREAM_DTMF` is considered a legacy stream type on Android 8+; the `AudioTrack` policy engine
may deny the `FAST` output flag, resulting in the logcat warning.  This does not currently
crash the app because the `ToneGenerator` creation is wrapped in a `try/catch`, but it can
silently disable DTMF tones on some devices.

### Reproduction hint
Open the Dialpad screen on a device running Android 8+ and tap dialpad keys with "dialpad beeps"
enabled; watch logcat for `AudioTrack` warnings.

---

## 9. SELinux `avc: denied` Entries

### Log signature
```
avc: denied { … } for … scontext=… tcontext=… tclass=…
```

### Root cause (code reference)
The sipdroid SIP stack attempts to open raw UDP/TCP sockets for SIP signalling and RTP audio
transport.  On Android 10+ (and hardened vendor builds) these socket operations may be denied
by SELinux policy unless the app holds the correct `INTERNET` permission *and* the SELinux
context allows it.  The most common denied operations are:

- `{ create }` for `udp_socket` / `tcp_socket`
- `{ connectto }` for `unix_stream_socket` (e.g., accessing a system audio service)

These denials can silently break SIP registration or audio routing.

### Reproduction hint
Enable SIP on a device with a strict SELinux policy (e.g., a Google Pixel running a recent
security patch) and check `avc` messages in logcat immediately after tapping the SIP call button.

---

## 10. ActivityThread "no activity for token"

### Log signature
```
ActivityThread: Activity … not found for token android.os.BinderProxy@…
```

### Root cause (code reference)
`SipManagerWrapper.init` block registers a `Receiver.StateListener` that calls
`context.startActivity(SipCallActivity.getIncomingCallIntent(…))` when an incoming SIP call
arrives:

```kotlin
override fun onCallStateChanged(state: Int, caller: String?) {
    when (state) {
        UserAgent.UA_STATE_INCOMING_CALL -> {
            val callIntent = SipCallActivity.getIncomingCallIntent(context, caller ?: "")
            callIntent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(callIntent)
        }
    }
}
```

`SipManagerWrapper` is a singleton holding the **application** `Context`.  If an incoming call
arrives while `SipCallActivity` (or any foreground activity) is being destroyed/recreated (e.g.,
during a configuration change), the `ActivityThread` may log "no activity for token" because
the token in the `ActivityRecord` no longer matches a live window.

### Reproduction hint
Rotate the screen while an incoming SIP call is ringing, or trigger an incoming call during
an activity recreation.

---

## 11. SIP Dialing Does Not Show Proper In-Call UI  *(Feature Gap)*

### Description
When a SIP call is placed, `SipCallActivity` is launched.  Its layout
(`app/src/main/res/layout/activity_sip_call.xml`) shows only:

- Caller URI as plain text
- A status string ("Dialing…", "Ongoing call", etc.)
- An optional call-duration counter
- Accept / End buttons

The standard PSTN `CallActivity` (launched for SIM calls) includes a rich in-call UI with
contact avatar, mute / speaker / hold controls, DTMF in-call dialpad, and proper
status/duration placement.  None of these elements are present in `SipCallActivity`, making
the SIP call experience visually inconsistent and functionally inferior.

### Relevant files
| File | Issue |
|------|-------|
| `app/src/main/res/layout/activity_sip_call.xml` | Minimal layout — no avatar, no action buttons |
| `app/src/main/kotlin/…/activities/SipCallActivity.kt` | No mute/speaker/DTMF handling |
| `app/src/main/kotlin/…/activities/CallActivity.kt` | Reference implementation for full in-call UI |

### Suggested fix direction
Extend `activity_sip_call.xml` with the same controls used in `activity_call.xml` and wire them
up in `SipCallActivity`, ideally by extracting a shared base class or fragment.

---

## 12. Dialpad Missing Third Button for IP / SIP Dialing  *(Feature Gap)*

### Description
`DialpadActivity` conditionally shows a second call button (`dialpad_call_two_button`) when two
SIM cards are detected (`areMultipleSIMsAvailable()`):

```kotlin
val callIconId = if (areMultipleSIMsAvailable()) {
    binding.dialpadCallTwoButton.beVisible()
    binding.dialpadCallTwoButton.setOnClickListener { initCall(dialpadInput.value, 1) }
    R.drawable.ic_phone_one_vector
} else {
    R.drawable.ic_phone_vector
}
```

There is no third button for initiating an IP/SIP call directly from the dialpad.  When SIP is
registered and the user taps a call button, a `SelectCallAccountDialog` chooser is shown instead,
but this chooser is only reached via `initCall()` when `sipWrapper.isRegistered == true` — the
flow is hidden behind a dialog rather than being a first-class dialpad affordance.

### Relevant files
| File | Issue |
|------|-------|
| `app/src/main/kotlin/…/activities/DialpadActivity.kt` | Only 0–2 call buttons; no SIP/IP button |
| `app/src/main/res/layout/activity_dialpad.xml` | No `dialpad_call_sip_button` view |
| `app/src/main/kotlin/…/dialogs/SelectCallAccountDialog.kt` | SIP option hidden in a dialog |

### Suggested fix direction
1. Add a third `ImageView` (`dialpad_call_sip_button`) to `activity_dialpad.xml`, positioned
   symmetrically with the existing two SIM buttons.
2. In `DialpadActivity.onCreate()`, show the SIP button only when `sipWrapper.isRegistered`
   (and hide/show dynamically by listening to `SipManagerWrapper.addStateListener()`).
3. Wire the SIP button's click listener to call `SipCallActivity.getOutgoingCallIntent()` directly,
   bypassing the dialog.
