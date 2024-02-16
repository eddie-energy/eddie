package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.vhd.QualityTypeList;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("DataFlowIssue")
class SeriesPeriodBuilderTest {

    static MyEnergyDataMarketDocument myEnergyDataMarketDocument;

    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JsonNullableModule());
        try (InputStream is = SeriesPeriodBuilderTest.class.getClassLoader().getResourceAsStream("MyEnergyDataMarketDocumentResponseListApiResponse.json")) {
            MyEnergyDataMarketDocumentResponseListApiResponse response = objectMapper.readValue(is, MyEnergyDataMarketDocumentResponseListApiResponse.class);
            myEnergyDataMarketDocument = response.getResult().getFirst().getMyEnergyDataMarketDocument();
        }
    }

    @Test
    void withPeriods_firstTimeSeries_inJson() {
        // Arrange
        SeriesPeriodBuilder builder = new SeriesPeriodBuilder();

        // Act
        builder.withPeriods(myEnergyDataMarketDocument.getTimeSeries().getFirst().getPeriod());
        var result = builder.build();

        // Assert
        var seriesPeriods = result.getSeriesPeriods();
        assertEquals(2, seriesPeriods.size());

        var firstSeriesPeriod = seriesPeriods.getFirst();
        assertEquals("PT1H", firstSeriesPeriod.getResolution());
        assertEquals("2024-02-11T23:00:00Z", firstSeriesPeriod.getTimeInterval().getStart());
        assertEquals("2024-02-12T23:00:00Z", firstSeriesPeriod.getTimeInterval().getEnd());
        assertEquals(24, firstSeriesPeriod.getPointList().getPoints().size());
        var firstPoint = firstSeriesPeriod.getPointList().getPoints().getFirst();
        assertEquals("1", firstPoint.getPosition());
        assertEquals(BigDecimal.valueOf(0.13), firstPoint.getEnergyQuantityQuantity());
        assertEquals(QualityTypeList.AS_PROVIDED, firstPoint.getEnergyQuantityQuality());
        var lastPoint = firstSeriesPeriod.getPointList().getPoints().getLast();
        assertEquals("24", lastPoint.getPosition());
        assertEquals(BigDecimal.valueOf(0.12), lastPoint.getEnergyQuantityQuantity());
        assertEquals(QualityTypeList.AS_PROVIDED, lastPoint.getEnergyQuantityQuality());

        var secondSeriesPeriod = seriesPeriods.getLast();
        assertEquals("PT1H", secondSeriesPeriod.getResolution());
        assertEquals("2024-02-12T23:00:00Z", secondSeriesPeriod.getTimeInterval().getStart());
        assertEquals("2024-02-13T23:00:00Z", secondSeriesPeriod.getTimeInterval().getEnd());
        assertEquals(24, secondSeriesPeriod.getPointList().getPoints().size());
        var firstPoint2 = secondSeriesPeriod.getPointList().getPoints().getFirst();
        assertEquals("1", firstPoint2.getPosition());
        assertEquals(BigDecimal.valueOf(0.12), firstPoint2.getEnergyQuantityQuantity());
        assertEquals(QualityTypeList.AS_PROVIDED, firstPoint2.getEnergyQuantityQuality());
        var lastPoint2 = secondSeriesPeriod.getPointList().getPoints().getLast();
        assertEquals("24", lastPoint2.getPosition());
        assertEquals(BigDecimal.valueOf(0.11), lastPoint2.getEnergyQuantityQuantity());
        assertEquals(QualityTypeList.AS_PROVIDED, lastPoint2.getEnergyQuantityQuality());
    }

    @Test
    void withPeriods_secondTimeSeries_inJson() {
        // Arrange
        SeriesPeriodBuilder builder = new SeriesPeriodBuilder();

        // Act
        builder.withPeriods(myEnergyDataMarketDocument.getTimeSeries().getLast().getPeriod());
        var result = builder.build();

        // Assert
        var seriesPeriods = result.getSeriesPeriods();
        assertEquals(2, seriesPeriods.size());

        var firstSeriesPeriod = seriesPeriods.getFirst();
        assertEquals("PT1H", firstSeriesPeriod.getResolution());
        assertEquals("2024-02-12T23:00:00Z", firstSeriesPeriod.getTimeInterval().getStart());
        assertEquals("2024-02-13T23:00:00Z", firstSeriesPeriod.getTimeInterval().getEnd());
        assertEquals(24, firstSeriesPeriod.getPointList().getPoints().size());
        var firstPoint = firstSeriesPeriod.getPointList().getPoints().getFirst();
        assertEquals("1", firstPoint.getPosition());
        assertEquals(BigDecimal.valueOf(0.03), firstPoint.getEnergyQuantityQuantity());
        assertEquals(QualityTypeList.AS_PROVIDED, firstPoint.getEnergyQuantityQuality());
        var lastPoint = firstSeriesPeriod.getPointList().getPoints().getLast();
        assertEquals("24", lastPoint.getPosition());
        assertEquals(BigDecimal.valueOf(0.15), lastPoint.getEnergyQuantityQuantity());
        assertEquals(QualityTypeList.AS_PROVIDED, lastPoint.getEnergyQuantityQuality());

        var secondSeriesPeriod = seriesPeriods.getLast();
        assertEquals("PT1H", secondSeriesPeriod.getResolution());
        assertEquals("2024-02-13T23:00:00Z", secondSeriesPeriod.getTimeInterval().getStart());
        assertEquals("2024-02-14T23:00:00Z", secondSeriesPeriod.getTimeInterval().getEnd());
        assertEquals(24, secondSeriesPeriod.getPointList().getPoints().size());
        var firstPoint2 = secondSeriesPeriod.getPointList().getPoints().getFirst();
        assertEquals("1", firstPoint2.getPosition());
        assertEquals(BigDecimal.valueOf(0.22), firstPoint2.getEnergyQuantityQuantity());
        assertEquals(QualityTypeList.AS_PROVIDED, firstPoint2.getEnergyQuantityQuality());
        var lastPoint2 = secondSeriesPeriod.getPointList().getPoints().getLast();
        assertEquals("24", lastPoint2.getPosition());
        assertEquals(BigDecimal.valueOf(0.05), lastPoint2.getEnergyQuantityQuantity());
        assertEquals(QualityTypeList.AS_PROVIDED, lastPoint2.getEnergyQuantityQuality());
    }
}