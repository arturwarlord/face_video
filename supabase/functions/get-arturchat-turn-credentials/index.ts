import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const METERED_TURN_CREDENTIALS_URL = Deno.env.get("METERED_TURN_CREDENTIALS_URL") ?? "";

function jsonResponse(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    },
  });
}

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") {
    return jsonResponse(405, { error: "Method not allowed." });
  }

  // The Supabase Edge gateway verifies the user's JWT before this function runs.
  // Keep a second explicit bearer-header check so this endpoint never serves
  // credentials to an anonymous webhook or unauthenticated request.
  const authorization = request.headers.get("Authorization") ?? "";
  if (!authorization.startsWith("Bearer ") || authorization.length < 40) {
    return jsonResponse(401, { error: "A signed-in ArturChat session is required." });
  }

  if (!METERED_TURN_CREDENTIALS_URL) {
    return jsonResponse(503, { error: "TURN relay credentials are not configured yet." });
  }

  try {
    const upstream = await fetch(METERED_TURN_CREDENTIALS_URL, {
      method: "GET",
      headers: { "Accept": "application/json" },
      signal: AbortSignal.timeout(7000),
    });
    if (!upstream.ok) {
      console.error("Metered TURN API returned", upstream.status);
      return jsonResponse(502, { error: "TURN relay provider is temporarily unavailable." });
    }

    const iceServers = await upstream.json();
    if (!Array.isArray(iceServers) || !iceServers.some((server) => {
      const urls = typeof server?.urls === "string"
        ? [server.urls]
        : Array.isArray(server?.urls) ? server.urls : [];
      return urls.some((url: unknown) =>
        typeof url === "string" && (url.startsWith("turn:") || url.startsWith("turns:"))
      );
    })) {
      return jsonResponse(502, { error: "TURN provider did not return relay servers." });
    }

    return jsonResponse(200, { iceServers });
  } catch (error) {
    console.error("Could not fetch Metered TURN credentials", error);
    return jsonResponse(502, { error: "Could not obtain temporary TURN credentials." });
  }
});
