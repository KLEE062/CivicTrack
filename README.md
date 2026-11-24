# CivicTrack

**Employee Management and Supervision System**

CivicTrack is an Android-based employee management and supervision application designed to support workforce attendance management, employee monitoring, and organizational oversight.

## Project Setup

### Prerequisites

- Android Studio
- Android SDK
- Kotlin-supported development environment
- Internet connection

### Run Locally

1. Open Android Studio.
2. Select **Open** and choose the CivicTrack project directory.
3. Allow Android Studio to complete Gradle synchronization.
4. Configure the required environment variables using `.env.example` as a reference.
5. Ensure that local configuration and credentials remain private.
6. Build and run the application on an emulator or compatible Android device.

## Technology Stack

- Kotlin
- Jetpack Compose
- Android SDK
- Supabase

## Project Structure

- `app/src/main/java/` — Application source code.
- `app/src/main/res/` — Android resources.
- `app/src/test/` — Unit tests.
- `app/src/androidTest/` — Instrumented tests.
- `gradle/` — Gradle wrapper and configuration.

## Security

- Do not commit `.env` files containing actual credentials.
- Keep private keys and service-role credentials out of version control.
- Use `.env.example` for documenting required configuration variables.

## Development

This repository contains the CivicTrack Android application and its supporting project configuration.
