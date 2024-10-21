package energy.eddie.aiida.models.datasource;

import java.util.Collections;
import java.util.List;

public enum DataSourceType {
    SMART_METER_ADAPTER("SMART_METER_ADAPTER", "Smart Meter Adapter", List.of("AT")),
    MICRO_TELEINFO_V3("MICRO_TELEINFO", "Micro Teleinfo v3", List.of("FR")),
    SMART_GATEWAYS_ADAPTER("SMART_GATEWAYS_ADAPTER", "Smart Gateways Adapter", List.of("NL", "BE", "CH")),
    SIMULATION("SIMULATION", "Simulation", Collections.emptyList());

    private final List<String> countries;
    private final String identifier;
    private final String name;

    DataSourceType(String identifier, String name, List<String> countries) {
        this.identifier = identifier;
        this.name = name;
        this.countries = countries;
    }

    public static DataSourceType fromIdentifier(String identifier) {
        for (DataSourceType type : DataSourceType.values()) {
            if (type.getIdentifier().equals(identifier)) {
                return type;
            }
        }
        return SIMULATION;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<String> getCountries() {
        return countries;
    }

    public String getName() {
        return name;
    }
}
