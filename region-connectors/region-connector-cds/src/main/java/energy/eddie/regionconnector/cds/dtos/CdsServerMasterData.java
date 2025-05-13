package energy.eddie.regionconnector.cds.dtos;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.master.data.Coverage;

import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public record CdsServerMasterData(String name, String id, URI baseUri, Set<Coverage> coverages) {
    public Set<String> countries() {
        Set<String> countries = new HashSet<>();
        for (var coverage : coverages()) {
            countries.add(coverage.countryCode().toLowerCase(Locale.ROOT));
        }
        return countries;
    }

    public Set<EnergyType> energyTypes() {
        Set<EnergyType> energyTypes = new HashSet<>();
        for (var coverage : coverages()) {
            energyTypes.add(coverage.energyType());
        }
        return energyTypes;
    }
}
