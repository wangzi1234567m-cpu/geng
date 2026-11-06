#!/usr/bin/env bash
set -euo pipefail

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=ON_ERROR_STOP=1 \
  --set=app_password="$GENG_APP_PASSWORD" \
  --set=migration_password="$GENG_MIGRATION_PASSWORD" \
  --file=/docker-entrypoint-initdb.d/bootstrap.sql.inc
