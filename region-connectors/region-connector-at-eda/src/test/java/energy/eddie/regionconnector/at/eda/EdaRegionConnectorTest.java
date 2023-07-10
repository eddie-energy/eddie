package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.*;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.*;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.at.eda.xml.builders.helper.DateTimeConverter;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdaRegionConnectorTest {

    @Test
    void connectorThrows_ifConfigurationNull() {
        // given
        var adapter = mock(EdaAdapter.class);
        var mapper = mock(EdaIdMapper.class);
        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(null, adapter, mapper));
    }

    @Test
    void connectorThrows_ifEdaAdapterNull() {
        // given
        var config = mock(AtConfiguration.class);
        var mapper = mock(EdaIdMapper.class);
        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(config, null, mapper));
    }

    @Test
    void connectorThrows_ifEdaIdMapperNull() {
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
        var mapper = mock(EdaIdMapper.class);
        // when
        // then
        assertDoesNotThrow(() -> new EdaRegionConnector(config, adapter, mapper));
    }

    @Test
    void subscribeToConsumptionRecordPublisher_doesNotThrow() throws TransmissionException {
        // given
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        when(adapter.getConsumptionRecordStream()).thenReturn(Flux.empty());
        var mapper = mock(EdaIdMapper.class);
        var connector = new EdaRegionConnector(config, adapter, mapper);

        // when
        // then
        assertDoesNotThrow(connector::getConsumptionRecordStream);
    }

    @Test
    void subscribeToConsumptionRecordPublisher_returnsCorrectlyMappedRecords() throws TransmissionException {
        var config = mock(AtConfiguration.class);
        var consumptionRecord = createConsumptionRecord();
        consumptionRecord.getProcessDirectory().setConversationId("test");

        var consumptionRecord2 = createConsumptionRecord();
        consumptionRecord2.getProcessDirectory().setConversationId("test2");

        var adapter = mock(EdaAdapter.class);
        TestPublisher<ConsumptionRecord> testPublisher = TestPublisher.create();
        when(adapter.getConsumptionRecordStream()).thenReturn(testPublisher.flux());

        var mapper = mock(EdaIdMapper.class);
        when(mapper.getMappingInfoForConversationIdOrRequestID(eq("test"), any())).thenReturn(Optional.of(new MappingInfo("permissionId", "connectionId")));
        when(mapper.getMappingInfoForConversationIdOrRequestID(eq("test2"), any())).thenReturn(Optional.of(new MappingInfo("test", "test")));

        var uut = new EdaRegionConnector(config, adapter, mapper);

        var source = JdkFlowAdapter.flowPublisherToFlux(uut.getConsumptionRecordStream());

        var unmapableConsumptionRecord = createConsumptionRecord();
        unmapableConsumptionRecord.getProcessDirectory().getEnergy().clear();

        StepVerifier.create(source)
                .then(() -> testPublisher.emit(unmapableConsumptionRecord, consumptionRecord, consumptionRecord2))
                .assertNext(cr -> {
                    assertEquals("connectionId", cr.getConnectionId());
                    assertEquals("permissionId", cr.getPermissionId());
                })
                .assertNext(cr -> {
                    assertEquals("test", cr.getConnectionId());
                    assertEquals("test", cr.getPermissionId());
                })
                .expectComplete().verify();
    }

    @Test
    void revokePermissionTest_notImplemented() throws TransmissionException {
        // given
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        var mapper = mock(EdaIdMapper.class);
        var connector = new EdaRegionConnector(config, adapter, mapper);

        // when
        // then
        assertThrows(UnsupportedOperationException.class, () -> connector.revokePermission("permissionId"));
    }

    @Test
    void sendCCMORequest_returnsRequest() throws TransmissionException, JAXBException, InvalidDsoIdException {
        // given
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        var mapper = mock(EdaIdMapper.class);
        var connector = new EdaRegionConnector(config, adapter, mapper);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = mock(AtConfiguration.class);
        when(atConfiguration.eligiblePartyId()).thenReturn("RC100007");
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
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        var mapper = mock(EdaIdMapper.class);
        var connector = new EdaRegionConnector(config, adapter, mapper);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = mock(AtConfiguration.class);
        when(atConfiguration.eligiblePartyId()).thenReturn("RC100007");
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D);

        // when
        // then
        assertThrows(NullPointerException.class, () -> connector.sendCCMORequest(null, ccmoRequest));
    }

    @Test
    void sendCCMORequest_throwsIfCcmoRequestNull() throws TransmissionException {
        // given
        var config = mock(AtConfiguration.class);
        var adapter = mock(EdaAdapter.class);
        var mapper = mock(EdaIdMapper.class);
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

}