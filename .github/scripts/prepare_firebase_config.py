#!/usr/bin/env python3
"""Validate and prepare ArturChat's Firebase Android client configuration."""
from __future__ import annotations

import base64
import binascii
import json
import os
from pathlib import Path
from typing import Any

PACKAGE_NAME = "com.facevideo.app"
SECRET_NAME = "FIREBASE_GOOGLE_SERVICES_BASE64"
OUTPUT_PATH = Path("app/google-services.json")


def parse_json_candidate(candidate: str) -> dict[str, Any] | None:
    try:
        value = json.loads(candidate)
    except (json.JSONDecodeError, TypeError):
        return None
    return value if isinstance(value, dict) else None


def main() -> None:
    raw = os.environ.get(SECRET_NAME, "").strip()
    if not raw:
        raise SystemExit(
            f"::error::{SECRET_NAME} is missing. Add the Android google-services.json "
            "contents (raw JSON or Base64) to GitHub Actions secrets."
        )

    candidates: list[tuple[str, dict[str, Any]]] = []
    raw_config = parse_json_candidate(raw)
    if raw_config is not None:
        candidates.append(("raw JSON", raw_config))

    compact = "".join(raw.split())
    padded = compact + ("=" * (-len(compact) % 4))
    try:
        decoded = base64.b64decode(padded, altchars=b"-_", validate=True).decode("utf-8")
    except (binascii.Error, UnicodeDecodeError, ValueError):
        decoded = ""

    if decoded:
        decoded_config = parse_json_candidate(decoded)
        if decoded_config is not None:
            candidates.append(("Base64", decoded_config))

    valid_config: tuple[str, dict[str, Any]] | None = None
    package_mismatch = False
    for source, config in candidates:
        project_info = config.get("project_info")
        project_id = project_info.get("project_id") if isinstance(project_info, dict) else None
        clients = config.get("client", [])
        package_names = {
            client.get("client_info", {}).get("android_client_info", {}).get("package_name")
            for client in clients
            if isinstance(client, dict) and isinstance(client.get("client_info"), dict)
        }
        if not project_id:
            continue
        if PACKAGE_NAME not in package_names:
            package_mismatch = True
            continue
        valid_config = (source, config)
        break

    if valid_config is None:
        if package_mismatch:
            raise SystemExit(
                f"::error::Firebase config does not contain Android package {PACKAGE_NAME}. "
                "Download google-services.json for the correct Android app."
            )
        raise SystemExit(
            f"::error::{SECRET_NAME} is not valid google-services.json JSON or Base64. "
            "Replace the GitHub Actions secret with the raw file contents or its Base64 value."
        )

    source, config = valid_config
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(json.dumps(config, separators=(",", ":")) + "\n", encoding="utf-8")
    OUTPUT_PATH.chmod(0o600)
    print(
        f"Firebase Android configuration validated ({source}) for {PACKAGE_NAME}; "
        "FCM token registration is enabled in this build."
    )


if __name__ == "__main__":
    main()
