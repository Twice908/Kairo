# Kairo

Kairo is a native Android, audio-first music player. This repository currently contains the Phase 1 Android foundation; playback and audio features are not implemented yet.

## Architecture

The `app` module is organized by package boundaries under `com.kairo.player`: `audio`, `playback`, `source`, `data`, `domain`, `network`, `cache`, `di`, `ui`, and `util`. The UI is built with Jetpack Compose, with Hilt available for dependency injection and Room available for local persistence.

## Technology

- Kotlin and Gradle Kotlin DSL with a Gradle Version Catalog
- Android API 36, minimum API 26, and JDK 17
- Jetpack Compose and Material 3
- Kotlin Coroutines, Hilt, and Room

## Build

Install JDK 17 and Android SDK Platform 36 with Build Tools 36.0.0. From the project root, run:

```sh
./gradlew assembleDebug
./gradlew test
```