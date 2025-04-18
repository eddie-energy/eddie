package energy.eddie.outbound.shared;

import energy.eddie.api.CommonInformationModelVersions;
import jakarta.annotation.Nullable;


public class TopicStructure {

    public static final String REDISTRIBUTION_TRANSACTION_RD_VALUE = "redistribution-transaction-rd";
    public static final String TERMINATION_MD_VALUE = "termination-md";
    public static final String CIM_0_91_08_VALUE = "cim_0_91_08";
    public static final String CIM_0_82_VALUE = "cim_0_82";

    private TopicStructure() {
        // No-Op
    }

    public static String toTopic(Direction direction, String eddieId, DataModels model, DocumentTypes type) {
        return direction.value() + "." + eddieId + "." + model.value() + "." + type.value();
    }

    public enum Direction {
        FW("fw"), EP("ep");
        private final String value;

        Direction(String value) {this.value = value;}

        public String value() {
            return value;
        }
    }

    public enum DataModels {
        CIM_0_82(CIM_0_82_VALUE, CommonInformationModelVersions.V0_82),
        CIM_0_91_08(CIM_0_91_08_VALUE, CommonInformationModelVersions.V0_91_08),
        AGNOSTIC("agnostic", null);

        private final String value;
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
    }

    public enum DocumentTypes {
        // CIM
        TERMINATION_MD(TopicStructure.TERMINATION_MD_VALUE),
        PERMISSION_MD("permission-md"),
        ACCOUNTING_POINT_MD("accounting-point-md"),
        VALIDATED_HISTORICAL_DATA_MD("validated-historical-data-md"),
        REDISTRIBUTION_TRANSACTION_RD(TopicStructure.REDISTRIBUTION_TRANSACTION_RD_VALUE),
        // AGNOSTIC
        RAW_DATA_MESSAGE("raw-data-message"),
        CONNECTION_STATUS_MESSAGE("connection-status-message"),
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
