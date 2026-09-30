# Kairo

> A personal, self-hosted, lossless music streaming ecosystem for Android.

## 📖 Overview

Kairo is a personal Android project built around a self-hosted music stack: local audio discovery, Navidrome/Subsonic connectivity, Media3 playback, and local metadata persistence. The repository currently contains the Android app foundation and the server-side Docker scaffolding needed for a private music workflow, but it is not yet a fully deployed end-to-end ecosystem.

This project is intended for a single owner and is not a public app or a Play Store release. The goal is to keep full ownership of the music library and infrastructure while streaming lossless FLAC/WAV content without depending on public music APIs or lossy public streaming services.

## 🎯 Project Vision

- Lossless audio streamed natively, without transcoding.
- Self-hosted server stack using slskd, Lidarr, Soularr, and Navidrome.
- No public API dependency for core streaming.
- Automated library sync from curated local sources and service integrations.
- Admin-only sync trigger from the Android client.
- Full ownership of the library and server infrastructure.
- Future P2 goal: per-song EQ automation based on metadata such as energy and mood.

## 🏗️ Architecture

```text
┌────────────────────────────────────────┐
│ Docker / Self-Hosted Server Stack      │
│                                        │
│ slskd ──► Lidarr ──► Soularr          │
│    │                                    │
│    └────────────► Navidrome ◄──────────┘
│                        │
│                        ▼
│            Kairo Sync API (FastAPI)
│                        │
│                        ▼
│                  Tailscale VPN
└────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────┐
│ Kairo Android App                      │
│ Kotlin + Compose + Hilt                │
│ Media3 / ExoPlayer + Room              │
│ Navidrome/Subsonic + local files       │
└────────────────────────────────────────┘
```

### Server Stack

| Component | Role |
| :--- | :--- |
| slskd | Soulseek client for FLAC downloads |
| Lidarr | Library manager and quality profiles |
| Soularr | Bridge between Lidarr and slskd |
| Navidrome | Subsonic-compatible streaming server |
| Kairo Sync API | FastAPI endpoint for manual sync and library refresh |
| Tailscale | Private VPN for remote access |

### Android Client (Kairo)

| Layer | Technology |
| :--- | :--- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Playback | Media3 / ExoPlayer |
| DI | Hilt |
| Persistence | Room |
| Networking | Retrofit, OkHttp, Kotlin Serialization |
| Auth | Subsonic token+salt configuration |

## 📊 Current Status

### ✅ What Is Built

