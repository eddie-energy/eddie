package energy.eddie.aiida.models.datasource;

public enum DataSourceType {
    SMART_METER_ADAPTER(Identifiers.SMART_METER_ADAPTER, "Österreichs Energie Adapter"),
    MICRO_TELEINFO(Identifiers.MICRO_TELEINFO, "Micro Teleinfo v3"),
    SMART_GATEWAYS_ADAPTER(Identifiers.SMART_GATEWAYS_ADAPTER, "Smart Gateways Adapter"),
    SHELLY(Identifiers.SHELLY, "Shelly"),
    INBOUND(Identifiers.INBOUND, "Inbound"),
    SIMULATION(Identifiers.SIMULATION, "Simulation"),
    MODBUS(Identifiers.MODBUS_TCP, "Modbus"),
    CIM_ADAPTER(Identifiers.CIM_ADAPTER, "CIM Adapter");

    private final String identifier;
    private final String name;

    DataSourceType(String identifier, String name) {
        this.identifier = identifier;
        this.name = name;
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
        public static final String SMART_GATEWAYS_ADAPTER = "SMART_GATEWAYS_ADAPTER";
        public static final String SHELLY = "SHELLY";
        public static final String INBOUND = "INBOUND";
        public static final String SIMULATION = "SIMULATION";
        public static final String MODBUS_TCP = "MODBUS";
        public static final String CIM_ADAPTER = "CIM_ADAPTER";

        private Identifiers() {}
    }
}
