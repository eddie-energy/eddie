// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import energy.eddie.regionconnector.fi.fingrid.services.cim.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FulfillmentTaskForAccountingPointDataTest {
    @Mock
    private Outbox outbox;
    @Mock
    private EnergyDataService energyDataService;

    @Test
    void givenAccountingPointData_whenReceived_thenFulfillPermissionRequest() {
        // Given
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var resp = TestResourceProvider.readCustomerDataFromFile(TestResourceProvider.CUSTOMER_DATA_JSON);
        var ap = new IdentifiableAccountingPointData(pr, resp);
        var publisher = TestPublisher.<IdentifiableAccountingPointData>create();
        when(energyDataService.getIdentifiableAccountingPointDataStream()).thenReturn(publisher.flux());

        // When
        new FulfillmentTaskForAccountingPointData(energyDataService, outbox);
        publisher.emit(ap);
        publisher.complete();

        // Then
        verify(outbox).commit(assertArg(ev -> assertAll(
                () -> assertEquals("pid", ev.permissionId()),
                () -> assertEquals(PermissionProcessStatus.FULFILLED, ev.status())
        )));
    }
}