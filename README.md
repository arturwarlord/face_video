# Face Video

Android-first internet messenger built with Kotlin, Jetpack Compose and Supabase.

## Current milestone — 0.3.0

- Native Android interface: Chats, Calls, Contacts, Profile.
- Demo conversations remain available only when backend credentials are absent; configured users receive their live Supabase conversations.
- Supabase Auth email/password sign-up, sign-in, persistent session status and sign-out.
- Supabase Kotlin SDK 3.8.0 with PostgREST and Realtime modules.
- Live chat list, username lookup, direct-chat creation, message loading/sending, and Realtime subscriptions.
- Applied PostgreSQL migrations for isolated Face Video profiles, direct/group chats, members, messages, triggers, indexes, Realtime and Row Level Security.
- GitHub Actions builds and tests the Android APK.

## Supabase project isolation

The connected Supabase project already contains the NASTOLKA application's tables. Face Video therefore uses only objects with the `face_video_` prefix and its own `face_video_private` helper schema. It does not rename, drop, or modify NASTOLKA tables, policies, or functions. Both apps still share the same Supabase project, Auth tenant, Realtime publication, and project quotas.

## Current backend status

The Android code now targets Supabase. The isolated Face Video schema is already applied to the connected Supabase project. Its tables and database functions are namespaced (`face_video_*` and `face_video_private`), while NASTOLKA's tables and functions remain untouched. GitHub Actions confirms that `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` are available to the build.

The configured chat list and conversation screen now use the live repository. Real registration and message exchange still need a hands-on test with two separate email accounts; video calling is a later milestone and does not connect yet.

## Connect your Supabase project

1. Open the connected Supabase project (the current project is also used by NASTOLKA).
2. In **Project Settings → API Keys**, copy the Project URL and the **publishable key** (the key beginning with `sb_publishable_`, or the legacy anon key if your project still uses it).
3. In GitHub, open **Settings → Secrets and variables → Actions → New repository secret**.
4. Create:
   - `SUPABASE_URL` — your project URL, e.g. `https://your-project-ref.supabase.co`
   - `SUPABASE_PUBLISHABLE_KEY` — the project's publishable key.
5. The initial Face Video schema and foreign-key indexes have already been applied. Keep migration files in sync with the versions `20261031070255` and `20261031070449`.
6. In Supabase **Authentication → Providers**, leave Email enabled. Confirm whether email verification should be required before first login.
7. Realtime is enabled for `public.face_video_messages`; no manual table selection is needed.

**Never put a Supabase secret/service_role key in the Android app, GitHub variables, or this repository.** The Android app uses only the publishable key and relies on database RLS for authorization.

## GitHub Actions

The workflow reads `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` from repository Actions secrets and passes them into Gradle at build time. When these secrets are absent, the app builds in demo mode instead of failing. Never use `service_role` or `sb_secret_...` keys in the mobile app.

Open **Actions → Android build**. On success, download the `face-video-debug-apk` artifact.

Minimum supported Android version: Android 8.0 (API 26).
