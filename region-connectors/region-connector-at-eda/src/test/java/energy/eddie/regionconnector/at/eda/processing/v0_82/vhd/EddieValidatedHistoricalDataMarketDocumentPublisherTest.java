package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd;

import energy.eddie.cim.v0_82.vhd.ESMPDateTimeIntervalComplexType;
import energy.eddie.cim.v0_82.vhd.MeasurementPointIDStringComplexType;
import energy.eddie.cim.v0_82.vhd.TimeSeriesComplexType;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
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
        ZonedDateTime date = xmlCalendar.toGregorianCalendar().toZonedDateTime();

        AtPermissionRequest permissionRequest = mock(AtPermissionRequest.class);
        when(permissionRequest.meteringPointId()).thenReturn(Optional.of(meteringPointId));

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByMeteringPointIdAndDate(meteringPointId, date.toLocalDate()))
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
        ZonedDateTime date = xmlCalendar.toGregorianCalendar().toZonedDateTime();

        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByMeteringPointIdAndDate(meteringPointId, date.toLocalDate()))
                .thenReturn(List.of());

        ValidatedHistoricalDataMarketDocument marketDocument = createMarketDocument(meteringPointId, xmlCalendar);

        var uut = new EddieValidatedHistoricalDataMarketDocumentPublisher(permissionRequestService);

        StepVerifier.create(uut.emitForEachPermissionRequest(marketDocument))
                .expectNextCount(0)
                .verifyComplete();
    }
}