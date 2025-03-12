package energy.eddie.aiida.models.datasource;

import java.util.Arrays;

public enum DataSourceType {
    OESTERREICHS_ENERGIE(Identifiers.OESTERREICHS_ENERGIE, "Österreichs Energie Adapter"),
    MICRO_TELEINFO_V3(Identifiers.MICRO_TELEINFO_V3, "Micro Teleinfo v3"),
    SMART_GATEWAYS(Identifiers.SMART_GATEWAYS, "Smart Gateways Adapter"),
    SIMULATION(Identifiers.SIMULATION, "Simulation");

    private final String identifier;
    private final String name;

    DataSourceType(String identifier, String name) {
        this.identifier = identifier;
        this.name = name;
    }

    public static DataSourceType fromIdentifier(String identifier) {
        return Arrays.stream(DataSourceType.values())
                     .filter(type -> type.identifier().equals(identifier))
                     .findFirst()
                     .orElseThrow();
    }

    public String identifier() {
        return identifier;
    }

    public String dataSourceName() {
        return name;
    }

    public static class Identifiers {
        public static final String OESTERREICHS_ENERGIE = "OESTERREICHS_ENERGIE";
        public static final String MICRO_TELEINFO_V3 = "MICRO_TELEINFO_V3";
        public static final String SMART_GATEWAYS = "SMART_GATEWAYS";
        public static final String SIMULATION = "SIMULATION";
    }
}
