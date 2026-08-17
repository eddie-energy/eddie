// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

/**
 * Supported mechanisms for provisioning inbound data.
 */
public enum InboundProvisioningType {
    NONE,
    /**
     * Retrieves records through the REST endpoint with an API key supplied in a request header.
     */
    REST_BEARER,

    /**
     * Retrieves records through the REST endpoint with an API key supplied as a query parameter.
     */
    REST_API_TOKEN,

    /** Publishes records to the AIIDA-managed MQTT broker. */
    MQTT_SERVER,

    /** Publishes records to an externally configured MQTT broker. */
    MQTT_CLIENT;

    public static class Identifiers {
        public static final String MQTT_CLIENT = "MQTT_CLIENT";

        private Identifiers() {
        }
    }
}
