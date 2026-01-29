// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountingPointDataFulfillmentTaskTest {
    @Spy
    private final IdentifiableDataStreams streams = new IdentifiableDataStreams();
    @Mock
    private Outbox outbox;
    @InjectMocks
    @SuppressWarnings("unused")
    private AccountingPointDataFulfillmentTask task;

    @Test
    void testTask_emitsFulfilled() {
        // Given

        // When
        streams.publishAccountingPointData(
                new CdsPermissionRequestBuilder()
                        .setPermissionId("pid")
                        .build(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        // Then
        verify(outbox).commit(assertArg(e -> assertAll(
                () -> assertEquals("pid", e.permissionId()),
                () -> assertEquals(PermissionProcessStatus.FULFILLED, e.status())
        )));
    }
}