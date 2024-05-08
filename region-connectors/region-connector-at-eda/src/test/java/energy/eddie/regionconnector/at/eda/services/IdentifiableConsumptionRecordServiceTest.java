package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

        TestPublisher<EdaConsumptionRecord> testPublisher = TestPublisher.create();
        var repository = mock(AtPermissionRequestRepository.class);
        when(repository.findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate(eq(identifiableMeteringPoint),
                                                                                    any()))
                .thenReturn(List.of(
                        new SimplePermissionRequest("pmId1", "connId1", "dataNeedId1", "test1", "any1",
                                                    PermissionProcessStatus.ACCEPTED),
                        new SimplePermissionRequest("pmId2", "connId2", "dataNeedId2", "test2", "any2",
                                                    PermissionProcessStatus.ACCEPTED)
                ));
        when(repository.findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate(eq(unidentifiableMeteringPoint),
                                                                                    any()))
                .thenReturn(List.of());

        IdentifiableConsumptionRecordService identifiableConsumptionRecordService = new IdentifiableConsumptionRecordService(
                testPublisher.flux(), repository);

        StepVerifier.create(identifiableConsumptionRecordService.getIdentifiableConsumptionRecordStream())
                    .then(() -> testPublisher.emit(identifiableConsumptionRecord, unidentifiableConsumptionRecord))
                    .assertNext(icr -> {
                        assertEquals(identifiableConsumptionRecord, icr.consumptionRecord());
                        assertEquals(2, icr.permissionRequests().size());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    private EdaConsumptionRecord createConsumptionRecord(String meteringPoint) {
        var meteringType = "L1";
        var consumptionValue = 10;
        var granularity = Granularity.PT15M;
        var unit = "KWH";
        return createConsumptionRecord(meteringPoint,
                                       meteringType,
                                       LocalDate.now(ZoneOffset.UTC),
                                       granularity,
                                       consumptionValue,
                                       unit);
    }

    private EdaConsumptionRecord createConsumptionRecord(
            String meteringPoint,
            String meteringType,
            LocalDate meteringPeriodStart,
            Granularity granularity,
            double consumptionValue,
            String unit
    ) {
        return new SimpleEdaConsumptionRecord()
                .setMeteringPoint(meteringPoint)
                .setStartDate(meteringPeriodStart)
                .setEndDate(meteringPeriodStart)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(granularity)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setBillingUnit(unit)
                                                .setEnergyPositions(List.of(new EnergyPosition(
                                                                            BigDecimal.valueOf(consumptionValue),
                                                                            meteringType)
                                                                    )
                                                )
                                ))
                ));
    }

    @Test
    void subscribeToConsumptionRecordPublisher_multipleSubscriptionsExecuteCodeOnlyOnce() {
        String identifiableMeteringPoint = "identifiableMeteringPoint";
        var identifiableConsumptionRecord = createConsumptionRecord(identifiableMeteringPoint);

        Sinks.Many<EdaConsumptionRecord> testPublisher = Sinks.many().unicast().onBackpressureBuffer();
        var repository = mock(AtPermissionRequestRepository.class);
        when(repository.findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate(eq(identifiableMeteringPoint),
                                                                                    any()))
                .thenReturn(List.of(
                        new SimplePermissionRequest("pmId1", "connId1", "dataNeedId1", "test1", "any1",
                                                    PermissionProcessStatus.ACCEPTED),
                        new SimplePermissionRequest("pmId2", "connId2", "dataNeedId2", "test2", "any2",
                                                    PermissionProcessStatus.ACCEPTED))
                );

        IdentifiableConsumptionRecordService identifiableConsumptionRecordService = new IdentifiableConsumptionRecordService(
                testPublisher.asFlux(), repository);

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
        verify(repository, times(1)).findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate(eq(
                                                                                                         identifiableMeteringPoint),
                                                                                                 any());
    }
}
