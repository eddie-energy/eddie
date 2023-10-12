package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.*;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequestAdapter;
import energy.eddie.regionconnector.at.eda.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtPendingAcknowledgmentPermissionRequestState;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtSentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.xml.builders.helper.DateTimeConverter;
import energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdaRegionConnectorTest {

    @Test
    void connectorThrows_ifConfigurationNull() {
        // given
        var adapter = mock(EdaAdapter.class);
        var repo = new InMemoryPermissionRequestRepository();
        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(null, adapter, repo));
    }

    @Test
    void connectorThrows_ifEdaAdapterNull() {
        // given
        var config = mock(AtConfiguration.class);
        var repo = new InMemoryPermissionRequestRepository();

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(config, null, repo));
    }

    @Test
    void connectorThrows_ifPermissionRequestRepoNull() {
        // given
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(config, adapter, null));
    }

    @Test
    void connectorConstructs() {
        // given
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var repo = new InMemoryPermissionRequestRepository();

        // when
        // then
        assertDoesNotThrow(() -> new EdaRegionConnector(config, adapter, repo));
    }

    @Test
    void subscribeToConsumptionRecordPublisher_doesNotThrow() throws TransmissionException {
        // given
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        when(adapter.getConsumptionRecordStream()).thenReturn(Flux.empty());
        var repo = new InMemoryPermissionRequestRepository();
        var connector = new EdaRegionConnector(config, adapter, repo);

        // when
        // then
        assertDoesNotThrow(connector::getConsumptionRecordStream);
    }

    @Test
    void subscribeToConsumptionRecordPublisher_returnsCorrectlyMappedRecords() throws TransmissionException {
        var config = mock(AtConfiguration.class);
        var consumptionRecord = createConsumptionRecord();
        consumptionRecord.getProcessDirectory().setConversationId("any1");

        var consumptionRecord2 = createConsumptionRecord();
        consumptionRecord2.getProcessDirectory().setConversationId("any2");

        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        TestPublisher<ConsumptionRecord> testPublisher = TestPublisher.create();
        when(adapter.getConsumptionRecordStream()).thenReturn(testPublisher.flux());

        var repo = new InMemoryPermissionRequestRepository();
        repo.save(new SimplePermissionRequest("pmId1", "connId1", "dataNeedId1", "test1", "any1", null));
        repo.save(new SimplePermissionRequest("pmId2", "connId2", "dataNeedId2", "test2", "any2", null));

        var uut = new EdaRegionConnector(config, adapter, repo);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConsumptionRecordStream());

        var unmapableConsumptionRecord = createConsumptionRecord();
        unmapableConsumptionRecord.getProcessDirectory().getEnergy().clear();

        StepVerifier.create(source)
                .then(() -> testPublisher.emit(unmapableConsumptionRecord, consumptionRecord, consumptionRecord2))
                .assertNext(csm -> {
                    assertEquals("connId1", csm.getConnectionId());
                    assertEquals("pmId1", csm.getPermissionId());
                    assertEquals("dataNeedId1", csm.getDataNeedId());
                })
                .assertNext(csm -> {
                    assertEquals("connId2", csm.getConnectionId());
                    assertEquals("pmId2", csm.getPermissionId());
                    assertEquals("dataNeedId2", csm.getDataNeedId());
                })
                .expectComplete().verify();
    }


    @Test
    void subscribeToConnectionStatusMessagePublisher_returnsAccepted_onAccepted() throws TransmissionException {
        var config = mock(AtConfiguration.class);

        var adapter = mock(EdaAdapter.class);
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());


        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var permissionRequest = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        permissionRequest.changeState(new AtSentToPermissionAdministratorPermissionRequestState(permissionRequest));

        var repo = new InMemoryPermissionRequestRepository();
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().multicast().onBackpressureBuffer();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(adapter, permissionStateMessages, repo);

        // wrap the request in a MessagingPermissionRequest to emit messages to the sink
        var messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);
        repo.save(new EdaPermissionRequestAdapter(permissionRequest, messagingPermissionRequest));

        var uut = new EdaRegionConnector(config, adapter, repo, permissionRequestFactory, permissionStateMessages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "", "messageId");

        StepVerifier.create(source)
                .assertNext(csm -> {
                    // Creating the MessagingPermissionRequest adds a message to the sink
                    assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, csm.status());
                })
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
                    assertEquals(PermissionProcessStatus.ACCEPTED, csm.status());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void subscribeToConnectionStatusMessagePublisher_doesNotReturnUnmappedMessages() throws TransmissionException {
        var config = mock(AtConfiguration.class);

        var adapter = mock(EdaAdapter.class);
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        var repo = new InMemoryPermissionRequestRepository();
        repo.save(new SimplePermissionRequest("permissionId", "connectionId", "dataNeedId", "test", "test", null));

        var uut = new EdaRegionConnector(config, adapter, repo);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());

        var unmapableCMRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "", "");

        StepVerifier.create(source)
                .then(() -> {
                    testPublisher.emit(unmapableCMRequestStatus);
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
        var config = mock(AtConfiguration.class);

        var adapter = mock(EdaAdapter.class);
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var request = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        request.changeState(new AtSentToPermissionAdministratorPermissionRequestState(request));

        var repo = new InMemoryPermissionRequestRepository();
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().multicast().onBackpressureBuffer();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(adapter, permissionStateMessages, repo);

        // wrap the request in a MessagingPermissionRequest to emit messages to the sink
        var messagingPermissionRequest = new MessagingPermissionRequest<>(request, permissionStateMessages);
        repo.save(new EdaPermissionRequestAdapter(request, messagingPermissionRequest));

        var uut = new EdaRegionConnector(config, adapter, repo, permissionRequestFactory, permissionStateMessages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ERROR, "", "messageId");

        StepVerifier.create(source)
                .assertNext(csm -> {
                    // Creating the MessagingPermissionRequest adds a message to the sink
                    assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, csm.status());
                })
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
    void subscribeToConnectionStatusMessagePublisher_returnsRejected_onRejected() throws TransmissionException {
        var config = mock(AtConfiguration.class);

        var adapter = mock(EdaAdapter.class);
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var request = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        request.changeState(new AtSentToPermissionAdministratorPermissionRequestState(request));

        var repo = new InMemoryPermissionRequestRepository();
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().multicast().onBackpressureBuffer();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(adapter, permissionStateMessages, repo);

        // wrap the request in a MessagingPermissionRequest to emit messages to the sink
        var messagingPermissionRequest = new MessagingPermissionRequest<>(request, permissionStateMessages);
        repo.save(new EdaPermissionRequestAdapter(request, messagingPermissionRequest));

        var uut = new EdaRegionConnector(config, adapter, repo, permissionRequestFactory, permissionStateMessages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.REJECTED, "", "messageId");

        StepVerifier.create(source)
                .assertNext(csm -> {
                    // Creating the MessagingPermissionRequest adds a message to the sink
                    assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, csm.status());
                })
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
                    assertEquals(PermissionProcessStatus.REJECTED, csm.status());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void subscribeToConnectionStatusMessagePublisher_returnsPendingAcknowledgement_onSentAndonDelivered() throws TransmissionException {
        var config = mock(AtConfiguration.class);

        var adapter = mock(EdaAdapter.class);
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var request = new EdaPermissionRequest("connectionId", "permissionId", ccmoRequest, null);
        request.changeState(new AtPendingAcknowledgmentPermissionRequestState(request));

        var repo = new InMemoryPermissionRequestRepository();
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().multicast().onBackpressureBuffer();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(adapter, permissionStateMessages, repo);

        // wrap the request in a MessagingPermissionRequest to emit messages to the sink
        var messagingPermissionRequest = new MessagingPermissionRequest<>(request, permissionStateMessages);
        repo.save(new EdaPermissionRequestAdapter(request, messagingPermissionRequest));

        var uut = new EdaRegionConnector(config, adapter, repo, permissionRequestFactory, permissionStateMessages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatusSent = new CMRequestStatus(CMRequestStatus.Status.SENT, "", "messageId");
        var cmRequestStatusDelivered = new CMRequestStatus(CMRequestStatus.Status.DELIVERED, "", "messageId");

        StepVerifier.create(source)
                .assertNext(csm -> {
                    // Creating the MessagingPermissionRequest adds a message to the sink
                    assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT, csm.status());
                })
                .then(() -> {
                    testPublisher.emit(cmRequestStatusSent);
                    testPublisher.emit(cmRequestStatusDelivered);
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
        var config = mock(AtConfiguration.class);

        var adapter = mock(EdaAdapter.class);
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.create();
        when(adapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());

        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var request = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        request.changeState(new AtPendingAcknowledgmentPermissionRequestState(request));

        var repo = new InMemoryPermissionRequestRepository();
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().multicast().onBackpressureBuffer();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(adapter, permissionStateMessages, repo);

        // wrap the request in a MessagingPermissionRequest to emit messages to the sink
        var messagingPermissionRequest = new MessagingPermissionRequest<>(request, permissionStateMessages);
        repo.save(new EdaPermissionRequestAdapter(request, messagingPermissionRequest));

        var uut = new EdaRegionConnector(config, adapter, repo, permissionRequestFactory, permissionStateMessages);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConnectionStatusMessageStream());
        var cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.RECEIVED, "", "messageId");

        StepVerifier.create(source)
                .assertNext(csm -> {
                    // Creating the MessagingPermissionRequest adds a message to the sink
                    assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT, csm.status());
                })
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
                .expectComplete()
                .verify();
    }

    @Test
    void terminateNonExistingPermission_throwsIllegalStateException() throws TransmissionException {
        // given
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var repo = new InMemoryPermissionRequestRepository();
        var connector = new EdaRegionConnector(config, adapter, repo);

        // when
        // then
        assertThrows(IllegalStateException.class, () -> connector.terminatePermission("permissionId"));
    }

    @Test
    void getMetadata_returnExpectedMetadata() throws TransmissionException {
        // given
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var repo = new InMemoryPermissionRequestRepository();
        var connector = new EdaRegionConnector(config, adapter, repo);

        // when
        var result = connector.getMetadata();

        // then
        assertEquals("at", result.countryCode());
        assertEquals("at-eda", result.mdaCode());
        assertEquals("Austria EDA", result.mdaDisplayName());
        assertEquals(5977915, result.coveredMeteringPoints());
        assertEquals("/region-connectors/at-eda/", result.urlPath());
    }

    @Test
    void close_ClosesRelatedResources() throws Exception {
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var repo = new InMemoryPermissionRequestRepository();

        var connector = new EdaRegionConnector(config, adapter, repo);

        connector.close();

        verify(adapter).close();
    }

    @Test
    void health_returnsHealthChecks() throws TransmissionException {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        var config = mock(AtConfiguration.class);
        var repo = new InMemoryPermissionRequestRepository();
        when(edaAdapter.health()).thenReturn(Map.of("service", HealthState.UP));
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var rc = new EdaRegionConnector(config, edaAdapter, repo);

        // When
        var res = rc.health();

        // Then
        assertEquals(Map.of("service", HealthState.UP), res);
    }

    private ConsumptionRecord createConsumptionRecord() {
        var meteringPoint = "meteringPoint";
        var meteringType = "L1";
        var consumptionValue = 10;
        var meteringInterval = MeteringIntervall.QH;
        var unit = UOMType.KWH;
        return createConsumptionRecord(meteringPoint, meteringType, ZonedDateTime.now(ZoneOffset.UTC), meteringInterval, consumptionValue, unit);
    }

    private ConsumptionRecord createConsumptionRecord(String meteringPoint, String meteringType, ZonedDateTime meteringPeriodStart, MeteringIntervall meteringIntervall, double consumptionValue, UOMType unit) {
        var edaCR = new ConsumptionRecord();
        ProcessDirectory processDirectory = new ProcessDirectory();
        edaCR.setProcessDirectory(processDirectory);
        processDirectory.setMeteringPoint(meteringPoint);
        Energy energy = new Energy();
        energy.setMeteringIntervall(meteringIntervall);
        energy.setNumberOfMeteringIntervall(BigInteger.valueOf(1));
        energy.setMeteringPeriodStart(DateTimeConverter.dateToXml(meteringPeriodStart.toLocalDate()));
        EnergyData energyData = new EnergyData();
        energyData.setUOM(unit);
        EnergyPosition energyPosition = new EnergyPosition();
        energyPosition.setMM(meteringType);
        energyPosition.setBQ(BigDecimal.valueOf(consumptionValue));
        energyData.getEP().add(energyPosition);
        energy.getEnergyData().add(energyData);
        processDirectory.getEnergy().add(energy);
        return edaCR;
    }

}