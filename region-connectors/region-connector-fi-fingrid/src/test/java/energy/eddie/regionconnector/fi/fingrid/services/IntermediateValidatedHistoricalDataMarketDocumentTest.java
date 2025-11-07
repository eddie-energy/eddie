package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    @Test
    void toVhd_withErrors_returnsVhdsWithErrors() {
        // Given
        var response = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_ERRORS);
        var intermediateVHD = new IntermediateValidatedHistoricalDataMarketDocument(List.of(response));

        // When
        var res = intermediateVHD.toVhds().getFirst();

        // Then
        assertAll(
                () -> assertEquals("773a97c3-ad9d-46ce-9d32-5123780c5b83", res.getMRID()),
                () -> assertEquals("2024-07-30T00:00:00Z", res.getCreatedDateTime()),
                () -> assertEquals("1111111111111", res.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals("0000000000000", res.getReceiverMarketParticipantMRID().getValue()),
                () -> assertFalse(res.getTimeSeriesList().getTimeSeries().isEmpty())
        );
        var timeSeries = res.getTimeSeriesList().getTimeSeries().getFirst();
        assertAll(

                () -> assertFalse(timeSeries.getReasonList().getReasons().isEmpty()),
                () -> assertNull(timeSeries.getEnergyMeasurementUnitName()),
                () -> assertNull(timeSeries.getMarketEvaluationPointMRID().getValue())
        );
        var reason = timeSeries.getReasonList().getReasons().getFirst();
        assertEquals("No data found", reason.getText());
    }

    @Test
    @SuppressWarnings("java:S5961")
    void toVhd_withValues_returnsVhdsWithTimeSeries() {
        // Given
        var response = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var intermediateVHD = new IntermediateValidatedHistoricalDataMarketDocument(List.of(response));

        // When
        var res = intermediateVHD.toVhds().getFirst();

        // Then
        assertAll(
                () -> assertEquals("8c5857e0-9286-4099-b249-cedf90353b48", res.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), res.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT, res.getType()),
                () -> assertEquals("2024-07-29T00:00:00Z", res.getCreatedDateTime()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   res.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER, res.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.REALISED, res.getProcessProcessType()),
                () -> assertEquals(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME,
                                   res.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("1111111111111", res.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME,
                                   res.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("0000000000000", res.getReceiverMarketParticipantMRID().getValue()),
                () -> assertFalse(res.getTimeSeriesList().getTimeSeries().isEmpty())
        );
        var timeSeries = res.getTimeSeriesList().getTimeSeries().getFirst();
        assertAll(
                () -> assertEquals(EnergyProductTypeList.ACTIVE_ENERGY, timeSeries.getProduct()),
                () -> assertEquals(DirectionTypeList.UP_AND_DOWN, timeSeries.getFlowDirectionDirection()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED,
                                   timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertFalse(timeSeries.getReasonList().getReasons().isEmpty()),
                () -> assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED,
                                   timeSeries.getReasonList().getReasons().getFirst().getCode()),
                () -> assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR, timeSeries.getEnergyMeasurementUnitName()),
                () -> assertEquals("642502030419633983", timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME,
                                   timeSeries.getMarketEvaluationPointMRID().getCodingScheme())
        );
        var reason = timeSeries.getReasonList().getReasons().getFirst();
        assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED, reason.getCode());
        var periods = timeSeries.getSeriesPeriodList().getSeriesPeriods();
        assertEquals(1, periods.size());
        var period = periods.getFirst();
        assertAll(
                () -> assertEquals("PT1H", period.getResolution()),
                () -> assertEquals("2024-07-21T00:00Z", period.getTimeInterval().getStart()),
                () -> assertEquals("2024-07-30T00:00Z", period.getTimeInterval().getEnd()),
                () -> assertEquals(2, period.getPointList().getPoints().size())
        );
        var points = period.getPointList().getPoints();
        var first = points.getFirst();
        assertAll(
                () -> assertEquals(BigDecimal.valueOf(1509000, 6), first.getEnergyQualityQuantityQuantity()),
                () -> assertEquals(QualityTypeList.AS_PROVIDED, first.getEnergyQualityQuantityQuality()),
                () -> assertEquals("165", first.getPosition())
        );
        var second = points.get(1);
        assertAll(
                () -> assertEquals(BigDecimal.valueOf(3353000, 6), second.getEnergyQualityQuantityQuantity()),
                () -> assertEquals(QualityTypeList.AS_PROVIDED, second.getEnergyQualityQuantityQuality()),
                () -> assertEquals("166", second.getPosition())
        );
    }
}