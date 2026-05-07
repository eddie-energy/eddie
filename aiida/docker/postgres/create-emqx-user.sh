#!/bin/sh
# SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
# SPDX-License-Identifier: Apache-2.0

set -eu

psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<SQL
CREATE USER emqx WITH PASSWORD '${EMQX_DATABASE_PASSWORD}';
SQL