package energy.eddie.aiida.services;

import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link PermissionService.ConcurrentHashMapConfig#expirationFutures()} returns a {@link ConcurrentHashMap}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = PermissionService.class)
@Import(PermissionService.ConcurrentHashMapConfig.class)
class ConcurrentHashMapConfigTest {
    @MockBean
    PermissionRepository repository;
    @MockBean
    Clock clock;
    @MockBean
    StreamerManager manager;
    @MockBean
    TaskScheduler scheduler;
    @MockBean
    PermissionService service;
    @Autowired
    private ApplicationContext context;

    @Test
    void testExpirationFuturesBean() {
        assertTrue(context.containsBean("expirationFutures"));
        assertNotNull(context.getBean("expirationFutures"));
        assertTrue(context.getBean("expirationFutures") instanceof ConcurrentHashMap);
    }
}
