package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtPendingAcknowledgmentPermissionRequestState;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtSentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.at.eda.provider.EdaEddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.regionconnector.at.eda.provider.EdaMvp1ConsumptionRecordProvider;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.services.IdentifiableConsumptionRecordService;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestFulfillmentService;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import energy.eddie.regionconnector.at.eda.services.RevocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EdaRegionConnectorIntegrationTest {
    @Autowired
    private PermissionRequestService requestService;
    @MockBean
    private EdaEddieValidatedHistoricalDataMarketDocumentProvider edaEddieValidatedHistoricalDataMarketDocumentProvider;
    @Autowired
    private AtPermissionRequestRepository repository;
    @Autowired
    private Sinks.Many<ConnectionStatusMessage> messages;
    @MockBean
    private EdaAdapter adapter;
    @MockBean
    private Flux<ConsumptionRecord> consumptionRecordFlux;
    @MockBean
    private RegionConnector ignored;
    @MockBean
    private RevocationService alsoIgnored;
    @MockBean
    private PermissionRequestFulfillmentService alsoAlsoIgnored;
    @MockBean
    private EdaMvp1ConsumptionRecordProvider alsoAlsoAlsoIgnored;
    @MockBean
    private IdentifiableConsumptionRecordService alsoAlsoAlsoAlsoIgnored;

    @Test
    void subscribeToConnectionStatusMessagePublisher_returnsAccepted_onAccepted() throws TransmissionException {
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream())
                .thenReturn(testPublisher.flux());
        EdaRegionConnector rc = new EdaRegionConnector(adapter, requestService, messages);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.meteringPointId()).thenReturn(Optional.of("meteringPointId"));
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        permissionRequest.changeState(new AtSentToPermissionAdministratorPermissionRequestState(permissionRequest));
        repository.save(permissionRequest);

        var source = JdkFlowAdapter.flowPublisherToFlux(((Mvp1ConnectionStatusMessageProvider) rc).getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "", "messageId");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(cmRequestStatus);
                    testPublisher.complete();
                    try {
                        rc.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .assertNext(csm -> assertAll(
                                () -> assertEquals("connectionId", csm.connectionId()),
                                () -> assertEquals("dataNeedId", csm.dataNeedId()),
                                () -> assertEquals(PermissionProcessStatus.ACCEPTED, csm.status())
                        )
                )
                .expectComplete()
                .verify();
    }

    @Test
    void acceptingPermissionRequest_updatesPermissionRequest() throws TransmissionException {
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream())
                .thenReturn(testPublisher.flux());
        EdaRegionConnector rc = new EdaRegionConnector(adapter, requestService, messages);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.meteringPointId()).thenReturn(Optional.of("meteringPointId"));
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        permissionRequest.changeState(new AtSentToPermissionAdministratorPermissionRequestState(permissionRequest));
        repository.save(permissionRequest);

        var source = JdkFlowAdapter.flowPublisherToFlux(((Mvp1ConnectionStatusMessageProvider) rc).getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "", "messageId");
        cmRequestStatus.setCmConsentId("consentId");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(cmRequestStatus);
                    testPublisher.complete();
                    try {
                        rc.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .assertNext(csm -> assertAll(
                                () -> assertTrue(repository.findByConsentId("consentId").isPresent()),
                                () -> assertEquals("permissionId", repository.findByConsentId("consentId").get().permissionId())
                        )
                )
                .expectComplete()
                .verify();
    }

    @Test
    void acceptingPermissionRequest_withoutConsentId_updatesPermissionRequest() throws TransmissionException {
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream())
                .thenReturn(testPublisher.flux());
        EdaRegionConnector rc = new EdaRegionConnector(adapter, requestService, messages);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.meteringPointId()).thenReturn(Optional.of("meteringPointId"));
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        permissionRequest.changeState(new AtSentToPermissionAdministratorPermissionRequestState(permissionRequest));
        repository.save(permissionRequest);

        var source = JdkFlowAdapter.flowPublisherToFlux(((Mvp1ConnectionStatusMessageProvider) rc).getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "", "messageId");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(cmRequestStatus);
                    testPublisher.complete();
                    try {
                        rc.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .assertNext(csm -> assertTrue(permissionRequest.consentId().isEmpty()))
                .expectComplete()
                .verify();
    }

    @Test
    void subscribeToConnectionStatusMessagePublisher_doesNotReturnUnmappedMessages() throws TransmissionException {
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        repository.save(new SimplePermissionRequest("permissionId", "connectionId", "dataNeedId", "test", "test", null));

        var uut = new EdaRegionConnector(adapter, requestService, sink);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());

        var unmapableCMRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "", "");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(unmapableCMRequestStatus);
                    testPublisher.complete();
                    try {
                        uut.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .expectComplete()
                .verify();
    }

    @Test
    void subscribeToConnectionStatusMessagePublisher_returnsInvalid_onError() throws TransmissionException {
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.start()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).minusDays(10));
        var request = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        request.changeState(new AtSentToPermissionAdministratorPermissionRequestState(request));
        repository.save(request);

        var uut = new EdaRegionConnector(adapter, requestService, messages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ERROR, "", "messageId");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(cmRequestStatus);
                    try {
                        uut.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .assertNext(csm -> {
                    assertEquals("connectionId", csm.connectionId());
                    assertEquals("permissionId", csm.permissionId());
                    assertEquals(PermissionProcessStatus.INVALID, csm.status());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void subscribeToConnectionStatusMessagePublisher_returnsInvalid_onErrorWithNoAcknowledgment() throws TransmissionException {
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var request = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        request.changeState(new AtPendingAcknowledgmentPermissionRequestState(request));
        repository.save(request);

        var uut = new EdaRegionConnector(adapter, requestService, messages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ERROR, "", "messageId");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(cmRequestStatus);
                    try {
                        uut.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .assertNext(csm -> {
                    assertEquals("connectionId", csm.connectionId());
                    assertEquals("permissionId", csm.permissionId());
                    assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, csm.status());
                })
                .assertNext(csm -> {
                    assertEquals("connectionId", csm.connectionId());
                    assertEquals("permissionId", csm.permissionId());
                    assertEquals(PermissionProcessStatus.INVALID, csm.status());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void subscribeToConnectionStatusMessagePublisher_returnsRejected_onRejected() throws TransmissionException {
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var request = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        request.changeState(new AtSentToPermissionAdministratorPermissionRequestState(request));
        repository.save(request);

        var uut = new EdaRegionConnector(adapter, requestService, messages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.REJECTED, "", "messageId");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(cmRequestStatus);
                    testPublisher.complete();
                    try {
                        uut.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .assertNext(csm -> {
                    assertEquals("connectionId", csm.connectionId());
                    assertEquals("permissionId", csm.permissionId());
                    assertEquals(PermissionProcessStatus.REJECTED, csm.status());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void subscribeToConnectionStatusMessagePublisher_returnsPendingAcknowledgement_onSentAndOnDelivered() throws TransmissionException {
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var request = new EdaPermissionRequest("connectionId", "permissionId", ccmoRequest, null);
        request.changeState(new AtPendingAcknowledgmentPermissionRequestState(request));
        repository.save(request);

        var uut = new EdaRegionConnector(adapter, requestService, messages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatusSent = new CMRequestStatus(CMRequestStatus.Status.SENT, "", "messageId");
        var cmRequestStatusDelivered = new CMRequestStatus(CMRequestStatus.Status.DELIVERED, "", "messageId");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(cmRequestStatusSent);
                    testPublisher.emit(cmRequestStatusDelivered);
                    testPublisher.complete();
                    try {
                        uut.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .expectComplete() // receiving SENT or DELIVERED should not change the state of the PermissionRequest and thus not emit a ConnectionStatusMessage
                .verify();
    }

    @Test
    void subscribeToConnectionStatusMessagePublisher_returnsSentToPermissionAdmin_onReceived() throws TransmissionException {
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var request = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        request.changeState(new AtPendingAcknowledgmentPermissionRequestState(request));

        repository.save(request);

        var uut = new EdaRegionConnector(adapter, requestService, messages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.RECEIVED, "", "messageId");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(cmRequestStatus);
                    testPublisher.complete();
                    try {
                        uut.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .assertNext(csm -> {
                    assertEquals("connectionId", csm.connectionId());
                    assertEquals("permissionId", csm.permissionId());
                    assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, csm.status());
                })
                .expectComplete()
                .verify();
    }
}