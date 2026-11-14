# P2PLanguageLounge

A modern, peer-to-peer real-time language exchange Android application built with **Jetpack Compose**, **Kotlin Multi-Module Architecture**, and an isolated P2P communication engine.

## Architecture & Modules

The project is structured according to Android modularization best practices:

```
P2PLanguageLounge/
├── app/                      # Application entry point & Jetpack Navigation
├── core-engine/              # P2P engine, connection state, data channel protocol
├── feature-matchmaking/      # Peer discovery, language pair filtering & lounge lobby
└── feature-canvas/           # Real-time collaborative whiteboard & stroke sync
```

### Module Responsibilities

1. **`:app` (Android Application)**
   - Configured with Jetpack Compose & Material 3 theme.
   - Orchestrates feature navigation (`NavHost`) between matchmaking and interactive session lounges.
   - Provides dependency wiring for the shared `P2PManager`.

2. **`:core-engine` (Android Library)**
   - Independent core module with zero UI dependencies.
   - Manages peer models (`Peer`), session states (`LoungeSession`), and the `P2PMessage` data exchange protocol (handshake, text, canvas stroke packets, audio events).
   - Provides `P2PManager` abstraction with reactive Kotlin `StateFlow` and `SharedFlow`.

3. **`:feature-matchmaking` (Android Library with Compose)**
   - UI and state management for peer discovery.
   - Allows users to select Native & Target language pairings.
   - Displays real-time radar search and interactive peer cards.

4. **`:feature-canvas` (Android Library with Compose)**
   - Real-time collaborative whiteboard supporting peer language tutoring, writing exercises, and drawing.
   - Gesture-based drawing engine using Compose `Canvas` and `pointerInput`.
   - Low-latency stroke packet broadcasting to peers via `core-engine`.

## Tech Stack
- **Language**: Kotlin 2.0+ (JVM target 17)
- **UI Framework**: Jetpack Compose (Compose BOM 2024.09.03) with Material 3
- **Build System**: Gradle 8.7 with Kotlin DSL (`build.gradle.kts`) & Version Catalog (`libs.versions.toml`)
- **Architecture**: Modular MVVM with Kotlin Coroutines & StateFlow

## Building the Project

```bash
# Check build tasks
./gradlew tasks

# Assemble debug build
./gradlew assembleDebug

# Run unit tests across all modules
./gradlew test
```
