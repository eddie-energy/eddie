// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.shared;

import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.RawDataMessage;

public class TopicConfiguration {
    private final String eddieId;

    public TopicConfiguration(String eddieId) {this.eddieId = eddieId;}

    /**
     * Endpoint for {@link RawDataMessage}.
     * Used to emit messages to the eligible party.
     */
    public String rawDataMessage() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.AGNOSTIC,
                       TopicStructure.DocumentTypes.RAW_DATA_MESSAGE);
    }

    /**
     * Endpoint for {@link ConnectionStatusMessage}.
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
     * Endpoint for CIM near real-time data market documents.
     * Used to emit messages to the eligible party.
     *
     * @param cimVersion the version of the CIM that's used
     * @throws IllegalArgumentException if the cimVersion is not {@code CIM_1_04} or {@code CIM_1_12}
     */
    public String nearRealTimeDataMarketDocument(TopicStructure.DataModels cimVersion) {
        return switch (cimVersion) {
            case CIM_1_04, CIM_1_12 -> toTopic(TopicStructure.Direction.EP,
                                               cimVersion,
                                               TopicStructure.DocumentTypes.NEAR_REAL_TIME_DATA_MD);
            default -> throw new IllegalArgumentException("Invalid cim version: " + cimVersion);
        };
    }

    /**
     * Endpoint for CIM acknowledgement market documents.
     * Used to emit messages to the eligible party.
     */
    public String acknowledgementMarketDocument() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.CIM_1_12,
                       TopicStructure.DocumentTypes.ACKNOWLEDGEMENT_MD);
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

    /**
     * Endpoint for agnostic permission commands.
     * Used to receive messages from the eligible party.
     */
    public String permissionCommand() {
        return toTopic(TopicStructure.Direction.FW,
                       TopicStructure.DataModels.AGNOSTIC,
                       TopicStructure.DocumentTypes.PERMISSION_COMMAND);
    }

    /**
     * Endpoint for CIM min-max envelope.
     * Used to receive messages from the eligible party.
     */
    public String minMaxEnvelopeDocument() {
        return toTopic(TopicStructure.Direction.FW,
                       TopicStructure.DataModels.CIM_1_12,
                       TopicStructure.DocumentTypes.MIN_MAX_ENVELOPE_MD);
    }

    /**
     * Endpoint for incoming CIM min-max envelopes.
     * Used by the eligible party to receive forwarded messages from AIIDA.
     */
    public String forwardedMinMaxEnvelopeDocument() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.CIM_1_12,
                       TopicStructure.DocumentTypes.MIN_MAX_ENVELOPE_MD);
    }

    /**
     * Endpoint for opaque envelope with any payload.
     * Used to receive messages from the eligible party.
     */
    public String opaqueEnvelope() {
        return toTopic(TopicStructure.Direction.FW,
                       TopicStructure.DataModels.AGNOSTIC,
                       TopicStructure.DocumentTypes.OPAQUE_ENVELOPE);
    }

    /**
     * Endpoint for incoming opaque envelopes with any payload.
     * Used by the eligible party to receive forwarded messages from AIIDA.
     */
    public String forwardedOpaqueEnvelope() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.AGNOSTIC,
                       TopicStructure.DocumentTypes.OPAQUE_ENVELOPE);
    }


    /**
     * Endpoint for CIM Energy Sharing Reference Data Market Document.
     * Used to emit messages to the eligible party.
     */
    public String energySharingReferenceDataMarketDocument() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.CIM_1_12,
                       TopicStructure.DocumentTypes.ENERGY_SHARING_REFERENCE_DATA_MD);
    }

    /**
     * Endpoint for CIM Request Permission Market Document, the direct successor of {@link #permissionMarketDocument()}.
     * Used to emit messages to the eligible party.
     */
    public String requestPermissionMarketDocument() {
        return toTopic(TopicStructure.Direction.EP,
                       TopicStructure.DataModels.CIM_1_12,
                       TopicStructure.DocumentTypes.REQUEST_PERMISSION_MD);
    }


    private String toTopic(
            TopicStructure.Direction direction,
            TopicStructure.DataModels dataModel,
            TopicStructure.DocumentTypes documentType
    ) {
        return TopicStructure.toTopic(direction, eddieId, dataModel, documentType);
    }
}
