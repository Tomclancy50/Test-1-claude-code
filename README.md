# Family WiFi Sync

Two small Android apps that copy your WiFi networks (name + password) from your
phone to your kids' Kindle Fire tablets over **Bluetooth**, so the tablets
auto-connect to your networks without you typing passwords on every device.

- **`parent-app`** — runs on your phone. You keep a list of WiFi networks and
  send them to a tablet.
- **`kid-app`** — runs on each Kindle Fire. It receives the networks and installs
  them so the tablet connects automatically when in range.
- **`shared`** — the data model + the Bluetooth (RFCOMM/SPP) protocol both apps speak.

---

## Important: the password reality on Android

**Android does not let any app read your already-saved WiFi passwords unless the
phone is rooted.** Those passwords live in a system file only `root` can read.
There is no API and no permission that exposes them to a normal app — this is an
OS security wall, not something the app can work around.

So the parent app works by letting you **enter each network's password once**.
To make that easy:

- The "Add network" form **pre-fills the name of the network your phone is
  currently on**, so you usually just type/paste the password.
- You can see a network's password on your phone any time via
  **Settings → WiFi → tap the network → Share** (it shows the password and a QR code).

You build the list once; after that, sending it to a tablet is a couple of taps.

## Why Bluetooth (not WiFi) for the transfer

A WiFi transfer would need both devices already on the same network — but getting
the tablet *onto* WiFi is the whole point. Bluetooth needs no existing network,
and Bluetooth pairing gives the transfer link-layer encryption for free.

---

## How the two apps talk

```
 Parent phone (client)                         Kindle Fire (server)
 ┌───────────────────┐    Bluetooth RFCOMM     ┌───────────────────┐
 │ pick a paired     │ ───── SyncRequest ────► │ "Ready to receive"│
 │ Kindle, tap Send  │      (networks, JSON)   │ installs into WiFi│
 │                   │ ◄──── SyncResult ────── │ replies w/ result │
 └───────────────────┘    (installed count)    └───────────────────┘
```

Wire format is a 4-byte length prefix + UTF-8 JSON (`shared/.../SppProtocol.kt`).

### How networks get installed on the tablet

The kid app picks the right method automatically based on the Fire OS / Android
version (`kid-app/.../WifiInstaller.kt`):

| Fire OS / Android on the tablet | Method | Behaviour |
| --- | --- | --- |
| Android **9 and older** (older Fire tablets) | `WifiManager.addNetwork` | Networks are saved; tablet connects **silently/automatically**. |
| Android **10+** (newer Fire OS) | `WifiNetworkSuggestion` | Tablet shows a **one-time "allow this network?"** prompt the first time it's in range, then auto-connects. This is the best a non-root app can do on modern Android. |

(WEP networks can only be installed on the older path; they're skipped on the
newer one, which the result message tells you.)

---

## Building

You need **Android Studio** (Ladybug / 2024.2 or newer) or a local Gradle 8.10+
with the Android SDK (compileSdk 34, JDK 17).

1. Open the project root in Android Studio and let it sync.
2. Select the **`parent-app`** run configuration → build/install to your phone.
3. Select the **`kid-app`** run configuration → build the APK for the Kindles.

From the command line (after generating the Gradle wrapper jar with
`gradle wrapper`, or using a local `gradle`):

```bash
./gradlew :parent-app:assembleDebug   # parent-app/build/outputs/apk/debug/*.apk
./gradlew :kid-app:assembleDebug      # kid-app/build/outputs/apk/debug/*.apk
```

### Installing the kid app on a Kindle Fire (sideloading)

Fire tablets don't have the Google Play Store, so install the APK directly:

1. On the Kindle: **Settings → Security & Privacy → Apps from Unknown Sources**
   (or **Settings → Device Options → Developer Options**) and allow installs.
2. Get `kid-app-debug.apk` onto the tablet (USB cable, email, or
   `adb install kid-app-debug.apk`) and tap it to install.
3. Repeat for each child's tablet.

---

## Using it

**One-time pairing (per tablet):**
1. On the Kindle, open **WiFi Sync — Kid** → tap **Make findable (pairing)**.
2. On your phone, open **WiFi Sync — Parent** → **Send to a Kindle** → **Scan** →
   tap the Kindle → accept the pairing request on **both** screens.

**Every sync after that:**
1. Phone: add/confirm the networks you want to share.
2. Kindle: tap **Ready to receive**.
3. Phone: **Send to a Kindle** → pick the tablet → **Send**.
4. The tablet installs the networks. On newer Fire OS, tap the one-time "allow"
   prompt when you're near each network the first time.

---

## Security & privacy notes

- Passwords are stored **encrypted at rest** on the phone
  (`EncryptedSharedPreferences`, AES-256, key in the Android Keystore) and are
  **excluded from cloud/device-transfer backups**.
- The transfer rides on a **paired** Bluetooth link, so a random nearby device
  can't grab your credentials — only a tablet you've paired with.
- The kid app does not keep a plaintext copy of passwords; it hands them to the
  system WiFi service and discards them.
- These are **debug**-signed builds. For longer-term use, sign release builds
  with your own keystore.

## Known limitations

- Can't auto-read saved passwords without root (see above) — manual entry by design.
- WPA/WPA2/WPA3-personal and open networks are supported. **Enterprise (802.1x)
  and captive-portal** networks can't be provisioned this way.
- On Android 10+, the suggestion API requires the one-time per-network approval
  and the kid app must stay installed for auto-connect to keep working.
- Very old Fire tablets (pre-2016, Android 5.0/API 21) are below the kid app's
  `minSdk 22` and aren't supported.
```
