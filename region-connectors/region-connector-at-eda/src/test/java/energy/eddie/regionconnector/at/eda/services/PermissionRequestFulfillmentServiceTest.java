package energy.eddie.regionconnector.at.eda.services;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.Energy;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ProcessDirectory;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestFulfillmentServiceTest {
    @Mock
    private Outbox outbox;

    @Test
    void service_callsFulfill_whenMeteringDataEndIsAfterPermissionRequestEnd() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        LocalDate meteringDataEnd = permissionRequestEnd.plusDays(1);
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd,
                                                                        PermissionProcessStatus.ACCEPTED);
        ConsumptionRecord consumptionRecord = createConsumptionRecord(meteringDataEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord, List.of(permissionRequest)));
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        // Then
        verify(outbox).commit(any());
    }

    private static EdaPermissionRequest createPermissionRequest(
            LocalDate meteringDataEnd,
            PermissionProcessStatus accepted
    ) {
        return new EdaPermissionRequest("cid", "pid", "dnid", "cmRequestId", "convId",
                                        "mid", "dsoId", null, meteringDataEnd,
                                        Granularity.PT15M, accepted, "", "consentId",
                                        null);
    }

    private ConsumptionRecord createConsumptionRecord(LocalDate date) {
        return new ConsumptionRecord()
                .withProcessDirectory(new ProcessDirectory().withEnergy(
                        new Energy().withMeteringPeriodEnd(DateTimeConverter.dateTimeToXml(date.atStartOfDay(AT_ZONE_ID)))));
    }

    @Test
    void service_doesNotCallFulfill_whenMeteringDataEndIsEqualPermissionRequestEnd() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC);
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd,
                                                                        PermissionProcessStatus.ACCEPTED);
        ConsumptionRecord consumptionRecord = createConsumptionRecord(permissionRequestEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord, List.of(permissionRequest)));
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void service_doesNotCallFulfill_whenMeteringDataEndIsBeforePermissionRequestEnd() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC);
        LocalDate meteringDataEnd = permissionRequestEnd.minusDays(1);
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd,
                                                                        PermissionProcessStatus.ACCEPTED);
        ConsumptionRecord consumptionRecord = createConsumptionRecord(meteringDataEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord, List.of(permissionRequest)));
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void service_doesNotCallFulfill_whenPermissionRequestEndIsNull() {
        // Given
        LocalDate meteringDataEnd = LocalDate.now(ZoneOffset.UTC);
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        AtPermissionRequest permissionRequest = createPermissionRequest(meteringDataEnd,
                                                                        PermissionProcessStatus.ACCEPTED);
        ConsumptionRecord consumptionRecord = createConsumptionRecord(meteringDataEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord, List.of(permissionRequest)));
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        // Then
        verify(outbox, never()).commit(any());
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"TERMINATED", "REVOKED", "FULFILLED", "INVALID", "MALFORMED"})
    void service_doesNotCallFulfilled_whenInATerminalState(PermissionProcessStatus status) {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        LocalDate meteringDataEnd = permissionRequestEnd.plusDays(1);
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd, status);
        ConsumptionRecord consumptionRecord = createConsumptionRecord(meteringDataEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord, List.of(permissionRequest)));
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void service_doesNotCallFulfilled_whenNoMeterReadingPresent() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd,
                                                                        PermissionProcessStatus.ACCEPTED);
        ConsumptionRecord consumptionRecord = new ConsumptionRecord()
                .withProcessDirectory(new ProcessDirectory());
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord, List.of(permissionRequest)));
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        // Then
        verify(outbox, never()).commit(any());
    }
}
