package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("DataFlowIssue")
class TimeSeriesBuilderTest {
    static MyEnergyDataMarketDocument myEnergyDataMarketDocument;

    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JsonNullableModule());
        try (InputStream is = TimeSeriesBuilderTest.class.getClassLoader().getResourceAsStream("MyEnergyDataMarketDocumentResponseListApiResponse.json")) {
            MyEnergyDataMarketDocumentResponseListApiResponse response = objectMapper.readValue(is, MyEnergyDataMarketDocumentResponseListApiResponse.class);
            myEnergyDataMarketDocument = response.getResult().getFirst().getMyEnergyDataMarketDocument();
        }
    }

    @Test
    void withTimeSeriesList_validatedTimeSeriesFromJson() {
        // Arrange
        String meteringPointId1 = "5713131791000XXXX1";
        String meteringPointId2 = "5713131791000XXXX2";
        SeriesPeriodBuilderFactory factory = mock(SeriesPeriodBuilderFactory.class);
        SeriesPeriodBuilder seriesPeriodBuilder = mock(SeriesPeriodBuilder.class);
        when(seriesPeriodBuilder.withPeriods(any())).thenReturn(seriesPeriodBuilder);
        when(factory.create()).thenReturn(seriesPeriodBuilder);

        TimeSeriesBuilder builder = new TimeSeriesBuilder(factory);

        // Act
        builder.withTimeSeriesList(myEnergyDataMarketDocument.getTimeSeries());
        var result = builder.build();

        // Assert
        var firstTimeSeries = result.getTimeSeries().getFirst();
        assertEquals(meteringPointId1, firstTimeSeries.getMRID());
        assertEquals(BusinessTypeList.CONSUMPTION, firstTimeSeries.getBusinessType());
        assertEquals(EnergyProductTypeList.ACTIVE_POWER, firstTimeSeries.getProduct());
        assertEquals(DirectionTypeList.DOWN, firstTimeSeries.getFlowDirectionDirection());
        assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED, firstTimeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity());
        assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR, firstTimeSeries.getEnergyMeasurementUnitName());
        assertEquals(CodingSchemeTypeList.GS1, firstTimeSeries.getMarketEvaluationPointMRID().getCodingScheme());
        assertEquals(meteringPointId1, firstTimeSeries.getMarketEvaluationPointMRID().getValue());
        assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED, firstTimeSeries.getReasonList().getReasons().getFirst().getCode());

        var lastTimeSeries = result.getTimeSeries().getLast();
        assertEquals(meteringPointId2, lastTimeSeries.getMRID());
        assertEquals(BusinessTypeList.PRODUCTION, lastTimeSeries.getBusinessType());
        assertEquals(EnergyProductTypeList.ACTIVE_POWER, lastTimeSeries.getProduct());
        assertEquals(DirectionTypeList.UP, lastTimeSeries.getFlowDirectionDirection());
        assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED, lastTimeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity());
        assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR, lastTimeSeries.getEnergyMeasurementUnitName());
        assertEquals(CodingSchemeTypeList.GS1, lastTimeSeries.getMarketEvaluationPointMRID().getCodingScheme());
        assertEquals(meteringPointId2, lastTimeSeries.getMarketEvaluationPointMRID().getValue());
        assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED, lastTimeSeries.getReasonList().getReasons().getFirst().getCode());

        verify(seriesPeriodBuilder).withPeriods(myEnergyDataMarketDocument.getTimeSeries().getFirst().getPeriod());
        verify(seriesPeriodBuilder).withPeriods(myEnergyDataMarketDocument.getTimeSeries().getLast().getPeriod());
        verify(seriesPeriodBuilder, times(2)).build();
        verify(factory, times(2)).create();
        verifyNoMoreInteractions(seriesPeriodBuilder, factory);
    }
}