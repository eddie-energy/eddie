// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.dataneeds.exceptions.DataNeedDisabledException;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    void testCalculateFor_throwsOnUnknownRegionConnector() {
        // Given
        // When
        // Then
        assertThrows(UnknownRegionConnectorException.class, () -> router.calculateFor("es-datadis", "dnid"));
    }

    @Test
    void testCalculateFor_throwsOnUnknownDataNeed() {
        // Given
        when(service.calculate("unknown")).thenReturn(new DataNeedNotFoundResult());
        // When
        // Then
        assertThrows(DataNeedNotFoundException.class, () -> router.calculateFor("at-eda", "unknown"));
    }

    @Test
    void testCalculateFor_returnsCalculation() throws UnknownRegionConnectorException, DataNeedNotFoundException {
        var timeframe = new Timeframe(LocalDate.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC));
        when(service.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT15M),
                        timeframe,
                        timeframe
                ));

        // When
        var res = router.calculateFor("at-eda", "dnid");

        // Then
        assertAll(
                () -> assertTrue(res.supportsDataNeed()),
                () -> assertEquals(timeframe, res.energyDataTimeframe()),
                () -> assertEquals(timeframe, res.permissionTimeframe()),
                () -> assertEquals(List.of(Granularity.PT15M), res.granularities())
        );
    }

    @Test
    void testCalculateFor_returnsCalculation_withAccountingPointData() throws UnknownRegionConnectorException, DataNeedNotFoundException {
        var timeframe = new Timeframe(LocalDate.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC));
        when(service.calculate("dnid")).thenReturn(new AccountingPointDataNeedResult(timeframe));

        // When
        var res = router.calculateFor("at-eda", "dnid");

        // Then
        assertAll(
                () -> assertTrue(res.supportsDataNeed()),
                () -> assertNull(res.energyDataTimeframe()),
                () -> assertEquals(timeframe, res.permissionTimeframe()),
                () -> assertNull(res.granularities())
        );
    }

    @Test
    void testCalculateFor_returnsCalculation_withUnsupportedDataNeed() throws UnknownRegionConnectorException, DataNeedNotFoundException {
        when(service.calculate("dnid")).thenReturn(new DataNeedNotSupportedResult("ERROR"));

        // When
        var res = router.calculateFor("at-eda", "dnid");

        // Then
        assertAll(
                () -> assertFalse(res.supportsDataNeed()),
                () -> assertNull(res.energyDataTimeframe()),
                () -> assertNull(res.permissionTimeframe()),
                () -> assertNull(res.granularities()),
                () -> assertEquals("ERROR", res.unsupportedDataNeedMessage())
        );
    }

    @Test
    void testCalculate_throwsOnUnknownDataNeed() {
        // Given
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.empty());
        // When
        // Then
        assertThrows(DataNeedNotFoundException.class, () -> router.calculate("unknown"));
    }

    @Test
    void testCalculate_returnsCalculation() throws DataNeedNotFoundException, DataNeedDisabledException {
        when(dataNeed.isEnabled()).thenReturn(true);
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(dataNeed));
        var timeframe = new Timeframe(LocalDate.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC));
        when(service.calculate(dataNeed))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT15M),
                        timeframe,
                        timeframe
                ));
        // When
        var res = router.calculate("dnid");

        // Then
        assertThat(res).containsOnlyKeys("at-eda");
    }

    @Test
    void testCalculate_throwsDataNeedDisabledException() {
        when(dataNeed.isEnabled()).thenReturn(false);
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(dataNeed));
        // When
        // Then
        assertThrows(DataNeedDisabledException.class, () -> router.calculate("dnid"));
    }
}