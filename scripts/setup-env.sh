#!/usr/bin/env bash
# Generates a local .env with random secrets (never committed). Use --force to overwrite.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -f .env && "${1:-}" != "--force" ]]; then
  echo ".env already exists (use --force to regenerate)"; exit 0
fi

rand() { openssl rand -base64 "$1" | tr -d '/+=\n' | cut -c1-"$2"; }

sed \
  -e "s|^DB_PASSWORD=.*|DB_PASSWORD=$(rand 24 24)|" \
  -e "s|^JWT_SECRET=.*|JWT_SECRET=$(rand 48 48)|" \
  -e "s|^BOOTSTRAP_ADMIN_PASSWORD=.*|BOOTSTRAP_ADMIN_PASSWORD=$(rand 18 16)Aa1|" \
  -e "s|^BOOTSTRAP_TECHNICIAN_PASSWORD=.*|BOOTSTRAP_TECHNICIAN_PASSWORD=$(rand 18 16)Aa1|" \
  -e "s|^GRAFANA_ADMIN_PASSWORD=.*|GRAFANA_ADMIN_PASSWORD=$(rand 18 16)|" \
  .env.example > .env
chmod 600 .env
echo ".env created. Admin: $(grep ^BOOTSTRAP_ADMIN_EMAIL .env | cut -d= -f2) / password in .env (BOOTSTRAP_ADMIN_PASSWORD)"
