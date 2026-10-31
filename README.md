# Face Video

Android-first internet messenger built with Kotlin, Jetpack Compose and Supabase.

## Current milestone — 0.3.0

- Native Android interface: Chats, Calls, Contacts, Profile.
- Demo conversations and local message composition remain available before the backend is configured.
- Supabase Auth email/password sign-up, sign-in, persistent session status and sign-out.
- Supabase Kotlin SDK 3.8.0 with PostgREST and Realtime modules.
- Database repository for listing chat summaries, loading/sending messages and observing realtime message changes.
- PostgreSQL migration for profiles, direct/group chats, chat members, messages, indexes, triggers and Row Level Security policies.
- GitHub Actions builds and tests the Android APK.

## Current backend status

The Android code now targets Supabase. The migration is committed under `supabase/migrations`, but it has **not yet been applied to your Supabase project**. The real backend will work after the project URL and publishable key are added to GitHub and the migration is deployed.

The sample chat screen is still a UI preview; it has not yet been wired to the live repository. That is the next app-integration milestone.

## Connect your Supabase project

1. Open the Supabase Dashboard and create/open the Face Video project.
2. In **Project Settings → API Keys**, copy the Project URL and the **publishable key** (the key beginning with `sb_publishable_`, or the legacy anon key if your project still uses it).
3. In GitHub, open **Settings → Secrets and variables → Actions → New repository secret**.
4. Create:
   - `SUPABASE_URL` — your project URL, e.g. `https://your-project-ref.supabase.co`
   - `SUPABASE_PUBLISHABLE_KEY` — the project's publishable key.
5. Apply the migration in `supabase/migrations/202610310001_initial_schema.sql` using Supabase migrations or the SQL Editor.
6. In Supabase **Authentication → Providers**, leave Email enabled. Confirm whether email verification should be required before first login.
7. Enable Realtime for the `public.messages` table if the migration did not automatically add it to the `supabase_realtime` publication.

**Never put a Supabase secret/service_role key in the Android app, GitHub variables, or this repository.** The Android app uses only the publishable key and relies on database RLS for authorization.

## GitHub Actions

The workflow reads `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` from repository Actions secrets and passes them into Gradle at build time. When these secrets are absent, the app builds in demo mode instead of failing.

Open **Actions → Android build**. On success, download the `face-video-debug-apk` artifact.

Minimum supported Android version: Android 8.0 (API 26).
