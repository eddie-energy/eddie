package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedCalculationServiceImplTest {
    @Spy
    private final RegionConnectorMetadata regionConnectorMetadata = new RegionConnectorMetadataImpl(
            "id",
            "AT",
            1,
            Period.ofDays(-10),
            Period.ofDays(10),
            List.of(Granularity.PT15M, Granularity.P1D),
            ZoneOffset.UTC
    );
    @Mock
    private ValidatedHistoricalDataDataNeed vhdDataNeed;
    @Mock
    private RelativeDuration duration;
    @Mock
    private GenericAiidaDataNeed aiidaDataNeed;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;
    private DataNeedCalculationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DataNeedCalculationServiceImpl(
                List.of(ValidatedHistoricalDataDataNeed.class, AccountingPointDataNeed.class),
                regionConnectorMetadata
        );
    }

    @Test
    void testCalculate_returnsCorrect_forFutureVHDDataNeed() {
        // Given
        when(vhdDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ZERO));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(5)));
        when(vhdDataNeed.minGranularity())
                .thenReturn(Granularity.PT5M);
        when(vhdDataNeed.maxGranularity())
                .thenReturn(Granularity.P1Y);
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(now, now.plusDays(5));

        // When
        var res = service.calculate(vhdDataNeed);

        // Then
        assertAll(
                () -> assertTrue(res.supportsDataNeed()),
                () -> assertEquals(List.of(Granularity.PT15M, Granularity.P1D), res.granularities()),
                () -> assertEquals(timeframe, res.permissionTimeframe()),
                () -> assertEquals(timeframe, res.energyDataTimeframe())
        );
    }

    @Test
    void testCalculate_returnsCorrect_forPastVHDDataNeed() {
        // Given
        when(vhdDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ofDays(-5)));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(-1)));
        when(vhdDataNeed.minGranularity())
                .thenReturn(Granularity.PT5M);
        when(vhdDataNeed.maxGranularity())
                .thenReturn(Granularity.P1Y);
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(now.minusDays(5), now.minusDays(1));

        // When
        var res = service.calculate(vhdDataNeed);

        // Then
        assertAll(
                () -> assertTrue(res.supportsDataNeed()),
                () -> assertEquals(List.of(Granularity.PT15M, Granularity.P1D), res.granularities()),
                () -> assertEquals(new Timeframe(now, now), res.permissionTimeframe()),
                () -> assertEquals(timeframe, res.energyDataTimeframe())
        );
    }

    @Test
    void testCalculate_returnsCorrect_forInvalidTimeframedVHDDataNeed() {
        // Given
        when(vhdDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ofDays(-100)));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(-1)));
        when(vhdDataNeed.minGranularity())
                .thenReturn(Granularity.PT5M);
        when(vhdDataNeed.maxGranularity())
                .thenReturn(Granularity.P1Y);

        // When
        var res = service.calculate(vhdDataNeed);

        // Then
        assertFalse(res.supportsDataNeed());
    }

    @Test
    void testCalculate_returnsEmptyCalculationOnUnsupportedDataNeed() {
        // Given
        // When
        var res = service.calculate(aiidaDataNeed);

        // Then
        assertAll(
                () -> assertFalse(res.supportsDataNeed()),
                () -> assertNull(res.energyDataTimeframe()),
                () -> assertNull(res.permissionTimeframe()),
                () -> assertNull(res.granularities())
        );
    }

    @Test
    void testCalculate_returnsCalculationOnNonEnergyDataNeed() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(now, now);

        // When
        var res = service.calculate(accountingPointDataNeed);

        // Then
        assertAll(
                () -> assertTrue(res.supportsDataNeed()),
                () -> assertNull(res.energyDataTimeframe()),
                () -> assertEquals(timeframe, res.permissionTimeframe()),
                () -> assertEquals(List.of(), res.granularities())
        );
    }

    @Test
    void testCalculate_returnsUnsupported_withFailingAdditionalCheck() {
        // Given
        when(vhdDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ofDays(-5)));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(-1)));
        when(vhdDataNeed.minGranularity())
                .thenReturn(Granularity.PT5M);
        when(vhdDataNeed.maxGranularity())
                .thenReturn(Granularity.P1Y);
        service = new DataNeedCalculationServiceImpl(
                List.of(ValidatedHistoricalDataDataNeed.class, AccountingPointDataNeed.class),
                regionConnectorMetadata,
                new PermissionEndIsEnergyDataEndStrategy(ZoneOffset.UTC),
                List.of(
                        dataNeed -> true,
                        dataNeed -> false
                )
        );
        // When
        var res = service.calculate(vhdDataNeed);

        // Then
        assertFalse(res.supportsDataNeed());
    }

    @Test
    void testCalculate_returnsSupported_withSuccessfullAdditionalCheck() {
        // Given
        when(vhdDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ofDays(-5)));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(-1)));
        when(vhdDataNeed.minGranularity())
                .thenReturn(Granularity.PT5M);
        when(vhdDataNeed.maxGranularity())
                .thenReturn(Granularity.P1Y);
        service = new DataNeedCalculationServiceImpl(
                List.of(ValidatedHistoricalDataDataNeed.class, AccountingPointDataNeed.class),
                regionConnectorMetadata,
                new PermissionEndIsEnergyDataEndStrategy(ZoneOffset.UTC),
                List.of(
                        dataNeed -> true
                )
        );
        // When
        var res = service.calculate(vhdDataNeed);

        // Then
        assertTrue(res.supportsDataNeed());
    }

    @Test
    void testCalculate_returnsNotSupported() {
        // Given
        // When
        var res = service.calculate(aiidaDataNeed);

        // Then
        assertFalse(res.supportsDataNeed());
    }

    @Test
    void testRegionConnectorId_returnsId() {
        // Given
        when(regionConnectorMetadata.id())
                .thenReturn("id");
        // When
        var res = service.regionConnectorId();

        // Then
        assertEquals("id", res);
    }

    private record RegionConnectorMetadataImpl(String id,
                                               String countryCode,
                                               long coveredMeteringPoints,
                                               Period earliestStart,
                                               Period latestEnd,
                                               List<Granularity> supportedGranularities,
                                               ZoneId timeZone) implements RegionConnectorMetadata {}
}