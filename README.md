# Kairo

Kairo is a native Android, audio-first music player. The current foundation includes a singleton Media3/ExoPlayer engine, typed playback state, queue controls, and source-format diagnostics. MediaSession and PlaybackService integration are reserved for a later phase.

## Architecture

The `app` module is organized by package boundaries under `com.kairo.player`: `audio`, `playback`, `source`, `data`, `domain`, `network`, `cache`, `di`, `ui`, and `util`. A Hilt singleton owns the long-lived ExoPlayer instance. Media3 input-format and decoder callbacks feed nullable audio diagnostics; unavailable source values are left unknown. Music sources expose provider-independent domain models, with user-selected local files and a fixture-only mock source currently registered. Room persists track metadata, playlists, and playback history; Retrofit/OkHttp/Kotlin Serialization provide HTTPS-only networking infrastructure without a configured provider endpoint. Media3 SimpleCache writes require explicit source permission. Jetpack Compose, Hilt, and Room remain available for UI, dependency injection, and local persistence.

## Technology

- Kotlin and Gradle Kotlin DSL with a Gradle Version Catalog
- Android API 36, minimum API 26, and JDK 17
- Jetpack Compose and Material 3
- AndroidX Media3 ExoPlayer and Common
- Kotlin Coroutines, Hilt, and Room

## Build

Install JDK 17 and Android SDK Platform 36 with Build Tools 36.0.0. From the project root, run:

```sh
./gradlew assembleDebug
./gradlew test
```