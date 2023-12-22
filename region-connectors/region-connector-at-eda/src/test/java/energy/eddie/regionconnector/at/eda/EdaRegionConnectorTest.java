package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.*;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.at.eda.processing.v0_82.ConsumptionRecordProcessor;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdaRegionConnectorTest {

    @Test
    void connectorThrows_ifEdaAdapterNull() {
        // given
        var requestService = mock(PermissionRequestService.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(null, requestService, consumptionRecordProcessor, sink));
    }

    @Test
    void connectorThrows_ifPermissionRequestRepoNull() {
        // given
        var adapter = mock(EdaAdapter.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(adapter, null, consumptionRecordProcessor, sink));
    }

    @Test
    void connectorThrows_ifConsumptionRecordProcessorNull() {
        // given
        var adapter = mock(EdaAdapter.class);
        var requestService = mock(PermissionRequestService.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(adapter, requestService, null, sink));
    }

    @Test
    void connectorThrows_ifConnectionStatusMessageSinkNull() {
        // given
        var adapter = mock(EdaAdapter.class);
        var requestService = mock(PermissionRequestService.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(adapter, requestService, consumptionRecordProcessor, null));
    }

    @Test
    void connectorConstructs() {
        // given
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        // when
        // then
        assertDoesNotThrow(() -> new EdaRegionConnector(adapter, requestService, consumptionRecordProcessor, sink));
    }

    @Test
    void subscribeToConsumptionRecordPublisher_doesNotThrow() throws TransmissionException {
        // given
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        when(adapter.getConsumptionRecordStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var connector = new EdaRegionConnector(adapter, requestService, consumptionRecordProcessor, sink);

        // when
        // then
        assertDoesNotThrow(connector::getConsumptionRecordStream);
    }

    @Test
    void subscribeToConsumptionRecordPublisher_returnsCorrectlyMappedRecords() throws TransmissionException {
        var consumptionRecord = createConsumptionRecord();
        var consumptionRecord2 = createConsumptionRecord();

        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        TestPublisher<ConsumptionRecord> testPublisher = TestPublisher.create();
        when(adapter.getConsumptionRecordStream()).thenReturn(testPublisher.flux());
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        var requestService = mock(PermissionRequestService.class);
        when(requestService.findByMeteringPointIdAndDate(anyString(), any()))
                .thenReturn(List.of(
                        new SimplePermissionRequest("pmId1", "connId1", "dataNeedId1", "test1", "any1", null))
                ).thenReturn(List.of(
                        new SimplePermissionRequest("pmId2", "connId2", "dataNeedId2", "test2", "any2", null))
                );

        var uut = new EdaRegionConnector(adapter, requestService, consumptionRecordProcessor, sink);

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
    void subscribeToConsumptionRecordPublisher_emitsConsumptionRecord_forEveryMatchingRequest() throws TransmissionException {
        var consumptionRecord = createConsumptionRecord();

        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        TestPublisher<ConsumptionRecord> testPublisher = TestPublisher.create();
        when(adapter.getConsumptionRecordStream()).thenReturn(testPublisher.flux());
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        var requestService = mock(PermissionRequestService.class);
        when(requestService.findByMeteringPointIdAndDate(anyString(), any()))
                .thenReturn(List.of(
                        new SimplePermissionRequest("pmId1", "connId1", "dataNeedId", "test1", "any1", null),
                        new SimplePermissionRequest("pmId2", "connId2", "dataNeedId", "test2", "any2", null))
                );

        var uut = new EdaRegionConnector(adapter, requestService, consumptionRecordProcessor, sink);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConsumptionRecordStream());

        StepVerifier.create(source)
                .then(() -> testPublisher.emit(consumptionRecord))
                .assertNext(csm -> {
                    assertEquals("connId1", csm.getConnectionId());
                    assertEquals("pmId1", csm.getPermissionId());
                })
                .assertNext(csm -> {
                    assertEquals("connId2", csm.getConnectionId());
                    assertEquals("pmId2", csm.getPermissionId());
                })
                .expectComplete().verify();
    }

    @Test
    void terminateNonExistingPermission_throwsIllegalStateException() throws TransmissionException {
        // given
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var connector = new EdaRegionConnector(adapter, requestService, consumptionRecordProcessor, sink);

        // when
        // then
        assertThrows(IllegalStateException.class, () -> connector.terminatePermission("permissionId"));
    }

    @Test
    void getMetadata_returnExpectedMetadata() throws TransmissionException {
        // given
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var connector = new EdaRegionConnector(adapter, requestService, consumptionRecordProcessor, sink);

        // when
        var result = connector.getMetadata();

        // then
        assertEquals(EdaRegionConnectorMetadata.getInstance(), result);
    }

    @Test
    void close_ClosesRelatedResources() throws Exception {
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        var connector = new EdaRegionConnector(adapter, requestService, consumptionRecordProcessor, sink);

        connector.close();

        verify(adapter).close();
    }

    @Test
    void health_returnsHealthChecks() throws TransmissionException {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        var requestService = mock(PermissionRequestService.class);
        when(edaAdapter.health()).thenReturn(Map.of("service", HealthState.UP));
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        var rc = new EdaRegionConnector(edaAdapter, requestService, consumptionRecordProcessor, sink);

        // When
        var res = rc.health();

        // Then
        assertEquals(Map.of("service", HealthState.UP), res);
    }

    @Test
    void close_emitsCompleteOnPublisherForConnectionStatusMessages() throws Exception {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var rc = new EdaRegionConnector(edaAdapter, requestService, consumptionRecordProcessor, sink);
        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(rc.getConnectionStatusMessageStream()))
                .expectComplete()
                .verifyLater();

        // When
        rc.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void close_emitsCompleteOnPublisherForConsumptionRecords() throws Exception {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getConsumptionRecordStream()).thenReturn(Flux.empty());
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        var consumptionRecordProcessor = mock(ConsumptionRecordProcessor.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var rc = new EdaRegionConnector(edaAdapter, requestService, consumptionRecordProcessor, sink);
        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(rc.getConsumptionRecordStream()))
                .expectComplete()
                .verifyLater();

        // When
        rc.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
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