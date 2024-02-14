package energy.eddie.regionconnector.at.eda.services;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.*;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IdentifiableConsumptionRecordServiceTest {

    @Test
    void subscribeToConsumptionRecordPublisher_returnsCorrectlyMappedRecords() {
        String identifiableMeteringPoint = "identifiableMeteringPoint";
        String unidentifiableMeteringPoint = "unidentifiableMeteringPoint";
        var identifiableConsumptionRecord = createConsumptionRecord(identifiableMeteringPoint);
        var unidentifiableConsumptionRecord = createConsumptionRecord(unidentifiableMeteringPoint);

        TestPublisher<ConsumptionRecord> testPublisher = TestPublisher.create();
        PermissionRequestState state = mock(PermissionRequestState.class);
        when(state.status())
                .thenReturn(PermissionProcessStatus.ACCEPTED)
                .thenReturn(PermissionProcessStatus.ACCEPTED)
                .thenReturn(PermissionProcessStatus.REJECTED);
        var requestService = mock(PermissionRequestService.class);
        when(requestService.findByMeteringPointIdAndDate(eq(identifiableMeteringPoint), any()))
                .thenReturn(List.of(
                                new SimplePermissionRequest("pmId1", "connId1", "dataNeedId1", "test1", "any1", state),
                                new SimplePermissionRequest("pmId2", "connId2", "dataNeedId2", "test2", "any2", state),
                                new SimplePermissionRequest("pmId3", "connId3", "dataNeedId3", "test3", "any3", state)
                        )
                );
        when(requestService.findByMeteringPointIdAndDate(eq(unidentifiableMeteringPoint), any()))
                .thenReturn(List.of());

        IdentifiableConsumptionRecordService identifiableConsumptionRecordService = new IdentifiableConsumptionRecordService(testPublisher.flux(), requestService);

        StepVerifier.create(identifiableConsumptionRecordService.getIdentifiableConsumptionRecordStream())
                .then(() -> testPublisher.emit(identifiableConsumptionRecord, unidentifiableConsumptionRecord))
                .assertNext(icr -> {
                    assertEquals(identifiableConsumptionRecord, icr.consumptionRecord());
                    assertEquals(2, icr.permissionRequests().size());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void subscribeToConsumptionRecordPublisher_multipleSubscriptionsExecuteCodeOnlyOnce() {
        String identifiableMeteringPoint = "identifiableMeteringPoint";
        var identifiableConsumptionRecord = createConsumptionRecord(identifiableMeteringPoint);

        Sinks.Many<ConsumptionRecord> testPublisher = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequestState state = mock(PermissionRequestState.class);
        when(state.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        var requestService = mock(PermissionRequestService.class);
        when(requestService.findByMeteringPointIdAndDate(eq(identifiableMeteringPoint), any()))
                .thenReturn(List.of(
                        new SimplePermissionRequest("pmId1", "connId1", "dataNeedId1", "test1", "any1", state),
                        new SimplePermissionRequest("pmId2", "connId2", "dataNeedId2", "test2", "any2", state))
                );

        IdentifiableConsumptionRecordService identifiableConsumptionRecordService = new IdentifiableConsumptionRecordService(testPublisher.asFlux(), requestService);

        var first = StepVerifier.create(identifiableConsumptionRecordService.getIdentifiableConsumptionRecordStream())
                .then(() -> {
                    testPublisher.tryEmitNext(identifiableConsumptionRecord);
                    testPublisher.tryEmitComplete();
                })
                .assertNext(icr -> {
                    assertEquals(identifiableConsumptionRecord, icr.consumptionRecord());
                    assertEquals(2, icr.permissionRequests().size());
                })
                .expectComplete()
                .verifyLater();

        var second = StepVerifier.create(identifiableConsumptionRecordService.getIdentifiableConsumptionRecordStream())
                .assertNext(icr -> {
                    assertEquals(identifiableConsumptionRecord, icr.consumptionRecord());
                    assertEquals(2, icr.permissionRequests().size());
                })
                .expectComplete()
                .verifyLater();

        first.verify(Duration.ofSeconds(2));
        second.verify(Duration.ofSeconds(2));
        verify(requestService, times(1)).findByMeteringPointIdAndDate(eq(identifiableMeteringPoint), any());
    }

    private ConsumptionRecord createConsumptionRecord(String meteringPoint) {
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