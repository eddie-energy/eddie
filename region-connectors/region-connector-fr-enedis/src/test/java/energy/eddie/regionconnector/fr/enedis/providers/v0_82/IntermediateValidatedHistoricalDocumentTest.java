package energy.eddie.regionconnector.fr.enedis.providers.v0_82;


import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.AggregateKind;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v0_82.vhd.SeriesPeriodComplexType;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static energy.eddie.cim.v0_82.vhd.EnergyProductTypeList.ACTIVE_ENERGY;
import static energy.eddie.cim.v0_82.vhd.EnergyProductTypeList.ACTIVE_POWER;
import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

        var intermediateVHD = intermediateValidatedHistoricalDocument(permissionRequest, meterReading);
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
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals("cid", res.connectionId()),
                () -> assertEquals("dnid", res.dataNeedId()),
                () -> assertEquals("clientId", marketDocument.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(esmpTimeInterval.start(), marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), marketDocument.getPeriodTimeInterval().getEnd()),
                () -> assertEquals(ACTIVE_ENERGY, timeSeries.getProduct()),
                () -> assertEquals(AggregateKind.SUM,
                                   timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals("24115050XXXXXX", timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(1, timeSeries.getSeriesPeriodList().getSeriesPeriods().size()),
                () -> assertEquals(7,
                                   timeSeries.getSeriesPeriodList()
                                             .getSeriesPeriods()
                                             .getFirst()
                                             .getPointList()
                                             .getPoints()
                                             .size()),
                () -> assertEquals(Granularity.P1D.name(), seriesPeriod.getResolution()),
                () -> assertEquals(esmpTimeInterval.start(), seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), seriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("0", seriesPeriod.getPointList().getPoints().getFirst().getPosition()),
                () -> assertEquals(new BigDecimal("0.0"),
                                   seriesPeriod.getPointList().getPoints().getFirst().getEnergyQuantityQuantity())
        );
    }

    private static IntermediateValidatedHistoricalDocument intermediateValidatedHistoricalDocument(
            FrEnedisPermissionRequest permissionRequest,
            MeterReading meterReading
    ) {
        var identifiableMeterReading = new IdentifiableMeterReading(permissionRequest,
                                                                    meterReading,
                                                                    MeterReadingType.CONSUMPTION);
        return new IntermediateValidatedHistoricalDocument(
                identifiableMeterReading,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "fallbackId"),
                new PlainEnedisConfiguration("clientId", "clientSecret", "basepath")
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

        var intermediateVHD = intermediateValidatedHistoricalDocument(permissionRequest, meterReading);
        var esmpTimeInterval = new EsmpTimeInterval(
                LocalDate.of(2024, 2, 26).atStartOfDay(ZONE_ID_FR),
                LocalDate.of(2024, 2, 27).atStartOfDay(ZONE_ID_FR)
        );
        var esmpMeterReadingInterval = new EsmpTimeInterval(
                LocalDate.of(2024, 2, 26).atStartOfDay(ZONE_ID_FR),
                LocalDate.of(2024, 2, 27).atStartOfDay(ZONE_ID_FR).minusMinutes(30)
        );
        // When
        var res = intermediateVHD.eddieValidatedHistoricalDataMarketDocument();


        ValidatedHistoricalDataMarketDocument marketDocument = res.marketDocument();
        var timeSeries = marketDocument.getTimeSeriesList().getTimeSeries().getFirst();
        SeriesPeriodComplexType seriesPeriod = timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst();

        // Then
        // The optional is checked, but intellij does not infer this.
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals("cid", res.connectionId()),
                () -> assertEquals("dnid", res.dataNeedId()),
                () -> assertEquals("clientId", marketDocument.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(esmpTimeInterval.start(), marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), marketDocument.getPeriodTimeInterval().getEnd()),
                () -> assertEquals(ACTIVE_POWER, timeSeries.getProduct()),
                () -> assertEquals(AggregateKind.AVERAGE,
                                   timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals("24115050XXXXXX", timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(1, timeSeries.getSeriesPeriodList().getSeriesPeriods().size()),
                () -> assertEquals(47,
                                   timeSeries.getSeriesPeriodList()
                                             .getSeriesPeriods()
                                             .getFirst()
                                             .getPointList()
                                             .getPoints()
                                             .size()), // for some reason enedis only returns 47 values instead of 48
                () -> assertEquals(Granularity.PT30M.name(), seriesPeriod.getResolution()),
                () -> assertEquals(esmpMeterReadingInterval.start(), seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals(esmpMeterReadingInterval.end(), seriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("0", seriesPeriod.getPointList().getPoints().getFirst().getPosition()),
                () -> assertEquals(new BigDecimal("0.0"),
                                   seriesPeriod.getPointList().getPoints().getFirst().getEnergyQuantityQuantity())
        );
    }

    @Test
    void testEddieValidatedHistoricalDataMarketDocument_ConsumptionLoadCurveWithChangingResolution_returnsMappedDocument() throws IOException {
        // Given
        var meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_WITH_CHANGING_RESOLUTION_1_DAY);
        var permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.permissionId()).thenReturn("pid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.granularity()).thenReturn(Granularity.PT30M);

        var intermediateVHD = intermediateValidatedHistoricalDocument(permissionRequest, meterReading);
        var esmpTimeInterval = new EsmpTimeInterval(
                LocalDate.of(2024, 2, 26).atStartOfDay(ZONE_ID_FR),
                LocalDate.of(2024, 2, 27).atStartOfDay(ZONE_ID_FR)
        );
        // When
        var res = intermediateVHD.eddieValidatedHistoricalDataMarketDocument();


        ValidatedHistoricalDataMarketDocument marketDocument = res.marketDocument();
        var timeSeries = marketDocument.getTimeSeriesList().getTimeSeries().getFirst();
        SeriesPeriodComplexType pt30mSeries = timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst();
        SeriesPeriodComplexType pt10mSeries = timeSeries.getSeriesPeriodList().getSeriesPeriods().get(1);
        SeriesPeriodComplexType pt15mSeries = timeSeries.getSeriesPeriodList().getSeriesPeriods().get(2);
        SeriesPeriodComplexType pt60mSeries = timeSeries.getSeriesPeriodList().getSeriesPeriods().getLast();

        // Then
        // The optional is checked, but intellij does not infer this.
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals("cid", res.connectionId()),
                () -> assertEquals("dnid", res.dataNeedId()),
                () -> assertEquals("clientId", marketDocument.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(esmpTimeInterval.start(), marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), marketDocument.getPeriodTimeInterval().getEnd()),
                () -> assertEquals(ACTIVE_POWER, timeSeries.getProduct()),
                () -> assertEquals(AggregateKind.AVERAGE,
                                   timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals("24115050XXXXXX", timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(4, timeSeries.getSeriesPeriodList().getSeriesPeriods().size()),
                // PT30M interval
                () -> assertEquals(2, pt30mSeries.getPointList().getPoints().size()),
                () -> assertEquals(Granularity.PT30M.name(), pt30mSeries.getResolution()),
                () -> assertEquals("2024-02-25T23:00Z", pt30mSeries.getTimeInterval().getStart()),
                () -> assertEquals("2024-02-26T00:00Z", pt30mSeries.getTimeInterval().getEnd()),
                // PT10M interval
                () -> assertEquals(3, pt10mSeries.getPointList().getPoints().size()),
                () -> assertEquals(Granularity.PT10M.name(), pt10mSeries.getResolution()),
                () -> assertEquals("2024-02-26T00:00Z", pt10mSeries.getTimeInterval().getStart()),
                () -> assertEquals("2024-02-26T00:30Z", pt10mSeries.getTimeInterval().getEnd()),
                // PT15M interval
                () -> assertEquals(2, pt15mSeries.getPointList().getPoints().size()),
                () -> assertEquals(Granularity.PT15M.name(), pt15mSeries.getResolution()),
                () -> assertEquals("2024-02-26T00:30Z", pt15mSeries.getTimeInterval().getStart()),
                () -> assertEquals("2024-02-26T01:00Z", pt15mSeries.getTimeInterval().getEnd()),
                // PT60M interval
                () -> assertEquals(21, pt60mSeries.getPointList().getPoints().size()),
                () -> assertEquals(Granularity.PT1H.name(), pt60mSeries.getResolution()),
                () -> assertEquals("2024-02-26T01:00Z", pt60mSeries.getTimeInterval().getStart()),
                () -> assertEquals("2024-02-26T22:00Z", pt60mSeries.getTimeInterval().getEnd())
                // last value is missing because ENEDIS does not return the last value
        );
    }
}
