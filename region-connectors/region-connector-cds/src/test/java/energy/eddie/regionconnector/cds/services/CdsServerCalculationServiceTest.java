// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.cds.client.CdsServerClient;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.dtos.CdsServerMasterData;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.master.data.Coverage;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsServerCalculationServiceTest {
    @Mock
    private CdsServerClientFactory factory;
    @Mock
    private CdsServerClient client;
    @Mock
    private DataNeedCalculationService calculationService;
    @InjectMocks
    private CdsServerCalculationService cdsServerCalculationService;

    @Test
    void testCalculate_whereCalculationIsNotValidatedHistoricalDataDataNeedResult_returnCalculation() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .build();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var calculationResult = new DataNeedNotSupportedResult("bla");
        when(calculationService.calculate("dnid", now))
                .thenReturn(calculationResult);


        // When
        var res = cdsServerCalculationService.calculate("dnid", cdsServer, now);

        // Then
        assertEquals(calculationResult, res);
    }

    @Test
    void testCalculate_whereDataNeedRequiresDifferentEnergyTypeThanCdsServerProvides_returnCalculation() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var cdsServer = new CdsServerBuilder().build();
        var calculationResult = createDataNeedResult(now, EnergyType.NATURAL_GAS);
        when(factory.get(cdsServer)).thenReturn(client);
        when(client.masterData()).thenReturn(Mono.just(
                new CdsServerMasterData(
                        "CDS Server",
                        "1",
                        URI.create("http://localhost"),
                        Set.of(new Coverage(EnergyType.ELECTRICITY, "us"))
                )
        ));
        when(calculationService.calculate("dnid", now))
                .thenReturn(calculationResult);

        // When
        var res = cdsServerCalculationService.calculate("dnid", cdsServer, now);

        // Then
        assertInstanceOf(DataNeedNotSupportedResult.class, res);
    }

    @Test
    void testCalculate_withValidDataNeed_returnCalculation() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .build();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var calculationResult = createDataNeedResult(now, EnergyType.ELECTRICITY);
        when(calculationService.calculate("dnid", now))
                .thenReturn(calculationResult);
        when(factory.get(cdsServer)).thenReturn(client);
        when(client.masterData()).thenReturn(Mono.just(
                new CdsServerMasterData(
                        "CDS Server",
                        "1",
                        URI.create("http://localhost"),
                        Set.of(new Coverage(EnergyType.ELECTRICITY, "us"))
                )
        ));


        // When
        var res = cdsServerCalculationService.calculate("dnid", cdsServer, now);

        // Then
        assertEquals(calculationResult, res);
    }

    private static @NonNull ValidatedHistoricalDataDataNeedResult createDataNeedResult(
            ZonedDateTime now,
            EnergyType energyType
    ) {
        var today = now.toLocalDate();
        var validatedHistoricalDataDataNeed = new ValidatedHistoricalDataDataNeed(new AbsoluteDuration(today, today),
                                                                                  energyType,
                                                                                  Granularity.PT5M,
                                                                                  Granularity.P1Y);
        return new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                         new Timeframe(today, today),
                                                         new Timeframe(today, today),
                                                         validatedHistoricalDataDataNeed);
    }
}