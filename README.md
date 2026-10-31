# Face Video

Android-first internet messenger built with Kotlin and Jetpack Compose.

## Current milestone — 0.2.0

- Native Android interface: Chats, Calls, Contacts, Profile.
- Chat search and local demo conversations.
- Firebase Email/Password sign-in and registration screen (activated when Firebase config is supplied).
- Firestore repository foundation for direct/group chats and live message listeners.
- Firestore security rules for user profiles, chat membership, and message creation.
- GitHub Actions unit tests and debug APK build.

## Honest implementation status

The app still uses sample conversations in the main chat screen. Real registration becomes active after a Firebase Android app is configured. The Firestore repository is prepared but is not yet connected to the visible chat list and message composer. Audio/video calls are not connected yet.

## Firebase setup (next integration step)

1. Create a Firebase project and add an Android app with package name `com.facevideo.app`.
2. Enable **Authentication → Email/Password**.
3. Create a **Cloud Firestore** database.
4. Publish the repository's `firestore.rules` in Firebase Console.
5. Download `google-services.json` for the Android app.
6. In GitHub, open **Settings → Secrets and variables → Actions → New repository secret**.
7. Add a secret named `FIREBASE_ANDROID_CONFIG` and paste the complete contents of `google-services.json` as its value.
8. Run the **Android build** workflow again. It restores the config file only during the build; the JSON file is ignored by Git.

Do not add Firebase Admin SDK service-account keys or private server credentials to the Android app or this repository. The Android Firebase config is not a replacement for Firestore security rules.

## Free-tier notes

Firebase Spark includes no-cost Authentication methods (excluding phone/SMS verification), Cloud Messaging, and bounded Firestore quotas. Exceeding a Spark quota can suspend that service until the quota resets; monitor usage before inviting many users.

## Build

Open **Actions → Android build**. On success, download the `face-video-debug-apk` artifact. Minimum supported Android version: Android 8.0 (API 26).
