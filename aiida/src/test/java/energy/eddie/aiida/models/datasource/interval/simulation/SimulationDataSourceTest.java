// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.interval.simulation;

import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.errors.datasource.InvalidSimulationRangeException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimulationDataSourceTest {
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");

    @Test
    void givenDtoWithoutBounds_appliesDefaults() {
        // given, when
        var dataSource = new SimulationDataSource(dto(null, null, null, null), USER_ID);

        // then
        assertEquals(SimulationDataSource.DEFAULT_MIN_CONSUMPTION_WATT, dataSource.minConsumptionWatt());
        assertEquals(SimulationDataSource.DEFAULT_MAX_CONSUMPTION_WATT, dataSource.maxConsumptionWatt());
        assertEquals(SimulationDataSource.DEFAULT_MIN_PRODUCTION_WATT, dataSource.minProductionWatt());
        assertEquals(SimulationDataSource.DEFAULT_MAX_PRODUCTION_WATT, dataSource.maxProductionWatt());
    }

    @Test
    void givenDtoWithBounds_appliesBounds() {
        // given, when
        var dataSource = new SimulationDataSource(dto(300L, 600L, 100L, 200L), USER_ID);

        // then
        assertEquals(300, dataSource.minConsumptionWatt());
        assertEquals(600, dataSource.maxConsumptionWatt());
        assertEquals(100, dataSource.minProductionWatt());
        assertEquals(200, dataSource.maxProductionWatt());
    }

    @Test
    void givenEqualBounds_isValid() {
        // given, when
        var dataSource = new SimulationDataSource(dto(500L, 500L, 0L, 0L), USER_ID);

        // then
        assertEquals(500, dataSource.minConsumptionWatt());
        assertEquals(500, dataSource.maxConsumptionWatt());
        assertEquals(0, dataSource.minProductionWatt());
        assertEquals(0, dataSource.maxProductionWatt());
    }

    @Test
    void givenMinConsumptionGreaterThanMax_throwsInvalidSimulationRangeException() {
        // given
        var dto = dto(2000L, 1000L, null, null);

        // when, then
        var exception = assertThrows(InvalidSimulationRangeException.class,
                                     () -> new SimulationDataSource(dto, USER_ID));
        assertEquals("The minimum consumption (2000 W) must not be greater than the maximum consumption (1000 W).",
                     exception.getMessage());
    }

    @Test
    void givenMinProductionGreaterThanMax_throwsInvalidSimulationRangeException() {
        // given
        var dto = dto(null, null, 3000L, 2000L);

        // when, then
        assertThrows(InvalidSimulationRangeException.class, () -> new SimulationDataSource(dto, USER_ID));
    }

    @Test
    void givenMinConsumptionGreaterThanDefaultedMax_throwsInvalidSimulationRangeException() {
        // given
        var dto = dto(5000L, null, null, null);

        // when, then
        assertThrows(InvalidSimulationRangeException.class, () -> new SimulationDataSource(dto, USER_ID));
    }

    @Test
    void givenInvalidBoundsOnUpdate_throwsAndKeepsPreviousBounds() {
        // given
        var dataSource = new SimulationDataSource(dto(300L, 600L, 100L, 200L), USER_ID);

        // when, then
        assertThrows(InvalidSimulationRangeException.class, () -> dataSource.update(dto(2000L, 1000L, 400L, 500L)));
        assertEquals(300, dataSource.minConsumptionWatt());
        assertEquals(600, dataSource.maxConsumptionWatt());
        assertEquals(100, dataSource.minProductionWatt());
        assertEquals(200, dataSource.maxProductionWatt());
    }

    private static SimulationDataSourceDto dto(Long minConsumptionWatt,
                                               Long maxConsumptionWatt,
                                               Long minProductionWatt,
                                               Long maxProductionWatt) {
        var dto = mock(SimulationDataSourceDto.class);
        when(dto.minConsumptionWatt()).thenReturn(minConsumptionWatt);
        when(dto.maxConsumptionWatt()).thenReturn(maxConsumptionWatt);
        when(dto.minProductionWatt()).thenReturn(minProductionWatt);
        when(dto.maxProductionWatt()).thenReturn(maxProductionWatt);
        return dto;
    }
}
