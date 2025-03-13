package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.master.data.Coverage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private DataNeedsService dataNeedsService;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @InjectMocks
    private CdsServerCalculationService cdsServerCalculationService;

    @Test
    void testCalculate_whereCalculationIsNotValidatedHistoricalDataDataNeedResult_returnCalculation() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setCoverages(Set.of(new Coverage(EnergyType.ELECTRICITY, "us")))
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
    void testCalculate_withDataNeedNotMatchingResult_returnCalculation() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setCoverages(Set.of(new Coverage(EnergyType.ELECTRICITY, "us")))
                .build();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = now.toLocalDate();
        var calculationResult = new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                          new Timeframe(today, today),
                                                                          new Timeframe(today, today));
        when(calculationService.calculate("dnid", now))
                .thenReturn(calculationResult);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new AiidaDataNeed(Set.of()));


        // When
        var res = cdsServerCalculationService.calculate("dnid", cdsServer, now);

        // Then
        assertEquals(calculationResult, res);
    }

    @Test
    void testCalculate_whereDataNeedRequiresDifferentEnergyTypeThanCdsServerProvides_returnCalculation() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setCoverages(Set.of(new Coverage(EnergyType.ELECTRICITY, "us")))
                .build();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = now.toLocalDate();
        var calculationResult = new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                          new Timeframe(today, today),
                                                                          new Timeframe(today, today));
        when(calculationService.calculate("dnid", now))
                .thenReturn(calculationResult);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(new AbsoluteDuration(today, today),
                                                                EnergyType.NATURAL_GAS,
                                                                Granularity.PT5M,
                                                                Granularity.P1Y));


        // When
        var res = cdsServerCalculationService.calculate("dnid", cdsServer, now);

        // Then
        assertInstanceOf(DataNeedNotSupportedResult.class, res);
    }
    @Test
    void testCalculate_withValidDataNeed_returnCalculation() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setCoverages(Set.of(new Coverage(EnergyType.ELECTRICITY, "us")))
                .build();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = now.toLocalDate();
        var calculationResult = new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                          new Timeframe(today, today),
                                                                          new Timeframe(today, today));
        when(calculationService.calculate("dnid", now))
                .thenReturn(calculationResult);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(new AbsoluteDuration(today, today),
                                                                EnergyType.ELECTRICITY,
                                                                Granularity.PT5M,
                                                                Granularity.P1Y));


        // When
        var res = cdsServerCalculationService.calculate("dnid", cdsServer, now);

        // Then
        assertEquals(calculationResult, res);
    }
}