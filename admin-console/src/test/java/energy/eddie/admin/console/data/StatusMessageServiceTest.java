package energy.eddie.admin.console.data;

import energy.eddie.api.v0_82.ConsentMarketDocumentServiceInterface;
import energy.eddie.cim.v0_82.cmd.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusMessageServiceTest {
    @Mock
    private StatusMessageRepository statusMessageRepository;
    @Mock
    private ConsentMarketDocumentServiceInterface consentMarketDocumentService;
    private TestPublisher<ConsentMarketDocument> testPublisher;

    @BeforeEach
    void setUp() {
        testPublisher = TestPublisher.create();
        when(consentMarketDocumentService.getConsentMarketDocumentStream())
                .thenReturn(testPublisher.flux());
        new StatusMessageService(statusMessageRepository, consentMarketDocumentService);
    }

    @Test
    void testReceivesValidConsentMarketDocument_saves() {
        // Given
        ConsentMarketDocument cmd = new ConsentMarketDocument()
                .withMRID("mrid")
                .withReceiverMarketParticipantMRID(new PartyIDStringComplexType()
                                                           .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                                                           .withValue("Enedis"))
                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                .withPermissionList(new ConsentMarketDocument.PermissionList()
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
                );
        // When
        testPublisher.emit(cmd);
        testPublisher.complete();

        // Then
        verify(statusMessageRepository)
                .save(assertArg(message -> assertAll(
                        () -> assertEquals("mrid", message.getPermissionId()),
                        () -> assertEquals("FRANCE_NATIONAL_CODING_SCHEME", message.getCountry()),
                        () -> assertEquals("Enedis", message.getDso()),
                        () -> assertEquals("2021-01-01T00:00:00Z", message.getStartDate()),
                        () -> assertEquals("A05", message.getStatus())
                )));
    }

    @Test
    void testReceivesAiidaConsentMarketDocument() {
        // Given
        ConsentMarketDocument cmd = new ConsentMarketDocument()
                .withMRID("mrid")
                .withReceiverMarketParticipantMRID(new PartyIDStringComplexType()
                                                           .withCodingScheme(null)
                                                           .withValue("Aiida"))
                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                .withPermissionList(new ConsentMarketDocument.PermissionList()
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
                );
        // When
        testPublisher.emit(cmd);

        // Then
        verify(statusMessageRepository)
                .save(assertArg(message -> assertAll(
                        () -> assertEquals("mrid", message.getPermissionId()),
                        () -> assertEquals("Unknown", message.getCountry()),
                        () -> assertEquals("Aiida", message.getDso()),
                        () -> assertEquals("2021-01-01T00:00:00Z", message.getStartDate()),
                        () -> assertEquals("A05", message.getStatus())
                )));
    }
}