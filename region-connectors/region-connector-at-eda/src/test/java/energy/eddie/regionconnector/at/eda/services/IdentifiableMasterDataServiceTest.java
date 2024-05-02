package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.AtEdaBeanConfig;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.EdaMasterData01p32InboundMessageFactory;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentifiableMasterDataServiceTest {
    private final Jaxb2Marshaller marshaller = new AtEdaBeanConfig().jaxb2Marshaller();
    @Mock
    AtPermissionRequestRepository repository;
    @Mock
    Outbox outbox;

    @Test
    void mapsMasterDataAndFulfillsPermissionRequest() throws IOException {
        EdaMasterData masterData = masterData();
        Sinks.Many<EdaMasterData> masterDataSink = Sinks.many().unicast().onBackpressureBuffer();
        when(repository.findByConversationIdOrCMRequestId(masterData.conversationId(), null))
                .thenReturn(Optional.of(new SimplePermissionRequest("pmId", "connId", "dataNeedId", "test", "any",
                                                                    PermissionProcessStatus.ACCEPTED)));

        var service = new IdentifiableMasterDataService(masterDataSink.asFlux(), repository, outbox);

        StepVerifier.create(service.getIdentifiableMasterDataStream())
                    .then(() -> masterDataSink.tryEmitNext(masterData))
                    .assertNext(identifiableMasterData -> assertAll(
                            () -> assertEquals(masterData, identifiableMasterData.masterData()),
                            () -> assertEquals("pmId", identifiableMasterData.permissionRequest().permissionId()),
                            () -> assertEquals("connId", identifiableMasterData.permissionRequest().connectionId())

                    ))
                    .then(masterDataSink::tryEmitComplete)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        ArgumentCaptor<SimpleEvent> eventCaptor = ArgumentCaptor.forClass(SimpleEvent.class);
        verify(outbox).commit(eventCaptor.capture());
        SimpleEvent event = eventCaptor.getValue();
        assertAll(
                () -> assertEquals("pmId", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.FULFILLED, event.status())
        );
    }

    private EdaMasterData masterData() throws IOException {
        try (var inputStream = IdentifiableMasterDataServiceTest.class.getClassLoader()
                                                                      .getResourceAsStream(
                                                                              "xsd/masterdata/_01p32/masterdata.xml")) {
            var factory = new EdaMasterData01p32InboundMessageFactory(marshaller);
            return factory.parseInputStream(inputStream);
        }
    }

    @Test
    void noPermissionRequestFound_doesNothing() throws IOException {
        EdaMasterData masterData = masterData();
        Sinks.Many<EdaMasterData> masterDataSink = Sinks.many().unicast().onBackpressureBuffer();
        when(repository.findByConversationIdOrCMRequestId(masterData.conversationId(), null))
                .thenReturn(Optional.empty());

        var service = new IdentifiableMasterDataService(masterDataSink.asFlux(), repository, outbox);

        StepVerifier.create(service.getIdentifiableMasterDataStream())
                    .then(() -> masterDataSink.tryEmitNext(masterData))
                    .expectNextCount(0)
                    .then(masterDataSink::tryEmitComplete)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verifyNoInteractions(outbox);
    }
}
