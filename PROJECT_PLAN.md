# Opportunity — MVP Project Plan

Source: product spec (section 24, MVP scope). This plan sequences that scope into phases, epics, and tickets. No V2/V3 features (AI decomposition, smart scheduling, calendar integration, adaptive reminders, insights) are included here — see "Deferred to V2" at the bottom.

Stack assumed per spec section 20: Kotlin + Jetpack Compose, Room, WorkManager, Material 3. Backend/sync is explicitly out of scope for MVP (offline-first, section 21).

---

## Phase 0 — Project Foundation
Nothing else can start until this lands.

- [ ] **0.1** Initialize Android project (Kotlin, Jetpack Compose, Material 3 theming, dynamic color support)
- [ ] **0.2** Set up module structure (e.g. `core/data`, `core/ui`, `feature/inbox`, `feature/today`, `feature/rightnow`, `feature/timer`)
- [ ] **0.3** Add Room, WorkManager, Navigation-Compose, DI (Hilt) dependencies
- [ ] **0.4** Set up basic CI (build + unit test on push) — optional but recommended before feature work piles up
- [ ] **0.5** Establish dark/light theme + typography scale per section 19 (Material 3, large typography, minimal nav)

---

## Phase 1 — Data Model & Persistence
Depends on: Phase 0. Everything else depends on this.

- [ ] **1.1** Define `Opportunity` Room entity per section 22 (id, title, description, type, status, priority, importance, urgency, deadline, preferredTime, estimatedDuration, actualDuration, recurrence, projectId, category, progress, sessions, createdAt, updatedAt, completedAt)
- [ ] **1.2** Define `type` enum: Task, Project, Goal, Habit, Idea, Someday (section 4)
- [ ] **1.3** Define `status` enum: Inbox, Active, InProgress, Completed, Postponed, Archived
- [ ] **1.4** Define `Reminder` entity/embedded object (frequency, nextReminder, escalation stub — full escalation logic is V2, but schema should hold the fields)
- [ ] **1.5** Define `Session` entity for timer/progress tracking (opportunityId, startedAt, durationMinutes, mode)
- [ ] **1.6** Define `Project` relationship: self-referential `parentId` on Opportunity (a Project is an Opportunity with children) — simplest MVP approach, avoids a separate table
- [ ] **1.7** Write DAOs: CRUD + queries (by status, by type, by deadline range, by projectId, postponement count increment)
- [ ] **1.8** Repository layer wrapping DAOs, exposing Flow-based reactive queries
- [ ] **1.9** Seed/migration strategy (Room migrations from day one, even pre-1.0, to avoid painful schema churn)

---

## Phase 2 — Capture & Inbox
Depends on: Phase 1. This is the core "capture everything" loop (sections 3.1, 5).

