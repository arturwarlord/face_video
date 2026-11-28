import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";
import { JWT } from "npm:google-auth-library@9";

type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  table: string;
  schema: string;
  record: Record<string, unknown>;
};

type PushTarget = { user_id: string; token: string };
type PushPayload = {
  type: "message" | "call" | "release";
  title: string;
  body: string;
  entityId: string;
  releaseVersion?: string;
  releaseUrl?: string;
  chatId?: string;
  mediaType?: "audio" | "video";
  senderId?: string;
  senderAvatarUrl?: string;
  createdAt?: string;
};

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const WEBHOOK_SECRET = Deno.env.get("ARTURCHAT_PUSH_WEBHOOK_SECRET") ?? "";
const SERVICE_ACCOUNT_JSON = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON") ?? "";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const GITHUB_REPOSITORY = "arturwarlord/face_video";
const GITHUB_OIDC_ISSUER = "https://token.actions.githubusercontent.com";
const GITHUB_OIDC_AUDIENCE = "a-chat-release";

function decodeBase64Url(value: string): Uint8Array {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
  return Uint8Array.from(atob(padded), (character) => character.charCodeAt(0));
}

function decodeJwtJson(value: string): Record<string, unknown> {
  return JSON.parse(new TextDecoder().decode(decodeBase64Url(value))) as Record<string, unknown>;
}

async function verifyGitHubActionsToken(token: string): Promise<boolean> {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return false;
    const header = decodeJwtJson(parts[0]);
    const claims = decodeJwtJson(parts[1]);
    if (header.alg !== "RS256" || typeof header.kid !== "string") return false;

    const audience = claims.aud;
    const audienceMatches = typeof audience === "string"
      ? audience === GITHUB_OIDC_AUDIENCE
      : Array.isArray(audience) && audience.includes(GITHUB_OIDC_AUDIENCE);
    if (
      claims.iss !== GITHUB_OIDC_ISSUER ||
      !audienceMatches ||
      claims.repository !== GITHUB_REPOSITORY ||
      claims.repository_owner !== "arturwarlord"
    ) return false;

    const ref = typeof claims.ref === "string" ? claims.ref : "";
    const eventName = claims.event_name;
    const mainManualRun = eventName === "workflow_dispatch" && ref === "refs/heads/main";
    const releaseBranchRun = eventName === "push" &&
      /^refs\/heads\/publish-release-v[0-9]+\.[0-9]+\.[0-9]+$/.test(ref);
    if (!mainManualRun && !releaseBranchRun) return false;

    const expectedSubject = "repo:" + GITHUB_REPOSITORY + ":ref:" + ref;
    // GitHub's immutable subject format includes owner and repository IDs.
    const immutableSubject =
      "repo:arturwarlord@69643781/face_video@1439900983:ref:" + ref;
    const expectedWorkflowRef = GITHUB_REPOSITORY + "/.github/workflows/release.yml@" + ref;
    if (
      (claims.sub !== expectedSubject && claims.sub !== immutableSubject) ||
      claims.workflow_ref !== expectedWorkflowRef
    ) return false;

    const now = Math.floor(Date.now() / 1000);
    const expiresAt = Number(claims.exp);
    const issuedAt = Number(claims.iat);
    const notBefore = claims.nbf == null ? 0 : Number(claims.nbf);
    if (
      !Number.isFinite(expiresAt) || expiresAt <= now ||
      !Number.isFinite(issuedAt) || issuedAt > now + 60 ||
      !Number.isFinite(notBefore) || notBefore > now + 30
    ) return false;

    const jwksResponse = await fetch(
      GITHUB_OIDC_ISSUER + "/.well-known/jwks",
      { headers: { Accept: "application/json" } },
    );
    if (!jwksResponse.ok) return false;
    const jwks = await jwksResponse.json() as { keys?: JsonWebKey[] };
    const jwk = jwks.keys?.find(
      (candidate) => (candidate as JsonWebKey & { kid?: string }).kid === header.kid
    );
    if (!jwk) return false;

    const publicKey = await crypto.subtle.importKey(
      "jwk",
      jwk,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"],
    );
    return await crypto.subtle.verify(
      { name: "RSASSA-PKCS1-v1_5" },
      publicKey,
      decodeBase64Url(parts[2]),
      new TextEncoder().encode(parts[0] + "." + parts[1]),
    );
  } catch (error) {
    console.error("GitHub Actions OIDC verification failed", error);
    return false;
  }
}


