# ArturChat

Android-first internet messenger built with Kotlin, Jetpack Compose and Supabase.

## Current development milestone — one-to-one audio calls

- Native Android interface: Chats, Calls, Contacts, Profile, plus an incoming-call dialog, actionable Android CallStyle notification (Accept/Decline), and active audio-call screen.
- Supabase Auth email/password registration, sign-in, persistent session status and sign-out.
- Live direct chats, username lookup, chronological Realtime message ordering and local-time timestamps.
- Per-chat and aggregate unread counters; when a chat has unread messages, the inbox preview shows the newest unread incoming message rather than a newer outgoing message.
- Per-recipient delivery/read receipts: one check for sent, two grey checks for delivered, and two blue checks for read. Receipt updates synchronize through Supabase Realtime.
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

The configured chat list and conversation screen use the live repository. Two-account registration, live message exchange, unread counters and read receipts are implemented. The call tables and signaling permissions are applied; audio calls remain available.

## Audio call implementation notes

- Calls are currently one-to-one and are started from the phone icon in a direct chat.
- Android requests microphone permission when the user starts or accepts a call.
- Incoming call prompts use Supabase Realtime while the app is open. Once FCM credentials and database webhooks are configured, messages and incoming calls can also be delivered while the app is backgrounded or closed.
- WebRTC waits for ICE gathering before sending the local SDP and includes two public STUN endpoints. It can also fetch short-lived TURN credentials through the authenticated Supabase Edge Function `get-arturchat-turn-credentials`; the Metered API key stays server-side. Networks with restrictive NAT/firewalls may still need this relay configured.
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

## Direct peer-to-peer audio calls (no TURN relay)

- Audio uses WebRTC directly between the two phones. Supabase transports signaling only; it does not relay voice media.
- STUN discovers public-facing addresses. No TURN server, Metered account, Open Relay endpoint, or media relay is configured.
- SDP includes locally gathered ICE candidates, with trickle ICE as a fallback. Signal rows are also polled as a recovery path alongside Realtime.
- If an established direct route is lost, the caller performs one ICE restart. The callee can request it through an ICE control signal. The restart renegotiates offer/answer and gathers a fresh ICE candidate generation without creating a new call record.
- The Android client records the selected candidate pair (candidate type, transport, address family), whether ICE is receiving packets, and recovery status. Raw IP addresses are not shown on the call screen.
- A single ICE restart can recover transient NAT mapping or network-path changes, but it cannot guarantee connectivity through symmetric/strict NAT or a carrier that blocks peer-to-peer UDP. Those cases still require TURN relay.
- STUN endpoints used for discovery: `stun:stun.l.google.com:19302` and `stun:stun.cloudflare.com:3478`. Voice media is not sent to these STUN services.
- Call signaling remains participant-scoped by RLS in `public.face_video_calls` and `public.face_video_call_signals`. The restart request uses the existing `ice` signal type and requires no database schema migration.
- Incoming call signaling is available while the app maintains its Supabase Realtime connection. Reliable wake-up after Android kills the app remains a separate push-notification requirement.


## Push notifications (Firebase Cloud Messaging)

ArturChat contains an Android FCM receiver, per-account device-token registration, and the deployed Supabase Edge Function `send-arturchat-push`. The push delivery path is not active until a Firebase Android config, server FCM key, and two Supabase Database Webhooks are configured.

**Current verified status (2026-11-02):** the Supabase push-token table has zero registered devices, and there are no Database Webhooks/trigger calls configured for `face_video_messages` or `face_video_calls`. The Android build previously did not receive `google-services.json`, so it compiled with FCM registration disabled.

Follow [docs/PUSH_NOTIFICATIONS_SETUP.md](docs/PUSH_NOTIFICATIONS_SETUP.md) for the one-time setup. GitHub Actions now accepts the Android client config through the repository secret `FIREBASE_GOOGLE_SERVICES_BASE64`, writes it to `app/google-services.json` only during the build, validates package ID `com.facevideo.app`, and keeps the file ignored by Git. The same optional step is included in debug, signed-candidate, and release workflows.

The Firebase service-account JSON and webhook secret belong only in Supabase Edge Function Secrets. Never store the service-account private key in GitHub, Android, or the repository. Supabase provides the legacy `SUPABASE_SERVICE_ROLE_KEY` environment variable to hosted Edge Functions; it must never be put in the Android app.

The Edge Function sends new-message notifications to the other members of a chat and incoming-call notifications only to the callee. After setup, open ArturChat on each phone once and sign in so each FCM token is registered. On Android 13+, allow notification permission. Test incoming messages and calls with the app backgrounded (not force-stopped), then test after locking the screen.

**Provider note:** Android push transport uses Google Firebase Cloud Messaging. It does not carry call audio; voice remains direct WebRTC peer-to-peer. If a strict no-Google requirement applies to push too, this provider must be replaced and the compatibility/coverage of an alternative must be assessed separately.


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
