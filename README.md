# ArturChat

Android-first internet messenger built with Kotlin, Jetpack Compose and Supabase.

## Current development milestone — one-to-one audio calls

- Native Android interface: Chats, Calls, Contacts, Profile, plus an incoming-call dialog and active audio-call screen.
- Supabase Auth email/password registration, sign-in, persistent session status and sign-out.
- Live direct chats, username lookup, message loading/sending and Realtime message synchronization.
- One-to-one audio calling with WebRTC: microphone capture, offer/answer negotiation, ICE candidate exchange, accept/decline/end, mute and call-state updates.
- Supabase Realtime carries call invitations and signaling; call and signaling rows are protected by participant-only RLS.
- Android system notifications support foreground Realtime and Firebase Cloud Messaging (FCM). The FCM client and server function are implemented; remote delivery becomes active after Firebase credentials and Supabase Database Webhooks are configured.
- WebRTC Android SDK io.github.webrtc-sdk:android:150.7871.04.
- GitHub Actions tests and builds the Android APK.
- Video calls and group calls are not implemented yet. FCM push delivery requires the Firebase and Supabase webhook setup below.

## Supabase project isolation

The legacy NASTOLKA database objects have been removed from the connected Supabase project. The remaining application schema belongs to ArturChat and uses the `face_video_` prefix plus its `face_video_private` helper schema.

## Current backend status

The Android code now targets Supabase. The ArturChat schema is applied to the connected Supabase project. Its tables and database functions are namespaced (`face_video_*` and `face_video_private`). All NASTOLKA-specific tables, functions, policies, and Realtime publication entries have been removed. GitHub Actions confirms that `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` are available to the build.

The configured chat list and conversation screen use the live repository. The user has confirmed successful two-account registration, live message exchange and synchronization. The call tables and signaling permissions are applied; audio calls are the active development milestone.

## Audio call implementation notes

- Calls are currently one-to-one and are started from the phone icon in a direct chat.
- Android requests microphone permission when the user starts or accepts a call.
- Incoming call prompts use Supabase Realtime while the app is open. Once FCM credentials and database webhooks are configured, messages and incoming calls can also be delivered while the app is backgrounded or closed.
- WebRTC waits for ICE gathering before sending the local SDP and includes two public STUN endpoints. Some mobile networks, carrier-grade NATs and restrictive firewalls still require a TURN relay; a free Metered/Open Relay credential can be configured as the next connectivity step.
- The call tables are public.face_video_calls and public.face_video_call_signals. Only authenticated users receive access, call rows are visible to their two participants, and signaling events can be read only by their recipient.
- The database guards call status transitions: only the callee can accept/decline, only the caller can cancel a ringing call, and either participant can end an accepted call.
- Group calls and video tracks/renderers are not enabled yet.

## Connect your Supabase project

1. Open the Supabase project connected to ArturChat.
2. In **Project Settings → API Keys**, copy the Project URL and the **publishable key** (the key beginning with `sb_publishable_`, or the legacy anon key if your project still uses it).
3. In GitHub, open **Settings → Secrets and variables → Actions → New repository secret**.
4. Create:
   - `SUPABASE_URL` — your project URL, e.g. `https://your-project-ref.supabase.co`
   - `SUPABASE_PUBLISHABLE_KEY` — the project's publishable key.
5. The initial ArturChat schema and foreign-key indexes have already been applied. Keep migration files in sync with the versions `20261031070255` and `20261031070449`.
6. In Supabase **Authentication → Providers**, leave Email enabled. Confirm whether email verification should be required before first login.
7. Realtime is enabled for `public.face_video_messages`; no manual table selection is needed.

**Never put a Supabase secret/service_role key in the Android app, GitHub variables, or this repository.** The Android app uses only the publishable key and relies on database RLS for authorization.

## Push notifications (Firebase Cloud Messaging)

ArturChat now includes the Android FCM service, per-account device-token registration and the deployed Supabase Edge Function `send-arturchat-push`. Tokens are stored in `public.face_video_push_tokens`; RLS restricts each user's rows to that user. The final external credentials and webhooks still need one-time setup:

