import "jsr:@supabase/functions-js/edge-runtime.d.ts";

// Open Relay publishes this shared TURN REST secret for its Static Auth
// endpoint. We mint short-lived, per-request credentials server-side so the
// Android app never contains the shared key.
const OPEN_RELAY_SHARED_SECRET = "openrelayprojectsecret";
const OPEN_RELAY_HOST = "staticauth.openrelay.metered.ca";
const CREDENTIAL_LIFETIME_SECONDS = 6 * 60 * 60;

function jsonResponse(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-store, max-age=0",
      "Pragma": "no-cache",
    },
  });
}

async function createTurnCredentials(): Promise<{ username: string; credential: string }> {
  // coturn TURN REST authentication: username = expiry:opaque-user-id,
  // credential = Base64(HMAC-SHA1(shared-secret, username)).
  const expiresAt = Math.floor(Date.now() / 1000) + CREDENTIAL_LIFETIME_SECONDS;
  const username = `${expiresAt}:arturchat-${crypto.randomUUID()}`;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(OPEN_RELAY_SHARED_SECRET),
    { name: "HMAC", hash: "SHA-1" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(username),
  );
  const bytes = new Uint8Array(signature);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return { username, credential: btoa(binary) };
}

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") {
    return jsonResponse(405, { error: "Method not allowed." });
  }

  // The Supabase Edge gateway verifies the user's JWT before this function runs.
  const authorization = request.headers.get("Authorization") ?? "";
  if (!authorization.startsWith("Bearer ") || authorization.length < 40) {
    return jsonResponse(401, { error: "A signed-in ArturChat session is required." });
  }

  try {
    const { username, credential } = await createTurnCredentials();
    const iceServers = [
      {
        urls: [
          `turn:${OPEN_RELAY_HOST}:80?transport=udp`,
          `turn:${OPEN_RELAY_HOST}:443?transport=udp`,
          `turn:${OPEN_RELAY_HOST}:443?transport=tcp`,
          `turns:${OPEN_RELAY_HOST}:443?transport=tcp`,
        ],
        username,
        credential,
      },
    ];

    return jsonResponse(200, { iceServers, expiresAt: username.split(":")[0] });
  } catch (error) {
    console.error("Could not mint Open Relay TURN credentials", error);
    return jsonResponse(500, { error: "Could not create temporary TURN credentials." });
  }
});
