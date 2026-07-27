#!/bin/sh
set -eu

API_BASE_URL="${VITE_API_BASE_URL:-http://localhost:8080}"

printf 'window.__APP_CONFIG__ = {\n  apiBaseUrl: "%s"\n};\n' "$API_BASE_URL" \
  > /usr/share/nginx/html/config.js

exec nginx -g 'daemon off;'
