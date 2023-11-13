package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd;

import energy.eddie.cim.validated_historical_data.v0_82.ESMPDateTimeIntervalComplexType;
import energy.eddie.cim.validated_historical_data.v0_82.MeasurementPointIDStringComplexType;
import energy.eddie.cim.validated_historical_data.v0_82.TimeSeriesComplexType;
import energy.eddie.cim.validated_historical_data.v0_82.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EddieValidatedHistoricalDataMarketDocumentPublisherTest {

    private static ValidatedHistoricalDataMarketDocument createMarketDocument(String meteringPointId, XMLGregorianCalendar xmlCalendar) {
        return new ValidatedHistoricalDataMarketDocument()
                .withPeriodTimeInterval(new ESMPDateTimeIntervalComplexType()
                        .withStart(xmlCalendar.toXMLFormat())
                )
                .withTimeSeriesList(new ValidatedHistoricalDataMarketDocument.TimeSeriesList()
                        .withTimeSeries(new TimeSeriesComplexType()
                                .withMarketEvaluationPointMRID(new MeasurementPointIDStringComplexType()
                                        .withValue(meteringPointId)
                                )
                        )
                );
    }

    @Test
    void emitForEachPermissionRequest_forThreePermissionRequest_emitsThreeTimes() {
        String meteringPointId = "meteringPointId";
        var xmlCalendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(2023, 1, 1, 0, 0, 0, 0, 0);
        LocalDate date = xmlCalendar.toGregorianCalendar().toZonedDateTime().toLocalDate();

        AtPermissionRequest permissionRequest = mock(AtPermissionRequest.class);
        when(permissionRequest.meteringPointId()).thenReturn(Optional.of(meteringPointId));

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByMeteringPointIdAndDate(meteringPointId, date))
                .thenReturn(List.of(permissionRequest, permissionRequest, permissionRequest));

        ValidatedHistoricalDataMarketDocument marketDocument = createMarketDocument(meteringPointId, xmlCalendar);

        var uut = new EddieValidatedHistoricalDataMarketDocumentPublisher(permissionRequestService);

        StepVerifier.create(uut.emitForEachPermissionRequest(marketDocument))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void emitForEachPermissionRequest_noPermissionRequest_emitsNothing() {
        String meteringPointId = "meteringPointId";
        var xmlCalendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(2023, 1, 1, 0, 0, 0, 0, 0);
        LocalDate date = xmlCalendar.toGregorianCalendar().toZonedDateTime().toLocalDate();

        AtPermissionRequest permissionRequest = mock(AtPermissionRequest.class);
        when(permissionRequest.meteringPointId()).thenReturn(Optional.of(meteringPointId));

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByMeteringPointIdAndDate(meteringPointId, date))
                .thenReturn(List.of());

        ValidatedHistoricalDataMarketDocument marketDocument = createMarketDocument(meteringPointId, xmlCalendar);

        var uut = new EddieValidatedHistoricalDataMarketDocumentPublisher(permissionRequestService);

        StepVerifier.create(uut.emitForEachPermissionRequest(marketDocument))
                .expectNextCount(0)
                .verifyComplete();
    }
}