package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.*;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.DefaultEnergyDataTimeframeStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.EnergyDataTimeframeStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionTimeframeStrategy;
import energy.eddie.regionconnector.shared.validation.GranularityChoice;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DataNeedCalculationServiceImpl implements DataNeedCalculationService<DataNeed> {
    private final DataNeedsService dataNeedsService;
    private final List<Class<? extends DataNeed>> supportedDataNeeds;
    private final RegionConnectorMetadata regionConnectorMetadata;
    private final GranularityChoice granularityChoice;
    private final PermissionTimeframeStrategy strategy;
    private final List<Predicate<DataNeed>> additionalChecks;
    private final EnergyDataTimeframeStrategy energyDataTimeframeStrategy;

    public DataNeedCalculationServiceImpl(
            DataNeedsService dataNeedsService,
            List<Class<? extends DataNeed>> supportedDataNeeds,
            RegionConnectorMetadata regionConnectorMetadata
    ) {
        this(dataNeedsService,
             supportedDataNeeds,
             regionConnectorMetadata,
             new PermissionEndIsEnergyDataEndStrategy(regionConnectorMetadata.timeZone()),
             new DefaultEnergyDataTimeframeStrategy(regionConnectorMetadata),
             List.of()
        );
    }

    public DataNeedCalculationServiceImpl(
            DataNeedsService dataNeedsService,
            List<Class<? extends DataNeed>> supportedDataNeeds,
            RegionConnectorMetadata regionConnectorMetadata,
            PermissionTimeframeStrategy strategy,
            EnergyDataTimeframeStrategy energyDataTimeframeStrategy,
            List<Predicate<DataNeed>> additionalChecks
    ) {
        this.dataNeedsService = dataNeedsService;
        this.supportedDataNeeds = supportedDataNeeds;
        this.regionConnectorMetadata = regionConnectorMetadata;
        this.granularityChoice = new GranularityChoice(regionConnectorMetadata.supportedGranularities());
        this.strategy = strategy;
        this.additionalChecks = additionalChecks;
        this.energyDataTimeframeStrategy = energyDataTimeframeStrategy;
    }

    @Override
    public DataNeedCalculationResult calculate(DataNeed dataNeed) {
        if (!dataNeed.isEnabled()) {
            return new DataNeedNotSupportedResult("Data need is disabled");
        }
        var filter = dataNeed.regionConnectorFilter();
        if (filter.isPresent()) {
            var regionConnectorId = regionConnectorMetadata.id();
            var rcIsInList = filter.get()
                                   .regionConnectorIds()
                                   .contains(regionConnectorId);

            var type = filter.get().type();
            if (type == RegionConnectorFilter.Type.ALLOWLIST && !rcIsInList) {
                return new DataNeedNotSupportedResult("Region connector " + regionConnectorMetadata.id() + " is not in the allowlist");
            }

            if (type == RegionConnectorFilter.Type.BLOCKLIST && rcIsInList) {
                return new DataNeedNotSupportedResult("Region connector " + regionConnectorMetadata.id() + " is in the blocklist");
            }
        }
        if (!supportsDataNeedType(dataNeed)) {
            var classes = regionConnectorMetadata.supportedDataNeeds()
                                                 .stream()
                                                 .map(Class::getSimpleName)
                                                 .collect(Collectors.joining(", "));
            return new DataNeedNotSupportedResult(
                    "Data need type \"%s\" not supported, region connector supports data needs of types %s"
                            .formatted(dataNeed.getClass().getSimpleName(), classes)
            );
        }
        if (!additionalChecks.stream().allMatch(check -> check.test(dataNeed))) {
            return new DataNeedNotSupportedResult("Data need not supported, additional checks have failed");
        }
        var supportedGranularities = supportedGranularities(dataNeed);
        if (!areGranularitiesSupported(dataNeed, supportedGranularities)) {
            return new DataNeedNotSupportedResult("Granularities are not supported");
        }
        Timeframe energyStartAndEndDate;
        try {
            energyStartAndEndDate = energyDataTimeframeStrategy.energyDataTimeframe(dataNeed);
        } catch (UnsupportedDataNeedException e) {
            return new DataNeedNotSupportedResult(e.errorReason());
        }

        var permissionStartAndEndDate = strategy.permissionTimeframe(energyStartAndEndDate);
        return switch (dataNeed) {
            case TimeframedDataNeed ignored -> new ValidatedHistoricalDataDataNeedResult(supportedGranularities,
                                                                                         permissionStartAndEndDate,
                                                                                         energyStartAndEndDate);
            case AccountingPointDataNeed ignored -> new AccountingPointDataNeedResult(permissionStartAndEndDate);
            default -> new DataNeedNotSupportedResult("Unknown data need type: %s".formatted(dataNeed.getClass()));
        };
    }

    @Override
    public DataNeedCalculationResult calculate(String dataNeedId) {
        var option = dataNeedsService.findById(dataNeedId);
        if (option.isEmpty()) {
            return new DataNeedNotFoundResult();
        }
        return calculate(option.get());
    }

    @Override
    public String regionConnectorId() {
        return regionConnectorMetadata.id();
    }

    private static boolean areGranularitiesSupported(DataNeed dataNeed, List<Granularity> supportedGranularities) {
        return !(dataNeed instanceof ValidatedHistoricalDataDataNeed) || !supportedGranularities.isEmpty();
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
}
