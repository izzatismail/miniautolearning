# AGENTS.md — Agent Instructions

Below is repo-specific guidance for coding agents. The section starting at **"Project"** below is the full design specification — keep it as the source of truth for what to build and why.

---

## Repo State
- Fresh Android Studio placeholder project, single `:app` module only
- AGP 9.2.1, Gradle 9.4.1, minSdk 26, targetSdk 36
- No AIDL files, no Kotlin source files in `app/src/main/java` yet
- compileSdk uses AGP 9 API: `version = release(36) { minorApiLevel = 1 }`
- Version catalog at `gradle/libs.versions.toml`
- Package: `com.izzatismail.miniautolearning`

## Branch + Commit Workflow
- Create a new branch before every set of changes
- Conventional commit prefixes: `feat:`, `fix:`, `chore:`, `docs:`
- Example: `feat: add IVehicleService and IVehicleCallback AIDL interfaces`
- **Verify the project compiles** (`./gradlew assembleDebug`) before committing
- After completing a milestone group: commit (squash if needed), push, create a PR

## Build Commands
- `./gradlew assembleDebug` — full build
- `./gradlew :module:assembleDebug` — single module
- Kotlin code style: `official` (no trailing commas)

## Technical Constraints
- **NO**: Jetpack Compose, Hilt, Dagger, Koin, Coroutines, Flow, RxJava, MVVM, Clean Architecture
- **USE**: Kotlin, XML layouts, View Binding (`buildFeatures { viewBinding = true }` in `build.gradle.kts`), standard Android SDK

## Required Multi-Module Architecture
Refactor the existing `:app` into this structure:
- `:common-aidl` — Android Library, contains `IVehicleService.aidl` and `IVehicleCallback.aidl`
- `:vehicle-service` — Android app module, runs in separate process (`android:process=":vehicle"` in manifest)
- `:app-dashboard` — refactored from existing `:app`
- `:app-climate` — new Android app module
- `:common-model` — Android Library, Parcelable models (Phase 3+)

## Initial Deliverable (Milestones 1-3)
Build and commit these three milestones together, then push + PR:
1. **AIDL contracts** — create `:common-aidl` module with `IVehicleService.aidl` and `IVehicleCallback.aidl` (oneway)
2. **VehicleRepository** — thread-safe repository with `synchronized` access, thread-name logging
3. **VehicleService + process separation** — bound service in `:vehicle-service`, verify with `adb shell ps | grep vehicle`

## Key Design Rules (from the spec below)
- **RemoteCallbackList** for callbacks — never a plain `ArrayList` (auto-cleans dead clients via `linkToDeath`)
- **`oneway`** on `IVehicleCallback.aidl` — fan-out must not block on a slow/dead client
- **`synchronized`** on all VehicleRepository read/write methods — guard against concurrent Binder threads
- **Main-thread re-dispatch** in VehicleManager — callbacks arrive on a Binder thread; re-post to main thread before calling app code
- **Real permissions** via `checkCallingOrSelfPermission` for `lockDoors`, `unlockDoors`, `setGear` — real `SecurityException`, not an if-check
- **Temperature range**: 16–30 °C, reject invalid values
- **Log thread name** (`Thread.currentThread().name`) in every IPC method

## Verification
- `adb shell ps | grep vehicle` — confirm process separation
- `adb logcat | grep -E "VehicleService|VehicleManager|Dashboard|Climate"` — trace IPC lifecycle
- `./gradlew assembleDebug` — compile check before every commit

---

# AGENTS.md

# Project

Mini Android Automotive – Project 3: Vehicle Service Simulation

## Objective

Build a simplified Android Automotive architecture using standard Android Studio and the Android SDK. The goal is to understand how Android Automotive separates applications from vehicle hardware using Binder IPC and AIDL — specifically the harder problems that only show up once **multiple clients** share **one service**: concurrency, callback fan-out, and permissioned access.

This project should simulate the architecture of Android Automotive without requiring AOSP, the Android Automotive emulator, or actual vehicle hardware.

The emphasis is on learning Android Platform concepts rather than building a feature-rich application.

---

# Learning Goals

By completing this project, the developer should understand:

