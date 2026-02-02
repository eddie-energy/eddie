package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.data.needs.MultipleDataNeedCalculationResult.CalculationResult;
import energy.eddie.api.agnostic.data.needs.MultipleDataNeedCalculationResult.InvalidDataNeedCombination;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.RegionConnectorFilter;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.AllowMultipleDataNeedsRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import static energy.eddie.dataneeds.rules.DataNeedRule.InboundAiidaDataNeedRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedCalculationServiceImplTest {
    private final RegionConnectorMetadata metadata = new RegionConnectorMetadataImpl(
            "id",
            "AT",
            1,
            Period.ofDays(-10),
            Period.ofDays(10),
            List.of(Granularity.PT15M, Granularity.P1D),
            List.of(ValidatedHistoricalDataDataNeed.class, AccountingPointDataNeed.class),
            ZoneOffset.UTC,
            List.of(EnergyType.ELECTRICITY));
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;

    @Test
    void givenUnknownDataNeedId_returnsDataNeedNotFoundResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.empty());
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata, List::of);
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res).isInstanceOf(DataNeedNotFoundResult.class);
    }

    @ParameterizedTest
    @MethodSource("regionConnectorFilterConfigurations")
    // needed for the isEnabled() call, when data need is allowed
    @MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
    void testCalculate_regionConnectorFilterReturnsExpectedResult(
            RegionConnectorFilter regionConnectorFilter,
            Class<? extends DataNeedCalculationResult> expected
    ) {
        // Given
        when(accountingPointDataNeed.isEnabled()).thenReturn(true);
        when(accountingPointDataNeed.regionConnectorFilter()).thenReturn(Optional.of(regionConnectorFilter));
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(accountingPointDataNeed));
        var calculationService = new DataNeedCalculationServiceImpl(
                dataNeedsService,
                metadata,
                () -> List.of(new AccountingPointDataNeedRule())
        );

        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res).isInstanceOf(expected);
    }

    @Test
    void givenDisabledDataNeed_returnsDataNeedNotSupportedResult() {
        // Given
        when(accountingPointDataNeed.isEnabled()).thenReturn(false);
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(accountingPointDataNeed));
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata, List::of);
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res).isInstanceOf(DataNeedNotSupportedResult.class);
    }

    @Test
    void givenUnsupportedDataNeed_returnsDataNeedNotSupportedResult() {
        // Given
        var dn = new OutboundAiidaDataNeed();
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(dn));
        var calculationService = new DataNeedCalculationServiceImpl(
                dataNeedsService,
                metadata,
                () -> List.of(new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, List.of()),
                              new AccountingPointDataNeedRule(),
                              new AllowMultipleDataNeedsRule())
        );
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res)
                .asInstanceOf(type(DataNeedNotSupportedResult.class))
                .extracting(DataNeedNotSupportedResult::message)
                .isEqualTo(
                        "Data need type \"OutboundAiidaDataNeed\" not supported, region connector supports data needs of types AccountingPointDataNeed, ValidatedHistoricalDataDataNeed"
                );
    }

    @Test
    void givenValidatedHistoricalDataDataNeed_withSupportedGranularities_returnsValidatedHistoricalDataNeedResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT15M,
                        Granularity.P1D
                )));
        var calculationService = new DataNeedCalculationServiceImpl(
                dataNeedsService,
                metadata,
                () -> List.of(
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.ELECTRICITY,
                                List.of(Granularity.PT15M, Granularity.P1D)
                        ),
                        new AllowMultipleDataNeedsRule()
                )
        );
        // When
        var res = calculationService.calculate("dnid");

        // Then
        var result = assertInstanceOf(ValidatedHistoricalDataDataNeedResult.class, res);
        assertEquals(List.of(Granularity.PT15M, Granularity.P1D), result.granularities());
    }

    @Test
    void testCalculateWithoutReferenceDateTime_givenValidatedHistoricalDataDataNeed_withSupportedGranularities_returnsValidatedHistoricalDataNeedResult() {
        // Given
        var value = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.P1D
        );
        var calculationService = new DataNeedCalculationServiceImpl(
                dataNeedsService,
                metadata,
                () -> List.of(new ValidatedHistoricalDataDataNeedRule(
                        EnergyType.ELECTRICITY,
                        List.of(Granularity.PT15M, Granularity.P1D)
                ))
        );

        // When
        var res = calculationService.calculate(value);

        // Then
        var result = assertInstanceOf(ValidatedHistoricalDataDataNeedResult.class, res);
        assertEquals(List.of(Granularity.PT15M, Granularity.P1D), result.granularities());
    }

    @Test
    void testCalculateWhereEnergyTimeframeStrategyAlwaysReturnsNull_givenValidatedHistoricalDataDataNeed_returnsUnsupportedDataNeed() {
        // Given
        var value = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.P1D
        );
        var calculationService = new DataNeedCalculationServiceImpl(
                dataNeedsService,
                metadata,
                new PermissionEndIsEnergyDataEndStrategy(),
                (dn, dt) -> null,
                () -> List.of(new ValidatedHistoricalDataDataNeedRule(
                        EnergyType.ELECTRICITY,
                        List.of(Granularity.PT15M, Granularity.P1D)
                ))
        );

        // When
        var res = calculationService.calculate(value);

        // Then
        assertInstanceOf(DataNeedNotSupportedResult.class, res);
    }

    @Test
    void givenValidatedHistoricalDataDataNeed_withUnsupportedGranularities_returnsUnsupportedDataNeedResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT5M,
                        Granularity.PT10M
                )));
        var calculationService = new DataNeedCalculationServiceImpl(
                dataNeedsService,
                metadata,
                () -> List.of(new ValidatedHistoricalDataDataNeedRule(
                        EnergyType.ELECTRICITY,
                        List.of(Granularity.PT15M, Granularity.P1D)
                ))
        );
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res).isInstanceOf(DataNeedNotSupportedResult.class);
    }

    @Test
    void givenValidatedHistoricalDataDataNeed_withUnsupportedEnergyType_returnsUnsupportedDataNeedResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                        EnergyType.NATURAL_GAS,
                        Granularity.PT5M,
                        Granularity.P1D
                )));
        var calculationService = new DataNeedCalculationServiceImpl(
                dataNeedsService,
                metadata,
                () -> List.of(new ValidatedHistoricalDataDataNeedRule(
                        EnergyType.ELECTRICITY,
                        List.of(Granularity.PT15M, Granularity.P1D)
                ))
        );
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res).isInstanceOf(DataNeedNotSupportedResult.class);
    }

    @Test
    void givenDataNeed_whereEnergyDataTimeframeStrategyThrows_returnsDataNeedNotSupportedResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(new AccountingPointDataNeed()));
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService,
                                                                    metadata,
                                                                    new PermissionEndIsEnergyDataEndStrategy(),
                                                                    (dn, referenceDateTime) -> {
                                                                        throw new UnsupportedDataNeedException("", "");
                                                                    },
                                                                    () -> List.of(new ValidatedHistoricalDataDataNeedRule(
                                                                            EnergyType.ELECTRICITY,
                                                                            List.of(Granularity.PT15M, Granularity.P1D)
                                                                    ))
        );
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res).isInstanceOf(DataNeedNotSupportedResult.class);
    }

    @Test
    void regionConnectorId_returnsId() {
        // Given
        var service = new DataNeedCalculationServiceImpl(dataNeedsService,
                                                         metadata,
                                                         List::of);

        // When
        var res = service.regionConnectorId();

        // Then
        assertEquals("id", res);
    }

    @Test
    void givenMultipleDataNeeds_whenCalculateAll_thenReturnsCorrectResults() {
        // Given
        var vhd = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT1H,
                Granularity.P1D
        );
        when(dataNeedsService.findById("vhd-dnid")).thenReturn(Optional.of(vhd));
        var ap = new AccountingPointDataNeed("name", "desc", "purpose", "https://localhost", true, null);
        when(dataNeedsService.findById("ap-dnid")).thenReturn(Optional.of(ap));
        List<DataNeedRule> dataNeedRules = List.of(
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, List.of(Granularity.PT1H)),
                new AccountingPointDataNeedRule(),
                new AllowMultipleDataNeedsRule()
        );
        var service = new DataNeedCalculationServiceImpl(dataNeedsService,
                                                         metadata,
                                                         new TestDataNeedRuleSet(dataNeedRules));

        // When
        var res = service.calculateAll(Set.of("vhd-dnid", "ap-dnid"));

        // Then
        assertThat(res)
                .asInstanceOf(type(CalculationResult.class))
                .extracting(CalculationResult::result)
                .asInstanceOf(map(String.class, DataNeedCalculationResult.class))
                .hasEntrySatisfying(vhd.id(),
                                    dn -> assertThat(dn).isInstanceOf(ValidatedHistoricalDataDataNeedResult.class))
                .hasEntrySatisfying(ap.id(),
                                    dn -> assertThat(dn).isInstanceOf(AccountingPointDataNeedResult.class))
                .hasSize(2);
    }

    @Test
    void givenSingleDataNeed_whenCalculateAll_thenReturnsCorrectResults() {
        // Given
        var vhd = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT1H,
                Granularity.P1D
        );
        when(dataNeedsService.findById("vhd-dnid")).thenReturn(Optional.of(vhd));
        List<DataNeedRule> dataNeedRules = List.of(
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, List.of(Granularity.PT1H)),
                new AccountingPointDataNeedRule()
        );
        var service = new DataNeedCalculationServiceImpl(dataNeedsService,
                                                         metadata,
                                                         new TestDataNeedRuleSet(dataNeedRules));

        // When
        var res = service.calculateAll(Set.of("vhd-dnid"));

        // Then
        assertThat(res)
                .asInstanceOf(type(CalculationResult.class))
                .extracting(CalculationResult::result)
                .asInstanceOf(map(String.class, DataNeedCalculationResult.class))
                .hasEntrySatisfying("vhd-dnid",
                                    dn -> assertThat(dn).isInstanceOf(ValidatedHistoricalDataDataNeedResult.class))
                .hasSize(1);
    }

    @Test
    void givenMultipleAccountingPointDataNeeds_whenCalculateAll_thenReturnsRepeatedDataNeedResult() {
        // Given
        var ap = new AccountingPointDataNeed("name", "desc", "purpose", "https://localhost", true, null);
        when(dataNeedsService.findById("ap-dnid-1")).thenReturn(Optional.of(ap));
        when(dataNeedsService.findById("ap-dnid-2")).thenReturn(Optional.of(ap));
        List<DataNeedRule> dataNeedRules = List.of(
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, List.of(Granularity.PT1H)),
                new AccountingPointDataNeedRule(),
                new AllowMultipleDataNeedsRule()
        );
        var service = new DataNeedCalculationServiceImpl(dataNeedsService,
                                                         metadata,
                                                         new TestDataNeedRuleSet(dataNeedRules));

        // When
        var res = service.calculateAll(Set.of("ap-dnid-1", "ap-dnid-2"));

        // Then
        assertThat(res).isInstanceOf(InvalidDataNeedCombination.class);
    }

    @Test
    void givenMultipleValidatedHistoricalDataDataNeedsWithSameEnergyType_whenCalculateAll_thenReturnsRepeatedDataNeedResult() {
        // Given
        var vhd = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT1H,
                Granularity.P1D
        );
        when(dataNeedsService.findById("vhd-dnid-1")).thenReturn(Optional.of(vhd));
        when(dataNeedsService.findById("vhd-dnid-2")).thenReturn(Optional.of(vhd));
        List<DataNeedRule> dataNeedRules = List.of(
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, List.of(Granularity.PT1H)),
                new AccountingPointDataNeedRule(),
                new AllowMultipleDataNeedsRule()
        );
        var service = new DataNeedCalculationServiceImpl(dataNeedsService,
                                                         metadata,
                                                         new TestDataNeedRuleSet(dataNeedRules));

        // When
        var res = service.calculateAll(Set.of("vhd-dnid-1", "vhd-dnid-2"));

        // Then
        assertThat(res).isInstanceOf(InvalidDataNeedCombination.class);
    }


    @Test
    void givenMixedDataDataNeeds_whenCalculateAll_thenReturnsInvalidMixedDataNeedResult() {
        // Given
        var vhd = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT1H,
                Granularity.P1D
        );
        when(dataNeedsService.findById("vhd-dnid")).thenReturn(Optional.of(vhd));
        when(dataNeedsService.findById("aiida-dnid")).thenReturn(Optional.of(new InboundAiidaDataNeed()));
        List<DataNeedRule> dataNeedRules = List.of(
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, List.of(Granularity.PT1H)),
                new InboundAiidaDataNeedRule(),
                new AllowMultipleDataNeedsRule()
        );
        var service = new DataNeedCalculationServiceImpl(dataNeedsService, metadata,
                                                         new TestDataNeedRuleSet(dataNeedRules));

        // When
        var res = service.calculateAll(Set.of("vhd-dnid", "aiida-dnid"));

        // Then
        assertThat(res).isInstanceOf(InvalidDataNeedCombination.class);
    }


    @Test
    void givenUnsupportedMultipleDataNeeds_whenCalculateAll_thenReturnsInvalidDataNeedCombination() {
        // Given
        var service = new DataNeedCalculationServiceImpl(dataNeedsService, metadata,
                                                         new TestDataNeedRuleSet(List.of()));

        // When
        var res = service.calculateAll(Set.of("vhd-dnid", "aiida-dnid"));

        // Then
        assertThat(res).isInstanceOf(InvalidDataNeedCombination.class);
    }

    @Test
    void givenUnknownDataNeed_whenCalculateAll_thenReturnsDataNeedNotFoundForUnknownDataNeed() {
        // Given
        when(dataNeedsService.findById("vhd-dnid")).thenReturn(Optional.empty());
        var ap = new AccountingPointDataNeed("name", "desc", "purpose", "https://localhost", true, null);
        when(dataNeedsService.findById("ap-dnid")).thenReturn(Optional.of(ap));
        List<DataNeedRule> dataNeedRules = List.of(
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, List.of(Granularity.PT1H)),
                new AccountingPointDataNeedRule(),
                new AllowMultipleDataNeedsRule()
        );
        var service = new DataNeedCalculationServiceImpl(dataNeedsService,
                                                         metadata,
                                                         new TestDataNeedRuleSet(dataNeedRules));

        // When
        var res = service.calculateAll(Set.of("vhd-dnid", "ap-dnid"));

        // Then
        assertThat(res)
                .asInstanceOf(type(CalculationResult.class))
                .extracting(CalculationResult::result)
                .asInstanceOf(map(String.class, DataNeedCalculationResult.class))
                .hasEntrySatisfying("vhd-dnid", dn -> assertThat(dn).isInstanceOf(DataNeedNotFoundResult.class))
                .hasEntrySatisfying(ap.id(),
                                    dn -> assertThat(dn).isInstanceOf(AccountingPointDataNeedResult.class))
                .hasSize(2);
    }

    private static Stream<Arguments> regionConnectorFilterConfigurations() {
        return Stream.of(
                Arguments.of(new RegionConnectorFilter(RegionConnectorFilter.Type.ALLOWLIST, List.of("id")),
                             AccountingPointDataNeedResult.class),
                Arguments.of(new RegionConnectorFilter(RegionConnectorFilter.Type.ALLOWLIST, List.of("notInList")),
                             DataNeedNotSupportedResult.class),
                Arguments.of(new RegionConnectorFilter(RegionConnectorFilter.Type.BLOCKLIST, List.of("id")),
                             DataNeedNotSupportedResult.class),
                Arguments.of(new RegionConnectorFilter(RegionConnectorFilter.Type.BLOCKLIST, List.of("notInList")),
                             AccountingPointDataNeedResult.class)
        );
    }

    private record RegionConnectorMetadataImpl(String id,
                                               String countryCode,
                                               long coveredMeteringPoints,
                                               Period earliestStart,
                                               Period latestEnd,
                                               List<Granularity> supportedGranularities,
                                               List<Class<? extends DataNeedInterface>> supportedDataNeeds,
                                               ZoneId timeZone,
                                               List<EnergyType> supportedEnergyTypes) implements RegionConnectorMetadata {
    }

    private record TestDataNeedRuleSet(List<DataNeedRule> dataNeedRules) implements DataNeedRuleSet {}
}