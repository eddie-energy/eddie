package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.*;
import at.ebutilities.schemata.customerprocesses.masterdata._01p30.MasterData;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.requests.*;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.at.eda.xml.builders.helper.DateTimeConverter;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Optional;
import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

class EdaRegionConnectorTest {

    @Test
    void connectorThrows_ifConfigurationNull() {
        // given
        var adapter = new MockEdaAdapter();
        var mapper = new InMemoryEdaIdMapper();
        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(null, adapter, mapper));
    }

    @Test
    void connectorThrows_ifEdaAdapterNull() {
        // given
        var config = new SimpleAtConfiguration("bla");
        var mapper = new InMemoryEdaIdMapper();
        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(config, null, mapper));
    }

    @Test
    void connectorThrows_ifEdaIdMapperNull() {
        // given
        var config = new SimpleAtConfiguration("bla");
        var adapter = new MockEdaAdapter();
        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(config, adapter, null));
    }

    @Test
    void connectorConstructs() {
        // given
        var config = new SimpleAtConfiguration("bla");
        var adapter = new MockEdaAdapter();
        var mapper = new InMemoryEdaIdMapper();
        // when
        // then
        assertDoesNotThrow(() -> new EdaRegionConnector(config, adapter, mapper));
    }

    @Test
    void subscribeToConsumptionRecordPublisher_doesNotThrow() throws TransmissionException {
        // given
        var config = new SimpleAtConfiguration("bla");
        var adapter = new MockEdaAdapter();
        var mapper = new InMemoryEdaIdMapper();
        var connector = new EdaRegionConnector(config, adapter, mapper);

        // when
        // then
        assertDoesNotThrow(connector::getConsumptionRecordStream);
    }

    @Test
    @Disabled("GH-100 Test never finishes so never fully tests the streaming component of the EdaRegionConnector.")
    void subscribeToConsumptionRecordPublisher_returnsRecords() throws TransmissionException {
        // given
        var config = new SimpleAtConfiguration("bla");
        var consumptionRecord = createConsumptionRecord();
        Sinks.Many<ConsumptionRecord> crSink = Sinks.many().multicast().onBackpressureBuffer();
        var adapter = new MockEdaAdapter() {
            @Override
            public Flux<ConsumptionRecord> getConsumptionRecordStream() {
                return Flux.just(consumptionRecord).delaySequence(Duration.ofSeconds(1));
            }
        };
        var mapper = new InMemoryEdaIdMapper() {
            @Override
            public Optional<MappingInfo> getMappingInfoForConversationIdOrRequestID(String conversationId, @Nullable String requestId) {
                return Optional.of(new MappingInfo("permissionId", "connectionId"));
            }
        };

        // when
        StepVerifier.create(
                        JdkFlowAdapter
                                .flowPublisherToFlux(
                                        new EdaRegionConnector(config, adapter, mapper)
                                                .getConsumptionRecordStream()
                                )
                )
                // then
                .assertNext(cr -> assertEquals("connectionId", cr.getConnectionId()))
                .verifyComplete();

    }

    @Test
    void revokePermissionTest_notImplemented() throws TransmissionException {
        // given
        var config = new SimpleAtConfiguration("bla");
        var adapter = new MockEdaAdapter();
        var mapper = new InMemoryEdaIdMapper();
        var connector = new EdaRegionConnector(config, adapter, mapper);

        // when
        // then
        assertThrows(UnsupportedOperationException.class, () -> connector.revokePermission("permissionId"));
    }

    @Test
    void sendCCMORequest_returnsRequest() throws TransmissionException, JAXBException, InvalidDsoIdException {
        // given
        var config = new SimpleAtConfiguration("bla");
        var adapter = new MockEdaAdapter();
        var mapper = new InMemoryEdaIdMapper();
        var connector = new EdaRegionConnector(config, adapter, mapper);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new SimpleAtConfiguration("RC100007");
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D);

        // when
        var result = connector.sendCCMORequest("connectionId", ccmoRequest);

        // then
        assertNotNull(result);
    }

    @Test
    void sendCCMORequest_throwsIfConnectionIdNull() throws TransmissionException {
        // given
        var config = new SimpleAtConfiguration("bla");
        var adapter = new MockEdaAdapter();
        var mapper = new InMemoryEdaIdMapper();
        var connector = new EdaRegionConnector(config, adapter, mapper);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new SimpleAtConfiguration("RC100007");
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D);

        // when
        // then
        assertThrows(NullPointerException.class, () -> connector.sendCCMORequest(null, ccmoRequest));
    }

    @Test
    void sendCCMORequest_throwsIfCcmoRequestNull() throws TransmissionException {
        // given
        var config = new SimpleAtConfiguration("bla");
        var adapter = new MockEdaAdapter();
        var mapper = new InMemoryEdaIdMapper();
        var connector = new EdaRegionConnector(config, adapter, mapper);

        // when
        // then
        assertThrows(NullPointerException.class, () -> connector.sendCCMORequest("connectionId", null));
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

    private record SimpleAtConfiguration(String eligiblePartyId) implements AtConfiguration {
        SimpleAtConfiguration {
            requireNonNull(eligiblePartyId);
        }

        @Override
        public ZoneId timeZone() {
            return ZoneOffset.UTC;
        }
    }

    private static class MockEdaAdapter implements EdaAdapter {
        @Override
        public Flux<CMRequestStatus> getCMRequestStatusStream() {
            return Flux.empty();
        }

        @Override
        public Flux<ConsumptionRecord> getConsumptionRecordStream() {
            return Flux.empty();
        }

        @Override
        public Flux<CMRevoke> getCMRevokeStream() {
            return Flux.empty();
        }

        @Override
        public Flux<MasterData> getMasterDataStream() {
            return Flux.empty();
        }

        @Override
        public void sendCMRequest(CMRequest request) throws TransmissionException, JAXBException {

        }

        @Override
        public void sendCMRevoke(CMRevoke revoke) throws TransmissionException, JAXBException {

        }

        @Override
        public void start() throws TransmissionException {

        }

        @Override
        public void close() throws Exception {

        }
    }

    private static class EmptySubscriber<T> implements Flow.Subscriber<T> {

        @Override
        public void onSubscribe(Flow.Subscription subscription) {

        }

        @Override
        public void onNext(T item) {

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {

        }
    }
}