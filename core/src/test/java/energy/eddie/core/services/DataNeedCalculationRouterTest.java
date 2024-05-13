package energy.eddie.core.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.utils.Pair;
import energy.eddie.core.services.data.need.Timeframe;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedCalculationRouterTest {
    @Mock
    private ValidatedHistoricalDataDataNeed dataNeed;
    @Mock
    private DataNeedCalculationService<DataNeed> service;
    @Mock
    private DataNeedsService dataNeedsService;
    @InjectMocks
    private DataNeedCalculationRouter router;

    @BeforeEach
    void setUp() {
        when(service.regionConnectorId())
                .thenReturn("at-eda");
        router.register(service);
    }

    @Test
    void testCalculate_throwsOnUnknownRegionConnector() {
        // Given
        // When
        // Then
        assertThrows(UnknownRegionConnectorException.class, () -> router.calculateFor("es-datadis", "dnid"));
    }

    @Test
    void testCalculate_throwsOnUnknownDataNeed() {
        // Given
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.empty());
        // When
        // Then
        assertThrows(DataNeedNotFoundException.class, () -> router.calculateFor("at-eda", "unknown"));
    }

    @Test
    void testCalculate_returnsEmptyCalculationOnUnsupportedDataNeed() throws UnknownRegionConnectorException, DataNeedNotFoundException {
        // Given
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(dataNeed));
        when(service.supportsDataNeed(dataNeed))
                .thenReturn(false);
        // When
        var res = router.calculateFor("at-eda", "dnid");

        // Then
        assertAll(
                () -> assertFalse(res.supportsDataNeed()),
                () -> assertNull(res.energyDataTimeframe()),
                () -> assertNull(res.permissionTimeframe()),
                () -> assertNull(res.granularities())
        );
    }

    @Test
    void testCalculate_returnsCalculationOnDataNeed() throws UnknownRegionConnectorException, DataNeedNotFoundException {
        // Given
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(dataNeed));
        when(service.supportsDataNeed(dataNeed))
                .thenReturn(true);
        when(service.supportedGranularities(dataNeed))
                .thenReturn(List.of(Granularity.PT15M));
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Pair<>(now, now);
        when(service.calculatePermissionStartAndEndDate(dataNeed))
                .thenReturn(timeframe);
        when(service.calculateEnergyDataStartAndEndDate(dataNeed))
                .thenReturn(timeframe);
        // When
        var res = router.calculateFor("at-eda", "dnid");

        // Then
        assertAll(
                () -> assertTrue(res.supportsDataNeed()),
                () -> assertEquals(new Timeframe(now, now), res.energyDataTimeframe()),
                () -> assertEquals(new Timeframe(now, now), res.permissionTimeframe()),
                () -> assertEquals(List.of(Granularity.PT15M), res.granularities())
        );
    }

    @Test
    void testCalculate_returnsCalculationOnNonEnergyDataNeed() throws UnknownRegionConnectorException, DataNeedNotFoundException {
        // Given
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(dataNeed));
        when(service.supportsDataNeed(dataNeed))
                .thenReturn(true);
        when(service.supportedGranularities(dataNeed))
                .thenReturn(List.of(Granularity.PT15M));
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Pair<>(now, now);
        when(service.calculatePermissionStartAndEndDate(dataNeed))
                .thenReturn(timeframe);
        when(service.calculateEnergyDataStartAndEndDate(dataNeed))
                .thenReturn(null);
        // When
        var res = router.calculateFor("at-eda", "dnid");

        // Then
        assertAll(
                () -> assertTrue(res.supportsDataNeed()),
                () -> assertNull(res.energyDataTimeframe()),
                () -> assertEquals(new Timeframe(now, now), res.permissionTimeframe()),
                () -> assertEquals(List.of(Granularity.PT15M), res.granularities())
        );
    }
}