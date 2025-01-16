package energy.eddie.regionconnector.us.green.button.services.utility.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Exports;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.us.green.button.services.historical.collection.DataNeedMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeterEventCallbacksTest {
    @Mock
    private MeterReadingRepository meterReadingRepository;
    @Mock
    private Outbox outbox;
    @Mock
    private GreenButtonApi api;
    @Mock
    private DataNeedMatcher matcher;
    @InjectMocks
    private MeterEventCallbacks meterEventCallbacks;

    public static Stream<Arguments> testOnMeterCreatedEvent_apiThrowsException_doesNothing() {
        var error = WebClientResponseException.create(404, "", null, null, null);
        return Stream.of(
                Arguments.of(new Exception()),
                Arguments.of(error)
        );
    }

    @Test
    void testOnHistoricalCollectionFinishedEvent_withoutMeterUid_doesNothing() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of());
        var event = getEvent(null);

        // When
        meterEventCallbacks.onHistoricalCollectionFinishedEvent(event, pr);

        // Then
        verify(outbox, never()).commit(any());
        verify(meterReadingRepository, never()).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @Test
    void testOnHistoricalCollectionFinishedEvent_withoutAcceptedPermissionRequest_doesNothing() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.FULFILLED, List.of());
        var event = getEvent("muid");

        // When
        meterEventCallbacks.onHistoricalCollectionFinishedEvent(event, pr);

        // Then
        verify(outbox, never()).commit(any());
        verify(meterReadingRepository, never()).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @Test
    void testOnHistoricalCollectionFinishedEvent_updatesAlreadyKnownMeter() {
        // Given
        var meterReading = getMeterReading(PollingStatus.DATA_NOT_READY);
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of(meterReading));
        var event = getEvent("muid");
        var meter = getMeter();
        var publisher = TestPublisher.<Meter>create();
        when(api.fetchMeter("muid", "company")).thenReturn(publisher.mono());
        when(matcher.isRelevantEnergyType(meter, pr)).thenReturn(true);
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(List.of(getMeterReading(PollingStatus.DATA_READY)));
        when(meterReadingRepository.existsById(any())).thenReturn(true);

        // When
        meterEventCallbacks.onHistoricalCollectionFinishedEvent(event, pr);

        // Then
        StepVerifier.create(publisher)
                    .then(() -> publisher.emit(meter))
                    .then(publisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(outbox).commit(isA(UsStartPollingEvent.class));
        verify(meterReadingRepository, never()).save(any());
        verify(meterReadingRepository).updateHistoricalCollectionStatusForMeter(PollingStatus.DATA_READY,
                                                                                "pid",
                                                                                "muid");
    }

    @Test
    void testOnHistoricalCollectionFinishedEvent_savesUnknownMeter() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of());
        var event = getEvent("muid");
        var meter = getMeter();
        var publisher = TestPublisher.<Meter>create();
        when(api.fetchMeter("muid", "company")).thenReturn(publisher.mono());
        when(matcher.isRelevantEnergyType(meter, pr)).thenReturn(true);
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(List.of(getMeterReading(PollingStatus.DATA_READY)));

        // When
        meterEventCallbacks.onHistoricalCollectionFinishedEvent(event, pr);

        // Then
        StepVerifier.create(publisher)
                    .then(() -> publisher.emit(meter))
                    .then(publisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(outbox).commit(isA(UsStartPollingEvent.class));
        verify(meterReadingRepository).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @Test
    void testOnHistoricalCollectionFinishedEvent_doesNotEmitStartPolling_forEmptyMeterList() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of());
        var event = getEvent("muid");
        var meter = getMeter();
        var publisher = TestPublisher.<Meter>create();
        when(api.fetchMeter("muid", "company")).thenReturn(publisher.mono());
        when(matcher.isRelevantEnergyType(meter, pr)).thenReturn(true);
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(List.of());

        // When
        meterEventCallbacks.onHistoricalCollectionFinishedEvent(event, pr);

        // Then
        StepVerifier.create(publisher)
                    .then(() -> publisher.emit(meter))
                    .then(publisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(outbox, never()).commit(any());
        verify(meterReadingRepository).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @Test
    void testOnHistoricalCollectionFinishedEvent_doesNotEmitStartPolling_ifOneMeterNotReady() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of());
        var event = getEvent("muid");
        var meter = getMeter();
        var publisher = TestPublisher.<Meter>create();
        when(api.fetchMeter("muid", "company")).thenReturn(publisher.mono());
        when(matcher.isRelevantEnergyType(meter, pr)).thenReturn(true);
        when(meterReadingRepository.findAllByPermissionId("pid"))
                .thenReturn(List.of(getMeterReading(PollingStatus.DATA_READY),
                                    getMeterReading(PollingStatus.DATA_NOT_READY)));

        // When
        meterEventCallbacks.onHistoricalCollectionFinishedEvent(event, pr);

        // Then
        StepVerifier.create(publisher)
                    .then(() -> publisher.emit(meter))
                    .then(publisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(outbox, never()).commit(any());
        verify(meterReadingRepository).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @Test
    void testOnHistoricalCollectionFinishedEvent_doesNothing_forWrongMeter() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of());
        var event = getEvent("muid");
        var meter = getMeter();
        var publisher = TestPublisher.<Meter>create();
        when(api.fetchMeter("muid", "company")).thenReturn(publisher.mono());
        when(matcher.isRelevantEnergyType(meter, pr)).thenReturn(false);

        // When
        meterEventCallbacks.onHistoricalCollectionFinishedEvent(event, pr);

        // Then
        StepVerifier.create(publisher)
                    .then(() -> publisher.emit(meter))
                    .then(publisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(outbox, never()).commit(isA(UsStartPollingEvent.class));
        verify(meterReadingRepository, never()).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @Test
    void testOnMeterCreatedEvent_withoutMeterUid_doesNothing() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of());
        var event = getEvent(null);

        // When
        meterEventCallbacks.onMeterCreatedEvent(event, pr);

        // Then
        verify(outbox, never()).commit(any());
        verify(meterReadingRepository, never()).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @Test
    void testOnMeterCreatedEvent_withoutAcceptedPermissionRequest_doesNothing() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.FULFILLED, List.of());
        var event = getEvent("muid");

        // When
        meterEventCallbacks.onMeterCreatedEvent(event, pr);

        // Then
        verify(outbox, never()).commit(any());
        verify(meterReadingRepository, never()).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @Test
    void testOnMeterCreatedEvent_doesNothing_forWrongMeter() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of());
        var event = getEvent("muid");
        var meter = getMeter();
        var publisher = TestPublisher.<Meter>create();
        when(api.fetchMeter("muid", "company")).thenReturn(publisher.mono());
        when(matcher.isRelevantEnergyType(meter, pr)).thenReturn(false);

        // When
        meterEventCallbacks.onMeterCreatedEvent(event, pr);

        // Then
        StepVerifier.create(publisher)
                    .then(() -> publisher.emit(meter))
                    .then(publisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(outbox, never()).commit(isA(UsStartPollingEvent.class));
        verify(meterReadingRepository, never()).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @Test
    void testOnMeterCreatedEvent_updatesMeterReading_onKnownMeter() {
        // Given
        var meterReading = getMeterReading(PollingStatus.DATA_READY);
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of(meterReading));
        var event = getEvent("muid");
        var meter = getMeter();
        var publisher = TestPublisher.<Meter>create();
        when(api.fetchMeter("muid", "company")).thenReturn(publisher.mono());
        when(matcher.isRelevantEnergyType(meter, pr)).thenReturn(true);
        when(meterReadingRepository.existsById(any())).thenReturn(true);

        // When
        meterEventCallbacks.onMeterCreatedEvent(event, pr);

        // Then
        StepVerifier.create(publisher)
                    .then(() -> publisher.emit(meter))
                    .then(publisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(outbox, never()).commit(isA(UsStartPollingEvent.class));
        verify(meterReadingRepository, never()).save(any());
        verify(meterReadingRepository).updateHistoricalCollectionStatusForMeter(PollingStatus.DATA_NOT_READY,
                                                                                "pid",
                                                                                "muid");
    }

    @Test
    void testOnMeterCreatedEvent_savesMeterReading_onUnknownMeter() {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of());
        var event = getEvent("muid");
        var meter = getMeter();
        var publisher = TestPublisher.<Meter>create();
        when(api.fetchMeter("muid", "company")).thenReturn(publisher.mono());
        when(matcher.isRelevantEnergyType(meter, pr)).thenReturn(true);

        // When
        meterEventCallbacks.onMeterCreatedEvent(event, pr);

        // Then
        StepVerifier.create(publisher)
                    .then(() -> publisher.emit(meter))
                    .then(publisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(outbox, never()).commit(isA(UsStartPollingEvent.class));
        verify(meterReadingRepository).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource
    void testOnMeterCreatedEvent_apiThrowsException_doesNothing(Exception e) {
        // Given
        var pr = getPermissionRequest(PermissionProcessStatus.ACCEPTED, List.of());
        var event = getEvent("muid");
        var publisher = TestPublisher.<Meter>create();
        when(api.fetchMeter("muid", "company"))
                .thenReturn(publisher.mono());

        // When
        meterEventCallbacks.onMeterCreatedEvent(event, pr);

        // Then
        StepVerifier.create(publisher)
                    .then(() -> publisher.error(e))
                    .then(publisher::complete)
                    .expectError()
                    .verify();
        verify(outbox, never()).commit(isA(UsStartPollingEvent.class));
        verify(meterReadingRepository, never()).save(any());
        verify(meterReadingRepository, never()).updateHistoricalCollectionStatusForMeter(any(), any(), any());
    }

    private static MeterReading getMeterReading(PollingStatus pollingStatus) {
        return new MeterReading("pid", "muid", null, pollingStatus);
    }

    private static WebhookEvent getEvent(String meterUid) {
        return new WebhookEvent(
                "uid",
                "historical_collection_finished_successful",
                ZonedDateTime.now(ZoneOffset.UTC),
                "webhook",
                URI.create("http://localhost:8080"),
                true,
                "authId",
                meterUid
        );
    }

    private static Meter getMeter() {
        return new Meter(
                "muid",
                "authId",
                ZonedDateTime.now(ZoneOffset.UTC),
                "e@mail",
                "uid",
                false,
                false,
                true,
                List.of(),
                "status",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                null,
                "DEMOUTILITY",
                0,
                List.of(),
                List.of(),
                0,
                List.of(),
                List.of(),
                new Exports(null, null, null, null, null),
                List.of(),
                List.of(),
                List.of(),
                List.of(), List.of());
    }

    private static GreenButtonPermissionRequest getPermissionRequest(
            PermissionProcessStatus status,
            List<@NotNull MeterReading> meterReadings
    ) {
        return new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                        .setStatus(status)
                                                        .setCompanyId("company")
                                                        .setLastMeterReadings(meterReadings)
                                                        .build();
    }
}