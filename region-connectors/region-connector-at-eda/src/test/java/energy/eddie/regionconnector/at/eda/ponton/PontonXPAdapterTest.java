package energy.eddie.regionconnector.at.eda.ponton;

import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.domainvalues.*;
import de.ponton.xp.adapter.api.domainvalues.internal.StatusMetaDataImpl;
import de.ponton.xp.adapter.api.messages.OutboundMessageStatusUpdate;
import de.ponton.xp.adapter.api.messages.internal.OutboundMessageStatusUpdateImpl;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.at.eda.dto.masterdata.*;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.ponton.messenger.HealthCheck;
import energy.eddie.regionconnector.at.eda.ponton.messenger.MessengerStatus;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import energy.eddie.regionconnector.at.eda.ponton.messenger.PontonMessengerConnectionTestImpl;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.services.IdentifiableConsumptionRecordService;
import energy.eddie.regionconnector.at.eda.services.IdentifiableMasterDataService;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PontonXPAdapterTest {

    @Mock
    private CCMORequest ccmoRequest;
    @Mock
    private CCMORevoke ccmoRevoke;

    @Spy
    private PontonMessengerConnectionTestImpl pontonMessengerConnection = new PontonMessengerConnectionTestImpl();

    @Mock
    private IdentifiableConsumptionRecordService identifiableConsumptionRecordService;
    @Mock
    private IdentifiableMasterDataService identifiableMasterDataService;
    @Mock
    @SuppressWarnings("unused")
    private TaskScheduler taskScheduler;

    @InjectMocks
    private PontonXPAdapter pontonXPAdapter;

    private static Stream<Arguments> outboundMessageStatusUpdateStream() {
        StatusMetaData metaData = StatusMetaDataImpl
                .newBuilder()
                .setConversationId(new ConversationId("conversationId"))
                .build();
        var builder = OutboundMessageStatusUpdateImpl
                .newBuilder()
                .setStatusMetaData(metaData)
                .setTransferId(new TransferId("transferId"));
        return Stream.of(
                Arguments.of(builder.setResult(OutboundStatusEnum.CONTENT_ERROR).build(),
                             NotificationMessageType.PONTON_ERROR),
                Arguments.of(builder.setResult(OutboundStatusEnum.CONFIG_ERROR).build(),
                             NotificationMessageType.PONTON_ERROR),
                Arguments.of(builder.setResult(OutboundStatusEnum.CONTENT_ERROR).build(),
                             NotificationMessageType.PONTON_ERROR),
                Arguments.of(builder.setResult(OutboundStatusEnum.FAILED).build(),
                             NotificationMessageType.PONTON_ERROR),
                Arguments.of(builder.setResult(OutboundStatusEnum.INFO).build(), null),
                Arguments.of(builder.setResult(OutboundStatusEnum.SUCCESS).build(), null),
                Arguments.of(builder.setResult(OutboundStatusEnum.PROCESSED_AND_QUEUED).build(), null)
        );
    }

    @Test
    void sendCMRequest_callsPontonMessengerConnection() throws TransmissionException, de.ponton.xp.adapter.api.TransmissionException, ConnectionException {
        // When
        pontonXPAdapter.sendCMRequest(ccmoRequest);
        pontonXPAdapter.close();

        // Then
        verify(pontonMessengerConnection).sendCMRequest(ccmoRequest);
    }

    @Test
    void sendCMRequest_PontonMessengerConnectionThrowsTransmissionException() {
        // Given
        pontonMessengerConnection.setThrowTransmissionException(true);

        // When & Then
        assertThrows(TransmissionException.class, () -> pontonXPAdapter.sendCMRequest(ccmoRequest));
        pontonXPAdapter.close();
    }

    @Test
    void sendCMRequest_PontonMessengerConnectionThrowsConnectionException() {
        // Given
        pontonMessengerConnection.setThrowConnectionException(true);

        // When & Then
        assertThrows(TransmissionException.class, () -> pontonXPAdapter.sendCMRequest(ccmoRequest));
        pontonXPAdapter.close();
    }

    @Test
    void sendCMRevoke_callsPontonMessengerConnection() throws TransmissionException, de.ponton.xp.adapter.api.TransmissionException, ConnectionException {
        // Given
        setupCCMORevoke();

        // When
        pontonXPAdapter.sendCMRevoke(ccmoRevoke);
        pontonXPAdapter.close();

        // Then
        verify(pontonMessengerConnection).sendCMRevoke(ccmoRevoke);
    }

    private void setupCCMORevoke() {
        when(ccmoRevoke.permissionRequest()).thenReturn(new SimplePermissionRequest("pid",
                                                                                    "cmRequestId",
                                                                                    "conversationId"));
    }

    @Test
    void sendCMRevoke_PontonMessengerConnectionThrowsTransmissionException() {
        // Given
        setupCCMORevoke();
        pontonMessengerConnection.setThrowTransmissionException(true);

        // When & Then
        assertThrows(TransmissionException.class, () -> pontonXPAdapter.sendCMRevoke(ccmoRevoke));
        pontonXPAdapter.close();
    }

    @Test
    void sendCMRevoke_PontonMessengerConnectionThrowsConnectionException() {
        // Given
        setupCCMORevoke();
        pontonMessengerConnection.setThrowConnectionException(true);

        // When & Then
        assertThrows(TransmissionException.class, () -> pontonXPAdapter.sendCMRevoke(ccmoRevoke));
        pontonXPAdapter.close();
    }

    @ParameterizedTest
    @EnumSource(NotificationMessageType.class)
    void handleCMNotificationMessage_whenPontonMessengerConnectionCallsCMNotificationHandler_withoutResponseData_emitsSuccess(
            NotificationMessageType notificationMessageType
    ) {
        // Given
        EdaCMNotification edaCMNotification = createEdaCMNotification(List.of());

        // When
        var stepVerifier = StepVerifier
                .create(pontonXPAdapter.getCMRequestStatusStream())
                .expectComplete();

        var messageResult = pontonMessengerConnection.cmNotificationHandler
                .handle(edaCMNotification, notificationMessageType);

        // Then
        assertAll(
                () -> assertEquals(InboundStatusEnum.SUCCESS, messageResult.status()),
                () -> assertEquals("EdaCMNotification with empty response data received.",
                                   messageResult.statusMessage())
        );
        pontonXPAdapter.close();
        stepVerifier.verify();
    }

    private static EdaCMNotification createEdaCMNotification(List<ResponseData> responseData) {
        return new EdaCMNotification() {
            @Override
            public String conversationId() {
                return "conversationId";
            }

            @Override
            public String cmRequestId() {
                return "cmRequestId";
            }

            @Override
            public List<ResponseData> responseData() {
                return responseData;
            }
        };
    }

    @Test
    void handleConsumptionRecordMessage_whenPontonMessengerConnectionCallsConsumptionRecordHandler_emitsRecord() {
        // Given
        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord();
        var identifiedConsumptionRecord = new IdentifiableConsumptionRecord(consumptionRecord, List.of(), null, null);
        when(identifiableConsumptionRecordService.mapToIdentifiableConsumptionRecord(consumptionRecord))
                .thenReturn(Optional.of(identifiedConsumptionRecord));


        // When
        var stepVerifier = StepVerifier
                .create(pontonXPAdapter.getConsumptionRecordStream())
                .expectNext(identifiedConsumptionRecord)
                .expectComplete();

        var messageResult = pontonMessengerConnection.consumptionRecordHandler
                .handle(consumptionRecord);

        // Then
        assertAll(
                () -> assertEquals(InboundStatusEnum.SUCCESS, messageResult.status()),
                () -> assertEquals("Successfully emitted object to backend.", messageResult.statusMessage())
        );
        pontonXPAdapter.close();
        stepVerifier.verify();
    }

    @Test
    void handleConsumptionRecordMessage_whenIdentifiableConsumptionRecordServiceCantMapRecord_returnsReject() {
        // Given
        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord().setDocumentCreationDateTime(
                ZonedDateTime.now(ZoneOffset.UTC));
        when(identifiableConsumptionRecordService.mapToIdentifiableConsumptionRecord(consumptionRecord))
                .thenReturn(Optional.empty());


        // When
        var messageResult = pontonMessengerConnection.consumptionRecordHandler
                .handle(consumptionRecord);

        // Then
        assertEquals(InboundStatusEnum.REJECTED, messageResult.status());
    }

    @Test
    void handleMasterDataMessage_whenPontonMessengerConnectionCallsMasterDataHandler_emitsMasterData() {
        // Given
        EdaMasterData masterData = masterData();
        var identifiedMasterData = new IdentifiableMasterData(masterData, null);
        when(identifiableMasterDataService.mapToIdentifiableMasterData(masterData))
                .thenReturn(Optional.of(identifiedMasterData));

        // When
        var stepVerifier = StepVerifier
                .create(pontonXPAdapter.getMasterDataStream())
                .expectNext(identifiedMasterData)
                .expectComplete();

        var messageResult = pontonMessengerConnection.masterDataHandler
                .handle(masterData);

        // Then
        assertAll(
                () -> assertEquals(InboundStatusEnum.SUCCESS, messageResult.status()),
                () -> assertEquals("Successfully emitted object to backend.", messageResult.statusMessage())
        );
        pontonXPAdapter.close();
        stepVerifier.verify();
    }

    private static EdaMasterData masterData() {
        return new EdaMasterData() {
            @Override
            public String conversationId() {
                return null;
            }

            @Override
            public String messageId() {
                return "";
            }

            @Override
            public Sector sector() {
                return null;
            }

            @Override
            public ZonedDateTime documentCreationDateTime() {
                return ZonedDateTime.now(ZoneOffset.UTC);
            }

            @Override
            public String senderMessageAddress() {
                return "";
            }

            @Override
            public String receiverMessageAddress() {
                return "";
            }

            @Override
            public String meteringPoint() {
                return null;
            }

            @Override
            public MeteringPointData meteringPointData() {
                return null;
            }

            @Override
            public Optional<BillingData> billingData() {
                return Optional.empty();
            }

            @Override
            public Optional<ContractPartner> contractPartner() {
                return Optional.empty();
            }

            @Override
            public Optional<DeliveryAddress> installationAddress() {
                return Optional.empty();
            }

            @Override
            public Optional<InvoiceRecipient> invoiceRecipient() {
                return Optional.empty();
            }

            @Override
            public Object originalMasterData() {
                return null;
            }
        };
    }

    @Test
    void handleMasterDataMessage_whenPontonMessengerConnectionCallsMasterDataHandler_whenAdapterClosed_returnsREJECTED() {
        // Given
        EdaMasterData masterData = masterData();
        when(identifiableMasterDataService.mapToIdentifiableMasterData(masterData))
                .thenReturn(Optional.of(new IdentifiableMasterData(masterData, null)));

        // When
        pontonXPAdapter.close();
        var messageResult = pontonMessengerConnection.masterDataHandler
                .handle(masterData);

        // Then
        assertEquals(InboundStatusEnum.REJECTED, messageResult.status());
    }

    @Test
    void handleMasterDataMessage_whenIdentifiableMasterDataServiceCantMapMasterData_returnsReject() {
        // Given
        EdaMasterData masterData = masterData();
        when(identifiableMasterDataService.mapToIdentifiableMasterData(masterData))
                .thenReturn(Optional.empty());

        // When
        pontonXPAdapter.close();
        var messageResult = pontonMessengerConnection.masterDataHandler
                .handle(masterData);

        // Then
        assertEquals(InboundStatusEnum.REJECTED, messageResult.status());
    }

    @Test
    void handleMasterDataMessage_whenPontonMessengerConnectionCallsMasterDataHandler_whenBufferOverflows_returnsTEMPORARRY_ERROR() {
        // Given
        EdaMasterData masterData = masterData();
        when(identifiableMasterDataService.mapToIdentifiableMasterData(masterData))
                .thenReturn(Optional.of(new IdentifiableMasterData(masterData, null)));

        // When
        var messageResult = pontonMessengerConnection.masterDataHandler.handle(masterData);
        while (messageResult.status() == InboundStatusEnum.SUCCESS) {
            messageResult = pontonMessengerConnection.masterDataHandler.handle(masterData); // Overflow the buffer
        }

        // Then
        assertEquals(InboundStatusEnum.TEMPORARY_ERROR, messageResult.status());
        pontonXPAdapter.close();
    }

    @Test
    void handleRevokeMessage_whenPontonMessengerConnectionCallsCmRevokeHandler_emitsRevoke() {
        // Given
        EdaCMRevoke cmRevoke = new SimpleEdaCMRevoke();

        // When
        var stepVerifier = StepVerifier
                .create(pontonXPAdapter.getCMRevokeStream())
                .expectNext(cmRevoke)
                .expectComplete();

        var messageResult = pontonMessengerConnection.cmRevokeHandler
                .handle(cmRevoke);

        // Then
        assertAll(
                () -> assertEquals(InboundStatusEnum.SUCCESS, messageResult.status()),
                () -> assertEquals("Successfully emitted object to backend.", messageResult.statusMessage())
        );
        pontonXPAdapter.close();
        stepVerifier.verify();
    }

    @ParameterizedTest
    @MethodSource("outboundMessageStatusUpdateStream")
    void handleCMNotificationMessage_whenPontonMessengerConnectionCallsCMNotificationHandler_withoutResponseData_emitsSuccess(
            OutboundMessageStatusUpdate outboundMessageStatusUpdate,
            @Nullable NotificationMessageType expectedMessageType
    ) {
        // When
        StepVerifier.Step<CMRequestStatus> stepVerifier = StepVerifier
                .create(pontonXPAdapter.getCMRequestStatusStream());

        if (expectedMessageType != null) {
            stepVerifier = stepVerifier
                    .assertNext(cmRequestStatus -> {
                        // Then
                        assertEquals(expectedMessageType, cmRequestStatus.messageType());
                    });
        }


        pontonMessengerConnection.outboundMessageStatusUpdateHandler
                .onOutboundMessageStatusUpdate(outboundMessageStatusUpdate);

        // Then
        pontonXPAdapter.close();
        stepVerifier.expectComplete().verify();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @ParameterizedTest
    @EnumSource(NotificationMessageType.class)
    void handleCMNotificationMessage_whenPontonMessengerConnectionCallsCMNotificationHandler_EmitsCMRequestStatus(
            NotificationMessageType notificationMessageType
    ) {
        // Given
        EdaCMNotification edaCMNotification = createEdaCMNotification(List.of(
                createResponseData("consentId"),
                createResponseData("consentId2")
        ));

        // When
        var stepVerifier = StepVerifier
                .create(pontonXPAdapter.getCMRequestStatusStream())
                .assertNext(cmRequestStatus -> {
                    // Then
                    assertAll(
                            () -> assertEquals(notificationMessageType, cmRequestStatus.messageType()),
                            () -> assertThat(cmRequestStatus.consentData()
                                                            .stream()
                                                            .map(ConsentData::cmConsentId)
                                                            .map(Optional::get)
                                                            .toList(), containsInAnyOrder("consentId", "consentId2"))
                    );
                })
                .expectComplete();

        var messageResult = pontonMessengerConnection.cmNotificationHandler
                .handle(edaCMNotification, notificationMessageType);

        // Then
        assertAll(
                () -> assertEquals(InboundStatusEnum.SUCCESS, messageResult.status()),
                () -> assertEquals("Successfully emitted object to backend.",
                                   messageResult.statusMessage())
        );
        pontonXPAdapter.close();
        stepVerifier.verify();
    }

    private ResponseData createResponseData(String consentId) {
        return new SimpleResponseData(consentId, null, List.of(1));
    }

    @Test
    void start_callsPontonMessengerConnectionStart() throws TransmissionException, de.ponton.xp.adapter.api.TransmissionException {
        // When
        pontonXPAdapter.start();

        // Then
        verify(pontonMessengerConnection).start();
        pontonXPAdapter.close();
    }

    @Test
    void health() {
        // Given
        pontonMessengerConnection.setMessengerStatus(new MessengerStatus(
                Map.of(
                        "xxx", new HealthCheck("xxx", true, "xxx"),
                        "yyy", new HealthCheck("yyy", false, "yyy")

                ),
                false
        ));

        // When
        var health = pontonXPAdapter.health();

        // Then
        assertAll(
                () -> assertEquals(3, health.size()),
                () -> assertEquals(HealthState.DOWN, health.get("pontonHost")),
                () -> assertEquals(HealthState.UP, health.get("pontonHost.xxx")),
                () -> assertEquals(HealthState.DOWN, health.get("pontonHost.yyy"))
        );
        pontonXPAdapter.close();
    }

    @Test
    void start_throwsTransmissionException() {
        // Given
        pontonMessengerConnection.setThrowTransmissionException(true);

        // When & Then
        assertThrows(TransmissionException.class, pontonXPAdapter::start);
        pontonXPAdapter.close();
    }
}