- [ ] **2.1** Quick-add UI: single text field, minimal friction, accessible from anywhere (FAB or persistent affordance)
- [ ] **2.2** Manual structured fields (editable after creation): title, type, priority, deadline, estimated duration, recurrence — per section 6, but MVP does NOT need full NLP parsing (that's section 3.2/16, V2-leaning); ship manual entry first
- [ ] **2.3** *(Stretch within MVP)* Basic natural-language date/duration parsing only (e.g. "tomorrow", "in 2 hours", "every evening") — a lightweight rule-based parser, not full AI extraction. Flag as optional if timeline is tight; full NLP task decomposition stays in V2.
- [ ] **2.4** Inbox screen (section 17.3): list of uncategorized/unfiled Opportunities, count badge
- [ ] **2.5** Triage actions from Inbox: assign type, set deadline/priority, archive, convert to project
- [ ] **2.6** Swipe actions (complete / postpone / archive) on list items

---

## Phase 3 — Core Lists & Organization
Depends on: Phase 2.

- [ ] **3.1** Today view (section 17.1): items due/planned today, grouped by priority color (🔴🟡🟢 per section 17.2 style)
- [ ] **3.2** Priority model: user-set priority + simple computed field (urgency + importance + deadline proximity — a basic weighted score, not the full "Opportunity Score" engine in section 23, which is V2)
- [ ] **3.3** Basic project grouping UI: view a Project's child tasks as a checklist, progress bar (section 4 "Projects" example: Jewellery Try-On App breakdown)
- [ ] **3.4** Explore screen (section 17.4): Goals, Ideas, Someday items — simple filtered lists by type, no AI classification (manual only for MVP)
- [ ] **3.5** Completion flow: mark complete, set completedAt, remove from active views

---

## Phase 4 — Timer System
Depends on: Phase 1 (Session entity). Can run in parallel with Phase 3.

- [ ] **4.1** Countdown timer mode (fixed duration, e.g. "30 min" — section 7)
- [ ] **4.2** Stopwatch mode (track actual duration)
- [ ] **4.3** Session logging: each timer run writes a `Session` record, updates `actualDuration`/progress on the Opportunity
- [ ] **4.4** Timer UI: progress bar, start/pause/stop, persists across app backgrounding (foreground service or WorkManager-backed timer so it survives)
- [ ] **4.5** *(Defer session-based "3x/week" and total-progress "10 hour goal" modes to V2 unless timeline allows — flag explicitly, since spec section 7 describes both but MVP list only requires "Timer" generically)*

---

## Phase 5 — Recurrence & Reminders
Depends on: Phase 1, Phase 2.

- [ ] **5.1** Recurrence rules: daily, weekly, custom interval, "every evening until completed" style (section 8 — the completion-linked stop condition specifically)
- [ ] **5.2** WorkManager-scheduled local notifications for reminders
- [ ] **5.3** Notification content: static/templated messages for MVP (the adaptive tone-shifting language in section 8 — "You've postponed this 3 times" — is V2 postponement intelligence; MVP just needs reliable fire-on-schedule reminders)
- [ ] **5.4** Notification actions: Start / Snooze / Reschedule (section 13) — snooze reschedules the next local notification; reschedule opens edit screen
- [ ] **5.5** Custom reminder frequency setting per item (section 24 item 9)
- [ ] **5.6** Postponement counter: increment on snooze/reschedule, store on Opportunity (no smart behavior yet — just the data, so V2 can build on it)

---

## Phase 6 — "Right Now" Recommendation (MVP-simplified)
Depends on: Phase 3, Phase 4. This is the differentiating feature (section 9) — worth keeping in MVP even simplified.

- [ ] **6.1** "Available time" input: user manually enters/selects how much time they have (no calendar integration yet — that's V2 per section 14)
- [ ] **6.2** Simple ranking algorithm: filter by estimatedDuration ≤ available time, sort by (deadline proximity, priority) — a basic version of the section 23 formula, not the full weighted score with historical behavior/context
- [ ] **6.3** Right Now screen UI: top recommendation + "other things you can do" list (section 9/17.2 mockup)
- [ ] **6.4** "Start" action wired to Phase 4 timer

---

## Phase 7 — Android-Native Surface
Depends on: Phase 3, Phase 6.

- [ ] **7.1** Home-screen widget: "What should I do next?" showing top Right Now recommendation + Start action (section 13, MVP item 15)
- [ ] **7.2** Widget tap → deep link into app at the relevant item
- [ ] **7.3** *(Defer to V2 unless trivial: Quick Settings tile, Share Sheet intake, text-selection capture, voice capture — spec section 13 lists these but MVP list (section 24) doesn't require them)*

---

## Phase 8 — Polish & MVP Hardening
Depends on: all above.

- [ ] **8.1** Empty states and no-guilt messaging per section 18 ("You didn't get to this today. That's okay.") — copy pass across Inbox, Today, Right Now
- [ ] **8.2** Accessibility pass (contrast, touch targets, TalkBack labels)
- [ ] **8.3** Manual QA pass across offline scenarios (airplane mode: create/edit/complete/timer/reminders all must work per section 21)
- [ ] **8.4** Performance check on list rendering with large item counts (hundreds of Opportunities, per section 2 "dozens or hundreds of things")
- [ ] **8.5** Internal dogfood / TestTrack release

---

## Suggested Sequencing

```
Phase 0 → Phase 1 → Phase 2 → Phase 3 ─┬→ Phase 6 → Phase 7 → Phase 8
                            Phase 4 ────┤
                            Phase 5 ────┘
```
Phases 4 and 5 can run in parallel with Phase 3 once Phase 2 lands, since they only depend on the data layer. Phase 6 needs both the list/priority logic (Phase 3) and the timer (Phase 4) wired up.

## Explicitly Deferred to V2 (do not build now)
Per spec sections 10, 11, 14, 15, 16: smart scheduling, calendar integration, full NLP task decomposition, adaptive/tone-aware reminder escalation, postponement-triggered prompts ("archive / reschedule / turn into recurring goal"), context-aware suggestions (location/energy/history), productivity insights screen, share-sheet/voice/quick-settings capture, session-based and total-progress timer modes (unless found trivial during Phase 4).

## Open Decisions Before Coding Starts
- NLP parsing in Phase 2.3: build a lightweight rule-based date/duration parser now, or push all parsing to V2 and ship manual-entry-only for MVP?
- Priority score in 3.2 / ranking in 6.2: confirm the specific weighting (deadline proximity vs. priority vs. duration fit) before implementing, since section 23's full formula is explicitly out of scope but *some* ordering logic is needed for Right Now to be useful.
- DI framework choice (Hilt vs. manual/Koin) — Hilt assumed in 0.3, confirm before scaffolding.
