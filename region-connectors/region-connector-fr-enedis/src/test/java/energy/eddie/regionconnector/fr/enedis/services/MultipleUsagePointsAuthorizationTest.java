// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.CimTestConfiguration;
import energy.eddie.regionconnector.fr.enedis.api.EnedisSubscribedServicesApi;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.Authorization;
import energy.eddie.regionconnector.fr.enedis.dto.subscription.ServiceSubscriptionsResponse;
import energy.eddie.regionconnector.fr.enedis.dto.subscription.SubscribedService;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrCreatedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrValidatedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.handler.AcceptedHandler;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionEventRepository;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.ServletWebRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Authorizes a permission request whose Enedis authorization covers two usage points, with the event bus, outbox,
 * handlers and database wired as in the application. Every additional usage point gets its own permission request,
 * which has to fetch metering data like the original one, since they share the same data need. Usage points that
 * already have a permission request from an earlier consent under the same authorization don't get another one.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// The outbox only emits events outside a transaction or after commit, like during a web request
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@Import({CimTestConfiguration.class, MultipleUsagePointsAuthorizationTest.Config.class, PermissionRequestService.class, AcceptedHandler.class})
class MultipleUsagePointsAuthorizationTest {
    private static final String FIRST_USAGE_POINT = "11111111111111";
    private static final String SECOND_USAGE_POINT = "22222222222222";
    private static final long AUTHORIZATION_ID = 1L;
    private static final String DATA_NEED_ID = "dnid";
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");

    // the database is shared between the tests
    private final String connectionId = UUID.randomUUID().toString();
    @Autowired
    private PermissionRequestService permissionRequestService;
    @Autowired
    private FrPermissionRequestRepository repository;
    @Autowired
    private Outbox outbox;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @MockitoBean
    private AccountingPointDataService accountingPointDataService;
    @MockitoBean
    private EnedisSubscribedServicesApi subscribedServicesApi;
    @SuppressWarnings("unused")
    @MockitoBean
    private DataNeedCalculationService calculationService;
    @SuppressWarnings("unused")
    @MockitoBean
    private DataNeedsService dataNeedsService;

    @Test
    void authorizePermissionRequest_withOpenEntityManagerInView_fetchesMeteringDataForEveryUsagePoint() throws PermissionNotFoundException {
        // Given
        var permissionId = givenValidatedPermissionRequest();
        givenSubscribedServices(FIRST_USAGE_POINT, SECOND_USAGE_POINT);

        // When
        var res = authorizeWithinOpenEntityManagerInView(permissionId);

        // Then
        assertEquals(Authorization.ACCEPTED, res);
        assertMeteringDataFetchedForEveryUsagePoint(permissionId);
    }

    @Test
    void authorizePermissionRequest_withoutSharedPersistenceContext_fetchesMeteringDataForEveryUsagePoint() throws PermissionNotFoundException {
        // Given
        var permissionId = givenValidatedPermissionRequest();
        givenSubscribedServices(FIRST_USAGE_POINT, SECOND_USAGE_POINT);

        // When
        var res = permissionRequestService.authorizePermissionRequest(permissionId, AUTHORIZATION_ID);

        // Then
        assertEquals(Authorization.ACCEPTED, res);
        assertMeteringDataFetchedForEveryUsagePoint(permissionId);
    }

    @Test
    void authorizePermissionRequest_forSecondConsentUnderSameAuthorization_acceptsOnlyNewUsagePoint() throws PermissionNotFoundException {
        // Given
        var firstPermissionId = givenValidatedPermissionRequest();
        givenSubscribedServices(FIRST_USAGE_POINT);
        authorizeWithinOpenEntityManagerInView(firstPermissionId);
        // Enedis adds the services of the second consent to the same authorization
        var secondPermissionId = givenValidatedPermissionRequest();
        givenSubscribedServices(FIRST_USAGE_POINT, SECOND_USAGE_POINT);

        // When
        var res = authorizeWithinOpenEntityManagerInView(secondPermissionId);

        // Then
        assertEquals(Authorization.ACCEPTED, res);
        var accepted = repository.findAllByConnectionIdAndDataNeedIdAndStatus(connectionId,
                                                                              DATA_NEED_ID,
                                                                              PermissionProcessStatus.ACCEPTED);
        assertEquals(2, accepted.size());
        verify(accountingPointDataService).fetchMeteringPointSegment(firstPermissionId, FIRST_USAGE_POINT);
        verify(accountingPointDataService).fetchMeteringPointSegment(secondPermissionId, SECOND_USAGE_POINT);
        verify(accountingPointDataService, times(2)).fetchMeteringPointSegment(anyString(), anyString());
        verify(accountingPointDataService, never()).fetchAccountingPointData(any(), anyString());
    }

