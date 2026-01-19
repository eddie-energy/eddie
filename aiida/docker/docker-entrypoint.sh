#!/bin/sh
chown -R aiida:eddie-energy /usr/local/aiida/security
exec "$@"