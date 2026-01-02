package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.RegionConnectorFilter;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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
    @Mock
    private OutboundAiidaDataNeed aiidaDataNeed;

    @Test
    void givenUnknownDataNeedId_returnsDataNeedNotFoundResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.empty());
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotFoundResult.class));
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
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);

        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(expected));
    }

    @Test
    void givenDisabledDataNeed_returnsDataNeedNotSupportedResult() {
        // Given
        when(accountingPointDataNeed.isEnabled()).thenReturn(false);
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(accountingPointDataNeed));
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotSupportedResult.class));
    }

    @Test
    void givenUnsupportedDataNeed_returnsDataNeedNotSupportedResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(aiidaDataNeed));
        when(aiidaDataNeed.isEnabled()).thenReturn(true);
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotSupportedResult.class));
        assertEquals(
                "Data need type \"OutboundAiidaDataNeed\" not supported, region connector supports data needs of types ValidatedHistoricalDataDataNeed, AccountingPointDataNeed",
                ((DataNeedNotSupportedResult) res).message()
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
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);
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
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);

        // When
        var res = calculationService.calculate(value);

        // Then
        var result = assertInstanceOf(ValidatedHistoricalDataDataNeedResult.class, res);
        assertEquals(List.of(Granularity.PT15M, Granularity.P1D), result.granularities());
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
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotSupportedResult.class));
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
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotSupportedResult.class));
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
                                                                    }
        );
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotSupportedResult.class));
    }

    @Test
    void regionConnectorId_returnsId() {
        // Given
        var service = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);

        // When
        var res = service.regionConnectorId();

        // Then
        assertEquals("id", res);
    }

    @Test
    void givenMultipleDataNeeds_whenCalculateAll_thenReturnsCorrectResults() {
        // Given
        when(dataNeedsService.findById("vhd-dnid"))
                .thenReturn(Optional.of(
                        new ValidatedHistoricalDataDataNeed(
                                new RelativeDuration(null, null, null),
                                EnergyType.ELECTRICITY,
                                Granularity.PT1H,
                                Granularity.P1D
                        )
                ));
        when(dataNeedsService.findById("ap-dnid"))
                .thenReturn(Optional.of(
                        new AccountingPointDataNeed("name", "desc", "purpose", "https://localhost", true, null)
                ));
        var service = new DataNeedCalculationServiceImpl(dataNeedsService, metadata);

        // When
        var res = service.calculateAll(Set.of("vhd-dnid", "ap-dnid"));

        // Then
        assertThat(res, allOf(
                hasEntry(equalTo("vhd-dnid"), instanceOf(ValidatedHistoricalDataDataNeedResult.class)),
                hasEntry(equalTo("ap-dnid"), instanceOf(AccountingPointDataNeedResult.class))
        ));
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
}