* Binder IPC
* AIDL interfaces
* Bound Services
* Multiple clients communicating with one service
* Service ownership of shared state
* **Concurrent access to shared state across Binder threads**
* Callback-based updates
* **`RemoteCallbackList` and why a plain list is unsafe for this**
* **The `oneway` keyword and why callback fan-out needs it**
* **Client-side threading when handling callbacks (Binder thread → main thread)**
* Separation between framework APIs and hardware access
* Why Android Automotive uses a centralized Car Service
* Permission-gated access to sensitive operations (real, not simulated)

---

# Background

The developer has extensive Android application experience in fintech but limited Android Platform or Automotive experience. This developer already completed a simpler single-client/single-service Binder project (Project 1: Calculator Service) and understands Stub/Proxy, `bindService()`, and basic Binder threading.

When explaining concepts, relate them to familiar backend architecture:

| Fintech         | Android Automotive |
| --------------- | ------------------- |
| Backend Service | VehicleService      |
| REST API        | AIDL Interface      |
| Retrofit        | Binder Proxy         |
| JSON            | Parcelable          |
| Database        | Vehicle State        |
| Webhook / SSE   | Binder Callback (AIDL listener) |
| DB row lock / mutex | `synchronized` block on VehicleRepository |
| API key / auth middleware | Android custom permission check |

Explain architectural decisions instead of simply generating code.

---

# Overall Architecture

Implement the following architecture:

```text
Dashboard App
Climate App
(Optional: Media App)

        │
        ▼

VehicleManager (Client Wrapper)

        │
        ▼

Binder / AIDL

        │
        ▼

VehicleService  ← multiple Binder pool threads may enter concurrently

        │
        ▼

VehicleRepository  ← must be thread-safe

        │
        ▼

Simulated Vehicle State
```

The project intentionally mirrors the structure of Android Automotive:

```text
Application

↓

Car API

↓

Car Service

↓

Vehicle HAL

↓

Vehicle Hardware
```

---

# Project Structure

Suggested multi-module project:

```text
MiniAutomotive/

├── app-dashboard/
├── app-climate/
├── vehicle-service/
├── common-aidl/
├── common-model/
└── docs/
```

---

# Process Separation (Required, Not Optional)

As with Project 1: splitting into Gradle modules does not by itself create separate processes. `vehicle-service` must run in its own process, verified via:

```bash
adb shell ps | grep vehicle
```

Either declare the service with `android:process=":vehicle"` (single APK, simpler), or install `app-dashboard`, `app-climate`, and `vehicle-service` as fully separate APKs (closer to how real Automotive system apps and CarService are actually separated). State the choice explicitly in code comments.

This matters even more here than in Project 1, because with three separate client apps all binding to one service, it's easy to accidentally let Gradle collapse everything into one process during quick local testing — always verify with `adb shell ps` before trusting your logs.

---

# Vehicle State

VehicleService owns all vehicle data.

Example:

```kotlin
speed = 60

gear = DRIVE

fuelLevel = 78

temperature = 22

doorsLocked = true

headlights = OFF
```

No client application should modify the state directly.

All interactions must go through Binder.

## Concurrency (Required Learning Point)

`VehicleRepository` is a single object inside `vehicle-service`'s process, but it can be entered **concurrently** by multiple Binder pool threads — e.g., Climate app calling `setTemperature()` at the same instant Dashboard app calls `getTemperature()`, each dispatched to a different thread from the pool.

Requirements:

* Guard all reads/writes to shared vehicle state with `synchronized` (on the repository instance, or a dedicated lock object) — or use `@Volatile` fields only where a single-field atomic read/write is genuinely sufficient, and explain in a comment why that's enough for that specific field.
* Log the thread name (`Thread.currentThread().name`) inside `VehicleRepository` methods to make concurrent entry visible in Logcat when triggered from two clients at once.
* As a deliberate exercise: fire rapid calls from Dashboard and Climate simultaneously and observe/log evidence of concurrent Binder thread entry, then discuss what would break without synchronization (a lost update / torn read).

---

# AIDL Interface

Create:

`IVehicleService.aidl`

Example operations:

```kotlin
int getSpeed()

int getFuelLevel()

int getTemperature()

void setTemperature(int value)

boolean areDoorsLocked()

void lockDoors()

void unlockDoors()

String getGear()

void setGear(String gear)

void registerCallback(IVehicleCallback callback)

void unregisterCallback(IVehicleCallback callback)
```

And a separate callback interface:

`IVehicleCallback.aidl`

```kotlin
oneway interface IVehicleCallback {
    void onSpeedChanged(int newSpeed)
    void onTemperatureChanged(int newTemperature)
    void onDoorLockChanged(boolean locked)
}
```

