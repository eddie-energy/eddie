// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.persistence;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.GreenButtonDataSourceInformation;
import energy.eddie.regionconnector.us.green.button.permission.events.UsCreatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsValidatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UsPermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private UsPermissionRequestRepository permissionRequestRepository;
    @Autowired
    private UsPermissionEventRepository permissionEventRepository;

    @Test
    void testNumberOfPermissionRequests() {
        // Given
        var dataSourceInformation = new GreenButtonDataSourceInformation("TEST", "US");
        permissionEventRepository.saveAndFlush(new UsCreatedEvent("pid",
                                                                  "cid",
                                                                  "dnid",
                                                                  "test",
                                                                  dataSourceInformation));
        permissionEventRepository.saveAndFlush(new UsCreatedEvent("otherPid",
                                                                  "cid",
                                                                  "dnid",
                                                                  "test",
                                                                  dataSourceInformation));


        // When
        var res = permissionRequestRepository.findAll();

        // Then
        assertEquals(2, res.spliterator().getExactSizeIfKnown());
    }

    @Test
    void testFindTimedOutPermissionRequests_findTimedOutPermissionRequests() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var dataSourceInformation = new GreenButtonDataSourceInformation("company", "US");
        permissionEventRepository.saveAndFlush(new UsCreatedEvent("pid",
                                                                  "cid",
                                                                  "dnid",
                                                                  "",
                                                                  dataSourceInformation,
                                                                  now.minusHours(25)));
        permissionEventRepository.saveAndFlush(new UsSimpleEvent("pid", PermissionProcessStatus.VALIDATED));
        permissionEventRepository.saveAndFlush(new UsCreatedEvent("otherPid",
                                                                  "cid",
                                                                  "dnid",
                                                                  "",
                                                                  dataSourceInformation,
                                                                  now));
        permissionEventRepository.saveAndFlush(new UsSimpleEvent("otherPid", PermissionProcessStatus.VALIDATED));


        // When
        var res = permissionRequestRepository.findStalePermissionRequests(24);

        // Then
        assertEquals(1, res.size());
        var pr = res.getFirst();
        assertEquals("pid", pr.permissionId());
    }

    @Test
    void testFindActiveOutPermissionRequests_findsActivePermissionRequests() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start1 = now.minusDays(2);
        var end1 = now.plusDays(2);
        var start2 = now.plusDays(10);
        var end2 = now.plusDays(20);
        permissionEventRepository.saveAndFlush(new UsValidatedEvent("pid1", start1, end1, Granularity.PT15M, "scope"));
        permissionEventRepository.saveAndFlush(new UsSimpleEvent("pid1", PermissionProcessStatus.ACCEPTED));
        permissionEventRepository.saveAndFlush(new UsValidatedEvent("pid2", start2, end2, Granularity.PT15M, "scope"));
        permissionEventRepository.saveAndFlush(new UsSimpleEvent("pid2", PermissionProcessStatus.ACCEPTED));

        // When
        var res = permissionRequestRepository.findActivePermissionRequests();

        // Then
        assertEquals(1, res.size());
        var pr = res.getFirst();
        assertEquals("pid1", pr.permissionId());
    }
}
