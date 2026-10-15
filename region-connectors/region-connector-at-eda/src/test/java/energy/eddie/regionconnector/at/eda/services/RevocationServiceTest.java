// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.data.needs.EnergyDirection;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaCMRevoke;
import energy.eddie.regionconnector.at.eda.handlers.integration.inbound.AtPermissionRequestProjectionTest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequestBuilder;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevocationServiceTest {
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<SimpleEvent> captor;

    @Test
    void revokeWithConsentId_revokesPermissionRequest() {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        var permissionRequest = new EdaPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .build();
        TestPublisher<EdaCMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var repository = mock(AtPermissionRequestRepository.class);
        when(repository.findByConsentId("consentId")).thenReturn(Optional.of(projection(permissionRequest)));
        EdaCMRevoke cmRevoke = new SimpleEdaCMRevoke().setConsentId("consentId");
        new RevocationService(edaAdapter, repository, outbox);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        verify(outbox).commit(captor.capture());
        assertEquals(PermissionProcessStatus.REVOKED, captor.getValue().status());
    }

    @Test
    void revokeWithoutConsentId_revokesPermissionRequest() {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        var permissionRequest = new EdaPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .build();
        TestPublisher<EdaCMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var repository = mock(AtPermissionRequestRepository.class);
        when(repository.findByConsentId("consentId")).thenReturn(Optional.empty());

        when(repository.findAcceptedAndFulfilledByMeteringPointIdAndDate(anyString(), any()))
                .thenReturn(List.of(projection(permissionRequest)));
        EdaCMRevoke cmRevoke = new SimpleEdaCMRevoke()
                .setConsentEnd(now)
                .setMeteringPoint("mpid")
                .setConsentId("consentId");
        new RevocationService(edaAdapter, repository, outbox);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        verify(outbox).commit(captor.capture());
        assertEquals(PermissionProcessStatus.REVOKED, captor.getValue().status());
    }

    @Test
    void revokeWithConsentId_fulfilledPermissionRequest_doesNotRevoke() {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        var permissionRequest = new EdaPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.FULFILLED)
                .build();
        TestPublisher<EdaCMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var repository = mock(AtPermissionRequestRepository.class);
        when(repository.findByConsentId("consentId")).thenReturn(Optional.of(projection(permissionRequest)));
        EdaCMRevoke cmRevoke = new SimpleEdaCMRevoke().setConsentId("consentId");
        new RevocationService(edaAdapter, repository, outbox);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        verifyNoInteractions(outbox);
        verify(repository, never()).findAcceptedAndFulfilledByMeteringPointIdAndDate(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"CREATED", "FULFILLED"})
    void revokeWithWrongPermissionState_doesNotRevokePermissionRequest(PermissionProcessStatus status) {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        var permissionRequest = new EdaPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(status)
                .build();
        TestPublisher<EdaCMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var repository = mock(AtPermissionRequestRepository.class);
        when(repository.findByConsentId("consentId")).thenReturn(Optional.empty());

        when(repository.findAcceptedAndFulfilledByMeteringPointIdAndDate(anyString(), any()))
                .thenReturn(List.of(projection(permissionRequest)));
        EdaCMRevoke cmRevoke = new SimpleEdaCMRevoke()
                .setConsentEnd(now)
                .setMeteringPoint("mpid")
                .setConsentId("consentId");
        new RevocationService(edaAdapter, repository, outbox);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        verify(outbox, never()).commit(any());
    }

    private static AtPermissionRequestProjection projection(EdaPermissionRequest epm) {
        return new AtPermissionRequestProjectionTest(
                epm.permissionId(),
                epm.connectionId(),
                epm.cmRequestId(),
                epm.conversationId(),
                LocalDate.now(ZoneId.systemDefault()),
                LocalDate.now(ZoneId.systemDefault()),
                epm.dataNeedId(),
                "dnid",
                null,
                null,
                epm.message(),
                AllowedGranularity.PT15M.name(),
                epm.status().name(),
                Instant.now(),
                EnergyDirection.CONSUMPTION, 100);
    }
}
