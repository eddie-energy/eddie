package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.EnedisAccountingPointDataApi;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.address.CustomerAddress;
import energy.eddie.regionconnector.fr.enedis.dto.contact.Contact;
import energy.eddie.regionconnector.fr.enedis.dto.contact.CustomerContact;
import energy.eddie.regionconnector.fr.enedis.dto.contract.Contract;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.contract.UsagePointContract;
import energy.eddie.regionconnector.fr.enedis.dto.identity.CustomerIdentity;
import energy.eddie.regionconnector.fr.enedis.dto.identity.Identity;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrUsagePointTypeEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisDataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.fr.enedis.providers.v0_82.SimpleFrEnedisPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountingPointDataServiceTest {
    private final String usagePointId = "usagePointId";
    private final String permissionId = "permissionId";
    @Spy
    Sinks.Many<IdentifiableAccountingPointData> sink = Sinks.many().unicast().onBackpressureBuffer();
    @Mock
    private Outbox outbox;
    @Mock
    private EnedisAccountingPointDataApi enedisApi;
    @InjectMocks
    private AccountingPointDataService accountingPointDataService;
    @Captor
    private ArgumentCaptor<FrUsagePointTypeEvent> usagePointTypeEventCaptor;
    @Captor
    private ArgumentCaptor<FrSimpleEvent> simpleEventCaptor;

    private static Stream<Arguments> validSegments() {
        return Stream.of(
                Arguments.of("C5", UsagePointType.CONSUMPTION),
                Arguments.of("P4", UsagePointType.PRODUCTION),
                Arguments.of("C4/P5", UsagePointType.CONSUMPTION_AND_PRODUCTION)
        );
    }

    private static Stream<Arguments> invalidSegments() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of((String) null),
                Arguments.of("XXX")
        );
    }

    @ParameterizedTest
    @MethodSource("validSegments")
    void fetchMeteringPointSegment_whenValidSegment_emitsUsagePointTypeEvent(String segment, UsagePointType expected) {
        // Given
        var customerContract = customerContract(segment);
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.just(customerContract));

        // When
        accountingPointDataService.fetchMeteringPointSegment(permissionId, usagePointId);

        // Then
        verify(outbox).commit(usagePointTypeEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionId, usagePointTypeEventCaptor.getValue().permissionId()),
                () -> assertEquals(expected, usagePointTypeEventCaptor.getValue().usagePointType())
        );
    }

    private static CustomerContract customerContract(@Nullable String segment) {
        return new CustomerContract(
                "customerId",
                List.of(new UsagePointContract(null, new Contract(
                        segment,
                        null, null, null, null, null, null, null
                )))
        );
    }

    @ParameterizedTest
    @MethodSource("invalidSegments")
    void fetchMeteringPointSegment_whenSegmentInvalid_emitsInvalidEvent(@Nullable String segment) {
        // Given
        var customerContract = customerContract(segment);
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.just(customerContract));

        // When
        accountingPointDataService.fetchMeteringPointSegment(permissionId, usagePointId);

        // Then
        verify(outbox).commit(simpleEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionId, simpleEventCaptor.getValue().permissionId()),
                () -> assertEquals(PermissionProcessStatus.INVALID, simpleEventCaptor.getValue().status())
        );
    }

    @Test
    void fetchMeteringPointSegment_whenCustomerContractContainsNoContracts_emitsInvalidEvent() {
        // Given
        var customerContract = new CustomerContract("customerId", List.of());
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.just(customerContract));

        // When
        accountingPointDataService.fetchMeteringPointSegment(permissionId, usagePointId);

        // Then
        verify(outbox).commit(simpleEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionId, simpleEventCaptor.getValue().permissionId()),
                () -> assertEquals(PermissionProcessStatus.INVALID, simpleEventCaptor.getValue().status())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {
            401, // Unauthorized, i.e. token expired
            429 // TooManyRequests
    })
    void fetchMeteringPointSegmentThrowsUnauthorizedOrTooManyRequests_retriesRequest(int statusCode) {
        // Given
        when(enedisApi.getContract(usagePointId))
                .thenReturn(Mono.error(WebClientResponseException.create(statusCode,
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)))
                .thenReturn(Mono.just(customerContract("C5")));
        VirtualTimeScheduler.getOrSet(); // yes, this is necessary

        // When
        accountingPointDataService.fetchMeteringPointSegment(permissionId, usagePointId);

        // Then
        StepVerifier.withVirtualTime(() -> {
                        accountingPointDataService.fetchMeteringPointSegment(permissionId, usagePointId);
                        return Mono.empty();
                    })
                    .thenAwait(Duration.ofMinutes(2))
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        verify(enedisApi, times(2)).getContract(usagePointId);
        verify(outbox).commit(usagePointTypeEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionId, usagePointTypeEventCaptor.getValue().permissionId()),
                () -> assertEquals(UsagePointType.CONSUMPTION, usagePointTypeEventCaptor.getValue().usagePointType())
        );
    }

    @Test
    void fetchMeteringPointSegmentThrowsForbidden_unfulfillablePermissionRequest() {
        // Given
        when(enedisApi.getContract(usagePointId))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(),
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)));
        // When
        accountingPointDataService.fetchMeteringPointSegment(permissionId, usagePointId);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())));
    }

    @Test
    void fetchAccountingPointData_emitsAccountingPointDataAndFulfilledEvent() {
        // Given
        var permissionRequest = permissionRequest();
        var customerContract = new CustomerContract("customerId", List.of());
        var customerAddress = new CustomerAddress("customerId", List.of());
        var customerIdentity = new CustomerIdentity("customerId", new Identity(Optional.empty(), Optional.empty()));
        var customerContact = new CustomerContact("customerId", new Contact("email", "phone"));
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.just(customerContract));
        when(enedisApi.getAddress(usagePointId)).thenReturn(Mono.just(customerAddress));
        when(enedisApi.getIdentity(usagePointId)).thenReturn(Mono.just(customerIdentity));
        when(enedisApi.getContact(usagePointId)).thenReturn(Mono.just(customerContact));
        StepVerifier.Step<IdentifiableAccountingPointData> stepVerifier = StepVerifier
                .create(sink.asFlux())
                .assertNext(data -> assertAll(
                        () -> assertEquals(permissionRequest, data.permissionRequest()),
                        () -> assertEquals(customerContract, data.contract()),
                        () -> assertEquals(customerAddress, data.address()),
                        () -> assertEquals(customerIdentity, data.identity()),
                        () -> assertEquals(customerContact, data.contact())
                ))
                .then(() -> sink.tryEmitComplete());

        // When
        accountingPointDataService.fetchAccountingPointData(permissionRequest, usagePointId);

        // Then
        stepVerifier.verifyComplete();
        verify(outbox).commit(simpleEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionId, simpleEventCaptor.getValue().permissionId()),
                () -> assertEquals(PermissionProcessStatus.FULFILLED, simpleEventCaptor.getValue().status())
        );
    }

    private FrEnedisPermissionRequest permissionRequest() {
        return new SimpleFrEnedisPermissionRequest(
                "usagePointId",
                null,
                UsagePointType.CONSUMPTION,
                Optional.empty(),
                "permissionId",
                "connectionId",
                "dataNeedId",
                PermissionProcessStatus.ACCEPTED,
                new EnedisDataSourceInformation(),
                ZonedDateTime.now(),
                LocalDate.now(),
                LocalDate.now()
        );
    }

    @Test
    void fetchAccountingPointData_emitsUnfulfillable() {
        // Given
        var permissionRequest = permissionRequest();
        var customerContract = new CustomerContract("customerId", List.of());
        var customerAddress = new CustomerAddress("customerId", List.of());
        var customerIdentity = new CustomerIdentity("customerId", new Identity(Optional.empty(), Optional.empty()));
        var customerContact = new CustomerContact("customerId", new Contact("email", "phone"));
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.just(customerContract));
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.error(
                WebClientResponseException.create(
                        HttpStatus.FORBIDDEN.value(),
                        "",
                        null,
                        null,
                        null
                )
        ));
        when(enedisApi.getAddress(usagePointId)).thenReturn(Mono.just(customerAddress));
        when(enedisApi.getIdentity(usagePointId)).thenReturn(Mono.just(customerIdentity));
        when(enedisApi.getContact(usagePointId)).thenReturn(Mono.just(customerContact));


        // When
        accountingPointDataService.fetchAccountingPointData(permissionRequest, usagePointId);

        // Then
        verifyNoInteractions(sink);
        verify(outbox).commit(simpleEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionId, simpleEventCaptor.getValue().permissionId()),
                () -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, simpleEventCaptor.getValue().status())
        );
    }
}
