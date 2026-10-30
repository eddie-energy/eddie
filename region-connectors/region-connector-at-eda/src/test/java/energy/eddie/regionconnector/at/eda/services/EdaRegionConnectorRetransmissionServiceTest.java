// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.*;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import energy.eddie.regionconnector.at.eda.requests.CPRequestResult;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequestBuilder;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdaRegionConnectorRetransmissionServiceTest {

    public static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    public static final String PERMISSION_ID = "id";
    private final AtConfiguration atConfiguration = new AtConfiguration("ep", null, "RETRY");
    private final EdaGroupingIdFactory groupingIdFactory = new EdaGroupingIdFactory(atConfiguration);
    @Mock
    private AtPermissionRequestRepository atPermissionRequestRepository;
    @Mock
    private EdaAdapter edaAdapter;

    public static Stream<Arguments> cpRequestResultsToRetransmissionResults() {
        return Stream.of(
                Arguments.of(CPRequestResult.Result.ACCEPTED, Success.class),
                Arguments.of(CPRequestResult.Result.METERING_POINT_NOT_ASSIGNED, Failure.class),
                Arguments.of(CPRequestResult.Result.METERING_POINT_NOT_FOUND, Failure.class),
                Arguments.of(CPRequestResult.Result.NO_DATA_AVAILABLE, DataNotAvailable.class),
                Arguments.of(CPRequestResult.Result.PONTON_ERROR, Failure.class),
                Arguments.of(CPRequestResult.Result.PROCESS_DATE_INVALID, Failure.class),
                Arguments.of(CPRequestResult.Result.UNKNOWN_RESPONSE_CODE_ERROR, Failure.class)
        );
    }

    @ParameterizedTest()
    @MethodSource("inScopeTimeFrames")
    @SuppressWarnings("ReturnValueIgnored")
    void requestRetransmission_whenTimeFrameInPermissionAndAdapterReturnsAccepted_resultIsSuccess(
            LocalDate from,
            LocalDate to,
            PermissionProcessStatus status,
            String ignoredMessage
    ) throws TransmissionException {
        // Given
        // Define the specific date and time you want to use for the test
        ZonedDateTime fixedDateTime = ZonedDateTime.now(AT_ZONE_ID);

        // Mock the static ZonedDateTime.now() method
        try (MockedStatic<ZonedDateTime> mockedZonedDateTime = mockStatic(ZonedDateTime.class)) {
            // Define the behavior of ZonedDateTime.now(zoneId) to return the fixedDateTime
            mockedZonedDateTime.when(() -> ZonedDateTime.now(AT_ZONE_ID)).thenReturn(fixedDateTime);
            TestPublisher<CPRequestResult> testPublisher = TestPublisher.create();
            when(edaAdapter.getCPRequestResultStream()).thenReturn(testPublisher.flux());
            setupFoundPermissionRequest(TODAY, AllowedGranularity.PT15M, status);
            var service = new EdaRegionConnectorRetransmissionService(atConfiguration,
                                                                      groupingIdFactory,
                                                                      edaAdapter,
                                                                      atPermissionRequestRepository);

            // When
            var stepVerifier = StepVerifier.create(
                    service.requestRetransmission(
                            getRetransmissionRequest(from, to)
                    )
            );

            var requestCaptor = ArgumentCaptor.forClass(CPRequestCR.class);
            verify(edaAdapter).sendCPRequest(requestCaptor.capture());
            var sentMessageId = requestCaptor.getValue().messageId();
            assertTrue(sentMessageId.startsWith("RETRYepT"));
            testPublisher.emit(new CPRequestResult(sentMessageId, CPRequestResult.Result.ACCEPTED));

            // Then
            stepVerifier
                    .expectNextMatches(result -> result instanceof Success(
                                               String permissionId,
                                               ZonedDateTime timestamp
                                       ) &&
                                                 permissionId.equals(PERMISSION_ID) &&
                                                 // like this because ZonedDateTime.now() is mocked
                                                 !timestamp.toOffsetDateTime().isAfter(OffsetDateTime.now(AT_ZONE_ID))
                    )
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        }
    }

    @ParameterizedTest()
    @MethodSource("cpRequestResultsToRetransmissionResults")
    @SuppressWarnings("ReturnValueIgnored")
    void requestRetransmission_whenRequestIsTransmittedViaTheAdapter_resultIsMappedAsExpected(
            CPRequestResult.Result cpResult,
            Class<? extends RetransmissionResult> expectedClass
    ) {
        // Given
        // Define the specific date and time you want to use for the test
        ZonedDateTime fixedDateTime = ZonedDateTime.now(AT_ZONE_ID);

        // Mock the static ZonedDateTime.now() method
        try (MockedStatic<ZonedDateTime> mockedZonedDateTime = mockStatic(ZonedDateTime.class)) {
            // Define the behavior of ZonedDateTime.now(zoneId) to return the fixedDateTime
            mockedZonedDateTime.when(() -> ZonedDateTime.now(AT_ZONE_ID)).thenReturn(fixedDateTime);
            TestPublisher<CPRequestResult> testPublisher = TestPublisher.create();
            when(edaAdapter.getCPRequestResultStream()).thenReturn(testPublisher.flux());
            setupFoundPermissionRequest(TODAY, AllowedGranularity.PT15M, PermissionProcessStatus.FULFILLED);
            var service = new EdaRegionConnectorRetransmissionService(atConfiguration,
                                                                      groupingIdFactory,
                                                                      edaAdapter,
                                                                      atPermissionRequestRepository);

            // When
            var stepVerifier = StepVerifier.create(
                    service.requestRetransmission(
                            getRetransmissionRequest(TODAY.minusDays(2), TODAY.minusDays(1))
                    )
            );

            testPublisher.emit(new CPRequestResult(
                    groupingIdFactory.create(fixedDateTime),
                    cpResult)
            );

            // Then
            stepVerifier
                    .expectNextMatches(result ->
                                               result.getClass().equals(expectedClass) &&
                                               result.permissionId().equals(PERMISSION_ID)
                    )
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        }
    }

    @Test
    void requestRetransmission_whenTransmissionFails_resultIsFailure() throws TransmissionException {
        // Given
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(edaAdapter.getCPRequestResultStream()).thenReturn(Flux.empty());
        doThrow(new TransmissionException(new Throwable())).when(edaAdapter).sendCPRequest(any());
        setupFoundPermissionRequest(today,
                                    AllowedGranularity.PT15M,
                                    PermissionProcessStatus.ACCEPTED); // setup master data permission
        var service = new EdaRegionConnectorRetransmissionService(atConfiguration,
                                                                  groupingIdFactory,
                                                                  edaAdapter,
                                                                  atPermissionRequestRepository);

        // When
        var stepVerifier = StepVerifier.create(
                service.requestRetransmission(
                        getRetransmissionRequest(today.minusDays(2), today.minusDays(1))
                )
        );

        // Then
        stepVerifier
                .expectNextMatches(result -> result instanceof Failure(
                                           String permissionId,
                                           ZonedDateTime timestamp,
                                           String message
                                   ) &&
                                             permissionId.equals(PERMISSION_ID) && message.equals(
                                           "Could not send request, investigate logs") &&
                                             !timestamp.isAfter(ZonedDateTime.now(AT_ZONE_ID))
                )
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void requestRetransmission_whenPermissionForMasterData_resultIsNotSupported() {
        // Given
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(edaAdapter.getCPRequestResultStream()).thenReturn(Flux.empty());
        setupFoundPermissionRequest(today, null, PermissionProcessStatus.ACCEPTED); // setup master data permission
        var service = new EdaRegionConnectorRetransmissionService(atConfiguration,
                                                                  groupingIdFactory,
                                                                  edaAdapter,
                                                                  atPermissionRequestRepository);

        // When
        var stepVerifier = StepVerifier.create(
                service.requestRetransmission(
                        getRetransmissionRequest(today.minusDays(2), today.minusDays(1))
                )
        );

        // Then
        stepVerifier
                .expectNextMatches(result -> result instanceof NotSupported(
                                           String permissionId,
                                           ZonedDateTime timestamp,
                                           String message
                                   ) &&
                                             permissionId.equals(PERMISSION_ID) && message.equals(
                                           "Retransmission of MasterData not supported") &&
                                             !timestamp.isAfter(ZonedDateTime.now(AT_ZONE_ID))
                )
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void requestRetransmission_whenNoPermissionIsFound_resultIsPermissionIdNotFound() {
        // Given
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(edaAdapter.getCPRequestResultStream()).thenReturn(Flux.empty());
        when(atPermissionRequestRepository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.empty());
        var service = new EdaRegionConnectorRetransmissionService(atConfiguration,
                                                                  groupingIdFactory,
                                                                  edaAdapter,
                                                                  atPermissionRequestRepository);

        // When
        var stepVerifier = StepVerifier.create(
                service.requestRetransmission(
                        getRetransmissionRequest(today.minusDays(2), today.minusDays(1))
                )
        );

        // Then
        stepVerifier
                .expectNextMatches(result -> result instanceof PermissionRequestNotFound(
                                           String permissionId,
                                           ZonedDateTime timestamp
                                   ) &&
                                             permissionId.equals(PERMISSION_ID) &&
                                             !timestamp.isAfter(ZonedDateTime.now(AT_ZONE_ID))
                )
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"ACCEPTED", "FULFILLED"}, mode = EnumSource.Mode.EXCLUDE)
    void requestRetransmission_whenNotAcceptedOrFulfilled_resultIsNoActivePermission(PermissionProcessStatus status) {
        // Given
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(edaAdapter.getCPRequestResultStream()).thenReturn(Flux.empty());
        setupFoundPermissionRequest(today, AllowedGranularity.PT15M, status);
        var service = new EdaRegionConnectorRetransmissionService(atConfiguration,
                                                                  groupingIdFactory,
                                                                  edaAdapter,
                                                                  atPermissionRequestRepository);

        // When
        var stepVerifier = StepVerifier.create(
                service.requestRetransmission(
                        getRetransmissionRequest(today.minusDays(2), today.minusDays(1))
                )
        );

        // Then
        stepVerifier
                .expectNextMatches(result -> result instanceof NoActivePermission(
                                           String permissionId,
                                           ZonedDateTime timestamp
                                   ) &&
                                             permissionId.equals(PERMISSION_ID) &&
                                             !timestamp.isAfter(ZonedDateTime.now(AT_ZONE_ID))
                )
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @ParameterizedTest()
    @MethodSource("outOfScopeTimeFrames")
    void requestRetransmission_whenTimeFrameNotInPermissionScope_resultsIsNoPermissionForTimeFrame(
            LocalDate from,
            LocalDate to,
            String ignoredMessage
    ) {
        // Given
        when(edaAdapter.getCPRequestResultStream()).thenReturn(Flux.empty());
        setupFoundPermissionRequest(TODAY, AllowedGranularity.PT15M, PermissionProcessStatus.ACCEPTED);
        var service = new EdaRegionConnectorRetransmissionService(atConfiguration,
                                                                  groupingIdFactory,
                                                                  edaAdapter,
                                                                  atPermissionRequestRepository);

        // When
        var stepVerifier = StepVerifier.create(
                service.requestRetransmission(
                        getRetransmissionRequest(from, to)
                )
        );

        // Then
        stepVerifier
                .expectNextMatches(result -> result instanceof NoPermissionForTimeFrame(
                                           String permissionId,
                                           ZonedDateTime timestamp
                                   ) &&
                                             permissionId.equals(PERMISSION_ID) &&
                                             !timestamp.isAfter(ZonedDateTime.now(AT_ZONE_ID))
                )
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16})
    void close_emitsFailureOnIncompleteResults(int nrOfRequests) throws Exception {
        // Given
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(edaAdapter.getCPRequestResultStream()).thenReturn(Flux.empty());
        setupFoundPermissionRequest(today, AllowedGranularity.PT15M, PermissionProcessStatus.ACCEPTED);
        var service = new EdaRegionConnectorRetransmissionService(atConfiguration,
                                                                  groupingIdFactory,
                                                                  edaAdapter,
                                                                  atPermissionRequestRepository);
        List<Mono<RetransmissionResult>> monos = new ArrayList<>();
        for (int i = 0; i < nrOfRequests; i++) {
            monos.add(service.requestRetransmission(
                    getRetransmissionRequest(today.minusDays(2), today.minusDays(1))
            ));
        }
        StepVerifier.Step<RetransmissionResult> stepVerifier = StepVerifier.create(
                Flux.fromIterable(monos).flatMap(mono -> mono)
        );

        // When
        service.close();

        // Then
        for (int i = 0; i < nrOfRequests; i++) {
            stepVerifier = stepVerifier
                    .expectNextMatches(result -> result instanceof Failure(
                                               String permissionId,
                                               ZonedDateTime timestamp,
                                               String message
                                       ) && permissionId.equals(PERMISSION_ID) && message.equals(
                                               "Service is shutting down, unclear if request got through to the MDA"
                                       ) && !timestamp.isAfter(ZonedDateTime.now(AT_ZONE_ID))
                    );
        }
        stepVerifier.expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    @ParameterizedTest
    @MethodSource("incompleteAssignments")
    void requestRetransmission_incompleteAssignment_doesNotSendRequest(String meteringPoint, String consentId)
            throws TransmissionException {
        when(edaAdapter.getCPRequestResultStream()).thenReturn(Flux.empty());
        var permission = permission(meteringPoint, consentId, TODAY, PermissionProcessStatus.ACCEPTED);
        when(atPermissionRequestRepository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permission));
        var service = new EdaRegionConnectorRetransmissionService(atConfiguration, groupingIdFactory, edaAdapter,
                                                                 atPermissionRequestRepository);

        StepVerifier.create(service.requestRetransmission(getRetransmissionRequest(TODAY.minusDays(2), TODAY.minusDays(1))))
                    .expectNextMatches(NoActivePermission.class::isInstance).verifyComplete();

        verify(edaAdapter, never()).sendCPRequest(any());
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"ACCEPTED", "FULFILLED"})
    void requestRetransmission_openEndedPermission_sendsOnlyItsOwnPoint(PermissionProcessStatus status)
            throws TransmissionException {
        var responses = TestPublisher.<CPRequestResult>create();
        when(edaAdapter.getCPRequestResultStream()).thenReturn(responses.flux());
        when(atPermissionRequestRepository.findByPermissionId(PERMISSION_ID))
                .thenReturn(Optional.of(permission("second-point", "second-consent", null, status)));
        var service = new EdaRegionConnectorRetransmissionService(atConfiguration, groupingIdFactory, edaAdapter,
                                                                 atPermissionRequestRepository);

        var result = service.requestRetransmission(getRetransmissionRequest(TODAY.minusDays(2), TODAY.minusDays(1)));

        var captor = ArgumentCaptor.forClass(CPRequestCR.class);
        verify(edaAdapter).sendCPRequest(captor.capture());
        var request = captor.getValue();
        assertEquals("second-point", request.meteringPointId());
        assertEquals(TODAY.minusDays(2), request.start());
        assertEquals(TODAY.minusDays(1), request.end());
        responses.emit(new CPRequestResult(request.messageId(), CPRequestResult.Result.ACCEPTED));
        StepVerifier.create(result).expectNextMatches(Success.class::isInstance).verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void requestRetransmission_emptyOrReversedInterval_doesNotSendRequest(int days) throws TransmissionException {
        when(edaAdapter.getCPRequestResultStream()).thenReturn(Flux.empty());
        setupFoundPermissionRequest(TODAY, AllowedGranularity.PT15M, PermissionProcessStatus.ACCEPTED);
        var service = new EdaRegionConnectorRetransmissionService(atConfiguration, groupingIdFactory, edaAdapter,
                                                                 atPermissionRequestRepository);
        var from = TODAY.minusDays(2);

        StepVerifier.create(service.requestRetransmission(getRetransmissionRequest(from, from.plusDays(days))))
                    .expectNextMatches(NoPermissionForTimeFrame.class::isInstance).verifyComplete();

        verify(edaAdapter, never()).sendCPRequest(any());
    }

    private static Stream<Arguments> incompleteAssignments() {
        return Stream.of(Arguments.of(null, "consent"), Arguments.of("", "consent"), Arguments.of(" ", "consent"),
                         Arguments.of("point", null), Arguments.of("point", ""), Arguments.of("point", " "));
    }

    private static energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest permission(
            String meteringPoint, String consentId, LocalDate end, PermissionProcessStatus status
    ) {
        return new EdaPermissionRequestBuilder().setPermissionId(PERMISSION_ID).setConnectionId("connection")
                .setDataNeedId("data-need").setDsoId("AT009000").setMeteringPointId(meteringPoint)
                .setConsentId(consentId).setStart(TODAY.minusWeeks(1)).setEnd(end)
                .setGranularity(AllowedGranularity.PT15M).setStatus(status).build();
    }

    private static RetransmissionRequest getRetransmissionRequest(LocalDate from, LocalDate to) {
        return new RetransmissionRequest(EdaRegionConnectorMetadata.REGION_CONNECTOR_ID, PERMISSION_ID, from, to);
    }

    private static Stream<Arguments> inScopeTimeFrames() {
        return Stream.of(
                Arguments.of(TODAY.minusWeeks(1),
                             TODAY,
                             PermissionProcessStatus.ACCEPTED,
                             "Time frame matches permission timeframe"),
                Arguments.of(TODAY.minusDays(1),
                             TODAY,
                             PermissionProcessStatus.ACCEPTED,
                             "Time frame yesterday to today"),
                Arguments.of(TODAY.minusDays(2),
                             TODAY.minusDays(1),
                             PermissionProcessStatus.ACCEPTED,
                             "Time frame day before yesterday to yesterday"),
                Arguments.of(TODAY.minusWeeks(1),
                             TODAY.minusDays(5),
                             PermissionProcessStatus.ACCEPTED,
                             "Time frame last week to last friday")
        );
    }

    private static Stream<Arguments> outOfScopeTimeFrames() {
        return Stream.of(
                Arguments.of(TODAY.minusYears(2), TODAY.minusYears(1), "Time frame is before permission start"),
                Arguments.of(TODAY.plusDays(1), TODAY.plusDays(2), "Time frame is after permission end"),
                Arguments.of(TODAY.minusWeeks(2),
                             TODAY.minusDays(1),
                             "Time frame starts before permission start, ends within permission"),
                Arguments.of(TODAY.minusDays(1),
                             TODAY.plusDays(1),
                             "Time frame starts within permission, ends after permission end")
        );
    }

    private void setupFoundPermissionRequest(
            LocalDate today,
            @Nullable AllowedGranularity granularity,
            PermissionProcessStatus permissionProcessStatus
    ) {
        when(atPermissionRequestRepository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(
                new SimplePermissionRequest(
                        PERMISSION_ID, "connectionId", "dataNeedId",
                        "cmRequestId", "conversationId", "dsoId",
                        Optional.of("meteringPointId"), today.minusWeeks(1), today,
                        permissionProcessStatus, Optional.of("consentId"), granularity
                )
        ));
    }
}
