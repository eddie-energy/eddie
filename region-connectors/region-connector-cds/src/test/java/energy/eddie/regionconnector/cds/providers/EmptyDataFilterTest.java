// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.vhd.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmptyDataFilterTest {
    @Mock
    private Outbox outbox;
    @InjectMocks
    private EmptyDataFilter filter;

    @Test
    void testFilter_forNonEmptyUsageSegments_returnsTrue() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var data = new IdentifiableValidatedHistoricalData(
                pr,
                new IdentifiableValidatedHistoricalData.Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                        List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                        List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner())
                )
        );

        // When
        var res = filter.test(data);

        // Then
        assertTrue(res);
        verify(outbox, never()).commit(any());
    }

    @Test
    void testFilter_forEmptyUsageSegments_returnsFalse() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var data = new IdentifiableValidatedHistoricalData(
                pr,
                new IdentifiableValidatedHistoricalData.Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                        List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                        List.of()
                )
        );

        // When
        var res = filter.test(data);

        // Then
        assertFalse(res);
        verify(outbox).commit(assertArg(e -> assertAll(
                () -> assertEquals("pid", e.permissionId()),
                () -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, e.status())
        )));
    }
}