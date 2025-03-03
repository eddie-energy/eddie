package energy.eddie.regionconnector.cds.services.client.creation;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CoverageTypes {
    private final List<Coverages200ResponseAllOfCoverageEntriesInner> coverages;

    public CoverageTypes(List<Coverages200ResponseAllOfCoverageEntriesInner> coverages) {this.coverages = coverages;}

    public Set<EnergyType> toEnergyTypes() {
        return coverages.stream()
                        .map(Coverages200ResponseAllOfCoverageEntriesInner::getCommodityTypes)
                        .flatMap(Collection::stream)
                        .map(this::of)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
    }

    @Nullable
    private EnergyType of(Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum commodityType) {
        return switch (commodityType) {
            case ELECTRICITY -> EnergyType.ELECTRICITY;
            case NATURAL_GAS -> EnergyType.NATURAL_GAS;
            default -> null;
        };
    }
}