Explain, in a comment above the callback interface, why it is marked `oneway`: calls become non-blocking, fire-and-forget from the service's perspective, so notifying N clients never stalls on a slow or dead one.

Keep interfaces simple during the first iteration. Introduce Parcelable models only after the basic architecture is working (see Phase 3 below).

---

# VehicleManager

Each client app should communicate only with a VehicleManager class.

Clients must never call Binder directly.

Example:

```kotlin
vehicleManager.getSpeed()

vehicleManager.lockDoors()

vehicleManager.setTemperature(20)

vehicleManager.registerCallback(listener)
```

VehicleManager should hide all Binder implementation details, including:

* Registering/unregistering the AIDL callback with the service
* Re-posting any callback invocation to the client's **main thread** before it reaches app code (callbacks arrive on a Binder thread in the client's own process — touching a `TextView` directly from there will crash)

This mimics Android's `CarPropertyManager` and other framework APIs, which do exactly this main-thread re-dispatch internally so app developers don't have to think about it.

---

# VehicleService

VehicleService is the single source of truth.

Responsibilities:

* Own vehicle state (via VehicleRepository)
* Validate requests
* Log IPC calls, including calling thread name
* Maintain registered callbacks via `android.os.RemoteCallbackList<IVehicleCallback>` — **not** a plain `ArrayList`
* Notify listeners on state change
* Prevent invalid state
* Enforce permissions on sensitive operations (see Phase 4)

Explain, in a comment where `RemoteCallbackList` is declared, why it's used instead of a plain list: it transparently handles a registered client's process dying (internally using `linkToDeath`), automatically removing dead callbacks so the service never tries to notify a client that no longer exists — this is the exact problem a plain `ArrayList<IVehicleCallback>` would silently get wrong.

Example:

Temperature range:

16–30 °C

Reject invalid values.

---

# Dashboard App

Display:

Speed

Fuel

Gear

Door status

Temperature

Phase 1: refresh every second (polling).
Phase 2 onward: replace polling with Binder callbacks — see Future Iterations.

---

# Climate App

Allow:

Increase temperature

Decrease temperature

Display current temperature

Changing the temperature should immediately be visible in the Dashboard app (once callbacks are implemented in Phase 2).

This demonstrates shared state through Binder IPC.

---

# Optional Media App

Read-only client.

Display:

Current speed

Current gear

Fuel level

This demonstrates multiple Binder clients — now three — accessing one service simultaneously, and registering independently for callbacks.

---

# Logging

Every IPC interaction should be visible in Logcat, including calling thread name.

Example:

Dashboard:

```text
Requesting speed...
```

VehicleService:

```text
Received getSpeed() on thread Binder:5521_3
Returning 60 km/h
```

Climate:

```text
Setting temperature to 20°C
```

VehicleService:

```text
Received setTemperature(20) on thread Binder:5521_7
Temperature updated
Notifying 2 registered callbacks (oneway)
```

Logs should clearly illustrate the Binder request/response lifecycle, concurrent thread entry, and callback fan-out.

---

# Future Iterations

The codebase should be structured to support the following enhancements without major refactoring:

## Phase 2 — Callbacks (replace polling)

Replace polling with Binder callbacks via `registerCallback()` / `unregisterCallback()` and `RemoteCallbackList`.

```text
Vehicle changes

↓

VehicleService

↓

oneway Callback (fan-out to all registered clients)

↓

Dashboard/Climate/Media update automatically (after main-thread re-dispatch)
```

---

## Phase 3 — Parcelable

Introduce Parcelable.

Create:

`VehicleStatus.kt`

Containing:

* speed
* gear
* fuel
* temperature
* door status

Return one object instead of multiple Binder calls. Explain `in`/`out`/`inout` directionality here, since it's the first time a non-primitive crosses the boundary.

---

## Phase 4 — Permissions (Real, Not Simulated)

Rather than an if-check pretending to be a permission system, use Android's actual custom permission mechanism:

1. Declare a custom permission in `vehicle-service`'s manifest, e.g. `com.example.mini.permission.CONTROL_VEHICLE`.
2. Require it on sensitive AIDL methods (`lockDoors()`, `unlockDoors()`, `setGear()`) by checking `context.checkCallingOrSelfPermission(...)` inside the Stub implementation before performing the action, throwing a `SecurityException` if not granted.
3. Only declare/request that permission in the client app(s) meant to have control (e.g. Dashboard), not in a read-only client (e.g. Media), and observe the resulting `SecurityException` when Media attempts a control call.

