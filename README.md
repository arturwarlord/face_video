# ArturChat

Android-first internet messenger built with Kotlin, Jetpack Compose and Supabase.

## Current milestone — 0.4.0

- Native Android interface: Chats, Calls, Contacts, Profile.
- Demo conversations remain available only when backend credentials are absent; configured users receive their live Supabase conversations.
- Supabase Auth email/password sign-up, sign-in, persistent session status and sign-out.
- Supabase Kotlin SDK 3.8.0 with PostgREST and Realtime modules.
- Live chat list, username lookup, direct-chat creation, message loading/sending, and Realtime subscriptions.
- Applied PostgreSQL migrations for isolated ArturChat profiles, direct/group chats, members, messages, triggers, indexes, Realtime and Row Level Security.
- GitHub Actions builds and tests the Android APK.

## Supabase project isolation

The connected Supabase project already contains the NASTOLKA application's tables. ArturChat therefore uses only objects with the `arturchat_` prefix and its own `arturchat_private` helper schema. It does not rename, drop, or modify NASTOLKA tables, policies, or functions. Both apps still share the same Supabase project, Auth tenant, Realtime publication, and project quotas.

## Current backend status

The Android code now targets Supabase. The isolated ArturChat schema is already applied to the connected Supabase project. Its tables and database functions are namespaced (`arturchat_*` and `arturchat_private`), while NASTOLKA's tables and functions remain untouched. GitHub Actions confirms that `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` are available to the build.

The configured chat list and conversation screen now use the live repository. Real registration and message exchange still need a hands-on test with two separate email accounts; video calling is a later milestone and does not connect yet.

## Connect your Supabase project

1. Open the connected Supabase project (the current project is also used by NASTOLKA).
2. In **Project Settings → API Keys**, copy the Project URL and the **publishable key** (the key beginning with `sb_publishable_`, or the legacy anon key if your project still uses it).
3. In GitHub, open **Settings → Secrets and variables → Actions → New repository secret**.
4. Create:
   - `SUPABASE_URL` — your project URL, e.g. `https://your-project-ref.supabase.co`
   - `SUPABASE_PUBLISHABLE_KEY` — the project's publishable key.
5. The initial ArturChat schema and foreign-key indexes have already been applied. Keep migration files in sync with the versions `20261031070255` and `20261031070449`.
6. In Supabase **Authentication → Providers**, leave Email enabled. Confirm whether email verification should be required before first login.
7. Realtime is enabled for `public.arturchat_messages`; no manual table selection is needed.

**Never put a Supabase secret/service_role key in the Android app, GitHub variables, or this repository.** The Android app uses only the publishable key and relies on database RLS for authorization.

## GitHub Actions

The workflow reads `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` from repository Actions secrets and passes them into Gradle at build time. When these secrets are absent, the app builds in demo mode instead of failing. Never use `service_role` or `sb_secret_...` keys in the mobile app.

Open **Actions → Android build**. On success, download the `face-video-debug-apk` artifact.

Minimum supported Android version: Android 8.0 (API 26).


## ArturChat in-app updates

- The app checks the public GitHub Releases endpoint when it starts.
- When a newer release exists, ArturChat offers an update inside the app.
- The APK is downloaded in the background, SHA-256 checked, then passed to Android Package Installer.
- Android displays its own final confirmation screen. On first use, the user must allow ArturChat to install updates from this source in Android settings.
- The application ID intentionally remains `com.facevideo.app` to preserve Android installation identity.

### One-time release signing setup

Before publishing the first stable release, add the following repository Actions secrets from the private signing setup archive:

- `ARTURCHAT_KEYSTORE_BASE64`
- `ARTURCHAT_KEYSTORE_PASSWORD`
- `ARTURCHAT_KEY_ALIAS`
- `ARTURCHAT_KEY_PASSWORD`

Keep the keystore and passwords backed up privately. Never commit them or include them in an APK.

After the secrets are saved, create a GitHub Release tag such as `v0.4.0`. The **ArturChat release** workflow tests the app, builds a release-signed APK, calculates its SHA-256 checksum and publishes both assets. Future tags (for example `v0.4.1`) become in-app updates automatically.

The current GitHub Actions debug APK is for testing only. It is not the signed release channel used by the updater.

**Important:** the first release signed with the new permanent key cannot update an already-installed APK signed with a different temporary debug key. Install the first signed ArturChat release once; all later releases can update over it without manually downloading APK files. Supabase messages are stored online, so they remain on the server, but you may need to sign in again after that one-time change of signing key.

The self-hosted updater is intended for direct APK distribution. If ArturChat is later published on Google Play, switch to the Play Core in-app update flow and remove the sideload-only install permission.