    private void assertMeteringDataFetchedForEveryUsagePoint(String permissionId) {
        var extraPermissionId = repository.findAllByConnectionIdAndDataNeedIdAndStatus(connectionId,
                                                                                      DATA_NEED_ID,
                                                                                      PermissionProcessStatus.ACCEPTED)
                                          .stream()
                                          .map(FrEnedisPermissionRequest::permissionId)
                                          .filter(id -> !id.equals(permissionId))
                                          .findFirst()
                                          .orElseThrow();
        verify(accountingPointDataService).fetchMeteringPointSegment(permissionId, FIRST_USAGE_POINT);
        verify(accountingPointDataService).fetchMeteringPointSegment(extraPermissionId, SECOND_USAGE_POINT);
        verify(accountingPointDataService, never()).fetchAccountingPointData(any(), anyString());
    }

    /**
     * Mirrors the way Spring's open-in-view support wraps a request to the authorization callback, so that all
     * repository calls in the request share one persistence context.
     */
    private Authorization authorizeWithinOpenEntityManagerInView(String permissionId) throws PermissionNotFoundException {
        var interceptor = new OpenEntityManagerInViewInterceptor();
        interceptor.setEntityManagerFactory(entityManagerFactory);
        var webRequest = new ServletWebRequest(new MockHttpServletRequest());
        interceptor.preHandle(webRequest);
        try {
            return permissionRequestService.authorizePermissionRequest(permissionId, AUTHORIZATION_ID);
        } finally {
            interceptor.afterCompletion(webRequest, null);
        }
    }

    /**
     * The original permission request is created and validated in earlier requests.
     */
    private String givenValidatedPermissionRequest() {
        var permissionId = UUID.randomUUID().toString();
        outbox.commit(new FrCreatedEvent(permissionId, connectionId, DATA_NEED_ID));
        outbox.commit(new FrValidatedEvent(permissionId,
                                           LocalDate.now(ZONE_ID_FR).minusDays(10),
                                           LocalDate.now(ZONE_ID_FR).minusDays(1),
                                           Granularity.PT30M));
        return permissionId;
    }

    private void givenSubscribedServices(String... usagePointIds) {
        var services = Arrays.stream(usagePointIds)
                            .map(MultipleUsagePointsAuthorizationTest::service)
                            .toList();
        when(subscribedServicesApi.getSubscribedServices(AUTHORIZATION_ID))
                .thenReturn(Mono.just(new ServiceSubscriptionsResponse((long) services.size(), services)));
    }

    private static SubscribedService service(String usagePointId) {
        return new SubscribedService(null, false, true, null, null, null, null, null, usagePointId,
                                     null, null, null, null, null, null, null, null, null);
    }

    @TestConfiguration
    static class Config {
        @Bean
        EventBus eventBus() {
            return new EventBusImpl();
        }

        @Bean
        Outbox outbox(EventBus eventBus, FrPermissionEventRepository permissionEventRepository) {
            return new Outbox(eventBus, permissionEventRepository);
        }

        @Bean
        ConnectionStatusMessageHandler<FrEnedisPermissionRequest> connectionStatusMessageHandler(
                EventBus eventBus,
                FrPermissionRequestRepository repository
        ) {
            return new ConnectionStatusMessageHandler<>(eventBus, repository, pr -> "");
        }

        @Bean
        EnedisConfiguration enedisConfiguration() {
            return new EnedisConfiguration("client-id", "client-secret", "http://localhost");
        }
    }
}
