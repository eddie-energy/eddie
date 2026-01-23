// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.CimTestConfiguration;
import energy.eddie.regionconnector.es.datadis.health.DatadisApiHealthIndicator;
import energy.eddie.regionconnector.es.datadis.permission.events.EsCreatedEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(CimTestConfiguration.class)
class EsPermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");
    @Autowired
    private EsPermissionRequestRepository permissionRequestRepository;
    @Autowired
    private EsPermissionEventRepository permissionEventRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private DatadisApiHealthIndicator healthIndicator;
    @SuppressWarnings("unused")
    @MockitoBean
    private DataNeedsService dataNeedsService;


    @Test
    void testFindStalePermissionRequests_returnsCorrectPermissionRequests() {
        // Given
        var lateClock = Clock.fixed(
                Instant.now(Clock.systemUTC()).minus(25, ChronoUnit.HOURS),
                ZoneOffset.UTC
        );
        permissionEventRepository.saveAll(List.of(
                new EsCreatedEvent("pid", "cid", "dnid", "nif", "mid", lateClock),
                new EsSimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR),
                new EsCreatedEvent("otherPid", "cid", "dnid", "nif", "mid"),
                new EsSimpleEvent("otherPid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
        ));

        // When
        var res = permissionRequestRepository.findStalePermissionRequests(24);

        // Then
        assertEquals(1, res.size());
        var pr = res.getFirst();
        assertEquals("pid", pr.permissionId());
    }
}