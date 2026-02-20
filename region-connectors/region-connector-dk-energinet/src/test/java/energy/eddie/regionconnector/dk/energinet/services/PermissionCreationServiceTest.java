// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.events.DKValidatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkCreatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkMalformedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionCreationServiceTest {
    private static final String VALID_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0Ijo1NTE2MjM5MDIyLCJleHAiOjU1MTYyMzkwMjJ9.Gce4NCqCL64_1GvTP7gVzHkyC4kXEG0RAgAfxfNdVno";
    private static final String INVALID_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0Ijo1MTYyMzkwMjIsImV4cCI6NTE2MjM5MDIyfQ.1JANCZUSNrVsGYTN7hcHE3EI3RpycwRwsQvdkv4EV3A";
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @InjectMocks
    private PermissionCreationService service;
    @Captor
    private ArgumentCaptor<DkCreatedEvent> createdCaptor;
    @Captor
    private ArgumentCaptor<DKValidatedEvent> validatedCaptor;

    @Test
    void testCreatePermissionRequest_emitsMalformedOnUnknownDataNeed() {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       "refreshToken",
                                                       "meteringPointId",
                                                       "dnid");
        when(calculationService.calculate("dnid")).thenReturn(new DataNeedNotFoundResult());
        // When
        // Then
        assertThrows(DataNeedNotFoundException.class, () -> service.createPermissionRequest(request));
        verify(outbox).commit(isA(DkCreatedEvent.class));
        verify(outbox).commit(isA(DkMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnUnsupportedDataNeed() {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       "refreshToken",
                                                       "meteringPointId",
                                                       "dnid");
        when(calculationService.calculate("dnid"))
                .thenReturn(new DataNeedNotSupportedResult(""));

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class, () -> service.createPermissionRequest(request));
        verify(outbox).commit(isA(DkCreatedEvent.class));
        verify(outbox).commit(isA(DkMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnAiidaDataNeed() {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       "refreshToken",
                                                       "meteringPointId",
                                                       "dnid");
        when(calculationService.calculate("dnid"))
                .thenReturn(new AiidaDataNeedResult(Set.of(),
                                                    Set.of(),
                                                    new Timeframe(LocalDate.now(), LocalDate.now())));

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class, () -> service.createPermissionRequest(request));
        verify(outbox).commit(isA(DkCreatedEvent.class));
        verify(outbox).commit(isA(DkMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnInvalidRefreshToken() {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       INVALID_REFRESH_TOKEN,
                                                       "meteringPointId",
                                                       "dnid");
        var now = LocalDate.now(ZoneOffset.UTC);
        when(calculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT15M),
                        new Timeframe(now, now.plusYears(2)),
                        new Timeframe(now.minusYears(2), now.plusYears(2))
                ));
        // When
        // Then
        assertThrows(InvalidRefreshTokenException.class, () -> service.createPermissionRequest(request));
        verify(outbox).commit(isA(DkCreatedEvent.class));
        verify(outbox).commit(isA(DkMalformedEvent.class));
    }

    @ParameterizedTest
    @EnumSource(value = Granularity.class, names = {"PT15M", "P1D", "P1M", "P1Y"})
    void testCreatePermissionRequest_emitsValidated(Granularity granularity) throws DataNeedNotFoundException, UnsupportedDataNeedException, InvalidRefreshTokenException {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       VALID_REFRESH_TOKEN,
                                                       "meteringPointId",
                                                       "dnid");
        var now = LocalDate.now(ZoneOffset.UTC);

        when(calculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(granularity),
                                                                      new Timeframe(now, now.plusYears(2)),
                                                                      new Timeframe(
                                                                              now.minusYears(2),
                                                                              now.plusYears(2)
                                                                      )
                ));

        // When
        service.createPermissionRequest(request);
        // Then
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(validatedCaptor.capture());
        var created = createdCaptor.getValue();
        assertAll(
                () -> assertEquals("cid", created.connectionId()),
                () -> assertEquals(VALID_REFRESH_TOKEN, created.refreshToken()),
                () -> assertEquals("meteringPointId", created.meteringPointId()),
                () -> assertEquals("dnid", created.dataNeedId())
        );
        var validated = validatedCaptor.getValue();
        assertAll(
                () -> assertEquals(now.minusYears(2), validated.start()),
                () -> assertEquals(now.plusYears(2), validated.end())
        );
    }

    @Test
    void testCreatePermissionRequest_forAccountingPointDataNeed_emitsValidated() throws DataNeedNotFoundException, UnsupportedDataNeedException, InvalidRefreshTokenException {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       VALID_REFRESH_TOKEN,
                                                       "meteringPointId",
                                                       "dnid");
        var now = LocalDate.now(DK_ZONE_ID);
        when(calculationService.calculate("dnid"))
                .thenReturn(new AccountingPointDataNeedResult(new Timeframe(now, now)));

        // When
        service.createPermissionRequest(request);
        // Then
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(validatedCaptor.capture());
        var created = createdCaptor.getValue();
        assertAll(
                () -> assertEquals("cid", created.connectionId()),
                () -> assertEquals(VALID_REFRESH_TOKEN, created.refreshToken()),
                () -> assertEquals("meteringPointId", created.meteringPointId()),
                () -> assertEquals("dnid", created.dataNeedId())
        );
        var validated = validatedCaptor.getValue();
        assertAll(
                () -> assertEquals(now, validated.start()),
                () -> assertEquals(now, validated.end())
        );
    }
}
