package energy.eddie.regionconnector.at.eda.provider.agnostic;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.MarketParticipantDirectory;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.ProcessDirectory;
import at.ebutilities.schemata.customerprocesses.masterdata._01p32.MasterData;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.EdaMasterData01p32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@Import(MarshallerConfig.class)
class EdaRawDataProviderTest {
    private final EdaConsumptionRecord mockConsumptionRecord = new SimpleEdaConsumptionRecord()
            .setOriginalConsumptionRecord(
                    new ConsumptionRecord()
                            .withProcessDirectory(new ProcessDirectory())
                            .withMarketParticipantDirectory(new MarketParticipantDirectory())
            );
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private Jaxb2Marshaller marshaller;

    @Test
    void getRawDataStream_whenReceivingIdentifiableConsumptionRecordWithMultiplePermissions_publishesMultipleRawDataMessages() {
        // Given
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        TestPublisher<IdentifiableMasterData> masterDataTestPublisher = TestPublisher.create();
        String expectedString1 = "expectedString1";
        String expectedString2 = "expectedString2";
        String expectedString3 = "expectedString3";
        List<AtPermissionRequest> permissionRequests = List.of(
                createPermissionRequest(expectedString1),
                createPermissionRequest(expectedString2),
                createPermissionRequest(expectedString3)
        );

        var provider = new EdaRawDataProvider(marshaller, testPublisher.flux(), masterDataTestPublisher.flux());

        var source = provider.getRawDataStream();

        // When
        StepVerifier.create(source)
                    .then(() -> {
                        testPublisher.next(new IdentifiableConsumptionRecord(mockConsumptionRecord,
                                                                             permissionRequests,
                                                                             null,
                                                                             null));
                        testPublisher.complete();
                        masterDataTestPublisher.complete();
                    })
                    // Then
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

        // Clean-Up
        provider.close();
    }

    @Test
    void getRawDataStream_whenReceivingIdentifiableMasterData_publishesRawDataMessage() {
        // Given
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        TestPublisher<IdentifiableMasterData> masterDataTestPublisher = TestPublisher.create();
        String expectedString = "expectedString";
        var permissionRequest = createPermissionRequest(expectedString);

        var provider = new EdaRawDataProvider(marshaller, testPublisher.flux(), masterDataTestPublisher.flux());

        var source = provider.getRawDataStream();

        // When
        StepVerifier.create(source)
                    .then(() -> {
                        masterDataTestPublisher.next(
                                new IdentifiableMasterData(
                                        new EdaMasterData01p32(new MasterData()),
                                        permissionRequest
                                )
                        );
                        testPublisher.next(new IdentifiableConsumptionRecord(mockConsumptionRecord,
                                                                             List.of(permissionRequest),
                                                                             null,
                                                                             null));
                        testPublisher.complete();
                        masterDataTestPublisher.complete();
                    })
                    // Then
                    .assertNext(rawData -> {
                        assertEquals(expectedString, rawData.permissionId());
                        assertEquals(expectedString, rawData.connectionId());
                        assertEquals(expectedString, rawData.dataNeedId());
                        assertThat(rawData.rawPayload()).startsWith("<?xml");
                    })
                    .expectNextCount(1)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));

        // Clean-Up
        provider.close();
    }

    private SimplePermissionRequest createPermissionRequest(String expected) {
        return new SimplePermissionRequest(expected, expected, expected);
    }
}
