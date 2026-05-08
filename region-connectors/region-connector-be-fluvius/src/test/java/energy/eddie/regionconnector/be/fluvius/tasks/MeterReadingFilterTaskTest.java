// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.tasks;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.MeteringOnMeter;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingFilterTaskTest {
    @Mock
    private DataNeedsService dataNeedsService;
    @InjectMocks
    private MeterReadingFilterTask task;

    @ParameterizedTest
    @MethodSource("apply_filterData_forEnergyTypeDataNeed")
    void apply_filterData_forEnergyTypeDataNeed(
            EnergyType energyType,
            energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType fluviusEnergyType
    ) {
        // Given
        var pr = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .dataNeedId("dnid")
                .build();
        var payload = new GetEnergyResponseModel(
                new MeteringOnMeter(
                        "ean",
                        fluviusEnergyType,
                        null
                )

        );
        var data = new GetEnergyResponseModelApiDataResponse(null, payload);
        var id = new IdentifiableMeteringData(pr, data);
        var dn = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                energyType,
                Granularity.PT1H,
                Granularity.P1D
        );
        when(dataNeedsService.getById("dnid")).thenReturn(dn);

        // When
        var res = task.test(id);

        // Then
        assertTrue(res);
    }

    private static Stream<Arguments> apply_filterData_forEnergyTypeDataNeed() {
        return Stream.of(
                Arguments.of(EnergyType.ELECTRICITY,
                             energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType.ELECTRICITY),
                Arguments.of(EnergyType.NATURAL_GAS,
                             energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType.GAS)
        );
    }
}