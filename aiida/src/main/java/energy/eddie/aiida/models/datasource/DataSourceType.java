package energy.eddie.aiida.models.datasource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public enum DataSourceType {
    OESTERREICHS_ENERGIE(Identifiers.OESTERREICHS_ENERGIE, "Österreichs Energie Adapter", Set.of("AT")),
    MICRO_TELEINFO_V3(Identifiers.MICRO_TELEINFO_V3, "Micro Teleinfo v3", Set.of("FR")),
    SMART_GATEWAYS(Identifiers.SMART_GATEWAYS, "Smart Gateways Adapter", Set.of("NL", "BE", "CH")),
    SIMULATION(Identifiers.SIMULATION, "Simulation", Collections.emptySet());

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

    public static class Identifiers {
        public static final String OESTERREICHS_ENERGIE = "OESTERREICHS_ENERGIE";
        public static final String MICRO_TELEINFO_V3 = "MICRO_TELEINFO_V3";
        public static final String SMART_GATEWAYS = "SMART_GATEWAYS";
        public static final String SIMULATION = "SIMULATION";
    }
}
