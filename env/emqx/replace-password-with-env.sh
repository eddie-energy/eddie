#!/bin/sh

# SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
# SPDX-License-Identifier: Apache-2.0

set -e

if [ -z "${REGION_CONNECTOR_AIIDA_MQTT_PASSWORD}" ]; then
  echo "Error: REGION_CONNECTOR_AIIDA_MQTT_PASSWORD environment variable is not set."
  exit 1
fi

PART_TO_REPLACE="REPLACED_BY_ENV"
INIT_USER_FILE="/tmp/init-user.json"
BOOTSTRAP_FILE="/opt/emqx/data/init-user.json"

sed "s|${PART_TO_REPLACE}|${REGION_CONNECTOR_AIIDA_MQTT_PASSWORD}|g" "${INIT_USER_FILE}" > "${BOOTSTRAP_FILE}"

exec /usr/bin/docker-entrypoint.sh emqx foreground