This is real platform mechanism, not a simulation — the same primitive Android itself uses to gate sensitive vehicle operations.

---

## Phase 5 — Vehicle HAL Simulation

Split VehicleService into:

```text
VehicleService

↓

Vehicle HAL

↓

VehicleRepository
```

Vehicle HAL becomes responsible for communicating with the simulated hardware layer.

---

## Phase 6 — Simulated CAN Messages

Every few seconds generate events such as:

```text
Speed = 62

Speed = 64

Door Open

Fuel = 75%
```

Vehicle HAL updates VehicleRepository (thread-safely — reuse the synchronization from earlier). VehicleService notifies applications via the same `RemoteCallbackList` fan-out built in Phase 2.

This approximates how Android Automotive receives updates from a real vehicle network.

---

# Code Comments (Required — This Is How the Developer Learns)

As with Project 1, every non-trivial Binder-related piece of code must be explained via inline comments, specifically:

* Why `RemoteCallbackList` is used over a plain list, at its declaration site.
* Why the callback AIDL interface is marked `oneway`, at its declaration site.
* Why `VehicleRepository` access is synchronized, at each guarded method, including what could go wrong without it.
* Why `VehicleManager` re-posts callbacks to the main thread before calling into app code.
* Why the Stub checks a permission before performing a sensitive action, and what real vehicle-security concern that maps to (e.g., you don't want an untrusted app remotely unlocking doors).

---

# Development Style / Milestones

Implement in this order. Do not skip ahead.

1. **AIDL contracts** — `IVehicleService.aidl` and `IVehicleCallback.aidl` — explain what's generated from each before implementing.
2. **VehicleRepository with synchronized access** — prove thread-safety understanding before adding IPC on top.
3. **VehicleService skeleton + process separation** — verify real separate process via `adb shell ps` before proceeding.
4. **VehicleManager wrapper** — clients only ever touch this, never raw Binder.
5. **Dashboard app with polling (Phase 1 baseline)** — get one client fully working end-to-end first.
6. **Add Climate app** — second client, same service — prove shared state works across two independent clients.
7. **Callbacks with `RemoteCallbackList` + `oneway` (Phase 2)** — replace polling, verify fan-out to both clients, verify main-thread re-dispatch in `VehicleManager`.
8. **Optional Media app** — third read-only client, registering independently — proves N-client scalability of the pattern.
9. **Parcelable `VehicleStatus` (Phase 3)**.
10. **Real permission enforcement (Phase 4)** — verify `SecurityException` on the unpermitted client.
11. **Vehicle HAL split + simulated CAN messages (Phases 5–6)**.
12. **Wrap-up review** — developer explains, unaided, everything in Success Criteria below.

After each milestone: explain what was built, how Android Automotive solves the same problem, how Binder participates, and compare to backend client-server communication where useful. Include ASCII/sequence diagrams whenever they improve understanding.

---

# Constraints

Do NOT use:

* Jetpack Compose
* Hilt
* Dagger
* Koin
* Coroutines
* Flow
* RxJava
* MVVM
* Clean Architecture

Use:

* Kotlin
* XML layouts
* View Binding
* Standard Android SDK APIs

Keep the code intentionally straightforward so the focus remains on understanding Binder IPC and Android Platform architecture.

---

# Success Criteria

By the end of this project, the developer should be able to explain, unaided:

* Why Android Automotive uses a centralized Car Service.
* How multiple applications safely share vehicle data.
* How Binder enables inter-process communication.
* The responsibilities of AIDL, Stub, Proxy, and the Binder driver.
* Why applications should not communicate directly with hardware.
* How Android's layered architecture (App → Framework → Service → HAL → Hardware) promotes security, maintainability, and scalability.
* **Why concurrent Binder calls require synchronized access to shared state, with a concrete example of what breaks without it.**
* **Why `RemoteCallbackList` exists and what problem it solves that a plain list doesn't (dead client cleanup via `linkToDeath`).**
* **Why the callback interface is `oneway` and what would happen to the whole service if it weren't.**
* **Why callbacks must be re-dispatched to the client's main thread before touching UI.**
* **How Android's real permission system gates sensitive vehicle operations, demonstrated with an actual `SecurityException`.**

The project should feel like a miniature Android Automotive platform rather than a collection of Android apps.
