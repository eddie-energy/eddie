package energy.eddie.regionconnector.dk.energinet.utils;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.enums.PointQualityEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumptionRecordMapperTest {
    @Test
    void mapToCIM_mapsEnerginetConsumptionRecord_asExpected() {
        //when
        var myEnergyMarketDocumentResponse = mock(MyEnergyDataMarketDocumentResponse.class);
        var myEnergyMarketDocument = mock(MyEnergyDataMarketDocument.class);
        var periodtimeInterval = mock(PeriodtimeInterval.class);
        var timeSeries = mock(TimeSeries.class);
        var period = mock(Period.class);
        var point = mock(Point.class);

        when(myEnergyMarketDocumentResponse.getMyEnergyDataMarketDocument()).thenReturn(myEnergyMarketDocument);
        when(myEnergyMarketDocument.getPeriodTimeInterval()).thenReturn(periodtimeInterval);
        when(myEnergyMarketDocument.getTimeSeries()).thenReturn(List.of(timeSeries));
        when(periodtimeInterval.getStart()).thenReturn("2022-03-31T22:00:00Z");
        when(timeSeries.getPeriod()).thenReturn(List.of(period));
        when(period.getResolution()).thenReturn(Granularity.P1D.name());
        when(period.getPoint()).thenReturn(List.of(point));
        when(point.getOutQuantityQuality()).thenReturn(PointQualityEnum.A04.name());
        when(point.getOutQuantityQuantity()).thenReturn("5.42");

        //when
        //then
        assertDoesNotThrow(() -> ConsumptionRecordMapper.timeSeriesToCIM(List.of(myEnergyMarketDocumentResponse)));
    }

    @Test
    void mapToCIM_mapsEnerginetConsumptionRecord_illegalPointQuality() {
        //given
        var illegalPointQualityEnum = PointQualityEnum.A02;

        var myEnergyMarketDocumentResponse = mock(MyEnergyDataMarketDocumentResponse.class);
        var myEnergyMarketDocument = mock(MyEnergyDataMarketDocument.class);
        var periodtimeInterval = mock(PeriodtimeInterval.class);
        var timeSeries = mock(TimeSeries.class);
        var period = mock(Period.class);
        var point = mock(Point.class);

        when(myEnergyMarketDocumentResponse.getMyEnergyDataMarketDocument()).thenReturn(myEnergyMarketDocument);
        when(myEnergyMarketDocument.getPeriodTimeInterval()).thenReturn(periodtimeInterval);
        when(myEnergyMarketDocument.getTimeSeries()).thenReturn(List.of(timeSeries));
        when(periodtimeInterval.getStart()).thenReturn("2022-03-31T22:00:00Z");
        when(timeSeries.getPeriod()).thenReturn(List.of(period));
        when(period.getResolution()).thenReturn(Granularity.P1D.name());
        when(period.getPoint()).thenReturn(List.of(point));
        when(point.getOutQuantityQuality()).thenReturn(illegalPointQualityEnum.name());
        when(point.getOutQuantityQuantity()).thenReturn("5.42");

        try {
            //when
            ConsumptionRecordMapper.timeSeriesToCIM(List.of(myEnergyMarketDocumentResponse));
        } catch (IllegalStateException exception) {
            //then
            assertEquals("Illegal Quality: " + illegalPointQualityEnum, exception.getMessage());
        }
    }

    @Test
    void mapToCIM_mapsEnerginetConsumptionRecord_energyMarketDocument_isNull() {
        //given
        var myEnergyMarketDocumentResponse = List.of(mock(MyEnergyDataMarketDocumentResponse.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ConsumptionRecordMapper.timeSeriesToCIM(myEnergyMarketDocumentResponse));
        assertEquals("No data available", exception.getMessage());
    }
}