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
  type: "message" | "call";
  title: string;
  body: string;
  entityId: string;
  chatId?: string;
};

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const WEBHOOK_SECRET = Deno.env.get("ARTURCHAT_PUSH_WEBHOOK_SECRET") ?? "";
const SERVICE_ACCOUNT_JSON = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON") ?? "";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
  auth: { persistSession: false, autoRefreshToken: false },
});

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
          notification: { title: payload.title, body: payload.body },
          data,
          android: {
            priority: "HIGH",
            notification: {
              channel_id: payload.type === "call" ? "arturchat_calls" : "arturchat_messages",
              sound: "default",
            },
          },
        },
      }),
    },
  );

  if (response.ok) return { sent: true, removedInvalidToken: false };
  const errorBody = await response.text();
  if (response.status === 404 || errorBody.includes("UNREGISTERED") || errorBody.includes("NOT_FOUND")) {
    await supabase.from("face_video_push_tokens")
      .delete().eq("user_id", target.user_id).eq("token", target.token);
    return { sent: false, removedInvalidToken: true };
  }
  console.error("FCM send failed", response.status, errorBody.slice(0, 1000));
  return { sent: false, removedInvalidToken: false };
}

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") return jsonResponse(405, { error: "Method not allowed." });
  if (!WEBHOOK_SECRET || request.headers.get("x-arturchat-webhook-secret") !== WEBHOOK_SECRET) {
    return jsonResponse(401, { error: "Invalid webhook secret." });
  }
  if (!SUPABASE_URL || !SERVICE_ROLE_KEY || !SERVICE_ACCOUNT_JSON) {
    console.error("Push function secrets are incomplete.");
    return jsonResponse(503, { error: "Push delivery is not configured." });
  }

  let payload: WebhookPayload;
  try {
    payload = await request.json();
  } catch {
    return jsonResponse(400, { error: "Invalid JSON payload." });
  }
  if (payload.schema !== "public" || payload.type !== "INSERT") {
    return jsonResponse(202, { skipped: true });
  }

  let recipients: string[] = [];
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

    const { data: sender } = await supabase.from("face_video_profiles")
      .select("display_name, username").eq("id", senderId).maybeSingle();
    notification = {
      type: "message",
      title: sender?.display_name?.trim() || sender?.username || "Новое сообщение",
      body: String(row.body ?? "").slice(0, 1000),
      entityId: messageId,
      chatId,
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
      body: "Входящий аудиозвонок в ArturChat",
      entityId: callId,
    };
  } else {
    return jsonResponse(202, { skipped: true });
  }

  if (!recipients.length) return jsonResponse(200, { sent: 0, reason: "No recipients." });
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
