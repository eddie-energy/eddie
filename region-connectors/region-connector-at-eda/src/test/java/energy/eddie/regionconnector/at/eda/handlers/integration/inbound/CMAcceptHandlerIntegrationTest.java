// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.events.*;
import energy.eddie.regionconnector.at.eda.persistence.EdaPermissionEventRepository;
import energy.eddie.regionconnector.at.eda.persistence.JpaPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.services.IdentifiableConsumptionRecordService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({CMAcceptHandler.class, CMAcceptHandlerIntegrationTest.OutboxConfiguration.class})
class CMAcceptHandlerIntegrationTest {
    private static final LocalDate START = LocalDate.of(2026, Month.JANUARY, 1);
    private static final LocalDate END = LocalDate.of(2028, Month.JANUARY, 1);
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");
    @Autowired
    private EdaPermissionEventRepository eventRepository;
    @Autowired
    private JpaPermissionRequestRepository requestRepository;
    @Autowired
    private CMAcceptHandler handler;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        eventRepository.saveAndFlush(new CreatedEvent("original", "connection", "data-need",
                                                      new EdaDataSourceInformation("AT009000"), null));
        eventRepository.saveAndFlush(new ValidatedEvent("original", START, END, AllowedGranularity.PT15M,
                                                        "request", "conversation", ValidatedEvent.NeedsToBeSent.NO));
        eventRepository.saveAndFlush(new SimpleEvent("original", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
    }

    @Test
    void acceptance_roundTripsEveryPointConsentAndCustomerContext() {
        handler.handleCMAccept(message(1, 2, 3));

        assertAcceptedPoints();
        var count = eventRepository.count();
        handler.handleCMAccept(message(3, 1, 2, 1));
        assertThat(eventRepository.count()).isEqualTo(count);
    }

    @Test
    void acceptance_partialAndLateMessages_reuseExistingAssignments() {
        handler.handleCMAccept(message(1));
        handler.handleCMAccept(message(3, 2));

        assertAcceptedPoints();
        assertThat(eventRepository.count()).isEqualTo(12);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void acceptance_concurrentDeliveries_createEachPointOnlyOnce(boolean disjointPoints) throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> acceptAfter(start, disjointPoints ? message(1) : message(1, 2, 3)));
            var second = executor.submit(() -> acceptAfter(start, disjointPoints ? message(3, 2) : message(3, 2, 1)));
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }

