// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.RegionConnectorFilter;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.DefaultEnergyDataTimeframeStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.EnergyDataTimeframeStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionTimeframeStrategy;
import energy.eddie.regionconnector.shared.validation.GranularityChoice;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Implementation of the {@link DataNeedCalculationService} that can be customized to fit the requirements of the region connector.
 */
@Transactional(value = Transactional.TxType.REQUIRED)
public class DataNeedCalculationServiceImpl implements DataNeedCalculationService<DataNeed> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataNeedCalculationServiceImpl.class);
    private final DataNeedsService dataNeedsService;
    private final RegionConnectorMetadata regionConnectorMetadata;
    private final PermissionTimeframeStrategy strategy;
    private final EnergyDataTimeframeStrategy energyDataTimeframeStrategy;
    private final DataNeedRuleSet dataNeedRuleSet;

    /**
     * Uses {@link PermissionEndIsEnergyDataEndStrategy} for the {@link PermissionTimeframeStrategy} and {@link DefaultEnergyDataTimeframeStrategy} for the {@link EnergyDataTimeframeStrategy}.
     * These are used to calculate the start and end of a permission request and the start and end of the metered data, if needed.
     *
     * @param dataNeedsService        service to get the data need
     * @param regionConnectorMetadata metadata of the region connector
     */
    public DataNeedCalculationServiceImpl(
            DataNeedsService dataNeedsService,
            RegionConnectorMetadata regionConnectorMetadata,
            DataNeedRuleSet dataNeedRuleSet
    ) {
        this(dataNeedsService,
             regionConnectorMetadata,
             new PermissionEndIsEnergyDataEndStrategy(),
             new DefaultEnergyDataTimeframeStrategy(regionConnectorMetadata),
             dataNeedRuleSet
        );
    }

    /**
     * Constructs an instance with custom {@link PermissionTimeframeStrategy} and {@link EnergyDataTimeframeStrategy}.
     * Furthermore, it allows adding additional checks for the data need.
     *
     * @param dataNeedsService            service to get the data need
     * @param regionConnectorMetadata     metadata of the region connector
     * @param strategy                    strategy that is used to calculate the permission timeframe
     * @param energyDataTimeframeStrategy strategy that is used to calculate the energy timeframe
     * @param dataNeedRuleSet             ruleset for data needs per region connector
     */
    public DataNeedCalculationServiceImpl(
            DataNeedsService dataNeedsService,
            RegionConnectorMetadata regionConnectorMetadata,
            PermissionTimeframeStrategy strategy,
            EnergyDataTimeframeStrategy energyDataTimeframeStrategy,
            DataNeedRuleSet dataNeedRuleSet
    ) {
        this.dataNeedsService = dataNeedsService;
        this.regionConnectorMetadata = regionConnectorMetadata;
        this.strategy = strategy;
        this.energyDataTimeframeStrategy = energyDataTimeframeStrategy;
        this.dataNeedRuleSet = dataNeedRuleSet;
    }

    @Override
    public DataNeedCalculationResult calculate(DataNeed dataNeed) {
        return calculate(dataNeed, ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public DataNeedCalculationResult calculate(DataNeed dataNeed, ZonedDateTime referenceDateTime) {
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
                return new DataNeedNotSupportedResult("Region connector " + regionConnectorId + " is not in the allowlist");
            }

            if (type == RegionConnectorFilter.Type.BLOCKLIST && rcIsInList) {
                return new DataNeedNotSupportedResult("Region connector " + regionConnectorId + " is in the blocklist");
            }
        }

        if (!dataNeedRuleSet.supportedDataNeeds().contains(dataNeed.getClass().getSimpleName())) {
            var classes = String.join(", ", dataNeedRuleSet.supportedDataNeeds());
            return new DataNeedNotSupportedResult(
                    "Data need type \"%s\" not supported, region connector supports data needs of types %s"
                            .formatted(dataNeed.getClass().getSimpleName(), classes)
            );
        }

        Timeframe energyStartAndEndDate;
        try {
            energyStartAndEndDate = energyDataTimeframeStrategy.energyDataTimeframe(dataNeed, referenceDateTime);
        } catch (UnsupportedDataNeedException e) {
            return new DataNeedNotSupportedResult(e.errorReason());
        }

        var permissionStartAndEndDate = strategy.permissionTimeframe(energyStartAndEndDate,
                                                                     ZonedDateTime.now(ZoneOffset.UTC));
        return switch (dataNeed) {
            case ValidatedHistoricalDataDataNeed vhdDataNeed when energyStartAndEndDate != null ->
                    onValidatedHistoricalDataNeed(vhdDataNeed, permissionStartAndEndDate, energyStartAndEndDate);
            case ValidatedHistoricalDataDataNeed vhdDataNeed -> {
                LOGGER.warn("Got no energy data timeframe for ValidatedHistoricalDataDataNeed {} with strategy {}",
                            vhdDataNeed.id(),
                            strategy.getClass());
                yield new DataNeedNotSupportedResult("Could not calculate timeframe for this data need");
            }
            case AiidaDataNeed ignored -> new ValidatedHistoricalDataDataNeedResult(List.of(),
                                                                                    permissionStartAndEndDate,
                                                                                    energyStartAndEndDate);
            case AccountingPointDataNeed ignored -> new AccountingPointDataNeedResult(permissionStartAndEndDate);
            default -> new DataNeedNotSupportedResult("Unknown data need type: %s".formatted(dataNeed.getClass()));
        };
    }

    @Override
    public DataNeedCalculationResult calculate(String dataNeedId) {
        return calculate(dataNeedId, ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public DataNeedCalculationResult calculate(String dataNeedId, ZonedDateTime referenceDateTime) {
        var option = dataNeedsService.findById(dataNeedId);
        if (option.isEmpty()) {
            return new DataNeedNotFoundResult();
        }
        return calculate(option.get(), referenceDateTime);
    }

    @Override
    public String regionConnectorId() {
        return regionConnectorMetadata.id();
    }

    private DataNeedCalculationResult onValidatedHistoricalDataNeed(
            ValidatedHistoricalDataDataNeed vhdDataNeed,
            Timeframe permissionStartAndEndDate,
            Timeframe energyStartAndEndDate
    ) {
        for (var rule : dataNeedRuleSet.dataNeedRules()) {
            if (rule instanceof ValidatedHistoricalDataDataNeedRule(
                    EnergyType energyType, List<Granularity> granularities
            )
                && energyType.equals(vhdDataNeed.energyType())) {
                var choice = new GranularityChoice(granularities);
                var supportedGranularities = choice.findAll(vhdDataNeed.minGranularity(),
                                                            vhdDataNeed.maxGranularity());

                if (supportedGranularities.isEmpty()) {
                    return new DataNeedNotSupportedResult("Granularities are not supported");
                }
                return new ValidatedHistoricalDataDataNeedResult(
                        supportedGranularities, permissionStartAndEndDate, energyStartAndEndDate);
            }
        }

        return new DataNeedNotSupportedResult("Energy type is not supported");
    }
}
