package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.BiFunction;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestFulfillmentServiceTest {
    @InjectMocks
    private FulfillmentService fulfillmentService;
    @Mock
    private Outbox outbox;
    @SuppressWarnings("unused")
    @Mock
    private BiFunction<String, PermissionProcessStatus, PermissionEvent> eventCtor;

    @Test
    void service_callsFulfill_whenMeteringDataEndIsAfterPermissionRequestEnd() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        LocalDate meteringDataEnd = permissionRequestEnd.plusDays(1);
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd,
                                                                        PermissionProcessStatus.ACCEPTED);
        EdaConsumptionRecord consumptionRecord = createConsumptionRecord(meteringDataEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox, fulfillmentService);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord,
                                                                  List.of(permissionRequest),
                                                                  null,
                                                                  meteringDataEnd));
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
                                        AllowedGranularity.PT15M, accepted, "", "consentId",
                                        null);
    }

    private EdaConsumptionRecord createConsumptionRecord(LocalDate date) {
        return new SimpleEdaConsumptionRecord()
                .setEndDate(date);
    }

    @Test
    void service_doesNotCallFulfilled_whenNoMeterReadingPresent() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd,
                                                                        PermissionProcessStatus.ACCEPTED);
        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord();
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox, fulfillmentService);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord,
                                                                  List.of(permissionRequest),
                                                                  null,
                                                                  null));
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void service_doesNotCallFulfill_whenMeteringDataEndIsEqualPermissionRequestEnd() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC);
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd,
                                                                        PermissionProcessStatus.ACCEPTED);
        EdaConsumptionRecord consumptionRecord = createConsumptionRecord(permissionRequestEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox, fulfillmentService);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord,
                                                                  List.of(permissionRequest),
                                                                  null,
                                                                  permissionRequestEnd));
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
        EdaConsumptionRecord consumptionRecord = createConsumptionRecord(meteringDataEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox, fulfillmentService);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord,
                                                                  List.of(permissionRequest),
                                                                  null,
                                                                  meteringDataEnd));
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
        EdaConsumptionRecord consumptionRecord = createConsumptionRecord(meteringDataEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox, fulfillmentService);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord,
                                                                  List.of(permissionRequest),
                                                                  null,
                                                                  meteringDataEnd));
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
        EdaConsumptionRecord consumptionRecord = createConsumptionRecord(meteringDataEnd);
        new PermissionRequestFulfillmentService(testPublisher.flux(), outbox, fulfillmentService);

        // When
        StepVerifier.create(testPublisher.flux())
                    .then(() -> {
                        testPublisher.next(
                                new IdentifiableConsumptionRecord(consumptionRecord,
                                                                  List.of(permissionRequest),
                                                                  null,
                                                                  meteringDataEnd));
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        // Then
        verify(outbox, never()).commit(any());
    }
}
