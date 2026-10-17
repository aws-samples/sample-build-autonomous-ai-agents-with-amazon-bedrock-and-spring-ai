#!/bin/sh
# Writes /usr/share/nginx/html/config.js from environment variables so one image
# can point at any set of UI/backend URLs. Run by the nginx image entrypoint.
set -eu

# Allow only URL-safe characters so a value cannot break out of the JS string.
check() {
  case "$2" in
    *[!A-Za-z0-9:/._~%-]*) echo "40-landing-config: invalid characters in $1" >&2; exit 1 ;;
  esac
}

AUTONOMOUS_UI_URL="${AUTONOMOUS_UI_URL:-http://localhost:5173}"
AUTONOMOUS_API_URL="${AUTONOMOUS_API_URL:-http://localhost:8080}"
IDP_UI_URL="${IDP_UI_URL:-http://localhost:5174}"
IDP_API_URL="${IDP_API_URL:-http://localhost:8081}"

check AUTONOMOUS_UI_URL "$AUTONOMOUS_UI_URL"
check AUTONOMOUS_API_URL "$AUTONOMOUS_API_URL"
check IDP_UI_URL "$IDP_UI_URL"
check IDP_API_URL "$IDP_API_URL"

cat > /usr/share/nginx/html/config.js <<EOF
window.LANDING_CONFIG = {
  autonomous: { ui: '${AUTONOMOUS_UI_URL}', api: '${AUTONOMOUS_API_URL}' },
  idp: { ui: '${IDP_UI_URL}', api: '${IDP_API_URL}' },
};
EOF
echo "40-landing-config: wrote config.js"
