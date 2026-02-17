// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.data.needs.MultipleDataNeedCalculationResult.CalculationResult;
import energy.eddie.api.agnostic.data.needs.MultipleDataNeedCalculationResult.InvalidDataNeedCombination;
import energy.eddie.core.dtos.MultipleDataNeedsOrErrorResult.MultipleDataNeeds;
import energy.eddie.core.dtos.MultipleDataNeedsOrErrorResult.MultipleDataNeedsError;
import energy.eddie.dataneeds.exceptions.DataNeedDisabledException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
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
    void testCalulcateFor_returnsCalculation_withAiidaData() throws UnknownRegionConnectorException, DataNeedNotFoundException {
        var timeframe = new Timeframe(LocalDate.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC));
        when(service.calculate("dnid")).thenReturn(new AiidaDataNeedResult(true, timeframe));

        // When
        var res = router.calculateFor("at-eda", "dnid");

        // Then
        assertAll(
                () -> assertTrue(res.supportsDataNeed()),
                () -> assertEquals(timeframe, res.energyDataTimeframe()),
                () -> assertNull(res.permissionTimeframe()),
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

    @Test
    void testCalculateForMultipleDataNeeds_withUnknownRegionConnector_throwsUnknownRegionConnectorException() {
        // Given
        // When
        // Then
        assertThrows(UnknownRegionConnectorException.class, () -> router.calculateFor("unknown", Set.of("dnid")));
    }

    @Test
    void testCalculateForMultipleDataNeeds_withInvalidCombinationOfDataNeeds_returnsInvalidDataNeedCombination() throws UnknownRegionConnectorException {
        // Given
        when(service.calculateAll(Set.of("dnid")))
                .thenReturn(new InvalidDataNeedCombination(Set.of("dnid"), "Invalid combination"));
        // When
        var res = router.calculateFor("at-eda", Set.of("dnid"));

        // Then
        assertThat(res)
                .asInstanceOf(type(MultipleDataNeedsError.class))
                .satisfies(error -> {
                    assertThat(error.offendingDataNeedIds()).singleElement()
                                                            .isEqualTo("dnid");
                    assertThat(error.message()).isEqualTo("Invalid combination");
                });
    }

    @Test
    void testCalculateForMultipleDataNeeds_withValidCombinationOfDataNeeds_returnsMultipleDataNeeds() throws UnknownRegionConnectorException {
        // Given
        when(service.calculateAll(Set.of("dnid")))
                .thenReturn(new CalculationResult(Map.of("dnid", new DataNeedNotSupportedResult("Error"))));
        // When
        var res = router.calculateFor("at-eda", Set.of("dnid"));

        // Then
        assertThat(res)
                .asInstanceOf(type(MultipleDataNeeds.class))
                .extracting(MultipleDataNeeds::result)
                .asInstanceOf(map(String.class, DataNeedCalculation.class))
                .containsExactlyEntriesOf(Map.of("dnid", new DataNeedCalculation(false, "Error")));
    }

    @Test
    void testFindRegionConnectorsSupportingDataNeedsForMultipleDataNeedsAndAllRegionConnectors_withValidCombinationOfDataNeeds_returnsSupportedRegionConnectors() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(service.calculateAll(Set.of("dnid")))
                .thenReturn(new CalculationResult(
                        Map.of("dnid", new AccountingPointDataNeedResult(new Timeframe(now, now)))
                ));
        // When
        var res = router.findRegionConnectorsSupportingDataNeeds(Set.of("dnid"));

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo("at-eda");
    }

    @ParameterizedTest
    @MethodSource
    void testFindRegionConnectorsSupportingDataNeedsMultipleDataNeedsAndAllRegionConnectors_withInvalidCombinationOfDataNeeds_returnsSupportedRegionConnectors(
            MultipleDataNeedCalculationResult result
    ) {
        // Given
        when(service.calculateAll(Set.of("dnid"))).thenReturn(result);
        // When
        var res = router.findRegionConnectorsSupportingDataNeeds(Set.of("dnid"));

        // Then
        assertThat(res).isEmpty();
    }

    private static Stream<Arguments> testFindRegionConnectorsSupportingDataNeedsMultipleDataNeedsAndAllRegionConnectors_withInvalidCombinationOfDataNeeds_returnsSupportedRegionConnectors() {
        return Stream.of(
                Arguments.of(new CalculationResult(Map.of("dnid", new DataNeedNotSupportedResult("Error")))),
                Arguments.of(new InvalidDataNeedCombination(Set.of("dnid"), "error"))
        );
    }
}