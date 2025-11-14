package energy.eddie.outbound.shared;

import energy.eddie.cim.CommonInformationModelVersions;
import jakarta.annotation.Nullable;


/**
 * Utility class to track different document types that can be consumed or produced by eddie
 */
public class TopicStructure {

    public static final String REDISTRIBUTION_TRANSACTION_RD_VALUE = "redistribution-transaction-rd";
    public static final String TERMINATION_MD_VALUE = "termination-md";
    public static final String CIM_1_04_VALUE = "cim_1_04";
    public static final String CIM_0_91_08_VALUE = "cim_0_91_08";
    public static final String CIM_0_82_VALUE = "cim_0_82";
    public static final String AGNOSTIC_VALUE = "agnostic";

    public static final String CONNECTION_STATUS_MESSAGE_VALUE = "connection-status-message";

    private TopicStructure() {
        // No-Op
    }

    /**
     * Creates a standard name for topics using a dot(".") as delimiter.
     *
     * @param direction Declares who the recipient of the document is.
     * @param eddieId   The ID of the eddie instance for the respective outbound connector.
     * @param model     The data model to which the document type belongs to.
     * @param type      The document type provided by the topic.
     * @return a string with the topic name
     */
    public static String toTopic(Direction direction, String eddieId, DataModels model, DocumentTypes type) {
        return direction.value() + "." + eddieId + "." + model.value() + "." + type.value();
    }

    /**
     * The recipient of a specific document.
     */
    public enum Direction {
        /**
         * The eddie framework(fw) is the recipient.
         */
        FW("fw"),
        /**
         * The eligible party(ep) is the recipient.
         */
        EP("ep");
        private final String value;

        Direction(String value) {this.value = value;}

        public String value() {
            return value;
        }
    }

    /**
     * All the supported data models
     */
    public enum DataModels {
        /**
         * Common Information Model version 0.82
         */
        CIM_0_82(CIM_0_82_VALUE, CommonInformationModelVersions.V0_82),
        /**
         * Common Information Model version 0.91.08
         */
        CIM_0_91_08(CIM_0_91_08_VALUE, CommonInformationModelVersions.V0_91_08),
        /**
         * Common Information Model version 0.1.04
         */
        CIM_1_04(CIM_1_04_VALUE, CommonInformationModelVersions.V1_04),
        /**
         * Eddie's internal model.
         */
        AGNOSTIC(AGNOSTIC_VALUE, null);

        private final String value;
        @SuppressWarnings("FieldCanBeLocal")
        @Nullable
        private final CommonInformationModelVersions version;

        DataModels(String value, @Nullable CommonInformationModelVersions version) {
            this.value = value;
            this.version = version;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Nullable
        public CommonInformationModelVersions version() {
            return version;
        }
    }

    /**
     * The document types that can be consumed or produced by eddie.
     */
    public enum DocumentTypes {
        // CIM
        TERMINATION_MD(TopicStructure.TERMINATION_MD_VALUE),
        PERMISSION_MD("permission-md"),
        ACCOUNTING_POINT_MD("accounting-point-md"),
        VALIDATED_HISTORICAL_DATA_MD("validated-historical-data-md"),
        REDISTRIBUTION_TRANSACTION_RD(TopicStructure.REDISTRIBUTION_TRANSACTION_RD_VALUE),
        NEAR_REAL_TIME_DATA_MD("near-real-time-data-md"),
        // AGNOSTIC
        RAW_DATA_MESSAGE("raw-data-message"),
        CONNECTION_STATUS_MESSAGE(CONNECTION_STATUS_MESSAGE_VALUE),
        ;
        private final String value;

        DocumentTypes(String value) {this.value = value;}

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
