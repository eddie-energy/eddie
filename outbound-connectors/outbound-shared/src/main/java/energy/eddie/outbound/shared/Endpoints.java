package energy.eddie.outbound.shared;

/**
 * Utility class that contains often used endpoints for outbound-connectors.
 * Endpoints can either be a REST endpoint, Kafka topic, or an AMQP queue.
 */
public final class Endpoints {

    private Endpoints() {
        // Utility Class
    }

    /**
     * Non-CIM compliant endpoints.
     */
    public static final class Agnostic {
        /**
         * Endpoint for {@link energy.eddie.api.agnostic.ConnectionStatusMessage}.
         * Used to emit messages to the eligible party.
         */
        public static final String CONNECTION_STATUS_MESSAGE = "status-messages";
        /**
         * Endpoint for {@link energy.eddie.api.agnostic.RawDataMessage}.
         * Used to emit messages to the eligible party.
         */
        public static final String RAW_DATA_IN_PROPRIETARY_FORMAT = "raw-data-in-proprietary-format";

        private Agnostic() {
            // Utility Class
        }
    }

    /**
     * CIM v0.82 compliant endpoints.
     */
    @SuppressWarnings("java:S101") // Used to determine between CIM versions
    public static final class V0_82 {
        /**
         * Endpoint for CIM permission market documents.
         * Used to emit messages to the eligible party.
         */
        public static final String PERMISSION_MARKET_DOCUMENTS = "permission-market-documents";
        /**
         * Endpoint for CIM validated historical data market documents.
         * Used to emit messages to the eligible party.
         */
        public static final String VALIDATED_HISTORICAL_DATA = "validated-historical-data";
        /**
         * Endpoint for CIM accounting point market documents.
         * Used to emit messages to the eligible party.
         */
        public static final String ACCOUNTING_POINT_MARKET_DOCUMENTS = "accounting-point-market-documents";
        /**
         * Endpoint for CIM permission market documents, which terminate permission requests.
         * Used to receive messages from the eligible party.
         */
        public static final String TERMINATIONS = "terminations";

        private V0_82() {
            // Utility Class
        }
    }

    /**
     * CIM v0.91.08 compliant endpoints.
     */
    @SuppressWarnings("java:S101") // Used to determine between CIM versions
    public static final class V0_91_08 {

        /**
         * Endpoint for CIM retransmission requests.
         * Used to receive messages from the eligible party.
         */
        public static final String RETRANSMISSIONS = "retransmissions";

        private V0_91_08() {}
    }
}
