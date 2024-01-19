package energy.eddie.regionconnector.es.datadis.providers.v0;

import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatadisMvp1ConsumptionRecordProviderTest {
    @Test
    void givenValueOnFlux_publishesConsumptionRecordOnFlow() {
        // Given
        var now = ZonedDateTime.now(ZoneId.systemDefault());
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation("conId", "dId", "nif", "mip", now, now, MeasurementType.HOURLY);
        new DatadisPermissionRequest("pId", requestForCreation, mock(AuthorizationApi.class));
        TestPublisher<IdentifiableMeteringData> publisher = TestPublisher.create();
        EsPermissionRequest mockRequest = mock(EsPermissionRequest.class);
        when(mockRequest.requestDataFrom()).thenReturn(now);
        when(mockRequest.requestDataTo()).thenReturn(now);
        when(mockRequest.connectionId()).thenReturn("conId");
        when(mockRequest.dataNeedId()).thenReturn("dId");
        when(mockRequest.permissionId()).thenReturn("pId");
        List<MeteringData> list = new ArrayList<>();
        list.add(new MeteringData("CUPS", now.toLocalDate(), now.toLocalTime(), 123.123, "foo", 456.456));
        var reading = new IdentifiableMeteringData(mockRequest, list);

        //noinspection resource StepVerifier closes provider
        var provider = new DatadisMvp1ConsumptionRecordProvider(publisher.flux());

        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(provider.getConsumptionRecordStream()))
                // When
                .then(() -> publisher.next(reading))
                // Then
                .assertNext(consumptionRecord -> assertAll(
                        () -> assertEquals("conId", consumptionRecord.getConnectionId()),
                        () -> assertEquals("pId", consumptionRecord.getPermissionId()),
                        () -> assertEquals("dId", consumptionRecord.getDataNeedId()),
                        () -> assertEquals(1, consumptionRecord.getConsumptionPoints().size())
                ))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}