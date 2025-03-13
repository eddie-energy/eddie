package energy.eddie.regionconnector.cds;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CdsRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "cds";
    private final CdsServerRepository repository;

    public CdsRegionConnectorMetadata(CdsServerRepository repository) {this.repository = repository;}

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public List<String> countryCodes() {
        var servers = repository.findAll();
        Set<String> countries = new HashSet<>();
        for (var server : servers) {
            countries.addAll(server.countryCodes());
        }
        return countries.stream().toList();
    }

    @Override
    public String countryCode() {
        return String.join("/", countryCodes());
    }

    @Override
    public long coveredMeteringPoints() {
        return 0;
    }

    @Override
    public Period earliestStart() {
        return Period.ofYears(-2);
    }

    @Override
    public Period latestEnd() {
        return Period.ofYears(3);
    }

    @Override
    public List<Granularity> supportedGranularities() {
        return Arrays.asList(Granularity.values());
    }

    @Override
    public ZoneId timeZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public List<EnergyType> supportedEnergyTypes() {
        var servers = repository.findAll();
        Set<EnergyType> energyTypes = new HashSet<>();
        for (var server : servers) {
            energyTypes.addAll(server.energyTypes());
        }
        return energyTypes.stream().toList();
    }

    @Override
    public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
        return List.of(ValidatedHistoricalDataDataNeed.class);
    }
}
