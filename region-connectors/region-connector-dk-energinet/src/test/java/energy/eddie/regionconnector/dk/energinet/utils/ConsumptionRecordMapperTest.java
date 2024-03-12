package energy.eddie.regionconnector.dk.energinet.utils;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.enums.PointQualityEnum;
import energy.eddie.regionconnector.dk.energinet.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsumptionRecordMapperTest {
    @Test
    void timeSeriesToConsumptionRecord_mapsEnerginetConsumptionRecord_asExpected() {
        // Given
        var point = new Point();
        point.setOutQuantityQuality(PointQualityEnum.A04.name());
        point.setOutQuantityQuantity("5.42");
        var period = new Period();
        period.setResolution(Granularity.P1D.name());
        period.setPoint(List.of(point));
        var timeSeries = new TimeSeries();
        timeSeries.setPeriod(List.of(period));
        var periodtimeInterval = new PeriodtimeInterval();
        periodtimeInterval.setStart("2022-03-31T22:00:00Z");
        var myEnergyMarketDocument = new MyEnergyDataMarketDocument();
        myEnergyMarketDocument.setPeriodTimeInterval(periodtimeInterval);
        myEnergyMarketDocument.setTimeSeries(List.of(timeSeries));
        var myEnergyMarketDocumentResponse = new MyEnergyDataMarketDocumentResponse();
        myEnergyMarketDocumentResponse.setMyEnergyDataMarketDocument(myEnergyMarketDocument);

        var permissionRequest = new SimplePermissionRequest("foo", "bar", "dId");
        IdentifiableApiResponse identifiableResponse = new IdentifiableApiResponse(permissionRequest, myEnergyMarketDocumentResponse);

        // When
        var consumptionRecord = ConsumptionRecordMapper.timeSeriesToConsumptionRecord(identifiableResponse);

        // Then
        assertEquals("foo", consumptionRecord.getPermissionId());
        assertEquals("bar", consumptionRecord.getConnectionId());
        assertEquals("dId", consumptionRecord.getDataNeedId());
    }

    @Test
    void timeSeriesToConsumptionRecord_mapsEnerginetConsumptionRecord_illegalPointQuality() {
        // Given
        var illegalPointQualityEnum = PointQualityEnum.A02;

        var point = new Point();
        point.setOutQuantityQuality(illegalPointQualityEnum.name());
        point.setOutQuantityQuantity("5.42");
        var permissionRequest = new SimplePermissionRequest();
        var period = new Period();
        period.setResolution(Granularity.P1D.name());
        period.setPoint(List.of(point));
        var timeSeries = new TimeSeries();
        timeSeries.setPeriod(List.of(period));
        var periodtimeInterval = new PeriodtimeInterval();
        periodtimeInterval.setStart("2022-03-31T22:00:00Z");
        var myEnergyMarketDocument = new MyEnergyDataMarketDocument();
        myEnergyMarketDocument.setPeriodTimeInterval(periodtimeInterval);
        myEnergyMarketDocument.setTimeSeries(List.of(timeSeries));
        var myEnergyMarketDocumentResponse = new MyEnergyDataMarketDocumentResponse();
        myEnergyMarketDocumentResponse.setMyEnergyDataMarketDocument(myEnergyMarketDocument);

        IdentifiableApiResponse identifiableResponse = new IdentifiableApiResponse(permissionRequest, myEnergyMarketDocumentResponse);

        try {
            // When
            ConsumptionRecordMapper.timeSeriesToConsumptionRecord(identifiableResponse);
        } catch (IllegalStateException exception) {
            // Then
            assertEquals("Illegal Quality: " + illegalPointQualityEnum, exception.getMessage());
        }
    }

    @Test
    void timeSeriesToConsumptionRecord_mapsEnerginetConsumptionRecord_energyMarketDocument_isNull() {
        // Given
        var myEnergyMarketDocumentResponse = new MyEnergyDataMarketDocumentResponse();
        var permissionRequest = new SimplePermissionRequest();
        IdentifiableApiResponse identifiableResponse = new IdentifiableApiResponse(permissionRequest, myEnergyMarketDocumentResponse);

        // When
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ConsumptionRecordMapper.timeSeriesToConsumptionRecord(identifiableResponse));

        // Then
        assertEquals("No data available", exception.getMessage());
    }
}
