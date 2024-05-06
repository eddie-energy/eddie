package energy.eddie.admin.console.data;

import energy.eddie.api.v0_82.ConsentMarketDocumentServiceInterface;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import reactor.core.publisher.Sinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StatusMessageServiceTest {

    @Mock
    private StatusMessageRepository statusMessageRepository;
    private StatusMessageService statusMessageService;
    @Mock
    private Logger logger;

    static ConsentMarketDocumentServiceInterface createService(Sinks.Many<ConsentMarketDocument> sink) {
        return sink::asFlux;
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ConsentMarketDocumentServiceInterface consentMarketDocumentService = createService(Sinks.many().multicast().onBackpressureBuffer());
        statusMessageService = new StatusMessageService(statusMessageRepository, consentMarketDocumentService);
        StatusMessageService.setLogger(logger);
    }

    @AfterEach
    void tearDown() {
        reset(statusMessageRepository, logger);
    }

    @Test
    void shouldInitializeService() {
        assertThat(statusMessageService).isNotNull();
    }

    @Test
    void shouldProcessMessageWhenSubscribedToFlux() {
        // Simulate an error when trying to save the StatusMessage because PermissionList and MktActivityRecords are null in message
        when(statusMessageRepository.save(any(StatusMessage.class)))
                .thenThrow(new RuntimeException("Couldn't save the message"));
        ConsentMarketDocument message = new ConsentMarketDocument();
        message.setMRID("testMRID");
        ConsentMarketDocument.PermissionList permissionList = new ConsentMarketDocument.PermissionList();
        message.setPermissionList(permissionList);
        statusMessageService.processMessage(message);

        verify(logger).error(eq("Error saving status message: {}"), any(), any(Throwable.class));
    }

    @Test
    void shouldLogErrorWhenConsentMarketDocumentServiceIsNull() {
        statusMessageService = new StatusMessageService(statusMessageRepository, null);
        StatusMessageService.setLogger(logger);
        statusMessageService.subscribeToFlux();
        verify(logger).error("ConsentMarketDocumentService is null. Cannot subscribe to Flux.");
    }
}