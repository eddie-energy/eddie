package energy.eddie.aiida.models.datasource;

import java.util.Arrays;

public enum DataSourceType {
    SMART_METER_ADAPTER(Identifiers.SMART_METER_ADAPTER, "Österreichs Energie Adapter"),
    MICRO_TELEINFO(Identifiers.MICRO_TELEINFO, "Micro Teleinfo v3"),
    SMART_GATEWAYS_ADAPTER(Identifiers.SMART_GATEWAYS, "Smart Gateways Adapter"),
    SIMULATION(Identifiers.SIMULATION, "Simulation"),
    MODBUS(Identifiers.MODBUS_TCP, "Modbus");

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
        public static final String SMART_METER_ADAPTER = "SMART_METER_ADAPTER";
        public static final String MICRO_TELEINFO = "MICRO_TELEINFO";
        public static final String SMART_GATEWAYS = "SMART_GATEWAYS_ADAPTER";
        public static final String SIMULATION = "SIMULATION";
        public static final String MODBUS_TCP = "MODBUS";

        private Identifiers() {}
    }
}
