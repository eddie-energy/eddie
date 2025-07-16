package energy.eddie.outbound.shared;

public class TopicConfiguration {
    private final String eddieId;

    public TopicConfiguration(String eddieId) {this.eddieId = eddieId;}

    /**
     * Endpoint for {@link energy.eddie.api.agnostic.RawDataMessage}.
     * Used to emit messages to the eligible party.
     */
    public String rawDataMessage() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.AGNOSTIC,
                       TopicStructure.DocumentTypes.RAW_DATA_MESSAGE);
    }

    /**
     * Endpoint for {@link energy.eddie.api.agnostic.ConnectionStatusMessage}.
     * Used to emit messages to the eligible party.
     */
    public String connectionStatusMessage() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.AGNOSTIC,
                       TopicStructure.DocumentTypes.CONNECTION_STATUS_MESSAGE);
    }

    /**
     * Endpoint for CIM permission market documents.
     * Used to emit messages to the eligible party.
     */
    public String permissionMarketDocument() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.CIM_0_82,
                       TopicStructure.DocumentTypes.PERMISSION_MD);
    }

    /**
     * Endpoint for CIM validated historical data market documents.
     * Used to emit messages to the eligible party.
     *
     * @param cimVersion the version of the CIM that's used
     * @throws IllegalArgumentException if the cimVersion is not {@code CIM_0_82} or {@code CIM_1_04}
     */
    public String validatedHistoricalDataMarketDocument(TopicStructure.DataModels cimVersion) {
        return switch (cimVersion) {
            case CIM_0_82, CIM_1_04 -> toTopic(TopicStructure.Direction.EP,
                                               cimVersion,
                                               TopicStructure.DocumentTypes.VALIDATED_HISTORICAL_DATA_MD);
            default -> throw new IllegalArgumentException("Invalid cim version: " + cimVersion);
        };
    }

    /**
     * Endpoint for CIM accounting point market documents.
     * Used to emit messages to the eligible party.
     */
    public String accountingPointMarketDocument() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.CIM_0_82,
                       TopicStructure.DocumentTypes.ACCOUNTING_POINT_MD);
    }

    /**
     * Endpoint for CIM permission market documents, which terminate permission requests.
     * Used to receive messages from the eligible party.
     */
    public String terminationMarketDocument() {
        return toTopic(TopicStructure.Direction.FW,
                       TopicStructure.DataModels.CIM_0_82,
                       TopicStructure.DocumentTypes.TERMINATION_MD);
    }

    /**
     * Endpoint for CIM retransmission requests.
     * Used to receive messages from the eligible party.
     */
    public String redistributionTransactionRequestDocument() {
        return toTopic(TopicStructure.Direction.FW,
                       TopicStructure.DataModels.CIM_0_91_08,
                       TopicStructure.DocumentTypes.REDISTRIBUTION_TRANSACTION_RD);
    }

    private String toTopic(
            TopicStructure.Direction direction,
            TopicStructure.DataModels dataModel,
            TopicStructure.DocumentTypes documentType
    ) {
        return TopicStructure.toTopic(direction, eddieId, dataModel, documentType);
    }
}
