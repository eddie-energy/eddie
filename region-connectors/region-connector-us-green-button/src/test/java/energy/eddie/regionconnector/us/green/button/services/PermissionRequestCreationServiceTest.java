// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingCredentialsException;
import energy.eddie.regionconnector.us.green.button.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.us.green.button.permission.events.UsCreatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMalformedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsValidatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRequestCreationServiceTest {
    @SuppressWarnings("unused")
    @Spy
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "http://localhost",
            Map.of("company", "client-id", "only-id", "client-id", "missing-token", "client-id"),
            Map.of("company", "client-secret", "only-secret", "client-secret", "missing-token", "client-secret"),
            Map.of("company", "token", "only-id", "token", "only-secret", "token"),
            "http://localhost",
            "secret");
    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private PermissionRequestCreationService creationService;

    @ParameterizedTest
    @MethodSource("createPermissionRequest_returnsPermissionRequest")
    void createPermissionRequest_returnsPermissionRequest(DataNeedCalculationResult calc) throws DataNeedNotFoundException, UnsupportedDataNeedException, MissingCredentialsException {
        // Given
        when(calculationService.calculate("dnid"))
                .thenReturn(calc);

        // When
        var res = creationService.createPermissionRequest(new PermissionRequestForCreation(
                "cid",
                "dnid",
                "http://localhost",
                "company",
                "US"
        ));

        // Then
        verify(outbox).commit(isA(UsCreatedEvent.class));
        verify(outbox).commit(isA(UsValidatedEvent.class));
        assertAll(
                () -> assertNotNull(res.permissionId()),
                () -> assertTrue(res.redirectUri().toString().startsWith("http://localhost"))
        );
    }

    @Test
    void createPermissionRequest_throwsOnUnknownDataNeedId() {
        // Given
        var permissionRequestForCreation = new PermissionRequestForCreation(
                "cid",
                "dnid",
                "http://localhost",
                "company",
                "US"
        );
        when(calculationService.calculate("dnid")).thenReturn(new DataNeedNotFoundResult());

        // When
        // Then
        assertThrows(DataNeedNotFoundException.class,
                     () -> creationService.createPermissionRequest(permissionRequestForCreation));
        verify(outbox).commit(isA(UsCreatedEvent.class));
        verify(outbox).commit(isA(UsMalformedEvent.class));
    }

    @Test
    void createPermissionRequest_withUnsupportedDataNeed_throws() {
        // Given
        when(calculationService.calculate("dnid"))
                .thenReturn(new DataNeedNotSupportedResult(""));

        var permissionRequestForCreation = new PermissionRequestForCreation(
                "cid",
                "dnid",
                "http://localhost",
                "company",
                "US"
        );

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> creationService.createPermissionRequest(permissionRequestForCreation));
        verify(outbox).commit(isA(UsCreatedEvent.class));
        verify(outbox).commit(isA(UsMalformedEvent.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"only-id", "only-secret", "missing-token"})
    void createPermissionRequest_throwsOnInvalidConfiguration(String company) {
        // Given
        var permissionRequestForCreation = new PermissionRequestForCreation(
                "cid",
                "dnid",
                "http://localhost",
                company,
                "US"
        );

        // When
        // Then
        assertThrows(Exception.class, () -> creationService.createPermissionRequest(permissionRequestForCreation));
        verify(outbox).commit(isA(UsCreatedEvent.class));
        verify(outbox).commit(isA(UsMalformedEvent.class));
    }

    @Test
    void testFindDataNeedIdByPermissionId_returnsDataNeedId() {
        // Given
        var pr = getPermissionRequest();
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));

        // When
        var res = creationService.findDataNeedIdByPermissionId("pid");

        // Then
        assertThat(res)
                .isPresent()
                .contains("dnid");
    }

    private static Stream<Arguments> createPermissionRequest_returnsPermissionRequest() {
        var now = LocalDate.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(
                        new ValidatedHistoricalDataDataNeedResult(
                                List.of(Granularity.PT15M),
                                new Timeframe(now, now),
                                new Timeframe(now.minusDays(10), now.minusDays(1))
                        )
                ),
                Arguments.of(new AccountingPointDataNeedResult(new Timeframe(now, now)))
        );
    }

    private static GreenButtonPermissionRequest getPermissionRequest() {
        return new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                        .setConnectionId("cid")
                                                        .setDataNeedId("dnid")
                                                        .setStatus(PermissionProcessStatus.ACCEPTED)
                                                        .build();
    }
}