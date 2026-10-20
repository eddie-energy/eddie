// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.EnedisAccountingPointDataApi;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.address.AddressData;
import energy.eddie.regionconnector.fr.enedis.dto.address.InstallationAddress;
import energy.eddie.regionconnector.fr.enedis.dto.address.UsagePointGeneralData;
import energy.eddie.regionconnector.fr.enedis.dto.situation.ContractualSituation;
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
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private EnergyDataStreams streams = new EnergyDataStreams();
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

    @ParameterizedTest
    @MethodSource("validSegments")
    void fetchMeteringPointSegment_whenValidSegment_emitsUsagePointTypeEvent(
            List<String> segments,
            UsagePointType expected
    ) {
        // Given
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.just(List.of(situation(segments))));

        // When
        accountingPointDataService.fetchMeteringPointSegment(permissionId, usagePointId);

        // Then
        verify(outbox).commit(usagePointTypeEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionId, usagePointTypeEventCaptor.getValue().permissionId()),
                () -> assertEquals(expected, usagePointTypeEventCaptor.getValue().usagePointType())
        );
    }

    @Test
    void fetchMeteringPointSegment_withMultipleSituations_unionsSegments() {
        // Given
        when(enedisApi.getContract(usagePointId))
                .thenReturn(Mono.just(List.of(situation(List.of("C5")), situation(List.of("P4")))));

        // When
        accountingPointDataService.fetchMeteringPointSegment(permissionId, usagePointId);

        // Then
        verify(outbox).commit(usagePointTypeEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionId, usagePointTypeEventCaptor.getValue().permissionId()),
                () -> assertEquals(UsagePointType.CONSUMPTION_AND_PRODUCTION,
                                   usagePointTypeEventCaptor.getValue().usagePointType())
        );
    }

    @ParameterizedTest
    @MethodSource("invalidSegments")
    void fetchMeteringPointSegment_whenSegmentInvalid_emitsInvalidEvent(@Nullable List<String> segments) {
        // Given
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.just(List.of(situation(segments))));

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
    void fetchMeteringPointSegment_whenNoSituation_emitsInvalidEvent() {
        // Given
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.just(List.of()));

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
                .thenReturn(Mono.just(List.of(situation(List.of("C5")))));
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
        var situation = situation(List.of("C5"));
        var generalData = new UsagePointGeneralData(
                new AddressData(new InstallationAddress(null, null, null, null, null, "75112")));
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.just(List.of(situation)));
        when(enedisApi.getAddress(usagePointId)).thenReturn(Mono.just(generalData));
        StepVerifier.Step<IdentifiableAccountingPointData> stepVerifier = StepVerifier
                .create(streams.getAccountingPointData())
                .assertNext(data -> assertAll(
                        () -> assertEquals(permissionRequest, data.permissionRequest()),
                        () -> assertEquals(List.of(situation), data.situations()),
                        () -> assertEquals(generalData, data.generalData())
                ))
                .then(streams::close);

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

    @Test
    void fetchAccountingPointData_emitsUnfulfillable() {
        // Given
        var permissionRequest = permissionRequest();
        var generalData = new UsagePointGeneralData(
                new AddressData(new InstallationAddress(null, null, null, null, null, "75112")));
        when(enedisApi.getContract(usagePointId)).thenReturn(Mono.error(
                WebClientResponseException.create(
                        HttpStatus.FORBIDDEN.value(),
                        "",
                        null,
                        null,
                        null
                )
        ));
        when(enedisApi.getAddress(usagePointId)).thenReturn(Mono.just(generalData));

        // When
        accountingPointDataService.fetchAccountingPointData(permissionRequest, usagePointId);

        // Then
        verifyNoInteractions(streams);
        verify(outbox).commit(simpleEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionId, simpleEventCaptor.getValue().permissionId()),
                () -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, simpleEventCaptor.getValue().status())
        );
    }

    private static Stream<Arguments> validSegments() {
        return Stream.of(
                Arguments.of(List.of("C5"), UsagePointType.CONSUMPTION),
                Arguments.of(List.of("P4"), UsagePointType.PRODUCTION),
                Arguments.of(List.of("C4", "P5"), UsagePointType.CONSUMPTION_AND_PRODUCTION)
        );
    }

    private static Stream<Arguments> invalidSegments() {
        return Stream.of(
                Arguments.of(List.of("XXX")),
                Arguments.of(List.of()),
                Arguments.of((Object) null)
        );
    }

    private static ContractualSituation situation(@Nullable List<String> segments) {
        return new ContractualSituation(
                "usagePointId", null, null, null, null,
                null, null, null, null, null, null, null,
                segments, null, null, null, null
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
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC)
        );
    }
}
