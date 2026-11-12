# ⌚ Opportunity Wear OS Companion

The wrist companion application for **Opportunity**, designed for quick decision-making and friction-free task completion directly from your smartwatch.

[![Wear OS](https://img.shields.io/badge/Platform-Wear%20OS-4285F4?style=flat-square&logo=wearos&logoColor=white)](https://developer.android.com/wear)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose for Wear OS](https://img.shields.io/badge/UI-Compose%20for%20Wear-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/training/wearables/compose)
[![Play Services Wearable](https://img.shields.io/badge/Data%20Layer-Google%20Play%20Services-34A853?style=flat-square&logo=googleplay&logoColor=white)](https://developers.google.com/android/reference/com/google/android/gms/wearable/package-summary)

---

## 🌟 Capabilities

- 🎯 **Glanceable "Right Now" Focus**: Displays your current highest-leverage task recommendation calculated on your phone.
- ⏱️ **Duration & Context**: Shows the estimated duration in minutes so you know if you have time before starting.
- ✅ **Wrist Completion**: One-tap "Done" button that dispatches a completion event back to the paired smartphone via Google Play Services Wearable MessageClient.
- 🔋 **Battery-Optimized Lifecycle**: Queries data on-demand without persistent background polling or socket connections.

---

## 📡 Communication Protocol

The watch and handheld phone communicate over the **Google Play Services Wearable Data Layer API**:

```
[ Wear OS Smartwatch ]                     [ Android Smartphone ]
          │                                           │
          │── GET /opportunity/rightnow/request ─────>│ (WearableListenerService)
          │                                           │
          │<── JSON Payload: { id, title, minutes } ──│ (RightNowUseCase recommendation)
          │                                           │
          │                                           │
(User taps "Done")                                    │
          │                                           │
          │── POST /opportunity/complete { id } ─────>│
          │                                           │ (OpportunityRepository.complete)
          │<── Acknowledged ──────────────────────────│
```

- **Request Path**: `/opportunity/rightnow/request`
- **Completion Path**: `/opportunity/complete`
- **Serialization**: Lightweight JSON transfer via Kotlinx Serialization

---

## 🛠️ Building & Deploying

### Prerequisites
- Android Studio Koala or newer with Android SDK Platform 34
- Connected Wear OS smartwatch (paired via Bluetooth or Wi-Fi) or Wear OS 4+ emulator

### Install via Gradle
```bash
./gradlew :wear:installDebug
```

### Run on Watch
```bash
adb -s <wear-device-id> shell am start -n com.opportunity.wear/.MainActivity
```
