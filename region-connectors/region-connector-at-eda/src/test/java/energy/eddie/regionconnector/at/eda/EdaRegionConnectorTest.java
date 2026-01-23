// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdaRegionConnectorTest {
    @Mock
    private EdaAdapter edaAdapter;
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;

    @Test
    void connectorConstructs() {
        // given

        // when
        // then
        assertDoesNotThrow(() -> new EdaRegionConnector(edaAdapter, repository, outbox));
    }

    @Test
    void terminateNonExistingPermission_doesNothing() throws TransmissionException {
        // given
        when(repository.findByPermissionId("permissionId")).thenReturn(Optional.empty());
        var regionConnector = new EdaRegionConnector(edaAdapter, repository, null);

        // when
        // then
        assertDoesNotThrow(() -> regionConnector.terminatePermission("permissionId"));

        verify(outbox, never()).commit(any());
    }

    @Test
    void terminateExistingPermission_emitsTermination() throws TransmissionException {
        // given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", "mid", "dsoId", start, end,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.ACCEPTED, "",
                                                         "consentId", ZonedDateTime.now(ZoneOffset.UTC));
        when(repository.findByPermissionId("permissionId")).thenReturn(Optional.of(permissionRequest));
        var regionConnector = new EdaRegionConnector(edaAdapter, repository, outbox);

        // when
        regionConnector.terminatePermission("permissionId");

        // then
        verify(outbox, times(2)).commit(any());
    }

    @Test
    void getMetadata_returnExpectedMetadata() throws TransmissionException {
        // given
        var regionConnector = new EdaRegionConnector(edaAdapter, repository, outbox);

        // when
        var result = regionConnector.getMetadata();

        // then
        assertEquals(EdaRegionConnectorMetadata.getInstance(), result);
    }

    @Test
    void close_ClosesRelatedResources() throws Exception {

        var regionConnector = new EdaRegionConnector(edaAdapter, repository, outbox);

        regionConnector.close();

        verify(edaAdapter).close();
    }
}
