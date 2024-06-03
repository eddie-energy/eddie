package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.DefaultEnergyDataTimeframeStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.EnergyDataTimeframeStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionTimeframeStrategy;
import energy.eddie.regionconnector.shared.validation.GranularityChoice;

import java.util.List;
import java.util.function.Predicate;

public class DataNeedCalculationServiceImpl implements DataNeedCalculationService<DataNeed> {
    private final List<Class<? extends DataNeed>> supportedDataNeeds;
    private final RegionConnectorMetadata regionConnectorMetadata;
    private final GranularityChoice granularityChoice;
    private final PermissionTimeframeStrategy strategy;
    private final List<Predicate<DataNeed>> additionalChecks;
    private final EnergyDataTimeframeStrategy energyDataTimeframeStrategy;

    public DataNeedCalculationServiceImpl(
            List<Class<? extends DataNeed>> supportedDataNeeds,
            RegionConnectorMetadata regionConnectorMetadata
    ) {
        this(
                supportedDataNeeds,
                regionConnectorMetadata,
                new PermissionEndIsEnergyDataEndStrategy(regionConnectorMetadata.timeZone()),
                new DefaultEnergyDataTimeframeStrategy(regionConnectorMetadata),
                List.of()
        );
    }

    public DataNeedCalculationServiceImpl(
            List<Class<? extends DataNeed>> supportedDataNeeds,
            RegionConnectorMetadata regionConnectorMetadata,
            PermissionTimeframeStrategy strategy,
            EnergyDataTimeframeStrategy energyDataTimeframeStrategy,
            List<Predicate<DataNeed>> additionalChecks
    ) {
        this.supportedDataNeeds = supportedDataNeeds;
        this.regionConnectorMetadata = regionConnectorMetadata;
        this.granularityChoice = new GranularityChoice(regionConnectorMetadata.supportedGranularities());
        this.strategy = strategy;
        this.additionalChecks = additionalChecks;
        this.energyDataTimeframeStrategy = energyDataTimeframeStrategy;
    }

    @Override
    public DataNeedCalculation calculate(DataNeed dataNeed) {
        if (!supportsDataNeedType(dataNeed)) {
            return new DataNeedCalculation(false, "Data need type not supported");
        }

        var supportedGranularities = supportedGranularities(dataNeed);
        Timeframe energyStartAndEndDate;
        try {
            energyStartAndEndDate = energyDataTimeframeStrategy.energyDataTimeframe(dataNeed);
        } catch (UnsupportedDataNeedException e) {
            return new DataNeedCalculation(false, e.errorReason());
        }

        var permissionStartAndEndDate = strategy.permissionTimeframe(energyStartAndEndDate);
        return new DataNeedCalculation(
                additionalChecks.stream()
                                .allMatch(check -> check.test(dataNeed)),
                null,
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

    @Override
    public String regionConnectorId() {
        return regionConnectorMetadata.id();
    }
}
