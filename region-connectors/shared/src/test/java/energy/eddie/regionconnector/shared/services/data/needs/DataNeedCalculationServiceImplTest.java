package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.RegionConnectorFilter;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.DefaultEnergyDataTimeframeStrategy;
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
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedCalculationServiceImplTest {
    private final List<Class<? extends DataNeed>> supportedDataNeeds = List.of(ValidatedHistoricalDataDataNeed.class,
                                                                               AccountingPointDataNeed.class);
    private final RegionConnectorMetadata metadata = new RegionConnectorMetadataImpl(
            "id",
            "AT",
            1,
            Period.ofDays(-10),
            Period.ofDays(10),
            List.of(Granularity.PT15M, Granularity.P1D),
            ZoneOffset.UTC,
            List.of(ValidatedHistoricalDataDataNeed.class, AccountingPointDataNeed.class)
    );
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;
    @Mock
    private GenericAiidaDataNeed genericAiidaDataNeed;

    @Test
    void givenUnknownDataNeedId_returnsDataNeedNotFoundResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.empty());
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, supportedDataNeeds, metadata);
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
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, supportedDataNeeds, metadata);

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
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, supportedDataNeeds, metadata);
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotSupportedResult.class));
    }

    @Test
    void givenUnsupportedDataNeed_returnsDataNeedNotSupportedResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(genericAiidaDataNeed));
        when(genericAiidaDataNeed.isEnabled()).thenReturn(true);
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, supportedDataNeeds, metadata);
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotSupportedResult.class));
        assertEquals(
                "Data need type \"GenericAiidaDataNeed\" not supported, region connector supports data needs of types ValidatedHistoricalDataDataNeed, AccountingPointDataNeed",
                ((DataNeedNotSupportedResult) res).message()
        );
    }

    @Test
    void givenDataNeed_withAdditionalChecksFailing_returnsDataNeedNotSupportedResult() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(new AccountingPointDataNeed()));
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService,
                                                                    supportedDataNeeds,
                                                                    metadata,
                                                                    new PermissionEndIsEnergyDataEndStrategy(ZoneOffset.UTC),
                                                                    new DefaultEnergyDataTimeframeStrategy(metadata),
                                                                    List.of(
                                                                            in -> true,
                                                                            in -> false
                                                                    ));
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotSupportedResult.class));
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
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, supportedDataNeeds, metadata);
        // When
        var res = calculationService.calculate("dnid");

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
        var calculationService = new DataNeedCalculationServiceImpl(dataNeedsService, supportedDataNeeds, metadata);
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
                                                                    supportedDataNeeds,
                                                                    metadata,
                                                                    new PermissionEndIsEnergyDataEndStrategy(ZoneOffset.UTC),
                                                                    dn -> {
                                                                        throw new UnsupportedDataNeedException("",
                                                                                                               "",
                                                                                                               "");
                                                                    },
                                                                    List.of());
        // When
        var res = calculationService.calculate("dnid");

        // Then
        assertThat(res, instanceOf(DataNeedNotSupportedResult.class));
    }

    @Test
    void regionConnectorId_returnsId() {
        // Given
        var service = new DataNeedCalculationServiceImpl(dataNeedsService, supportedDataNeeds, metadata);

        // When
        var res = service.regionConnectorId();

        // Then
        assertEquals("id", res);
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
                                               ZoneId timeZone,
                                               List<Class<? extends DataNeedInterface>> supportedDataNeeds) implements RegionConnectorMetadata {
    }
}