#!/usr/bin/env bash
set -euo pipefail
[[ "${DEBUG:-}" == "1" ]] && set -x

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

usage() {
  cat <<'EOF'
Usage: APIURL=<base-url> [USERNAME=...] [EMAIL=...] [PASSWORD=...] spec-api/run-api-tests.sh

Run the RealWorld Postman collection against a Conduit-compatible API.

Required:
  APIURL   Base URL for API requests (no trailing slash)

Examples:
  # This project (no /api prefix):
  APIURL=http://localhost:8080 spec-api/run-api-tests.sh

  # Hosted RealWorld reference API (optional):
  APIURL=https://api.realworld.io/api spec-api/run-api-tests.sh

Optional:
  USERNAME  Default: u<timestamp>
  EMAIL     Default: <USERNAME>@mail.com
  PASSWORD  Default: password
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ -z "${APIURL:-}" ]]; then
  usage >&2
  exit 1
fi

USERNAME=${USERNAME:-u$(date +%s)}
EMAIL=${EMAIL:-$USERNAME@mail.com}
PASSWORD=${PASSWORD:-password}

npx newman run "$SCRIPTDIR/Conduit.postman_collection.json" \
  --delay-request 500 \
  --global-var "APIURL=$APIURL" \
  --global-var "USERNAME=$USERNAME" \
  --global-var "EMAIL=$EMAIL" \
  --global-var "PASSWORD=$PASSWORD"