1. In Firebase Console, create or select a project and add an Android app with package ID `com.facevideo.app`. Download `google-services.json` and add it in GitHub at `app/google-services.json` (the file contains client configuration, not a private service-account key). The Gradle build automatically activates Google Services when this file is present.
2. In Firebase Console → Project settings → Service accounts, generate a private service-account JSON key. Do not commit it or add it to the Android app. Keep it for the Supabase Edge Function secret in the next step.
3. In Supabase Dashboard → Edge Functions → Secrets, set:
   - `FCM_SERVICE_ACCOUNT_JSON` — the complete Firebase service-account JSON.
   - `ARTURCHAT_PUSH_WEBHOOK_SECRET` — a long random value that will be shared only with the two database webhooks.
   - `SUPABASE_SERVICE_ROLE_KEY` — the server-only Supabase service-role/secret key. It must never be placed in Android or GitHub.
4. Create two Supabase Database Webhooks (POST / INSERT), one for `public.face_video_messages` and one for `public.face_video_calls`. Both call `https://mitvjkhlsvxcnzhbiwye.supabase.co/functions/v1/send-arturchat-push`. Add header `x-arturchat-webhook-secret` with the same value as `ARTURCHAT_PUSH_WEBHOOK_SECRET`, plus `Content-Type: application/json`.
5. Build and install the signed release on each phone. Sign in and open ArturChat once on each device so its FCM token is registered. On Android 13+, grant notification permission.

The Edge Function sends new-message notifications to the other members of a chat and incoming-call notifications only to the callee. It ignores message notifications sent by the same account. The Android app also keeps its foreground Realtime notification fallback. Phone manufacturers may apply their own battery and background restrictions.

The Firebase service-account key, Supabase service-role key and webhook secret are private server credentials. Only the Android `google-services.json` client config belongs in the app build.

## GitHub Actions

The workflow reads `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` from repository Actions secrets and passes them into Gradle at build time. When these secrets are absent, the app builds in demo mode instead of failing. Never use `service_role` or `sb_secret_...` keys in the mobile app.

Open **Actions → Android build**. On success, download the `arturchat-debug-apk` artifact.

Minimum supported Android version: Android 8.0 (API 26).


## ArturChat in-app updates

- The app checks the public GitHub Releases endpoint when it starts.
- When a newer release exists, ArturChat offers an update inside the app.
- The APK is downloaded in the background, SHA-256 checked, then passed to Android Package Installer.
- Android displays its own final confirmation screen. On first use, the user must allow ArturChat to install updates from this source in Android settings.
- The application ID intentionally remains `com.facevideo.app` to preserve Android installation identity.

### One-time release signing setup

The release workflow reads the following repository Actions secrets from the private signing setup archive:

- `ARTURCHAT_KEYSTORE_BASE64`
- `ARTURCHAT_KEYSTORE_PASSWORD`
- `ARTURCHAT_KEY_ALIAS`
- `ARTURCHAT_KEY_PASSWORD`

Keep the keystore and passwords backed up privately. Never commit them or include them in an APK.

The first stable release, `v0.4.0`, has already been published. To publish a later release, open **Actions → ArturChat release → Run workflow**, choose the `main` branch, and enter the next semantic version in `release_tag`. The workflow creates the tag, tests the app, verifies the APK signature, calculates its SHA-256 checksum and publishes both assets. The current stable release is `v0.4.3`; the next fix release is planned as `v0.4.4` after Android Actions verification and the two-device call test. Tag pushes also trigger the workflow automatically.

The current GitHub Actions debug APK is for testing only. It is not the signed release channel used by the updater.

**Important:** the first release signed with the new permanent key cannot update an already-installed APK signed with a different temporary debug key. Install the first signed ArturChat release once; all later releases can update over it without manually downloading APK files. Supabase messages are stored online, so they remain on the server, but you may need to sign in again after that one-time change of signing key.

The current published stable release is v0.4.3. Release v0.4.4 contains the ICE gathering and Android message-notification fixes; it must still be verified on two physical phones.

The self-hosted updater is intended for direct APK distribution. If ArturChat is later published on Google Play, switch to the Play Core in-app update flow and remove the sideload-only install permission.
