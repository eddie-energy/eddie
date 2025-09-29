#!/bin/sh
set -e

if [ -n "$CUSTOM_CA_CERT" ] && [ -f "$CUSTOM_CA_CERT" ]; then
  echo "Importing custom CA certificate: $CUSTOM_CA_CERT"
  keytool -importcert -noprompt \
    -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit \
    -file "$CUSTOM_CA_CERT" \
    -alias custom-ca || true
else
  echo "No custom CA cert provided, skipping import"
fi

exec java -jar example-app-backend.jar