package energy.eddie.regionconnector.fr.enedis.providers.v0_82;


import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.v0_82.vhd.AggregateKind;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v0_82.vhd.SeriesPeriodComplexType;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static energy.eddie.cim.v0_82.vhd.EnergyProductTypeList.ACTIVE_ENERGY;
import static energy.eddie.cim.v0_82.vhd.EnergyProductTypeList.ACTIVE_POWER;
import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntermediateValidatedHistoricalDocumentTest {


    @Test
    void testEddieValidatedHistoricalDataMarketDocument_DailyConsumption_returnsMappedDocument() throws IOException {
        // Given
        var meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK);
        var permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.permissionId()).thenReturn("pid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.granularity()).thenReturn(Granularity.P1D);

        var identifiableMeterReading = new IdentifiableMeterReading(permissionRequest, meterReading);
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                identifiableMeterReading,
                () -> CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                new PlainEnedisConfiguration("clientId", "clientSecret", "basepath", 24)
        );
        var esmpTimeInterval = new EsmpTimeInterval(
                LocalDate.of(2024, 2, 1).atStartOfDay(ZONE_ID_FR),
                LocalDate.of(2024, 2, 8).atStartOfDay(ZONE_ID_FR)
        );
        // When
        var res = intermediateVHD.eddieValidatedHistoricalDataMarketDocument();


        ValidatedHistoricalDataMarketDocument marketDocument = res.marketDocument();
        var timeSeries = marketDocument.getTimeSeriesList().getTimeSeries().getFirst();
        SeriesPeriodComplexType seriesPeriod = timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst();

        // Then
        // The optional is checked, but intellij does not infer this.
        //noinspection OptionalGetWithoutIsPresent
        assertAll(
                () -> assertTrue(res.permissionId().isPresent()),
                () -> assertTrue(res.connectionId().isPresent()),
                () -> assertTrue(res.dataNeedId().isPresent()),
                () -> assertEquals("pid", res.permissionId().get()),
                () -> assertEquals("cid", res.connectionId().get()),
                () -> assertEquals("dnid", res.dataNeedId().get()),
                () -> assertEquals("clientId", marketDocument.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(esmpTimeInterval.start(), marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), marketDocument.getPeriodTimeInterval().getEnd()),
                () -> assertEquals(ACTIVE_ENERGY, timeSeries.getProduct()),
                () -> assertEquals(AggregateKind.SUM, timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals("24115050XXXXXX", timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(1, timeSeries.getSeriesPeriodList().getSeriesPeriods().size()),
                () -> assertEquals(7, timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst().getPointList().getPoints().size()),
                () -> assertEquals(Granularity.P1D.name(), seriesPeriod.getResolution()),
                () -> assertEquals(esmpTimeInterval.start(), seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), seriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("0", seriesPeriod.getPointList().getPoints().getFirst().getPosition()),
                () -> assertEquals(new BigDecimal(0), seriesPeriod.getPointList().getPoints().getFirst().getEnergyQuantityQuantity())
        );
    }

    @Test
    void testEddieValidatedHistoricalDataMarketDocument_ConsumptionLoadCurve_returnsMappedDocument() throws IOException {
        // Given
        var meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY);
        var permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.permissionId()).thenReturn("pid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.granularity()).thenReturn(Granularity.PT30M);

        var identifiableMeterReading = new IdentifiableMeterReading(permissionRequest, meterReading);
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                identifiableMeterReading,
                () -> CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                new PlainEnedisConfiguration("clientId", "clientSecret", "basepath", 24)
        );
        var esmpTimeInterval = new EsmpTimeInterval(
                LocalDate.of(2024, 2, 26).atStartOfDay(ZONE_ID_FR),
                LocalDate.of(2024, 2, 27).atStartOfDay(ZONE_ID_FR)
        );
        // When
        var res = intermediateVHD.eddieValidatedHistoricalDataMarketDocument();


        ValidatedHistoricalDataMarketDocument marketDocument = res.marketDocument();
        var timeSeries = marketDocument.getTimeSeriesList().getTimeSeries().getFirst();
        SeriesPeriodComplexType seriesPeriod = timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst();

        // Then
        // The optional is checked, but intellij does not infer this.
        //noinspection OptionalGetWithoutIsPresent
        assertAll(
                () -> assertTrue(res.permissionId().isPresent()),
                () -> assertTrue(res.connectionId().isPresent()),
                () -> assertTrue(res.dataNeedId().isPresent()),
                () -> assertEquals("pid", res.permissionId().get()),
                () -> assertEquals("cid", res.connectionId().get()),
                () -> assertEquals("dnid", res.dataNeedId().get()),
                () -> assertEquals("clientId", marketDocument.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(esmpTimeInterval.start(), marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), marketDocument.getPeriodTimeInterval().getEnd()),
                () -> assertEquals(ACTIVE_POWER, timeSeries.getProduct()),
                () -> assertEquals(AggregateKind.AVERAGE, timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals("24115050XXXXXX", timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(1, timeSeries.getSeriesPeriodList().getSeriesPeriods().size()),
                () -> assertEquals(47, timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst().getPointList().getPoints().size()), // for some reason enedis only returns 47 values instead of 48
                () -> assertEquals(Granularity.PT30M.name(), seriesPeriod.getResolution()),
                () -> assertEquals(esmpTimeInterval.start(), seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), seriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("0", seriesPeriod.getPointList().getPoints().getFirst().getPosition()),
                () -> assertEquals(new BigDecimal(0), seriesPeriod.getPointList().getPoints().getFirst().getEnergyQuantityQuantity())
        );
    }
}
