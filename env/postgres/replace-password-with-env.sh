#!/bin/sh
set -e

if [ -z "${EMQX_DATABASE_PASSWORD}" ]; then
  echo "Error: EMQX_DATABASE_PASSWORD environment variable is not set."
  exit 1
fi

PART_TO_REPLACE="REPLACED_BY_ENV"
CREATE_EMQX_USER_FILE="/tmp/create-emqx-user.sql"
BOOTSTRAP_FILE="/tmp/adapted-create-emqx-user.sql"

sed "s|${PART_TO_REPLACE}|${EMQX_DATABASE_PASSWORD}|g" "${CREATE_EMQX_USER_FILE}" > "${BOOTSTRAP_FILE}"

psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f ${BOOTSTRAP_FILE}