package energy.eddie.regionconnector.at.eda.processing.agnostic;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ProcessDirectory;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdaRawDataProviderTest {
    @Test
    void givenConsumptionRecord_publishesOnFlux() throws JAXBException {
        // Given
        var permissionId = "foo";
        var connectionId = "bar";
        var dataNeedId = "dataNeedId";
        var conversationId = "SomeConvId";
        var permissionRequest = mock(EdaPermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn(permissionId);
        when(permissionRequest.connectionId()).thenReturn(connectionId);
        when(permissionRequest.dataNeedId()).thenReturn(dataNeedId);
        when(permissionRequest.dataSourceInformation()).thenReturn(new EdaDataSourceInformation("1234"));

        var mockRecord = mock(ConsumptionRecord.class);
        var mockDirectory = mock(ProcessDirectory.class);
        when(mockDirectory.getConversationId()).thenReturn(conversationId);
        when(mockRecord.getProcessDirectory()).thenReturn(mockDirectory);

        var mockAdapter = mock(EdaAdapter.class);
        TestPublisher<ConsumptionRecord> testPublisher = TestPublisher.create();
        when(mockAdapter.getConsumptionRecordStream()).thenReturn(testPublisher.flux());

        var mockService = mock(PermissionRequestService.class);
        when(mockService.findByConversationIdOrCMRequestId(eq(conversationId), any())).thenReturn(Optional.of(permissionRequest));

        var provider = new EdaRawDataProvider(mockAdapter, mockService);


        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(provider.getRawDataStream()))
                .expectSubscription()
                // When
                .then(() -> testPublisher.next(mockRecord))
                // Then
                .assertNext(message -> assertAll(
                        () -> assertEquals(permissionId, message.permissionId()),
                        () -> assertEquals(connectionId, message.connectionId()),
                        () -> assertEquals(dataNeedId, message.dataNeedId()),
                        () -> assertThat(message.rawPayload()).startsWith("<?xml"))
                )
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}