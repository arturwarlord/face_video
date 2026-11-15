# ArturChat push notifications setup

This guide documents the current remote push setup for incoming calls and new messages. Push notifications carry metadata only; voice and video remain on the existing direct WebRTC P2P path. No TURN, Coturn, VPN routing or media relay is introduced.

## Current server status

The Supabase project `mitvjkhlsvxcnzhbiwye` is configured with:

- `pg_net` enabled (version 0.20.4).
- `send-arturchat-push` Edge Function deployed and active (version 8).
- An encrypted, internally generated webhook secret in Supabase Vault.
- An AFTER INSERT trigger on `public.face_video_messages`.
- An AFTER INSERT trigger on `public.face_video_calls`.
- A private trigger function and a verifier callable only by `service_role`.

The triggers send the default INSERT payload to the Edge Function asynchronously via `net.http_post`. They do not block message or call creation if push delivery itself fails. No trigger is created on `face_video_call_signals`, so ICE/SDP candidates never produce notifications.

The database-to-function authorization path was tested successfully. FCM device tokens are registered for both test accounts. Incoming calls are sent as a high-priority FCM notification plus data payload with a 45-second expiry; Android posts that notification itself when the app is backgrounded or its process is absent. The data payload is passed to the launcher Activity when the user taps the alert. In the foreground, the app still uses its native CallStyle notification.

## 1. Firebase Android configuration

1. Open Firebase Console and select the Firebase project used for ArturChat notifications.
2. Add an Android app with package name **`com.facevideo.app`**.
3. Download `google-services.json`.
4. In GitHub repository **Settings → Secrets and variables → Actions**, create or update the repository secret:
   - Name: `FIREBASE_GOOGLE_SERVICES_BASE64`
   - Value: either the complete raw JSON file contents or its Base64-encoded contents.
5. Never paste the secret into chat or commit `google-services.json` to the repository.

The signed-candidate and Android workflows validate that the config belongs to `com.facevideo.app` before building. The temporary JSON file is created only on the Actions runner.

## 2. Firebase server credentials in Supabase

1. In Firebase Console → **Project settings → Service accounts**, generate a private key and download the service-account JSON.
2. In Supabase Dashboard → project `mitvjkhlsvxcnzhbiwye` → **Edge Functions → Secrets**, confirm this secret exists:
   - `FCM_SERVICE_ACCOUNT_JSON`: the full Firebase service-account JSON.
3. Do not commit or share this JSON. The platform-provided `SUPABASE_SERVICE_ROLE_KEY` remains server-side and must never be placed in GitHub or the Android app.

`ARTURCHAT_PUSH_WEBHOOK_SECRET` may remain configured as a legacy/manual-webhook fallback, but the automated database triggers use the separate random secret stored inside Supabase Vault. You do not need to copy or expose the Vault secret.

## 3. Database event wiring

No manual Database Webhooks need to be created. The repository migrations install the triggers and the secure Vault-backed request authentication.

| Trigger | Table | Event |
|---|---|---|
| `arturchat_push_messages_insert` | `public.face_video_messages` | AFTER INSERT |
| `arturchat_push_calls_insert` | `public.face_video_calls` | AFTER INSERT |

Do not add a webhook or trigger to `face_video_call_signals`.

## 4. Signed app build and device registration

The Firebase-enabled candidate is **ArturChat 0.7.49** (`versionCode=7049`). The signed GitHub Actions workflow:

1. Validates the Firebase Android configuration for `com.facevideo.app`.
2. Runs unit tests.
3. Builds the release APK with the existing signing key.
4. Verifies the package name, version and APK signature.
5. Uploads the APK and SHA-256 checksum as an Actions artifact. It does not publish a GitHub Release.

After a successful workflow:

1. Download the signed APK artifact from the workflow run.
2. Install it over the existing signed app on both phones.
3. Open ArturChat on each phone, sign in, and grant Android notification permission.
4. Check only token counts in Supabase SQL Editor:

   ```sql
   select
     count(*) as registered_device_tokens,
     count(distinct user_id) as registered_users
   from public.face_video_push_tokens;
   ```

   Two signed-in phones should register at least two device tokens. Do not expose token values in screenshots or logs.

At the last server check, the token table contained zero rows; real phone delivery cannot be confirmed until both devices register their FCM tokens.

## 5. Device delivery test

- Send a message from phone A to phone B while B is on the home screen.
- Call B while B is in the background and its screen is locked.
- Repeat with the roles reversed.
- Test with the app backgrounded or swiped away from Recents. Android Force Stop from Settings deliberately blocks background execution and FCM delivery until the user manually opens the app again.
- On POCO/Xiaomi devices, enable ArturChat Autostart and set battery/background restrictions to unrestricted where available.

FCM carries notification metadata only. Audio/video media continues through the existing direct P2P WebRTC connection.

## Troubleshooting

- **Token count remains 0:** make sure the APK came from the successful 0.7.48 signed workflow, open the app after installation, sign in, and grant notifications.
- **Firebase config step fails:** replace the GitHub Actions secret with the complete `google-services.json` text or its valid Base64 representation. Do not send it in chat.
- **Tokens exist but notifications do not arrive:** inspect Supabase Edge Function logs for `send-arturchat-push`, confirm `FCM_SERVICE_ACCOUNT_JSON` is valid JSON with `project_id`, `client_email`, and `private_key`, and verify Android notification permission/channel settings.
- **Function returns “Push delivery is not configured”:** confirm `FCM_SERVICE_ACCOUNT_JSON` exists in Edge Function secrets.
- **Notification is hidden or silent:** check the ArturChat Messages/Calls notification channels, Do Not Disturb, and manufacturer battery/autostart settings.

## Provider boundary

FCM is a Google service. It carries only notification metadata; voice and video continue to travel directly between ArturChat peers using WebRTC. If the app must avoid all foreign services, a different push provider would need to be selected and separately validated for the target Android devices and countries.
