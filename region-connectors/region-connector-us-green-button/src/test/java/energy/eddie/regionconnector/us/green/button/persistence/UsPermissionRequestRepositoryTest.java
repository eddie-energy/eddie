package energy.eddie.regionconnector.us.green.button.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.GreenButtonDataSourceInformation;
import energy.eddie.regionconnector.us.green.button.permission.events.UsCreatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UsPermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private UsPermissionRequestRepository permissionRequestRepository;
    @Autowired
    private UsPermissionEventRepository permissionEventRepository;

    @Test
    void testNumberOfPermissionRequests() {
        // Given
        var dataSourceInformation = new GreenButtonDataSourceInformation("TEST", "US");
        permissionEventRepository.saveAndFlush(new UsCreatedEvent("pid", "cid", "dnid", "test", dataSourceInformation));
        permissionEventRepository.saveAndFlush(new UsSimpleEvent("pid", PermissionProcessStatus.VALIDATED));
        permissionEventRepository.saveAndFlush(new UsCreatedEvent("otherPid",
                                                                  "cid",
                                                                  "dnid",
                                                                  "test",
                                                                  dataSourceInformation));
        permissionEventRepository.saveAndFlush(new UsSimpleEvent("otherPid", PermissionProcessStatus.VALIDATED));


        // When
        var res = permissionRequestRepository.findAll();
        // Calculate the size by iterating through the Iterable
        long size = 0;
        for (var ignored : res) {size++;}

        // Then
        assertEquals(2, size);
    }
}