        assertAcceptedPoints();
        assertThat(eventRepository.count()).isEqualTo(12);
    }

    @Test
    void acceptance_lateReceiptAcknowledgement_doesNotDowngradeAcceptedPermissions() {
        handler.handleCMAccept(message(1, 2, 3));
        var count = eventRepository.count();

        new CMAnswerHandler(new Outbox(eventBus, eventRepository), requestRepository).handleCMAnswer(
                new CMRequestStatus(NotificationMessageType.CCMO_ANSWER, "conversation", "received"));

        assertAcceptedPoints();
        assertThat(eventRepository.count()).isEqualTo(count);
    }

    @Test
    void acceptance_afterTerminationConversationChanged_doesNotRecreateTerminatedConsent() {
        handler.handleCMAccept(message(1, 2, 3));
        var terminatedId = requestRepository.findByConsentId("consent-2").orElseThrow().getPermissionId();
        eventRepository.saveAndFlush(new TerminationEvent(terminatedId, "termination-conversation"));
        eventRepository.saveAndFlush(new SimpleEvent(terminatedId, PermissionProcessStatus.EXTERNALLY_TERMINATED));
        var count = eventRepository.count();

        handler.handleCMAccept(message(2));

        assertThat(eventRepository.count()).isEqualTo(count);
        handler.handleCMAccept(message(4, 2));
        assertThat(eventRepository.count()).isEqualTo(count + 4);
        var terminated = requestRepository.findByConsentId("consent-2").orElseThrow();
        assertThat(terminated.getPermissionId()).isEqualTo(terminatedId);
        assertThat(terminated.getStatus()).isEqualTo("EXTERNALLY_TERMINATED");
        var additional = requestRepository.findByConsentId("consent-4").orElseThrow();
        assertThat(additional.getConversationId()).isEqualTo("conversation");
        assertThat(additional.getConnectionId()).isEqualTo("connection");
        assertThat(additional.getDataNeedId()).isEqualTo("data-need");
    }

    @Test
    void acceptance_rollback_removesEntireEventBatchAndPublishesNothing() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            handler.handleCMAccept(message(1, 2, 3));
            assertThat(eventRepository.count()).isEqualTo(12);
            verifyNoInteractions(eventBus);
            status.setRollbackOnly();
        });

        assertThat(eventRepository.count()).isEqualTo(3);
        assertThat(requestRepository.findByPermissionId("original")).get()
                .extracting(request -> request.status()).isEqualTo(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        verifyNoInteractions(eventBus);
    }

    @Test
    void projection_equalTimestamps_preservesEventOrderAndAllContext() {
        handler.handleCMAccept(message(1, 2, 3));
        jdbcTemplate.update("UPDATE at_eda.permission_event SET event_created = TIMESTAMPTZ '2026-01-01 00:00:00+00'");

        assertAcceptedPoints();
    }

    @Test
    void projection_legacyMixedStream_doesNotReturnAnotherConsentsCurrentPoint() {
        eventRepository.saveAndFlush(new AcceptedEvent("original", "point-1", "consent-1", "accepted"));
        eventRepository.saveAndFlush(new AcceptedEvent("original", "point-2", "consent-2", "accepted"));

        assertThat(requestRepository.findByConsentId("consent-1")).isEmpty();
        assertThat(requestRepository.findByConsentId("consent-2")).get()
                .extracting(request -> request.getMeteringPointId()).isEqualTo("point-2");
        assertThat(requestRepository.findAcceptedAndFulfilledByMeteringPointIdAndDate("point-1", START)).isEmpty();
        assertThat(eventRepository.findInconsistentPermissionIds(List.of("original"))).containsExactly("original");

        var count = eventRepository.count();
        handler.handleCMAccept(message(1, 2, 3));
        assertThat(eventRepository.count()).isEqualTo(count);
        verifyNoInteractions(eventBus);
    }

    @Test
    void consumptionRecord_retransmissionConversation_routesByAuthorizedPointAndDate() {
        handler.handleCMAccept(message(1, 2, 3));
        var consumptionRecord = new SimpleEdaConsumptionRecord().setConversationId("new-retransmission-conversation")
                .setMeteringPoint("point-2").setStartDate(START.plusDays(2)).setEndDate(START.plusDays(3));

        var result = new IdentifiableConsumptionRecordService(requestRepository).mapToIdentifiableConsumptionRecord(consumptionRecord);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().permissionRequests()).singleElement().satisfies(request -> {
            assertThat(request.meteringPointId()).contains("point-2");
            assertThat(request.consentId()).contains("consent-2");
            assertThat(request.connectionId()).isEqualTo("connection");
            assertThat(request.dataNeedId()).isEqualTo("data-need");
        });
    }

    private void assertAcceptedPoints() {
        var requests = requestRepository.findByConversationIdOrCMRequestId("conversation", "request");
        assertThat(requests).hasSize(3);
        assertThat(requests).extracting(request -> request.getMeteringPointId()).containsExactlyInAnyOrder("point-1", "point-2", "point-3");
        for (int point = 1; point <= 3; point++) {
            var request = requestRepository.findByConsentId("consent-" + point).orElseThrow();
            assertThat(request.getMeteringPointId()).isEqualTo("point-" + point);
            assertThat(request.getConsentId()).isEqualTo("consent-" + point);
            assertThat(request.getStatus()).isEqualTo("ACCEPTED");
            assertThat(request.getConnectionId()).isEqualTo("connection");
            assertThat(request.getDataNeedId()).isEqualTo("data-need");
            assertThat(request.getPermissionStart()).isEqualTo(START);
            assertThat(request.getPermissionEnd()).isEqualTo(END);
            assertThat(requestRepository.findAcceptedAndFulfilledByMeteringPointIdAndDate("point-" + point, START))
                    .singleElement().extracting(candidate -> candidate.getPermissionId()).isEqualTo(request.getPermissionId());
        }
    }

    private void acceptAfter(CountDownLatch start, CMRequestStatus message) {
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent acceptance did not start");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
        handler.handleCMAccept(message);
    }

    private static CMRequestStatus message(Integer... points) {
        var consents = List.of(points).stream().map(point -> new ConsentData(
                List.of(ResponseCode.KnownResponseCodes.ACCEPTED.getCode()), "accepted",
                Optional.of("consent-" + point), Optional.of("point-" + point))).toList();
        return new CMRequestStatus(NotificationMessageType.CCMO_ACCEPT, "conversation", "request", consents);
    }

    @TestConfiguration
    static class OutboxConfiguration {
        @Bean
        Outbox outbox(EventBus eventBus, EdaPermissionEventRepository repository) {
            return new Outbox(eventBus, repository);
        }
    }
}
