// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.ee.elering;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.ee.elering.persistence.EePermissionEventRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
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
@SpringBootTest(classes = EleringBeanConfig.class)
class EleringBeanConfigTest {

    @MockitoBean
    private EePermissionEventRepository eePermissionEventRepository;
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
        assertNotNull(permissionEventSupplier);
    }

    @Test
    void testPermissionEventSupplier() {
        assertSame(eePermissionEventRepository, permissionEventSupplier.get());
    }
}
