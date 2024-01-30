package energy.eddie.regionconnector.at.eda.provider.v0;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.processing.mvp1.Mvp1ConsumptionRecordMapper;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdaMvp1ConsumptionRecordProviderTest {
    private final ConsumptionRecord mockConsumptionRecord = mock(ConsumptionRecord.class);

    @Test
    void getConsumptionRecordStream_whenReceivingIdentifiableConsumptionRecordWithMultiplePermissions_publishesMultipleMvp1ConsumptionRecords() throws Exception {
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        String expectedString1 = "expectedString1";
        String expectedString2 = "expectedString2";
        String expectedString3 = "expectedString3";
        List<AtPermissionRequest> permissionRequests = List.of(
                createPermissionRequest(expectedString1),
                createPermissionRequest(expectedString2),
                createPermissionRequest(expectedString3)
        );
        Mvp1ConsumptionRecordMapper mockConsumptionRecordMapper = mock(Mvp1ConsumptionRecordMapper.class);
        when(mockConsumptionRecordMapper.mapToMvp1ConsumptionRecord(mockConsumptionRecord)).thenReturn(new energy.eddie.api.v0.ConsumptionRecord());

        try (EdaMvp1ConsumptionRecordProvider edaMvp1ConsumptionRecordProvider = new EdaMvp1ConsumptionRecordProvider(mockConsumptionRecordMapper, testPublisher.flux())) {

            var source = JdkFlowAdapter.flowPublisherToFlux(edaMvp1ConsumptionRecordProvider.getConsumptionRecordStream());

            StepVerifier.create(source)
                    .then(() -> {
                        testPublisher.next(new IdentifiableConsumptionRecord(mockConsumptionRecord, permissionRequests));
                        testPublisher.complete();
                    })
                    .assertNext(consumptionRecord -> {
                        assertEquals(expectedString1, consumptionRecord.getPermissionId());
                        assertEquals(expectedString1, consumptionRecord.getConnectionId());
                        assertEquals(expectedString1, consumptionRecord.getDataNeedId());
                    })
                    .assertNext(consumptionRecord -> {
                        assertEquals(expectedString2, consumptionRecord.getPermissionId());
                        assertEquals(expectedString2, consumptionRecord.getConnectionId());
                        assertEquals(expectedString2, consumptionRecord.getDataNeedId());
                    })
                    .assertNext(consumptionRecord -> {
                        assertEquals(expectedString3, consumptionRecord.getPermissionId());
                        assertEquals(expectedString3, consumptionRecord.getConnectionId());
                        assertEquals(expectedString3, consumptionRecord.getDataNeedId());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        }
    }

    @Test
    void getConsumptionRecordStream_whenMapperThrows_producesNothing() throws Exception {
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        String expectedString1 = "expectedString1";
        String expectedString2 = "expectedString2";
        String expectedString3 = "expectedString3";
        List<AtPermissionRequest> permissionRequests = List.of(
                createPermissionRequest(expectedString1),
                createPermissionRequest(expectedString2),
                createPermissionRequest(expectedString3)
        );
        Mvp1ConsumptionRecordMapper mockConsumptionRecordMapper = mock(Mvp1ConsumptionRecordMapper.class);
        when(mockConsumptionRecordMapper.mapToMvp1ConsumptionRecord(mockConsumptionRecord)).thenThrow(new InvalidMappingException(""));

        try (EdaMvp1ConsumptionRecordProvider edaMvp1ConsumptionRecordProvider = new EdaMvp1ConsumptionRecordProvider(mockConsumptionRecordMapper, testPublisher.flux())) {

            var source = JdkFlowAdapter.flowPublisherToFlux(edaMvp1ConsumptionRecordProvider.getConsumptionRecordStream());

            StepVerifier.create(source)
                    .then(() -> testPublisher.next(new IdentifiableConsumptionRecord(mockConsumptionRecord, permissionRequests)))
                    .expectNextCount(0)
                    .thenCancel()
                    .verify(Duration.ofSeconds(2));
        }
    }

    private SimplePermissionRequest createPermissionRequest(String expected) {
        return new SimplePermissionRequest(expected, expected, expected);
    }
}