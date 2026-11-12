# ⚡ Opportunity — Smart Personal Task & Life Management System

[![Android](https://img.shields.io/badge/Platform-Android%20(API%2026--34)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Glance](https://img.shields.io/badge/Widgets-Jetpack%20Glance%201.1-4285F4?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/develop/ui/compose/glance)
[![Wear OS](https://img.shields.io/badge/Companion-Wear%20OS-4285F4?style=flat-square&logo=wearos&logoColor=white)](wear/README.md)
[![Backend](https://img.shields.io/badge/Sync-Node.js%20%2B%20Prisma-339933?style=flat-square&logo=nodedotjs&logoColor=white)](backend/README.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

> **Work on what matters right now — backed by context, realistic time capacity, and zero-guilt planning.**

**Opportunity** is an offline-first personal productivity, focus, and task planning ecosystem for Android and Wear OS. Instead of overwhelming you with flat checklists, Opportunity scores your tasks dynamically against available time, deadlines, location context, and postponement history to surface the single best thing to work on **right now** — paired with persistent focus timers, adaptive reminders, a 6-widget Glance home-screen suite, and an optional cross-device sync service.

---

## 🌟 Core Capabilities

### 🎯 Focus ("Right Now") Decision Engine
- **Deterministic Multi-Factor Scoring** ([`OpportunityScorer`](app/src/main/java/com/opportunity/app/domain/usecase/OpportunityScorer.kt)): Evaluates deadline proximity, urgency, importance, effort fit against your chosen time window (`15m`–`90m`), user priority, and postponement cost.
- **Zero-Decision Hero Flow**: Highlights one primary focus recommendation with a glowing session ring and one-tap **START FOCUS**, plus quick-fit alternatives in *"If you only have a few minutes"*.

### 📅 Capacity-Aware Today Planner
- **Three Actionable Buckets** ([`TodayPlanUseCase`](app/src/main/java/com/opportunity/app/domain/usecase/TodayPlanUseCase.kt)): Automatically organizes active work into **Must Happen** (overdue and due today), **Recommended**, and **If You Have Time**.
- **Calendar Free-Window Matching**: Reads device calendar events locally via Android's `CalendarContract` ([`CalendarRepository`](app/src/main/java/com/opportunity/app/calendar/CalendarRepository.kt)) to detect over-capacity days and suggest what to defer.
- **Instant Live Search**: Filter tasks across all buckets by title or category right from the Today screen.

### 🧩 Home Screen Widget Suite (Jetpack Glance 1.1)
Six dark-amber themed widgets powered by a shared Hilt entry point ([`WidgetDataProvider`](app/src/main/java/com/opportunity/app/widget/WidgetDataProvider.kt)):

| Widget | Purpose | Key Details |
|:-------|:--------|:------------|
| **What Should I Do Next?** | Current focus card | Priority/category pill, duration estimate, `+ Add` shortcut, direct task deep link |
| **Task List** | Today's top tasks | Priority indicators, duration chips, quick `+ Add` capture, empty-day state |
| **Quick Add** | Instant capture | One-tap Quick Capture trigger with live Inbox item count badge |
| **Daily Progress** | Daily execution tracker | Tasks completed vs. planned, percentage, `LinearProgressIndicator`, minutes focused |
| **Streak & Momentum** | Consistency tracker | Consecutive-day completion streak, weekly completed total, milestone prompts |
| **Upcoming Deadlines** | Chronological radar | Color-coded urgency badges (`OVERDUE`, `TODAY`, `TOMORROW`, or date) |

### ⏱️ Persistent Foreground Session Timers
- **Countdown & Stopwatch Modes**: Backed by [`TimerController`](app/src/main/java/com/opportunity/app/timer/TimerController.kt), [`TimerForegroundService`](app/src/main/java/com/opportunity/app/timer/TimerForegroundService.kt) (`foregroundServiceType="specialUse"`), and [`TimerPersistence`](app/src/main/java/com/opportunity/app/timer/TimerPersistence.kt) so timers survive backgrounding and OEM process kills.
- **Atomic Session Logging**: Logs `SessionEntity` records and updates `actualDurationMinutes` atomically inside a Room `withTransaction` block.

### ⚡ Multi-Modal Fast Capture
- **Quick Capture Sheet**: Low-friction bottom sheet accessible via the global `+` FAB.
- **System-Wide Android Intake**: Share Sheet (`ACTION_SEND`), text selection (`ACTION_PROCESS_TEXT`), and a Quick Settings tile ([`QuickAddTileService`](app/src/main/java/com/opportunity/app/capture/QuickAddTileService.kt)) routed through [`QuickCaptureActivity`](app/src/main/java/com/opportunity/app/capture/QuickCaptureActivity.kt).

### 🔔 Adaptive Reminders & Context Awareness
- **Tone-Aware Notifications** ([`AdaptiveReminderUseCase`](app/src/main/java/com/opportunity/app/domain/usecase/AdaptiveReminderUseCase.kt)): Escalates smoothly from gentle check-ins to urgent alerts for approaching or overdue deadlines, with interactive **Done**, **Start**, **Snooze**, and **Reschedule** actions.
- **Location Context** ([`LocationRepository`](app/src/main/java/com/opportunity/app/location/LocationRepository.kt), [`NearbyPlaceUseCase`](app/src/main/java/com/opportunity/app/domain/usecase/NearbyPlaceUseCase.kt)): Matches saved places (e.g., *College*, *Gym*) against task categories with a 2-minute TTL location cache.
- **Postponement Intelligence** ([`PostponementAdvisor`](app/src/main/java/com/opportunity/app/domain/usecase/PostponementAdvisor.kt)): Flags repeatedly deferred items and prompts you to reschedule, convert to a recurring habit, or archive.

### 🎨 Smoked-Glass Warm Theme System
- **Warm Neutral Canvas + Single Bold Accent**: Grounded in `#191512` dark warm surfaces with custom [`GlassCard`](app/src/main/java/com/opportunity/app/ui/common/GlassCard.kt) depth and **5 user-selectable accents** ([`AccentPalette`](app/src/main/java/com/opportunity/app/ui/theme/AccentPalette.kt)): **Coral**, **Amber**, **Sage**, **Dusty Blue**, and **Rose**, persisted via DataStore.

### 🤖 Optional AI Layer & Companion Ecosystem
- **Claude Intelligence** ([`ClaudeClient`](app/src/main/java/com/opportunity/app/ai/ClaudeClient.kt)): Optional Anthropic Messages API integration for NLP capture enrichment, project decomposition, and weekly Growth reflections. Works 100% offline without a key.
- **Wear OS Companion** ([`wear/`](wear/README.md)): Standalone smartwatch app synced over Google Play Services `Wearable.MessageClient`.
- **Cross-Device Sync Backend** ([`backend/`](backend/README.md)): Optional Node.js + Express + Prisma + PostgreSQL service with timestamp-based conflict resolution ([`SyncRepository`](app/src/main/java/com/opportunity/app/sync/SyncRepository.kt)).

---

## 🏗️ Architecture & Repository Layout

```
Opportunity/
├── app/                          # Android mobile application (Compose, Hilt, Room v2, WorkManager, Glance)
│   ├── src/main/java/com/opportunity/app/
│   │   ├── ai/                   # Optional Anthropic Claude client & prompts
│   │   ├── calendar/             # Local Android CalendarContract free-window reader
│   │   ├── capture/              # Share Sheet, PROCESS_TEXT & Quick Settings tile
│   │   ├── config/               # Centralized BuildConfig / environment configuration (AppConfig)
│   │   ├── data/                 # Room v2 DB (MIGRATION_1_2), DAOs, Entities, OpportunityRepository
│   │   ├── di/                   # Hilt DI modules
│   │   ├── domain/               # Models & deterministic use cases (Scorer, TodayPlan, AdaptiveReminder)
│   │   ├── location/             # Fused location (2m TTL cache) & saved Places DataStore
│   │   ├── reminder/             # WorkManager sweep, interactive notification receiver
│   │   ├── sync/                 # Retrofit sync client, timestamp merge repository, WorkManager worker
│   │   ├── timer/                # Foreground timer service, controller & persistence
│   │   ├── ui/                   # 4-tab Compose UI (Focus, Tasks, Growth, Vault) + Theme system
│   │   ├── util/                 # Shared date, duration & relative deadline formatters
│   │   └── widget/               # 6 Jetpack Glance widgets + WidgetDataProvider + WidgetUpdater
│   └── src/test/                 # JUnit unit tests for domain scoring & adaptive reminders
├── wear/                         # Wear OS companion module (see wear/README.md)
├── backend/                      # Versioned Express + Prisma + PostgreSQL sync API (/api/v1, see backend/README.md)
├── .github/workflows/ci.yml      # GitHub Actions CI for Android & Backend
├── PROJECT_PLAN.md               # Original MVP architecture & phase breakdown
└── CONTRIBUTING.md               # Contribution & coding guidelines
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Koala or newer recommended) with **JDK 17**
- **Android SDK** (`minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`)
- *(Optional)* **Node.js 20+** and **PostgreSQL 16+** if running the sync backend

### 1. Clone & Configure
```bash
git clone git@github.com:jvstin47/opportunity-android-task-planner.git
cd opportunity-android-task-planner
cp local.properties.example local.properties
```

Edit `local.properties` as needed (both optional — the app runs completely offline without them):
```properties
sdk.dir=/path/to/Android/sdk
ANTHROPIC_API_KEY=sk-ant-...                 # Optional: enables AI capture parsing & weekly reflections
OPPORTUNITY_SYNC_BASE_URL=http://10.0.2.2:4000/ # Optional: points emulator to local backend/
```

### 2. Build, Test & Install
```bash
# Build debug APKs (app + wear)
./gradlew :app:assembleDebug :wear:assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Build release APK with R8 shrinking/obfuscation
./gradlew :app:assembleRelease

# Install onto a connected ADB device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Companion Modules
- **Backend Sync Service**: See [backend/README.md](backend/README.md) for PostgreSQL setup, Prisma migrations, and REST endpoints.
- **Wear OS App**: See [wear/README.md](wear/README.md) for smartwatch deployment and Data Layer protocol details.

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
