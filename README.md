# Face Video

Android-first internet messenger with a dark, modern interface. The first milestone establishes the native Android project, navigation, chat UI, local message composition, and automated APK builds.

## Current milestone — 0.1.0

- Native Android app built with Kotlin and Jetpack Compose.
- Main sections: Chats, Calls, Contacts, Profile.
- Chat search and sample conversations.
- Message composition inside a conversation (local in-memory demo only).
- Call entry points prepared in the interface.
- GitHub Actions runs unit tests and builds a debug APK on pushes to `main`.

## Important status

This is an **interface and local interaction prototype**. Messages are not sent to another device, accounts are not registered, and audio/video calls do not connect yet. The next milestones add authentication, a real-time messaging backend, media storage, push notifications, and WebRTC calling.

## Build

The GitHub Actions workflow is `.github/workflows/android.yml`. Open the **Actions** tab, select **Android build**, and open the latest run. After a successful run, download the `face-video-debug-apk` artifact.

The app supports Android 8.0 (API 26) and later. The initial target is Android; iOS and desktop are not included in this milestone.
