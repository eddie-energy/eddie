// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.agnostic.data.needs.EnergyDirection;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.events.*;
import energy.eddie.regionconnector.at.eda.persistence.EdaPermissionEventRepository;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.cim.agnostic.PermissionProcessStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CMAcceptHandlerTest {
    private static final LocalDate START = LocalDate.of(2026, Month.JANUARY, 1);
    private static final LocalDate END = LocalDate.of(2028, Month.JANUARY, 1);
    private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private EdaPermissionEventRepository eventRepository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private CMAcceptHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(eventRepository.findFirstByPermissionIdInOrderByIdAsc(anyList()))
                 .thenReturn(Optional.of(new SimpleEvent("original", PermissionProcessStatus.CREATED)));
    }

    @Test
    void handleCMAccept_multiplePoints_assignsEachEventSequenceToItsOwnPermission() {
        requests(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, null, null));

        handler.handleCMAccept(message(consent("consent-1", "point-1"), consent("consent-2", "point-2"),
                                      consent("consent-3", "point-3")));

        var events = events(9);
        var first = assertInstanceOf(AcceptedEvent.class, events.getFirst());
        assertAccepted(first, "original", "point-1", "consent-1");
        assertNewPermission(events.subList(1, 5), "point-2", "consent-2");
        assertNewPermission(events.subList(5, 9), "point-3", "consent-3");
        assertEquals(3, events.stream().map(PersistablePermissionEvent::permissionId).distinct().count());
    }

    @Test
    void handleCMAccept_knownPointLaterInPayload_preservesOriginalPointAssignment() {
        requests(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, "point-1", null));

        handler.handleCMAccept(message(consent("consent-2", "point-2"), consent("consent-1", "point-1")));

        var events = events(5);
        assertNewPermission(events.subList(0, 4), "point-2", "consent-2");
        assertAccepted((AcceptedEvent) events.getLast(), "original", "point-1", "consent-1");
    }

    @Test
    void handleCMAccept_duplicateEntry_acceptsOnlyOnce() {
        requests(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, null, null));

        handler.handleCMAccept(message(consent("consent-1", "point-1"), consent("consent-1", "point-1")));

        assertAccepted((AcceptedEvent) events(1).getFirst(), "original", "point-1", "consent-1");
    }

    @Test
    void handleCMAccept_repeatedAndReorderedMessage_doesNotCreateEvents() {
        requests(projection("second", ACCEPTED, "point-2", "consent-2"),
                 projection("original", ACCEPTED, "point-1", "consent-1"));

        handler.handleCMAccept(message(consent("consent-1", "point-1"), consent("consent-2", "point-2")));

        verifyNoInteractions(outbox);
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class,
            names = {"ACCEPTED", "FULFILLED", "REVOKED", "EXTERNALLY_TERMINATED", "REQUIRES_EXTERNAL_TERMINATION"})
    void handleCMAccept_repeatedConsent_doesNotResetStatus(PermissionProcessStatus status) {
        requests(projection("original", status, "point-1", "consent-1"));

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        verifyNoInteractions(outbox);
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"ACCEPTED", "FULFILLED"})
    void handleCMAccept_lateAdditionalPoint_createsOnlyTheNewPermission(PermissionProcessStatus status) {
        requests(projection("original", status, "point-1", "consent-1"));

        handler.handleCMAccept(message(consent("consent-2", "point-2")));

        assertNewPermission(events(4), "point-2", "consent-2");
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, mode = EnumSource.Mode.EXCLUDE,
            names = {"VALIDATED", "SENT_TO_PERMISSION_ADMINISTRATOR", "ACCEPTED", "FULFILLED"})
    void handleCMAccept_closedRequest_doesNotCreateAdditionalPermissions(PermissionProcessStatus status) {
        requests(projection("original", status, "point-1", "consent-1"));

        handler.handleCMAccept(message(consent("consent-2", "point-2")));

        verifyNoInteractions(outbox);
    }

    @Test
    void handleCMAccept_beforeReceiptAnswer_recordsSentBeforeAccepted() {
        requests(projection("original", VALIDATED, "point-1", null));

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        var events = events(2);
        assertEquals(List.of(SENT_TO_PERMISSION_ADMINISTRATOR, ACCEPTED),
                     events.stream().map(PersistablePermissionEvent::status).toList());
        assertTrue(events.stream().allMatch(event -> event.permissionId().equals("original")));
    }

    @Test
    void handleCMAccept_partialDelivery_reusesMatchingUnacceptedPoint() {
        requests(projection("original", ACCEPTED, "point-1", "consent-1"),
                 projection("second", SENT_TO_PERMISSION_ADMINISTRATOR, "point-2", null));

        handler.handleCMAccept(message(consent("consent-2", "point-2")));

        assertAccepted((AcceptedEvent) events(1).getFirst(), "second", "point-2", "consent-2");
    }

    @Test
    void handleCMAccept_deliveryCommittedWhileWaitingForLock_rereadsAssignments() {
        when(repository.findByOriginalConversationIdOrCMRequestId("conversation", "request"))
                .thenReturn(List.of(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, "point-1", null)))
                .thenReturn(List.of(projection("original", ACCEPTED, "point-1", "consent-1")));

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        verify(repository, times(2)).findByOriginalConversationIdOrCMRequestId("conversation", "request");
        verifyNoInteractions(outbox);
    }

    @Test
    void handleCMAccept_missingPersistedEvents_failsWithoutPublishing() {
        requests(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, null, null));
        when(eventRepository.findFirstByPermissionIdInOrderByIdAsc(anyList())).thenReturn(Optional.empty());
        var acceptance = message(consent("consent-1", "point-1"));

        assertThrows(IllegalStateException.class,
                     () -> handler.handleCMAccept(acceptance));

        verifyNoInteractions(outbox);
    }

    @Test
    void handleCMAccept_legacyMixedConsentStream_requiresReconciliation() {
        requests(projection("original", ACCEPTED, "point-1", "consent-1"));
        when(eventRepository.findInconsistentPermissionIds(List.of("original"))).thenReturn(List.of("original"));

        handler.handleCMAccept(message(consent("consent-2", "point-2")));

        verifyNoInteractions(outbox);
    }

    @Test
    void handleCMAccept_exactConversation_winsOverReusedRequestId() {
        var other = projection("other", SENT_TO_PERMISSION_ADMINISTRATOR, null, null, "other-conversation");
        requests(other, projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, "point-1", null));

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        assertAccepted((AcceptedEvent) events(1).getFirst(), "original", "point-1", "consent-1");
        verify(eventRepository).findFirstByPermissionIdInOrderByIdAsc(List.of("original"));
    }

    @Test
    void handleCMAccept_uniqueRequestIdFallback_acceptsMatchingPoint() {
        requests(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, "point-1", null, "other-conversation"));

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        assertAccepted((AcceptedEvent) events(1).getFirst(), "original", "point-1", "consent-1");
    }

    @Test
    void handleCMAccept_ambiguousRequestIdFallback_doesNotPickFirst() {
        requests(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, null, null, "first-conversation"),
                 projection("other", SENT_TO_PERMISSION_ADMINISTRATOR, null, null, "second-conversation"));

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        verifyNoInteractions(eventRepository, outbox);
    }

    @Test
    void handleCMAccept_sharedConversationAcrossCustomerContexts_doesNotMixPermissions() {
        var other = new AtPermissionRequestProjectionTest("other", "other-connection", "request", "conversation",
                START, END, "other-data-need", "AT009000", null, null, "message", "PT15M",
                SENT_TO_PERMISSION_ADMINISTRATOR.name(), CREATED, EnergyDirection.CONSUMPTION, 100);
        requests(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, null, null), other);

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        verifyNoInteractions(eventRepository, outbox);
    }

    @Test
    void handleCMAccept_noRequest_doesNothing() {
        requests();

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        verifyNoInteractions(eventRepository, outbox);
    }

    @Test
    void handleCMAccept_emptyConsentList_doesNotReadOrWritePermissions() {
        handler.handleCMAccept(message());

        verifyNoInteractions(repository, eventRepository, outbox);
    }

    @ParameterizedTest
    @MethodSource("incompleteConsents")
    void handleCMAccept_incompleteConsent_doesNotCreateOrphanPermissions(ConsentData consent) {
        handler.handleCMAccept(message(consent));

        verifyNoInteractions(repository, eventRepository, outbox);
    }

    @Test
    void handleCMAccept_incompleteEntry_followedByValidEntry_acceptsValidPoint() {
        requests(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, null, null));

        handler.handleCMAccept(message(consent(null, "point-1"), consent("consent-2", "point-2")));

        assertAccepted((AcceptedEvent) events(1).getFirst(), "original", "point-2", "consent-2");
    }

    @Test
    void handleCMAccept_conflictingPointsForConsent_rejectsWholeMessage() {
        handler.handleCMAccept(message(consent("same-consent", "point-1"), consent("same-consent", "point-2")));

        verifyNoInteractions(repository, eventRepository, outbox);
    }

    @Test
    void handleCMAccept_conflictingConsentsForPoint_rejectsWholeMessage() {
        handler.handleCMAccept(message(consent("consent-1", "same-point"), consent("consent-2", "same-point")));

        verifyNoInteractions(repository, eventRepository, outbox);
    }

    @Test
    void handleCMAccept_consentAlreadyBoundToDifferentPoint_doesNotReassignIt() {
        requests(projection("original", ACCEPTED, "point-1", "consent-1"));

        handler.handleCMAccept(message(consent("consent-1", "point-2")));

        verifyNoInteractions(outbox);
    }

    @Test
    void handleCMAccept_pointAlreadyBoundToDifferentConsent_doesNotReplaceIt() {
        requests(projection("original", ACCEPTED, "point-1", "consent-1"));

        handler.handleCMAccept(message(consent("consent-2", "point-1")));

        verifyNoInteractions(outbox);
    }

    @Test
    void handleCMAccept_duplicateExistingAssignments_doesNotChooseOne() {
        requests(projection("original", ACCEPTED, "point-1", "consent-1"),
                 projection("duplicate", ACCEPTED, "point-1", "consent-1"));

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        verifyNoInteractions(outbox);
    }

    @Test
    void handleCMAccept_multipleUnassignedPermissions_doesNotChooseOne() {
        requests(projection("original", SENT_TO_PERMISSION_ADMINISTRATOR, null, null),
                 projection("duplicate", SENT_TO_PERMISSION_ADMINISTRATOR, null, null));

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        verifyNoInteractions(outbox);
    }

    @Test
    void handleCMAccept_terminalPointAlongsideOpenSibling_doesNotReopenTerminalPoint() {
        requests(projection("original", TIMED_OUT, "point-1", null),
                 projection("sibling", ACCEPTED, "point-2", "consent-2"));

        handler.handleCMAccept(message(consent("consent-1", "point-1")));

        verifyNoInteractions(outbox);
    }

    private void requests(AtPermissionRequestProjection... requests) {
        when(repository.findByOriginalConversationIdOrCMRequestId("conversation", "request")).thenReturn(List.of(requests));
    }

    private List<PersistablePermissionEvent> events(int count) {
        var captor = ArgumentCaptor.forClass(PersistablePermissionEvent.class);
        verify(outbox, times(count)).commit(captor.capture());
        return captor.getAllValues();
    }

    private static AtPermissionRequestProjection projection(
            String permissionId, PermissionProcessStatus status, String meteringPoint, String consentId
    ) {
        return projection(permissionId, status, meteringPoint, consentId, "conversation");
    }

    private static AtPermissionRequestProjection projection(
            String permissionId, PermissionProcessStatus status, String meteringPoint, String consentId, String conversation
    ) {
        return new AtPermissionRequestProjectionTest(permissionId, "connection", "request", conversation, START, END,
                "data-need", "AT009000", meteringPoint, consentId, "message", "PT15M", status.name(), CREATED,
                EnergyDirection.CONSUMPTION, 100);
    }

    private static ConsentData consent(String consentId, String meteringPoint) {
        return new ConsentData(List.of(ResponseCode.KnownResponseCodes.ACCEPTED.getCode()), "accepted",
                               Optional.ofNullable(consentId), Optional.ofNullable(meteringPoint));
    }

    private static CMRequestStatus message(ConsentData... consents) {
        return new CMRequestStatus(NotificationMessageType.CCMO_ACCEPT, "conversation", "request", List.of(consents));
    }

    private static Stream<ConsentData> incompleteConsents() {
        return Stream.of(consent(null, "point"), consent("", "point"), consent(" ", "point"),
                         consent("consent", null), consent("consent", ""), consent("consent", " "));
    }

    private static void assertAccepted(AcceptedEvent event, String permissionId, String meteringPoint, String consentId) {
        assertAll(() -> assertEquals(permissionId, event.permissionId()),
                  () -> assertEquals(ACCEPTED, event.status()),
                  () -> assertEquals(meteringPoint, event.meteringPointId()),
                  () -> assertEquals(consentId, event.cmConsentId()));
    }

    private static void assertNewPermission(List<PersistablePermissionEvent> events, String meteringPoint, String consentId) {
        var created = assertInstanceOf(CreatedEvent.class, events.getFirst());
        var validated = assertInstanceOf(ValidatedEvent.class, events.get(1));
        assertInstanceOf(EdaAnswerEvent.class, events.get(2));
        var accepted = assertInstanceOf(AcceptedEvent.class, events.getLast());
        assertNotEquals("original", created.permissionId());
        assertTrue(events.stream().allMatch(event -> event.permissionId().equals(created.permissionId())));
        assertEquals(List.of(PermissionProcessStatus.CREATED, VALIDATED, SENT_TO_PERMISSION_ADMINISTRATOR, ACCEPTED),
                     events.stream().map(PersistablePermissionEvent::status).toList());
        assertAll(() -> assertEquals("connection", created.connectionId()),
                  () -> assertEquals("data-need", created.dataNeedId()),
                  () -> assertEquals(CREATED, created.created().toInstant()),
                  () -> assertEquals(meteringPoint, created.meteringPointId()),
                  () -> assertEquals("AT009000", created.dataSourceInformation().meteredDataAdministratorId()),
                  () -> assertEquals(START, validated.start()),
                  () -> assertEquals(END, validated.end()),
                  () -> assertEquals("PT15M", validated.granularity().name()),
                  () -> assertEquals("request", validated.cmRequestId()),
                  () -> assertEquals("conversation", validated.conversationId()),
                  () -> assertEquals(EnergyDirection.CONSUMPTION, validated.energyDirection()),
                  () -> assertEquals(100, validated.participationFactor()),
                  () -> assertFalse(validated.needsToBeSent()));
        assertAccepted(accepted, created.permissionId(), meteringPoint, consentId);
    }
}
