package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionTimeframeStrategy;
import energy.eddie.regionconnector.shared.validation.GranularityChoice;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

public class DataNeedCalculationServiceImpl implements DataNeedCalculationService<DataNeed> {
    private final List<Class<? extends DataNeed>> supportedDataNeeds;
    private final Period earliestStart;
    private final Period latestEnd;
    private final RegionConnectorMetadata regionConnectorMetadata;
    private final GranularityChoice granularityChoice;
    private final ZoneId referenceTimezone;
    private final PermissionTimeframeStrategy strategy;

    public DataNeedCalculationServiceImpl(
            List<Granularity> granularities,
            List<Class<? extends DataNeed>> supportedDataNeeds,
            Period earliestStart,
            Period latestEnd,
            RegionConnectorMetadata regionConnectorMetadata,
            ZoneId referenceTimezone
    ) {
        this(
                granularities,
                supportedDataNeeds,
                earliestStart,
                latestEnd,
                regionConnectorMetadata,
                referenceTimezone,
                new PermissionEndIsEnergyDataEndStrategy(referenceTimezone)
        );
    }

    public DataNeedCalculationServiceImpl(
            List<Granularity> granularities,
            List<Class<? extends DataNeed>> supportedDataNeeds,
            Period earliestStart,
            Period latestEnd,
            RegionConnectorMetadata regionConnectorMetadata,
            ZoneId referenceTimezone,
            PermissionTimeframeStrategy strategy
    ) {
        this.supportedDataNeeds = supportedDataNeeds;
        this.earliestStart = earliestStart;
        this.latestEnd = latestEnd;
        this.regionConnectorMetadata = regionConnectorMetadata;
        this.granularityChoice = new GranularityChoice(granularities);
        this.referenceTimezone = referenceTimezone;
        this.strategy = strategy;
    }

    @Override
    public DataNeedCalculation calculate(DataNeed dataNeed) {
        if (!supportsDataNeedType(dataNeed)) {
            return new DataNeedCalculation(false);
        }
        var supportedGranularities = supportedGranularities(dataNeed);
        Timeframe energyStartAndEndDate;
        try {
            energyStartAndEndDate = calculateEnergyDataStartAndEndDateOrThrow(dataNeed);
        } catch (UnsupportedDataNeedException e) {
            return new DataNeedCalculation(false);
        }
        var permissionStartAndEndDate = strategy.permissionTimeframe(energyStartAndEndDate);
        return new DataNeedCalculation(
                true,
                supportedGranularities,
                permissionStartAndEndDate,
                energyStartAndEndDate
        );
    }

    /**
     * Determines if the region-connector supports this data need type.
     *
     * @param dataNeed the data need, which should be checked
     * @return if the data need is supported
     */
    private boolean supportsDataNeedType(DataNeed dataNeed) {
        for (Class<? extends DataNeed> supportedDataNeed : supportedDataNeeds) {
            if (supportedDataNeed.isInstance(dataNeed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates a list of supported granularities based on a data need. If the data need is not a timeframed data
     * need, an empty list is returned.
     *
     * @param dataNeed the data need
     * @return a list of supported granularities, which can be empty if the data need is not a timeframed data need.
     */
    private List<Granularity> supportedGranularities(DataNeed dataNeed) {
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhdDN)) {
            return List.of();
        }
        return granularityChoice.findAll(vhdDN.minGranularity(), vhdDN.maxGranularity());
    }

    /**
     * Calculates the start and end date of the energy data, if the data need requires energy data. Otherwise, it
     * returns null.
     *
     * @param dataNeed the data need
     * @return start and end date of the energy data.
     * @throws UnsupportedDataNeedException if the data need is not meant for energy data
     */
    @Nullable
    private Timeframe calculateEnergyDataStartAndEndDateOrThrow(DataNeed dataNeed) throws UnsupportedDataNeedException {
        if (!(dataNeed instanceof TimeframedDataNeed timeframedDataNeed)) {
            return null;
        }
        DataNeedWrapper wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(timeframedDataNeed,
                                                                                       LocalDate.now(referenceTimezone),
                                                                                       earliestStart,
                                                                                       latestEnd);
        return new Timeframe(wrapper.calculatedStart(), wrapper.calculatedEnd());
    }

    @Override
    public String regionConnectorId() {
        return regionConnectorMetadata.id();
    }
}