let supabaseAdmin: ReturnType<typeof createClient> | null = null;

function getSupabaseAdmin() {
  if (!SUPABASE_URL || !SERVICE_ROLE_KEY) {
    throw new Error("Supabase server credentials are not configured.");
  }
  if (!supabaseAdmin) {
    supabaseAdmin = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
  }
  return supabaseAdmin;
}

function jsonResponse(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

async function getFcmAccessToken(account: { client_email: string; private_key: string }) {
  const jwt = new JWT({
    email: account.client_email,
    key: account.private_key,
    scopes: [FCM_SCOPE],
  });
  const credentials = await jwt.authorize();
  if (!credentials.access_token) throw new Error("Google OAuth did not return an access token.");
  return credentials.access_token;
}

async function sendToDevice(
  target: PushTarget,
  projectId: string,
  accessToken: string,
  payload: PushPayload,
) {
  const data: Record<string, string> = {
    type: payload.type,
    title: payload.title,
    body: payload.body,
    entity_id: payload.entityId,
  };
  if (payload.chatId) data.chat_id = payload.chatId;
  if (payload.mediaType) data.media_type = payload.mediaType;
  if (payload.senderId) data.sender_id = payload.senderId;
  if (payload.senderAvatarUrl) data.sender_avatar_url = payload.senderAvatarUrl;
  if (payload.createdAt) data.created_at = payload.createdAt;
  if (payload.releaseVersion) data.release_version = payload.releaseVersion;
  if (payload.releaseUrl) data.release_url = payload.releaseUrl;

  const response = await fetch(
    "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + accessToken,
      },
      body: JSON.stringify({
        message: {
          token: target.token,
          data,
          android: payload.type === "call"
            ? {
                priority: "HIGH",
                ttl: "45s",
                notification: {
                  channel_id: "arturchat_calls_v2",
                  sound: "default",
                  visibility: "PUBLIC",
                  notification_priority: "PRIORITY_MAX",
                  default_vibrate_timings: true,
                  tag: "arturchat-call-" + payload.entityId,
                },
              }
            : {
                // Data-only messages allow the Android client to render native
                // MessagingStyle threads with each sender's actual avatar.
                priority: "HIGH",
                ttl: "604800s",
              },
          ...(payload.type === "call"
            ? {
                // Keep system-managed notification + data for background calls.
                notification: { title: payload.title, body: payload.body },
              }
            : {}),
        },
      }),
    },
  );

  if (response.ok) return { sent: true, removedInvalidToken: false };
  const errorBody = await response.text();
  if (response.status === 404 || errorBody.includes("UNREGISTERED") || errorBody.includes("NOT_FOUND")) {
    await getSupabaseAdmin().from("face_video_push_tokens")
      .delete().eq("user_id", target.user_id).eq("token", target.token);
    return { sent: false, removedInvalidToken: true };
  }
  console.error("FCM send failed", response.status, errorBody.slice(0, 1000));
  return { sent: false, removedInvalidToken: false };
}

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") return jsonResponse(405, { error: "Method not allowed." });

  let incoming: Record<string, unknown>;
  try {
    const parsed = await request.json();
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return jsonResponse(400, { error: "Invalid JSON payload." });
    }
    incoming = parsed as Record<string, unknown>;
  } catch {
    return jsonResponse(400, { error: "Invalid JSON payload." });
  }

  // Release pushes are sent only by the signed A-Chat GitHub Actions workflow.
  // Existing database webhooks retain their independent Vault/shared-secret authentication.
  if (incoming.kind === "release") {
    const authorization = request.headers.get("authorization") ?? "";
    const githubToken = authorization.startsWith("Bearer ") ? authorization.slice(7).trim() : "";
    if (!githubToken || !(await verifyGitHubActionsToken(githubToken))) {
      return jsonResponse(401, { error: "Invalid GitHub Actions identity." });
    }
    if (!SUPABASE_URL || !SERVICE_ROLE_KEY || !SERVICE_ACCOUNT_JSON) {
      console.error("Push function secrets are incomplete.");
      return jsonResponse(503, { error: "Push delivery is not configured." });
    }

    const version = String(incoming.version ?? "");
    const releaseUrl = String(incoming.release_url ?? "");
    if (!/^[0-9]+\.[0-9]+\.[0-9]+$/.test(version)) {
      return jsonResponse(400, { error: "Invalid release version." });
    }
    const expectedReleaseUrl = "https://github.com/" + GITHUB_REPOSITORY + "/releases/tag/v" + version;
    if (releaseUrl !== expectedReleaseUrl) {
      return jsonResponse(400, { error: "Invalid release URL." });
    }

    const supabase = getSupabaseAdmin();
    const targets: PushTarget[] = [];
    const pageSize = 500;
    for (let offset = 0; ; offset += pageSize) {
      const { data, error } = await supabase.from("face_video_push_tokens")
        .select("user_id, token")
        .range(offset, offset + pageSize - 1);
      if (error) {
        console.error("Could not load release push tokens", error);
        return jsonResponse(500, { error: "Could not load release push tokens." });
      }
      targets.push(...((data ?? []) as PushTarget[]));
      if (!data || data.length < pageSize) break;
    }

    const targetByToken = new Map<string, PushTarget>();
    for (const target of targets) {
      if (target.token.trim().length > 0 && !targetByToken.has(target.token)) {
        targetByToken.set(target.token, target);
      }
    }
    const uniqueTargets = [...targetByToken.values()];
    if (!uniqueTargets.length) {
      return jsonResponse(200, { sent: 0, attempted: 0, reason: "No registered devices." });
    }

    let serviceAccount: { project_id: string; client_email: string; private_key: string };
    try {
      serviceAccount = JSON.parse(SERVICE_ACCOUNT_JSON);
    } catch {
      return jsonResponse(500, { error: "FCM_SERVICE_ACCOUNT_JSON is not valid JSON." });
    }
    if (!serviceAccount.project_id || !serviceAccount.client_email || !serviceAccount.private_key) {
      return jsonResponse(500, { error: "Firebase service-account JSON is incomplete." });
    }

    const notification: PushPayload = {
      type: "release",
      title: "Вышла новая версия A-Chat",
      body: "Версия " + version + " уже доступна. Открой A-Chat, чтобы посмотреть обновление.",
      entityId: "release:" + version,
      releaseVersion: version,
      releaseUrl,
    };
    try {
      const accessToken = await getFcmAccessToken(serviceAccount);
      const results = await Promise.all(uniqueTargets.map((target) =>
        sendToDevice(target, serviceAccount.project_id, accessToken, notification)
      ));
      return jsonResponse(200, {
        sent: results.filter((result) => result.sent).length,
        attempted: uniqueTargets.length,
        removedInvalidTokens: results.filter((result) => result.removedInvalidToken).length,
        version,
      });
    } catch (error) {
      console.error("Release push delivery failed", error);
      return jsonResponse(500, { error: "Release push delivery failed." });
    }
  }

  const legacySecret = request.headers.get("x-arturchat-webhook-secret") ?? "";
  const vaultSecret = request.headers.get("x-arturchat-push-secret") ?? "";
  let webhookAuthorized = Boolean(WEBHOOK_SECRET) && legacySecret === WEBHOOK_SECRET;

  if (!webhookAuthorized && vaultSecret) {
    try {
      const { data, error } = await getSupabaseAdmin().rpc("verify_arturchat_push_secret", {
        p_supplied_secret: vaultSecret,
      });
      webhookAuthorized = !error && data === true;
      if (error) console.error("Webhook secret verification failed", error.message);
    } catch (error) {
      console.error("Webhook secret verification unavailable", error);
    }
  }

  if (!webhookAuthorized) {
    return jsonResponse(401, { error: "Invalid webhook secret." });
  }
  if (!SUPABASE_URL || !SERVICE_ROLE_KEY || !SERVICE_ACCOUNT_JSON) {
    console.error("Push function secrets are incomplete.");
    return jsonResponse(503, { error: "Push delivery is not configured." });
  }

  const supabase = getSupabaseAdmin();
  const payload = incoming as unknown as WebhookPayload;
  if (payload.schema !== "public" || payload.type !== "INSERT") {
    return jsonResponse(202, { skipped: true });
  }

  let recipients: string[] = [];
  let mutedRecipientCount = 0;
  let notification: PushPayload;

  if (payload.table === "face_video_messages") {
    const row = payload.record;
    const chatId = String(row.chat_id ?? "");
    const senderId = String(row.sender_id ?? "");
    const messageId = String(row.id ?? "");
    if (!chatId || !senderId || !messageId) {
      return jsonResponse(400, { error: "Message webhook lacks required fields." });
    }

    const { data: members, error } = await supabase.from("face_video_chat_members")
      .select("user_id").eq("chat_id", chatId).neq("user_id", senderId);
    if (error) {
      console.error("Could not load chat members", error);
      return jsonResponse(500, { error: "Could not load message recipients." });
    }
    recipients = (members ?? []).map((member) => String(member.user_id));

    // Suppress message notifications only for recipients who muted this chat.
    // Incoming-call notifications use the separate call branch and remain unaffected.
    if (recipients.length > 0) {
      const { data: mutedRows, error: muteError } = await supabase
        .from("face_video_chat_notification_settings")
        .select("user_id")
        .eq("chat_id", chatId)
        .eq("is_muted", true)
        .in("user_id", recipients);
      if (muteError) {
        console.error("Could not load chat notification settings", muteError);
        return jsonResponse(500, { error: "Could not load chat notification settings." });
      }
      const mutedUserIds = new Set((mutedRows ?? []).map((row) => String(row.user_id)));
      const previousRecipientCount = recipients.length;
      recipients = recipients.filter((userId) => !mutedUserIds.has(userId));
      mutedRecipientCount = previousRecipientCount - recipients.length;
    }

    const { data: sender } = await supabase.from("face_video_profiles")
      .select("display_name, username, avatar_url").eq("id", senderId).maybeSingle();
    notification = {
      type: "message",
      title: sender?.display_name?.trim() || sender?.username || "Новое сообщение",
      body: String(row.body ?? "").slice(0, 1000),
      entityId: messageId,
      chatId,
      senderId,
      senderAvatarUrl: typeof sender?.avatar_url === "string" ? sender.avatar_url : undefined,
      createdAt: typeof row.created_at === "string" ? row.created_at : undefined,
    };
  } else if (payload.table === "face_video_calls") {
    const row = payload.record;
    const callId = String(row.id ?? "");
    const callerId = String(row.caller_id ?? "");
    const calleeId = String(row.callee_id ?? "");
    if (!callId || !callerId || !calleeId) {
      return jsonResponse(400, { error: "Call webhook lacks required fields." });
    }
    recipients = [calleeId];
    const { data: caller } = await supabase.from("face_video_profiles")
      .select("display_name, username").eq("id", callerId).maybeSingle();
    notification = {
      type: "call",
      title: (caller?.display_name?.trim() || caller?.username || "Собеседник") + " звонит вам",
      body: row.media_type === "video"
        ? "Входящий видеозвонок в A-Chat"
        : "Входящий аудиозвонок в A-Chat",
      entityId: callId,
      mediaType: row.media_type === "video" ? "video" : "audio",
    };
  } else {
    return jsonResponse(202, { skipped: true });
  }

  if (!recipients.length) {
    return jsonResponse(200, {
      sent: 0,
      reason: mutedRecipientCount > 0 ? "All recipients muted." : "No recipients.",
      mutedRecipients: mutedRecipientCount,
    });
  }
  const { data: targets, error: tokenError } = await supabase.from("face_video_push_tokens")
    .select("user_id, token").in("user_id", recipients);
  if (tokenError) {
    console.error("Could not load FCM tokens", tokenError);
    return jsonResponse(500, { error: "Could not load push tokens." });
  }
  if (!targets?.length) return jsonResponse(200, { sent: 0, reason: "No registered devices." });

  let serviceAccount: { project_id: string; client_email: string; private_key: string };
  try {
    serviceAccount = JSON.parse(SERVICE_ACCOUNT_JSON);
  } catch {
    return jsonResponse(500, { error: "FCM_SERVICE_ACCOUNT_JSON is not valid JSON." });
  }
  if (!serviceAccount.project_id || !serviceAccount.client_email || !serviceAccount.private_key) {
    return jsonResponse(500, { error: "Firebase service-account JSON is incomplete." });
  }

  try {
    const accessToken = await getFcmAccessToken(serviceAccount);
    const results = await Promise.all((targets as PushTarget[]).map((target) =>
      sendToDevice(target, serviceAccount.project_id, accessToken, notification)
    ));
    return jsonResponse(200, {
      sent: results.filter((result) => result.sent).length,
      attempted: targets.length,
      removedInvalidTokens: results.filter((result) => result.removedInvalidToken).length,
    });
  } catch (error) {
    console.error("Push delivery failed", error);
    return jsonResponse(500, { error: "Push delivery failed." });
  }
});
