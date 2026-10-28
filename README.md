# looplog Android

looplog is a multi-module Android habit tracker built with Kotlin and Jetpack Compose. It supports habit scheduling, progress tracking, statistics, home-screen widgets, encrypted backups, biometric locking, and on-device AI-assisted habit creation.

## Highlights

- Multi-module clean architecture across `app`, `feature`, and `core` modules
- Jetpack Compose UI with adaptive theming and reusable design-system components
- Room persistence, DataStore preferences, Hilt dependency injection, and WorkManager jobs
- Habit scheduling, widgets, statistics, encrypted backup/import, and biometric app locking
- On-device generative AI integration with schema validation and safety handling
- App Functions and Android platform integrations
- Unit, Robolectric, and instrumentation test coverage

## Project structure

```text
app/                 Application shell, navigation, notifications, and platform integration
feature/habit/       Habit creation, tracking, statistics, alarms, widgets, and AI flows
feature/settings/    Preferences, appearance, backup, privacy, and feedback UI
core/ai-engine/      On-device model orchestration and structured generation
core/analytics/      Analytics abstractions and provider implementations
core/data/           Repository implementations and backup handling
core/domain/         Use cases
core/storage/        Room database and DataStore persistence
core/design-system/  Compose theme, typography, colors, and shared visual primitives
core/model/          Shared domain models
core/ui/             Reusable UI components
core/utils/          Shared utilities
model_pack/          Play Asset Delivery module definition; model binary omitted
```

## Configuration

Service credentials and environment-specific files are not included. Firebase-backed features require an `app/google-services.json` file, Sentry requires project-specific configuration, and the Play Asset Delivery module requires a compatible LiteRT model binary.

Android signing configuration is supplied separately for release builds. Local Android SDK configuration belongs in `local.properties`.

## Technology

Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager, Navigation, Firebase integrations, Sentry, LiteRT, ML Kit GenAI, App Functions, JUnit 5, MockK, Truth, Robolectric, and Gradle version catalogs.

## Rights

Copyright © 2026. All rights reserved. No license is granted to copy, modify, distribute, sublicense, or commercially use the original project code or assets.

Third-party components remain subject to their respective licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
