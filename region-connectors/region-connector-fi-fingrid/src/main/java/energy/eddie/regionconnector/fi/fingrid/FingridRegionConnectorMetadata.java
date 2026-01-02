package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.fi.fingrid.data.needs.FingridDataNeedRuleSet;

import java.time.Period;
import java.time.ZoneId;
import java.util.List;

public class FingridRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final RegionConnectorMetadata INSTANCE = new FingridRegionConnectorMetadata();
    public static final String REGION_CONNECTOR_ID = "fi-fingrid";
    public static final ZoneId ZONE_ID_FINLAND = ZoneId.of("Europe/Helsinki");
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(ValidatedHistoricalDataDataNeed.class, AccountingPointDataNeed.class);

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "FI";
    }

    @Override
    public long coveredMeteringPoints() {
        return 4_000_000;
    }

    @Override
    public Period earliestStart() {
        return Period.ofYears(-6);
    }

    @Override
    public Period latestEnd() {
        return Period.ofYears(2);
    }

    @Override
    public List<Granularity> supportedGranularities() {
        return FingridDataNeedRuleSet.SUPPORTED_GRANULARITIES;
    }

    @Override
    public ZoneId timeZone() {
        return ZONE_ID_FINLAND;
    }

    @Override
    public List<EnergyType> supportedEnergyTypes() {
        return List.of(EnergyType.ELECTRICITY);
    }

    @Override
    public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
        return List.copyOf(SUPPORTED_DATA_NEEDS);
    }
}
