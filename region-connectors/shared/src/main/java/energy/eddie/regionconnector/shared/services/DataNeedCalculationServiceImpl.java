package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.shared.validation.GranularityChoice;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;

public class DataNeedCalculationServiceImpl implements DataNeedCalculationService<DataNeed> {
    private final List<Class<? extends DataNeed>> supportedDataNeeds;
    private final Period earliestStart;
    private final Period latestEnd;
    private final RegionConnectorMetadata regionConnectorMetadata;
    private final GranularityChoice granularityChoice;

    public DataNeedCalculationServiceImpl(
            List<Granularity> granularities,
            List<Class<? extends DataNeed>> supportedDataNeeds,
            Period earliestStart,
            Period latestEnd,
            RegionConnectorMetadata regionConnectorMetadata
    ) {
        this.supportedDataNeeds = supportedDataNeeds;
        this.earliestStart = earliestStart;
        this.latestEnd = latestEnd;
        this.regionConnectorMetadata = regionConnectorMetadata;
        this.granularityChoice = new GranularityChoice(granularities);
    }

    @Override
    public boolean supportsDataNeed(DataNeed dataNeed) {
        for (Class<? extends DataNeed> supportedDataNeed : supportedDataNeeds) {
            if (supportedDataNeed.isInstance(dataNeed)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Granularity> supportedGranularities(DataNeed dataNeed) {
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhdDN)) {
            return List.of();
        }
        return granularityChoice.findAll(vhdDN.minGranularity(), vhdDN.maxGranularity());
    }

    /**
     * Currently, only supports the start and end date for a permission specific for Austria.
     *
     * @param dataNeed the data need, which should be used for the calculations
     * @return the start and end date of the permission
     */
    @Override
    public Pair<LocalDate, LocalDate> calculatePermissionStartAndEndDate(DataNeed dataNeed) {
        var now = LocalDate.now(ZoneOffset.UTC);
        var res = calculateEnergyDataStartAndEndDate(dataNeed);
        if (res != null && res.value().isAfter(now)) {
            return new Pair<>(now, res.value());
        }
        return new Pair<>(now, now);
    }

    @Override
    @Nullable
    public Pair<LocalDate, LocalDate> calculateEnergyDataStartAndEndDate(DataNeed dataNeed) {
        if (!(dataNeed instanceof TimeframedDataNeed timeframedDataNeed)) {
            return null;
        }
        var wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(timeframedDataNeed,
                                                                           LocalDate.now(ZoneOffset.UTC),
                                                                           earliestStart,
                                                                           latestEnd);
        return new Pair<>(wrapper.calculatedStart(), wrapper.calculatedEnd());
    }

    @Override
    public String regionConnectorId() {
        return regionConnectorMetadata.id();
    }
}
