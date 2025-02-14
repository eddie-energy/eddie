package energy.eddie.aiida.models.datasource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public enum DataSourceType {
    SMART_METER_ADAPTER("SMART_METER_ADAPTER", "Smart Meter Adapter", Set.of("AT")),
    MICRO_TELEINFO_V3("MICRO_TELEINFO", "Micro Teleinfo v3", Set.of("FR")),
    SMART_GATEWAYS_ADAPTER("SMART_GATEWAYS_ADAPTER", "Smart Gateways Adapter", Set.of("NL", "BE", "CH")),
    SIMULATION("SIMULATION", "Simulation", Collections.emptySet());

    private final Set<String> countries;
    private final String identifier;
    private final String name;

    DataSourceType(String identifier, String name, Set<String> countries) {
        this.identifier = identifier;
        this.name = name;
        this.countries = Set.copyOf(countries);
    }

    public static DataSourceType fromIdentifier(String identifier) {
        return Arrays.stream(DataSourceType.values())
                     .filter(type -> type.identifier().equals(identifier))
                     .findFirst()
                     .orElseThrow();
    }

    public Set<String> countries() {
        return countries;
    }

    public String identifier() {
        return identifier;
    }

    public String dataSourceName() {
        return name;
    }
}