- Android application entry points and Compose shell in [app/src/main/java/com/kairo/player/MainActivity.kt](app/src/main/java/com/kairo/player/MainActivity.kt), [app/src/main/java/com/kairo/player/KairoApplication.kt](app/src/main/java/com/kairo/player/KairoApplication.kt), and [app/src/main/java/com/kairo/player/ui/KairoApp.kt](app/src/main/java/com/kairo/player/ui/KairoApp.kt).
- Media3-based playback service and controller in [app/src/main/java/com/kairo/player/playback/PlaybackService.kt](app/src/main/java/com/kairo/player/playback/PlaybackService.kt), [app/src/main/java/com/kairo/player/playback/PlaybackController.kt](app/src/main/java/com/kairo/player/playback/PlaybackController.kt), [app/src/main/java/com/kairo/player/playback/AudioPlayer.kt](app/src/main/java/com/kairo/player/playback/AudioPlayer.kt), and [app/src/main/java/com/kairo/player/playback/PlaybackState.kt](app/src/main/java/com/kairo/player/playback/PlaybackState.kt).
- Audio format and lossless detection in [app/src/main/java/com/kairo/player/audio/AudioFormatInfo.kt](app/src/main/java/com/kairo/player/audio/AudioFormatInfo.kt), [app/src/main/java/com/kairo/player/audio/AudioQualityManager.kt](app/src/main/java/com/kairo/player/audio/AudioQualityManager.kt), and [app/src/main/java/com/kairo/player/audio/AudioDiagnostics.kt](app/src/main/java/com/kairo/player/audio/AudioDiagnostics.kt).
- Local music source and SAF-based file picking in [app/src/main/java/com/kairo/player/source/local/LocalMusicSource.kt](app/src/main/java/com/kairo/player/source/local/LocalMusicSource.kt), [app/src/main/java/com/kairo/player/source/local/LocalAudioDocumentStore.kt](app/src/main/java/com/kairo/player/source/local/LocalAudioDocumentStore.kt), and [app/src/main/java/com/kairo/player/source/local/SafLocalAudioDocumentStore.kt](app/src/main/java/com/kairo/player/source/local/SafLocalAudioDocumentStore.kt).
- Navidrome/Subsonic integration in [app/src/main/java/com/kairo/player/server/ServerConfig.kt](app/src/main/java/com/kairo/player/server/ServerConfig.kt), [app/src/main/java/com/kairo/player/server/SubsonicAuthInterceptor.kt](app/src/main/java/com/kairo/player/server/SubsonicAuthInterceptor.kt), [app/src/main/java/com/kairo/player/server/SubsonicUrlBuilder.kt](app/src/main/java/com/kairo/player/server/SubsonicUrlBuilder.kt), [app/src/main/java/com/kairo/player/server/SubsonicModels.kt](app/src/main/java/com/kairo/player/server/SubsonicModels.kt), [app/src/main/java/com/kairo/player/server/NavidromeApiService.kt](app/src/main/java/com/kairo/player/server/NavidromeApiService.kt), and [app/src/main/java/com/kairo/player/source/navidrome/NavidromeMusicSource.kt](app/src/main/java/com/kairo/player/source/navidrome/NavidromeMusicSource.kt).
- Music source abstraction and quality selection in [app/src/main/java/com/kairo/player/source/MusicSource.kt](app/src/main/java/com/kairo/player/source/MusicSource.kt), [app/src/main/java/com/kairo/player/source/MusicSourceRegistry.kt](app/src/main/java/com/kairo/player/source/MusicSourceRegistry.kt), [app/src/main/java/com/kairo/player/source/QualityResolver.kt](app/src/main/java/com/kairo/player/source/QualityResolver.kt), and [app/src/main/java/com/kairo/player/source/StreamResolver.kt](app/src/main/java/com/kairo/player/source/StreamResolver.kt).
- Room persistence for tracks, artists, albums, playlists, and history under [app/src/main/java/com/kairo/player/data/local](app/src/main/java/com/kairo/player/data/local) and [app/src/main/java/com/kairo/player/data/repository](app/src/main/java/com/kairo/player/data/repository).
- Media cache layer in [app/src/main/java/com/kairo/player/cache/AudioCache.kt](app/src/main/java/com/kairo/player/cache/AudioCache.kt) and [app/src/main/java/com/kairo/player/cache/Media3AudioCache.kt](app/src/main/java/com/kairo/player/cache/Media3AudioCache.kt).
- Docker-based server scaffolding in [docker-compose.yml](docker-compose.yml) and the FastAPI endpoint in [kairo-sync-api/main.py](kairo-sync-api/main.py).

### 🚧 What Is In Progress

- The app layer is functional as a local + Navidrome foundation, but end-to-end validation against a real server stack remains incomplete.
- The deployment stack is defined in Docker Compose, but credentials and volume paths remain placeholders.
- The project has a strong UI shell and playback foundation, but not a fully matured admin sync workflow or server orchestration layer.

### ❌ What Is Not Started

- Oracle Cloud deployment of the Docker stack.
- Full library browsing flows for albums/artists/tracks beyond the base UI shell.
- Admin sync UI with polling/progress state.
- Local Basic Auth with admin/user roles.
- Production-grade end-to-end testing against a live Navidrome instance.
- P2 EQ automation pipeline based on song metadata.

## 🛠️ Tech Stack

