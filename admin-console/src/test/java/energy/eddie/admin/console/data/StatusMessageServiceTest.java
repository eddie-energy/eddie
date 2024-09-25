package energy.eddie.admin.console.data;

import energy.eddie.api.v0_82.PermissionMarketDocumentServiceInterface;
import energy.eddie.cim.v0_82.pmd.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusMessageServiceTest {
    @Mock
    private StatusMessageRepository statusMessageRepository;
    @Mock
    private PermissionMarketDocumentServiceInterface permissionMarketDocumentService;
    private TestPublisher<PermissionEnvelope> testPublisher;

    @BeforeEach
    void setUp() {
        testPublisher = TestPublisher.create();
        when(permissionMarketDocumentService.getPermissionMarketDocumentStream())
                .thenReturn(testPublisher.flux());
        new StatusMessageService(statusMessageRepository, permissionMarketDocumentService);
    }

    @Test
    void testReceivesValidPermissionMarketDocument_saves() {
        // Given
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("mrid")
                                .withReceiverMarketParticipantMRID(new PartyIDStringComplexType()
                                                                           .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                                                                           .withValue("Enedis"))
                                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                                .withPermissionList(new PermissionMarketDocumentComplexType.PermissionList()
                                                            .withPermissions(new PermissionComplexType()
                                                                                     .withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                                                                                                        .withMktActivityRecords(
                                                                                                                                new MktActivityRecordComplexType()
                                                                                                                                        .withCreatedDateTime(
                                                                                                                                                "2021-01-01T00:00:00Z")
                                                                                                                                        .withStatus(
                                                                                                                                                StatusTypeList.A05)
                                                                                                                        )
                                                                                     )
                                                            )
                                )
                );
        // When
        StepVerifier.create(testPublisher)
                    .then(() -> {
                        testPublisher.emit(pmd);
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    // Then
                    .then(() -> verify(statusMessageRepository)
                            .save(assertArg(message -> assertAll(
                                    () -> assertEquals("mrid", message.getPermissionId()),
                                    () -> assertEquals("NFR", message.getCountry()),
                                    () -> assertEquals("Enedis", message.getDso()),
                                    () -> assertEquals("2021-01-01T00:00:00Z", message.getStartDate()),
                                    () -> assertEquals("A05", message.getStatus())
                            ))))
                    .verifyComplete();
    }

    @Test
    void testReceivesAiidaPermissionMarketDocument() {
        // Given
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("mrid")
                                .withReceiverMarketParticipantMRID(new PartyIDStringComplexType()
                                                                           .withCodingScheme(null)
                                                                           .withValue("Aiida"))
                                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                                .withPermissionList(new PermissionMarketDocumentComplexType.PermissionList()
                                                            .withPermissions(new PermissionComplexType()
                                                                                     .withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                                                                                                        .withMktActivityRecords(
                                                                                                                                new MktActivityRecordComplexType()
                                                                                                                                        .withCreatedDateTime(
                                                                                                                                                "2021-01-01T00:00:00Z")
                                                                                                                                        .withStatus(
                                                                                                                                                StatusTypeList.A05)
                                                                                                                        )
                                                                                     )
                                                            )
                                )
                );
        // When
        StepVerifier.create(testPublisher)
                    .then(() -> {
                        testPublisher.emit(pmd);
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    // Then
                    .then(() -> verify(statusMessageRepository)
                            .save(assertArg(message -> assertAll(
                                    () -> assertEquals("mrid", message.getPermissionId()),
                                    () -> assertEquals("Unknown", message.getCountry()),
                                    () -> assertEquals("Aiida", message.getDso()),
                                    () -> assertEquals("2021-01-01T00:00:00Z", message.getStartDate()),
                                    () -> assertEquals("A05", message.getStatus())
                            ))))
                    .verifyComplete();
    }
}