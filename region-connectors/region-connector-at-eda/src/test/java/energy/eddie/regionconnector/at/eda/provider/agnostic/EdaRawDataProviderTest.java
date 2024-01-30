package energy.eddie.regionconnector.at.eda.provider.agnostic;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class EdaRawDataProviderTest {
    private final ConsumptionRecord mockConsumptionRecord = mock(ConsumptionRecord.class);

    @Test
    void getRawDataStream_whenReceivingIdentifiableConsumptionRecordWithMultiplePermissions_publishesMultipleRawDataMessages() throws JAXBException {
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        String expectedString1 = "expectedString1";
        String expectedString2 = "expectedString2";
        String expectedString3 = "expectedString3";
        List<AtPermissionRequest> permissionRequests = List.of(
                createPermissionRequest(expectedString1),
                createPermissionRequest(expectedString2),
                createPermissionRequest(expectedString3)
        );

        try (var provider = new EdaRawDataProvider(testPublisher.flux())) {

            var source = JdkFlowAdapter.flowPublisherToFlux(provider.getRawDataStream());

            StepVerifier.create(source)
                    .then(() -> {
                        testPublisher.next(new IdentifiableConsumptionRecord(mockConsumptionRecord, permissionRequests));
                        testPublisher.complete();
                    })
                    .assertNext(rawData -> {
                        assertEquals(expectedString1, rawData.permissionId());
                        assertEquals(expectedString1, rawData.connectionId());
                        assertEquals(expectedString1, rawData.dataNeedId());
                        assertThat(rawData.rawPayload()).startsWith("<?xml");
                    })
                    .assertNext(rawData -> {
                        assertEquals(expectedString2, rawData.permissionId());
                        assertEquals(expectedString2, rawData.connectionId());
                        assertEquals(expectedString2, rawData.dataNeedId());
                        assertThat(rawData.rawPayload()).startsWith("<?xml");
                    })
                    .assertNext(rawData -> {
                        assertEquals(expectedString3, rawData.permissionId());
                        assertEquals(expectedString3, rawData.connectionId());
                        assertEquals(expectedString3, rawData.dataNeedId());
                        assertThat(rawData.rawPayload()).startsWith("<?xml");
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        }
    }

    private SimplePermissionRequest createPermissionRequest(String expected) {
        return new SimplePermissionRequest(expected, expected, expected);
    }
}