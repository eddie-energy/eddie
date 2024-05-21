package energy.eddie.admin.console.data;

import energy.eddie.api.v0_82.ConsentMarketDocumentServiceInterface;
import energy.eddie.cim.v0_82.cmd.*;
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

    @Test
    void testReceivesValidConsentMarketDocument_saves() {
        // Given
        TestPublisher<ConsentMarketDocument> testPublisher = TestPublisher.create();
        when(consentMarketDocumentService.getConsentMarketDocumentStream())
                .thenReturn(testPublisher.flux());
        StatusMessageService statusMessageService = new StatusMessageService(statusMessageRepository,
                                                                             consentMarketDocumentService);

        ConsentMarketDocument cmd = new ConsentMarketDocument()
                .withMRID("mrid")
                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                .withPermissionList(new ConsentMarketDocument.PermissionList()
                        .withPermissions(new PermissionComplexType()
                                .withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                        .withMktActivityRecords(new MktActivityRecordComplexType()
                                                .withCreatedDateTime("2021-01-01T00:00:00Z")
                                                .withStatus(StatusTypeList.A05)
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
                        () -> assertEquals("2021-01-01T00:00:00Z", message.getTimestamp()),
                        () -> assertEquals("A05", message.getStatus())
                )));
    }
}
