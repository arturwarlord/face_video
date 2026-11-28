# A-Chat push notifications

A-Chat uses Firebase Cloud Messaging (FCM) for remote message and incoming-call notifications. The Supabase push Edge Function is active, and database triggers already forward new messages and calls to it. Voice calls themselves continue to use WebRTC; FCM carries notification metadata only.

## Current diagnosis

On 22 November 2026, the signed candidate build log showed that `FIREBASE_GOOGLE_SERVICES_BASE64` could not be decoded. The workflow nevertheless continued and produced an APK with `FIREBASE_CONFIGURED=false`. That build skips FCM token registration in `PushTokenRegistrar`, so the server has no device address to send notifications to.

The workflow now accepts either raw `google-services.json` contents or Base64, validates the Android package name, and **fails the build** instead of quietly producing an APK without notifications.

## 1. Configure the Android Firebase app

1. Open Firebase Console and select the Firebase project used by A-Chat.
2. Add an Android app with package name **`com.facevideo.app`** (or open the existing app).
3. Download its `google-services.json`.
4. On Windows, open PowerShell in the folder containing that file and copy a Base64 version:

   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes(".\google-services.json")) | Set-Clipboard
   ```

5. In GitHub, open **Settings → Secrets and variables → Actions** and replace the repository secret:
   - Name: `FIREBASE_GOOGLE_SERVICES_BASE64`
   - Value: paste the Base64 text from the clipboard.

The build helper also accepts the complete raw JSON contents if that is what is already stored in the secret. Do not include PowerShell prompt text, file paths, quotes around the value, or any other text. Do not commit `google-services.json` or paste it into chat.

## 2. Check the Supabase server configuration

The database already has an `AFTER INSERT` trigger on `face_video_messages` and `face_video_calls`. It invokes the active `send-arturchat-push` Edge Function using the private Vault webhook secret. **Do not create additional Database Webhooks**; duplicate webhooks could send duplicate notifications.

The Edge Function also needs the Firebase service-account secret `FCM_SERVICE_ACCOUNT_JSON` in Supabase Edge Function secrets. It is the service-account JSON for the same Firebase project as the Android app configuration. Supabase supplies `SUPABASE_SERVICE_ROLE_KEY` to the Edge Function automatically; never put that key in the Android app or GitHub.

## 3. Build and register the devices

1. After replacing the GitHub secret, open **Actions → A-Chat 0.7.55 signed installable APK** and run the workflow, or push the next code change to `main`.
2. Download the artifact only after all steps finish successfully. The Firebase validation step must say that the Android configuration was validated.
3. Install the signed APK over the existing A-Chat installation on each phone.
4. Open A-Chat with an internet connection, sign in, and allow Android notifications.
5. Verify the registered device count in Supabase SQL Editor without displaying token values:

   ```sql
   select
     count(*) as registered_device_tokens,
     count(distinct user_id) as registered_users
   from public.face_video_push_tokens;
   ```

Each phone should register at least one token. Once a token is registered, future notifications can arrive without opening A-Chat each time.

## 4. Test delivery

- Send a message from one phone while the other phone is on the home screen.
- Repeat with the app backgrounded and the screen locked.
- Test an incoming audio and video call in both directions.

The Edge Function's response should report a positive `sent` count. A response of `No registered devices.` means the Android app has not registered a token. Check the GitHub Firebase validation step and then open the new app build once while signed in.

## Troubleshooting

- **The signed build fails at Firebase validation:** replace `FIREBASE_GOOGLE_SERVICES_BASE64` with the raw contents of the correct `google-services.json` or its Base64 encoding. The file must contain package `com.facevideo.app`.
- **The token count is still zero:** install the validated build, open A-Chat online, sign in, and check that Firebase token registration is not reporting an error.
- **Tokens exist but pushes are not sent:** inspect Supabase Edge Function logs for `send-arturchat-push` and verify the `FCM_SERVICE_ACCOUNT_JSON` secret.
- **The push is sent but no banner appears:** check Android notification permission, Do Not Disturb, and A-Chat's Messages and Calls notification-channel settings.
- **The phone does not receive background pushes:** Android Force stop blocks background delivery until the app is opened again. On Xiaomi/MIUI, also allow A-Chat autostart and unrestricted background activity.

