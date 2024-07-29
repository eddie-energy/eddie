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

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentifiableMasterDataServiceTest {
    private final Jaxb2Marshaller marshaller = new AtEdaBeanConfig().jaxb2Marshaller();
    @Mock
    AtPermissionRequestRepository repository;
    @Mock
    Outbox outbox;

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void mapsMasterDataAndFulfillsPermissionRequest() throws IOException {
        // Given
        ArgumentCaptor<SimpleEvent> eventCaptor = ArgumentCaptor.forClass(SimpleEvent.class);
        EdaMasterData masterData = masterData();
        when(repository.findByConversationIdAndMeteringPointId(masterData.conversationId(), masterData.meteringPoint()))
                .thenReturn(Optional.of(new SimplePermissionRequest("pmId", "connId", "dataNeedId", "test", "any",
                                                                    PermissionProcessStatus.ACCEPTED)));

        var service = new IdentifiableMasterDataService(repository, outbox);

        // When
        var result = service.mapToIdentifiableMasterData(masterData);

        // Then
        assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(masterData, result.get().masterData()),
                () -> assertEquals("pmId", result.get().permissionRequest().permissionId()),
                () -> assertEquals("connId", result.get().permissionRequest().connectionId())
        );

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
        // Given
        EdaMasterData masterData = masterData();
        when(repository.findByConversationIdAndMeteringPointId(masterData.conversationId(), masterData.meteringPoint()))
                .thenReturn(Optional.empty());

        var service = new IdentifiableMasterDataService(repository, outbox);

        // When
        var result = service.mapToIdentifiableMasterData(masterData);

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(outbox);
    }
}
