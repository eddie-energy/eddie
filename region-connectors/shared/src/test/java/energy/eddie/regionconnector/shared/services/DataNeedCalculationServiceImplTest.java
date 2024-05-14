package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedCalculationServiceImplTest {
    @Mock
    private ValidatedHistoricalDataDataNeed vhdDataNeed;
    @Mock
    private RelativeDuration duration;
    @Mock
    private GenericAiidaDataNeed aiidaDataNeed;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;
    @Mock
    private RegionConnectorMetadata regionConnectorMetadata;
    private DataNeedCalculationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DataNeedCalculationServiceImpl(
                List.of(Granularity.PT5M, Granularity.PT15M, Granularity.P1D),
                List.of(ValidatedHistoricalDataDataNeed.class),
                Period.ofDays(-10),
                Period.ofDays(10),
                regionConnectorMetadata
        );
    }

    @Test
    void testSupportedGranularities_returnsCorrectGranularities() {
        // Given
        when(vhdDataNeed.minGranularity())
                .thenReturn(Granularity.PT15M);
        when(vhdDataNeed.maxGranularity())
                .thenReturn(Granularity.P1Y);

        // When
        var res = service.supportedGranularities(vhdDataNeed);

        // Then
        assertEquals(List.of(Granularity.PT15M, Granularity.P1D), res);
    }

    @Test
    void testSupportedGranularities_ofAccountingPointDataNeed_returnsEmptyList() {
        // Given
        // When
        var res = service.supportedGranularities(accountingPointDataNeed);

        // Then
        assertTrue(res.isEmpty());
    }

    @Test
    void testSupportedDataNeed_returnsTrue() {
        // Given
        // When
        var res = service.supportsDataNeed(vhdDataNeed);

        // Then
        assertTrue(res);
    }

    @Test
    void testSupportedDataNeed_returnsFalse() {
        // Given
        // When
        var res = service.supportsDataNeed(aiidaDataNeed);

        // Then
        assertFalse(res);
    }

    @Test
    void testCalculatePermissionStartAndEnd_ofAccountingPointDataNeed_returnsCorrect() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        // When
        var res = service.calculatePermissionStartAndEndDate(accountingPointDataNeed);

        // Then
        assertAll(
                () -> assertEquals(now, res.key()),
                () -> assertEquals(now, res.value())
        );
    }

    @Test
    void testCalculatePermissionStartAndEnd_ofFutureDataNeed_returnsCorrect() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(vhdDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ofDays(-100)));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(5)));
        // When
        var res = service.calculatePermissionStartAndEndDate(vhdDataNeed);

        // Then
        assertAll(
                () -> assertEquals(now, res.key()),
                () -> assertEquals(now.plusDays(5), res.value())
        );
    }

    @Test
    void testCalculatePermissionStartAndEnd_ofHistoricDataNeed_returnsCorrect() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(vhdDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ofDays(-100)));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(-5)));
        // When
        var res = service.calculatePermissionStartAndEndDate(vhdDataNeed);

        // Then
        assertAll(
                () -> assertEquals(now, res.key()),
                () -> assertEquals(now, res.value())
        );
    }

    @Test
    void testCalculateEnergyStartAndEnd_ofHistoricDataNeed_returnsCorrect() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(vhdDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ofDays(-100)));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(-5)));
        // When
        var res = service.calculateEnergyDataStartAndEndDate(vhdDataNeed);

        // Then
        assertAll(
                () -> assertNotNull(res),
                () -> assertEquals(now.minusDays(100), res.key()),
                () -> assertEquals(now.minusDays(5), res.value())
        );
    }

    @Test
    void testCalculateEnergyStartAndEnd_ofFutureDataNeed_returnsCorrect() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(vhdDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ofDays(5)));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(10)));
        // When
        var res = service.calculateEnergyDataStartAndEndDate(vhdDataNeed);

        // Then
        assertAll(
                () -> assertNotNull(res),
                () -> assertEquals(now.plusDays(5), res.key()),
                () -> assertEquals(now.plusDays(10), res.value())
        );
    }

    @Test
    void testCalculateEnergyStartAndEnd_ofAccountPointDataNeed_returnsCorrect() {
        // Given
        // When
        var res = service.calculateEnergyDataStartAndEndDate(accountingPointDataNeed);

        // Then
        assertNull(res);
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
}