- Kotlin 2.2.20
- AGP 8.13.2
- KSP 2.2.20-2.0.2
- Hilt 2.57.1
- Room 2.8.3
- Media3 1.11.1
- Retrofit 3.0.0
- OkHttp 4.12.0
- Kotlin Serialization 1.9.0
- Jetpack Compose BOM 2025.10.00
- Coroutines 1.10.2
- JUnit 4.13.2
- Robolectric 4.17
- Android compileSdk 36, minSdk 26, Java 17

## 📁 Project Structure

```text
app/src/main/java/com/kairo/player/
├── audio/                  # format metadata and diagnostics
├── cache/                 # Media3 cache wrapper
├── data/
│   ├── local/             # Room database, converters, entities, DAOs
│   └── repository/       # track, playlist, and history access
├── di/                   # Hilt modules
├── domain/
│   └── model/            # Track, Artist, Album, StreamInfo, etc.
├── network/              # Retrofit and OkHttp setup
├── playback/             # ExoPlayer service, controller, queue, state
├── server/               # Navidrome/Subsonic networking and config
├── source/
│   ├── local/            # local file import and metadata parsing
│   ├── mock/             # fixture source
│   ├── navidrome/        # remote provider implementation
│   └── ...               # registry and quality selection
├── ui/
│   ├── components/       # reusable Compose UI pieces
│   ├── screens/          # Home, Search, Queue, Library, Diagnostics, Settings
│   ├── theme/            # styling and theme
│   └── ...               # app shell and viewmodel
├── KairoApplication.kt   # Hilt app entry point
├── MainActivity.kt       # Compose host activity
└── util/                 # reserved for future helpers
```

## 🚀 Getting Started

### Prerequisites

- Android Studio latest stable
- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Docker for the server stack
- Oracle Cloud free-tier VM for deployment

### Build the Android App

```bash
./gradlew assembleDebug
./gradlew test
```

### Deploy the Server Stack

The repository contains the foundation of a private server stack in [docker-compose.yml](docker-compose.yml). The intended deployment is:

1. slskd for FLAC acquisition
2. Lidarr for library management
3. Soularr for the Lidarr-to-slskd bridge
4. Navidrome for streaming
5. Kairo Sync API for manual sync triggers
6. Tailscale for private remote access

This is a deployment plan, not a fully proven production environment yet.

## 🔐 Security Model

- Kairo is intended for personal, private use and is not distributed publicly.
- Navidrome/Subsonic requests use token+salt-based authentication stored locally in the app.
- The FastAPI sync endpoint in [kairo-sync-api/main.py](kairo-sync-api/main.py) uses Basic Auth.
- Remote access is intended to happen through Tailscale rather than direct Internet exposure.
- Secret management and production credentials are still placeholders in the repo.

## 🗺️ Roadmap

### P1 (Current)

- [ ] Deploy Docker stack to the Oracle Cloud VM
- [ ] Configure slskd, Lidarr, Soularr, and Navidrome
- [ ] Implement complete library browsing screens in the app
- [ ] Add admin sync screen with progress polling
- [ ] Implement local Basic Auth with admin/user roles
- [ ] Test end-to-end search, streaming, and sync flow

### P2 (Future)

- [ ] Fetch per-song audio features such as energy, valence, and tempo
- [ ] Map audio features to EQ presets
- [ ] Apply EQ through Media3 effects in ExoPlayer
- [ ] Store EQ profiles per track in Room

## 🤝 Contributing

This is a personal project and is not open for external contributions.

## 📜 License

Private / personal use only. Not for distribution.

## 🙏 Acknowledgements

- [Navidrome](https://www.navidrome.org/)
- [Lidarr](https://lidarr.audio/)
- [slskd](https://github.com/slskd/slskd)
- [Soularr](https://github.com/mrusse08/soularr)
- [Tailscale](https://tailscale.com/)
- [Media3 ExoPlayer](https://developer.android.com/media/media3)
