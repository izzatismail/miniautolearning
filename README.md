# MiniAutomotive — Simulated Android Automotive Architecture

A hands-on learning project that simulates Android Automotive's layered architecture — **multiple apps, one centralized vehicle service, shared state over Binder IPC** — without requiring AOSP, the Automotive emulator, or real vehicle hardware.

Built as preparation for Full Stack Developer (AOSP / Android Automotive) roles, as a follow-up to a simpler single-client Binder/AIDL project (Calculator Service).

## Why This Project

Real Android Automotive routes every vehicle interaction — speed, climate, door locks — through a single system service (`CarService`) talking to a Vehicle HAL. Apps never touch hardware directly.

This project rebuilds that pattern in miniature:

```
App → Manager (client wrapper) → Binder/AIDL → Service → Repository → State
```

...compared to the real thing:

```
App → Car API → Car Service → Vehicle HAL → Vehicle Hardware
```

The goal is to feel, in code, *why* this layering exists — not just read about it.

## What This Project Adds Beyond a Basic Binder Exercise

This isn't just "client calls service." It specifically tackles the harder problems that only appear with **multiple concurrent clients**:

✅ Concurrent access to shared state across Binder pool threads (and why it needs synchronization)  
✅ Callback fan-out to many clients using `RemoteCallbackList` (not a plain list — handles dead clients automatically)  
✅ The `oneway` AIDL keyword, and why callback notifications must not block  
✅ Client-side thread re-dispatch (Binder thread → main thread) before touching UI  
✅ Real Android permission enforcement (`SecurityException`), not a simulated if-check  

## Architecture

```
Dashboard App   Climate App   (Media App - optional)
      │              │              │
      └──────────────┴──────────────┘
                     │
              VehicleManager
           (hides all Binder details)
                     │
              Binder / AIDL
                     │
              VehicleService
        (multiple Binder threads may
         enter here concurrently)
                     │
             VehicleRepository
          (thread-safe, synchronized)
                     │
           Simulated Vehicle State
```

## Project Structure

```
MiniAutomotive/
├── app-dashboard/     # Displays speed, fuel, gear, doors, temperature
├── app-climate/       # Adjusts temperature; changes reflect in Dashboard
├── vehicle-service/   # Owns state, runs in its own process
├── common-aidl/       # IVehicleService.aidl, IVehicleCallback.aidl
├── common-model/      # Shared Parcelable models (Phase 3+)
└── docs/
```

## Key Technical Points

### Real Process Separation
`vehicle-service` runs in its own OS process (`android:process=":vehicle"` or a separate installed APK). Verified with:
```bash
adb shell ps | grep vehicle
```

### Concurrency
Multiple clients (Dashboard, Climate) can call into `VehicleService` at the same instant, dispatched to different Binder pool threads. `VehicleRepository` guards shared state with `synchronized` to prevent lost updates or torn reads — proven with thread-name logging during simultaneous calls from two clients.

### Callback Fan-Out
State changes are pushed to all registered clients via `RemoteCallbackList<IVehicleCallback>`, marked `oneway` so a slow or dead client can't stall the notification loop. `RemoteCallbackList` also auto-removes callbacks for clients whose process has died, using `linkToDeath` internally.

### Real Permissions
Sensitive operations (`lockDoors()`, `setGear()`) are gated by an actual Android custom permission, checked via `checkCallingOrSelfPermission()` inside the service — not a fake if-statement. An unpermitted client gets a real `SecurityException`.

## Build Phases

| Phase | What's Added |
|---|---|
| 1 | Basic AIDL, polling clients, thread-safe repository |
| 2 | Callbacks replace polling (`RemoteCallbackList`, `oneway`) |
| 3 | Parcelable `VehicleStatus` object |
| 4 | Real permission enforcement |
| 5 | Vehicle HAL layer split out |
| 6 | Simulated CAN bus events (periodic state changes) |

See `AGENTS.md` for the full milestone-by-milestone roadmap.

## How to Use This Repo

1. Read `AGENTS.md` for the detailed learning roadmap and rationale behind each design choice.
2. Follow the milestones in order — each depends on the previous.
3. Run all apps on an emulator/device, watch Logcat filtered by tag to see concurrent Binder threads and callback fan-out in real time.
4. Deliberately break things (kill a client mid-registration, fire simultaneous writes) to observe the safety mechanisms working.

```bash
adb logcat | grep -E "VehicleService|VehicleManager|Dashboard|Climate"
```

## Interview-Ready Takeaways

- *"How does Android Automotive let multiple apps share one vehicle's state safely?"*
- *"What happens if two apps write to shared state through Binder at the same time?"*
- *"How do you notify many clients of a state change without one slow client blocking the rest?"*
- *"What happens if a registered callback's client process crashes?"*
- *"How would you prevent an untrusted app from unlocking doors?"*

## What This Project Deliberately Excludes

No Hilt, Dagger, Compose, Coroutines, RxJava, MVVM, or Clean Architecture. Kept intentionally simple (Kotlin, XML, View Binding) so Binder concepts stay the focus.

## License

MIT

---

**This is a learning project, not production code.** The goal is understanding Android Automotive's platform architecture, not building a real vehicle app.
