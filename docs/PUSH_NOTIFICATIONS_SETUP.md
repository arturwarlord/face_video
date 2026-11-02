# ArturChat push notifications setup

This guide activates remote notifications for incoming calls and new messages. The voice path is unchanged: calls continue to use direct WebRTC P2P. FCM carries only a small notification payload.

## What is already in the repository

- Android FCM dependency and `ArturChatFirebaseMessagingService`.
- Device token registration in `public.face_video_push_tokens`.
- Supabase Edge Function `send-arturchat-push` (already deployed to project `mitvjkhlsvxcnzhbiwye`).
- Notification channels for messages and calls.
- GitHub Actions support for building with a Firebase Android client configuration.

The missing one-time configuration was confirmed on 2026-11-02: zero FCM tokens registered, no database webhooks configured, and no `google-services.json` was included in GitHub Actions builds.

## 1. Create Firebase Android app configuration

1. Open Firebase Console and select or create the Firebase project that will send ArturChat notifications.
2. Add an Android app with package name **`com.facevideo.app`**.
3. Download its `google-services.json`.
4. On Windows, in PowerShell in the folder containing that file, run:

   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes(".\google-services.json")) | Set-Clipboard
   ```

5. Open the GitHub repository **Settings → Secrets and variables → Actions → New repository secret**:
   - Name: `FIREBASE_GOOGLE_SERVICES_BASE64`
   - Value: paste the clipboard contents.
6. Do not send this value in chat. Do not commit `google-services.json`; it is ignored by `.gitignore`.

The Android config is a client configuration, not the Firebase service-account private key. GitHub Actions decodes it into `app/google-services.json` for the build, validates that it contains package `com.facevideo.app`, and deletes the temporary runner after the workflow.

## 2. Add server-side Firebase credentials to Supabase

1. In Firebase Console → **Project settings → Service accounts**, generate a private key and download the service-account JSON.
2. Open Supabase Dashboard → project `mitvjkhlsvxcnzhbiwye` → **Edge Functions → Secrets**.
3. Add these two custom secrets:

   | Name | Value |
   |---|---|
   | `FCM_SERVICE_ACCOUNT_JSON` | Entire contents of the downloaded service-account JSON |
   | `ARTURCHAT_PUSH_WEBHOOK_SECRET` | A long random string you create |

4. Do not commit or share the service-account JSON or webhook secret. The hosted Supabase Edge Function already receives `SUPABASE_SERVICE_ROLE_KEY` from the platform; never place that key in GitHub or the Android app.

## 3. Connect database events to the push function

The Edge Function validates the `x-arturchat-webhook-secret` header. Create two **Database Webhooks** in Supabase Dashboard → **Database → Webhooks**. Both use HTTP **POST**, schema `public`, event **INSERT**, and the endpoint:

```text
https://mitvjkhlsvxcnzhbiwye.supabase.co/functions/v1/send-arturchat-push
```

Add these headers to both webhooks:

```text
Content-Type: application/json
x-arturchat-webhook-secret: <the same value stored as ARTURCHAT_PUSH_WEBHOOK_SECRET>
```

Create the two webhook subscriptions:

| Webhook name | Table | Event |
|---|---|---|
| ArturChat new messages | `face_video_messages` | INSERT |
| ArturChat incoming calls | `face_video_calls` | INSERT |

Use the default Database Webhook JSON body. The function expects `type`, `schema`, `table`, and `record` fields.

Do not create a webhook for `face_video_call_signals`: ICE/SDP candidates are frequent and must never produce user notifications.

## 4. Build and register both devices

1. After adding the GitHub secret, open **Actions → ArturChat signed candidate APK** and run it manually, or push a subsequent change to `main`. The signed candidate workflow now prepares Firebase config automatically and builds version **0.4.7**.
2. Download the successful `arturchat-signed-candidate-apk` artifact and install it on both phones over the existing signed version.
3. Open ArturChat on each phone and sign in. Grant Android notification permission when prompted.
4. Verify only token counts in Supabase SQL Editor:

   ```sql
   select
     count(*) as registered_device_tokens,
     count(distinct user_id) as registered_users
   from public.face_video_push_tokens;
   ```

   With two logged-in phones, expect at least two token rows. Do not expose the token values in screenshots or logs.

## 5. Test delivery

- Send a message from phone A to phone B while B is on the home screen.
- Call B while B is in the background and its screen is locked.
- Tap the incoming-call notification. ArturChat should open and show the existing **Accept / Decline** dialog.
- Repeat with the roles reversed.

Test with the app backgrounded or screen locked. Android's **Force stop** action deliberately blocks app background execution and push delivery until the user opens the app again. Xiaomi/MIUI battery restrictions may also need the user to allow ArturChat autostart and unrestricted background activity.

## Troubleshooting

- **Token count remains 0:** check that `FIREBASE_GOOGLE_SERVICES_BASE64` exists in GitHub Actions, that the workflow log says Firebase config was prepared, and that both phones opened the new build at least once after signing in.
- **Token rows exist but notifications do not arrive:** check both webhook configurations, confirm the header value exactly matches `ARTURCHAT_PUSH_WEBHOOK_SECRET`, and inspect Supabase Edge Function logs for `send-arturchat-push`.
- **Function says push delivery is not configured:** verify `FCM_SERVICE_ACCOUNT_JSON` is valid service-account JSON with `project_id`, `client_email`, and `private_key`.
- **Notification appears but tapping it does not show the call dialog:** keep the call in ringing state and check whether the app can load `face_video_calls` for the signed-in callee. The incoming-call query is protected by RLS.
- **Notifications are silent or hidden:** check Android notification permission, the ArturChat Calls channel importance, Do Not Disturb, and the phone manufacturer's background/battery settings.

## Provider boundary

FCM is a Google service. It only carries notification metadata; voice audio still travels directly between ArturChat peers using WebRTC. If ArturChat must avoid all foreign services, do not add these Firebase credentials: a domestic push provider needs to be selected and separately validated for the target Android devices and countries.
