package energy.eddie.regionconnector.cds.client;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.master.data.Coverage;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class CoverageTypes {
    private final List<Coverages200ResponseAllOfCoverageEntriesInner> coverages;

    CoverageTypes(List<Coverages200ResponseAllOfCoverageEntriesInner> coverages) {this.coverages = coverages;}

    Set<Coverage> toCoverages() {
        Set<Coverage> set = new HashSet<>();
        for (var coverage : coverages) {
            var commodityTypes = coverage.getCommodityTypes();
            for (var commodityTypesEnum : commodityTypes) {
                var energyType = of(commodityTypesEnum);
                if (energyType != null) {
                    set.add(new Coverage(energyType, coverage.getCountry().toLowerCase(Locale.ROOT)));
                }
            }
        }
        return set;
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
