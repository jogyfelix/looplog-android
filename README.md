# LoopLog Android — Portfolio Snapshot

LoopLog is a multi-module Android habit tracker built with Kotlin and Jetpack Compose. This repository is a sanitized, source-only snapshot published to demonstrate the application's architecture and implementation during interviews.

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

## Sanitization notice

This is not the production repository. The following are intentionally omitted:

- Git history, branches, tags, and release artifacts
- Firebase project configuration and API identifiers
- Sentry DSN and production observability configuration
- Signing keys and release credentials
- Play Store and deployment automation
- The private on-device model binary
- Internal planning files and machine-specific configuration

Some service-backed functionality therefore requires private configuration and this snapshot is not maintained as a standalone distribution. The production repository remains private.

## Technology

Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager, Navigation, Firebase integrations, Sentry, LiteRT, ML Kit GenAI, App Functions, JUnit 5, MockK, Truth, Robolectric, and Gradle version catalogs.

## Rights

Copyright © 2026. All rights reserved. This repository is source-visible for portfolio review only. No license is granted to copy, modify, distribute, sublicense, or commercially use the original project code or assets.

Third-party components remain subject to their respective licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
