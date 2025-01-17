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

/**
 * Implementation of the {@link DataNeedCalculationService} that can be customized to fit the requirements of the region connector.
 */
public class DataNeedCalculationServiceImpl implements DataNeedCalculationService<DataNeed> {
    private final DataNeedsService dataNeedsService;
    private final RegionConnectorMetadata regionConnectorMetadata;
    private final PermissionTimeframeStrategy strategy;
    private final List<Predicate<DataNeed>> additionalChecks;
    private final EnergyDataTimeframeStrategy energyDataTimeframeStrategy;

    /**
     * Uses {@link PermissionEndIsEnergyDataEndStrategy} for the {@link PermissionTimeframeStrategy} and {@link DefaultEnergyDataTimeframeStrategy} for the {@link EnergyDataTimeframeStrategy}.
     * These are used to calculate the start and end of a permission request and the start and end of the metered data, if needed.
     *
     * @param dataNeedsService        service to get the data need
     * @param regionConnectorMetadata metadata of the region connector
     */
    public DataNeedCalculationServiceImpl(
            DataNeedsService dataNeedsService,
            RegionConnectorMetadata regionConnectorMetadata
    ) {
        this(dataNeedsService,
             regionConnectorMetadata,
             new PermissionEndIsEnergyDataEndStrategy(regionConnectorMetadata.timeZone()),
             new DefaultEnergyDataTimeframeStrategy(regionConnectorMetadata),
             List.of()
        );
    }

    /**
     * Constructs an instance with custom {@link PermissionTimeframeStrategy} and {@link EnergyDataTimeframeStrategy}.
     * Furthermore, it allows adding additional checks for the data need.
     * @param dataNeedsService service to get the data need
     * @param regionConnectorMetadata metadata of the region connector
     * @param strategy strategy that is used to calculate the permission timeframe
     * @param energyDataTimeframeStrategy strategy that is used to calculate the energy timeframe
     * @param additionalChecks additional checks
     */
    public DataNeedCalculationServiceImpl(
            DataNeedsService dataNeedsService,
            RegionConnectorMetadata regionConnectorMetadata,
            PermissionTimeframeStrategy strategy,
            EnergyDataTimeframeStrategy energyDataTimeframeStrategy,
            List<Predicate<DataNeed>> additionalChecks
    ) {
        this.dataNeedsService = dataNeedsService;
        this.regionConnectorMetadata = regionConnectorMetadata;
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
        if(!isEnergyTypeSupported(dataNeed)) {
            return new DataNeedNotSupportedResult("Energy type is not supported");
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

    private boolean isEnergyTypeSupported(DataNeed dataNeed) {
        return !(dataNeed instanceof ValidatedHistoricalDataDataNeed vhd)
               || regionConnectorMetadata.supportedEnergyTypes().contains(vhd.energyType());
    }
    /**
     * Determines if the region-connector supports this data need type.
     *
     * @param dataNeed the data need, which should be checked
     * @return if the data need is supported
     */
    private boolean supportsDataNeedType(DataNeed dataNeed) {
        for (Class<? extends DataNeedInterface> supportedDataNeed : regionConnectorMetadata.supportedDataNeeds()) {
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
        var choice = new GranularityChoice(regionConnectorMetadata.granularitiesFor(vhdDN.energyType()));
        return choice.findAll(vhdDN.minGranularity(), vhdDN.maxGranularity());
    }
}
