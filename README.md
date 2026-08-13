# ⚡ Opportunity — Smart Personal Task & Life Management System

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-757575?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Wear OS](https://img.shields.io/badge/Companion-Wear%20OS-4285F4?style=flat-square&logo=wearos&logoColor=white)](https://developer.android.com/wear)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

> **Work on what matters right now, backed by context, capacity, and zero-guilt planning.**

**Opportunity** is a modern, offline-first personal productivity and task management system for Android. Built with Jetpack Compose, Room, WorkManager, and Hilt, Opportunity uses a deterministic scoring engine to calculate dynamic priority scores based on deadline proximity, effort fit, location context, and past postponement behavior — helping you decide what to do *Right Now* without friction or overwhelm.

---

## 🌟 Key Features

- 🎯 **"Right Now" Recommendation Engine**: Computes the optimal task to work on next based on your available time, current location, urgency, and effort fit.
- 📅 **Capacity-Aware Today Planner**: Buckets tasks into *Must Happen*, *Recommended*, and *If You Have Time*, checking total planned duration against your actual free calendar windows.
- 📍 **Location & Calendar Context**: Detects saved places (e.g., College, Office) and reads free calendar windows locally via native Android APIs (`CalendarContract`).
- ⚡ **Multi-Modal Fast Capture**: Quick-add bar, Android Share Sheet intake, text selection (`PROCESS_TEXT`), Quick Settings QS tile, and voice input.
- ⏱️ **Foreground Session Timers**: Stopwatch and countdown timers backed by an Android Foreground Service so session tracking survives app backgrounding.
- ⌚ **Wear OS Companion Module**: Standalone Wear OS app that requests the phone's current recommendation over `Wearable.MessageClient` and marks tasks complete from your wrist.
- 🔔 **Adaptive Reminders**: WorkManager sweep scheduling interactive local notifications with quick *Start*, *Snooze*, and *Reschedule* actions.
- 🌐 **Opt-in Cross-Device Sync**: Optional sync server built with Node.js, Express, Prisma, and PostgreSQL using timestamp-based conflict resolution.

---

## 🏗️ Architecture & Modules

```
Opportunity/
├── app/        # Primary Android mobile application (Jetpack Compose + Hilt + Room + WorkManager)
├── wear/       # Companion Wear OS application (Wearable Data Layer API)
└── backend/    # Optional cross-device sync server (Node.js + Express + Prisma + Postgres)
```

- **Offline-First Core**: All core domain logic, scoring algorithms, database queries, and timers function 100% locally without network dependency.
- **Unidirectional Data Flow**: Powered by Kotlin Flow, Coroutines, StateFlow, and Hilt ViewModel architecture.
- **Deterministic Domain Engine**: [`OpportunityScorer.kt`](app/src/main/java/com/opportunity/app/domain/usecase/OpportunityScorer.kt) weights urgency, importance, time fit, user preference, postponement cost, and effort.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Koala+ recommended) or JDK 17 + Android SDK
- **Android SDK** API 34 (Android 14)
- *(Optional)* **Node.js 20+** & **PostgreSQL** if running the sync backend

### Building & Running the Android App

1. Clone the repository:
   ```bash
   git clone git@github.com:jvstin47/opportunity-android-task-planner.git
   cd opportunity-android-task-planner
   ```

2. Copy the sample local properties:
   ```bash
   cp local.properties.example local.properties
   ```

3. Open the project in Android Studio, or build from the command line:
   ```bash
   ./gradlew :app:assembleDebug
   ```

4. Run unit tests:
   ```bash
   ./gradlew test
   ```

### Running the Backend (Optional Cross-Device Sync)

```bash
cd backend
cp .env.example .env   # Configure DATABASE_URL and JWT_SECRET
npm install
npm run prisma:migrate
npm run dev
```

### Running the Wear OS Companion

Install `:wear:installDebug` onto a paired Wear OS device or emulator alongside the mobile app:
```bash
./gradlew :wear:installDebug
```

---

## 📋 Implementation Roadmap

See [PROJECT_PLAN.md](PROJECT_PLAN.md) for the detailed phase breakdown.

- [x] **Phase 0**: Project Foundation & Material 3 Setup
- [x] **Phase 1**: Room Persistence & Domain Entities
- [x] **Phase 2**: Multi-Modal Quick Capture & Triage
- [x] **Phase 3**: Core Today Plan & Bucket System
- [x] **Phase 4**: Foreground Timer & Session Logging
- [x] **Phase 5**: WorkManager Reminders & Interactive Notifications
- [x] **Phase 6**: "Right Now" Recommendation Engine
- [x] **Phase 7**: Android Glance Home Screen Widget & QS Tile
- [x] **Phase 8**: Wear OS Companion & Sync Backend

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
