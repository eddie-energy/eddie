// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.si.moj.elektro;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.si.moj.elektro.permission.request.MojElektroPermissionRequest;
import energy.eddie.regionconnector.si.moj.elektro.persistence.SiPermissionEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MojElektroBeanConfig.class)
class MojElektroBeanConfigTest {

    @MockitoBean
    private SiPermissionEventRepository siPermissionEventRepository;
    @MockitoBean
    private ConnectionStatusMessageHandler<MojElektroPermissionRequest> connectionStatusMessageHandler;
    @MockitoBean
    private PermissionMarketDocumentMessageHandler<MojElektroPermissionRequest> permissionMarketDocumentMessageHandler;
    @MockitoBean
    private DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    @Autowired
    private Supplier<PermissionEventRepository> permissionEventSupplier;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private Outbox outbox;

    @Test
    void testBeansLoaded() {
        assertNotNull(eventBus);
        assertNotNull(outbox);
        assertNotNull(connectionStatusMessageHandler);
        assertNotNull(permissionMarketDocumentMessageHandler);
        assertNotNull(dataNeedCalculationService);
        assertNotNull(permissionEventSupplier);
    }

    @Test
    void testPermissionEventSupplier() {
        assertSame(siPermissionEventRepository, permissionEventSupplier.get());
    }
}
