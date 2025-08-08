package energy.eddie.core.services;

import java.util.Arrays;
import java.util.Optional;

public enum RegionConnectorRepository {
    AT("at-eda"),
    NL("nl-mijn-aansluiting"),
    BE("be-fluvius"),
    CDS("cds"),
    DK("dk-energinet"),
    ES("es-datadis"),
    FI("fi-fingrid"),
    FR("fr-enedis"),
    US("us-green-button"),
    AIIDA("aiida");

    private final String beanName;

    RegionConnectorRepository(String beanName) {
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }

    public static Optional<RegionConnectorRepository> fromCountryCode(String code) {
        return Arrays.stream(values()).filter(region -> region.name().equalsIgnoreCase(code))
                .findFirst();
    }
}
