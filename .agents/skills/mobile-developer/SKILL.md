---
name: mobile-developer
description: Expert mobile engineering guide for Android (Kotlin, Jetpack Compose, Coroutines, Hardware APIs, CameraX, Services, C2 Automation) and cross-platform native architectures. Use when building, refactoring, debugging, or enhancing mobile applications.
---

# Mobile Developer Skill

## Core Principles
1. **Modern Android Stack**: 
   - UI: 100% Jetpack Compose with Material3 + custom Liquid Glass design tokens.
   - Concurrency: Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Main`, `StateFlow`, `SharedFlow`).
   - Architecture: Clean MVI/MVVM pattern with clear separation between UI, Domain, and Hardware layers.
   - Lifecycle: Lifecycle-aware components (`rememberCoroutineScope`, `collectAsStateWithLifecycle`).

2. **Hardware & System Integration**:
   - CameraX for background and preview snapshots.
   - AudioManager for audio routing, ringer mode overrides, and emergency alarms.
   - LocationServices (FusedLocationProviderClient / LocationManager) with battery-conscious priority settings.
   - NetworkCapabilities & ConnectivityManager for live telemetry.
   - Foreground Services with appropriate notification channels for persistent background C2 listeners.

3. **Performance & Resilience**:
   - Zero UI thread blocking: All file I/O, network requests, and hardware calls run on `Dispatchers.IO`.
   - 15-second hardware warm-up timeout wrappers on GPS and Camera calls.
   - Crash immunity: Try-catch wrappers with graceful degraded fallbacks.
   - Low memory footprint: Stream large payloads; recycle bitmaps immediately.
