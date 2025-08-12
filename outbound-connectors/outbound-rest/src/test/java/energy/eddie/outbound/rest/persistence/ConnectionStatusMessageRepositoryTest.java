package energy.eddie.outbound.rest.persistence;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.rest.TestDataSourceInformation;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.persistence.specifications.InsertionTimeSpecification;
import energy.eddie.outbound.rest.persistence.specifications.JsonPathSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ConnectionStatusMessageRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");
    @Autowired
    private ConnectionStatusMessageRepository connectionStatusMessageRepository;

    @Test
    void specificationForJsonPath_returnsCorrectConnectionStatusMessage() {
        // Given
        var spec = new JsonPathSpecification<ConnectionStatusMessageModel>("permissionId", "pid");
        var payload = new ConnectionStatusMessage(
                "cid",
                "pid",
                "dnid",
                new TestDataSourceInformation(
                        "at",
                        "at-eda",
                        "eda",
                        "eda"
                ),
                PermissionProcessStatus.ACCEPTED
        );
        var otherPayload = new ConnectionStatusMessage(
                "cid",
                "other-pid",
                "dnid",
                new TestDataSourceInformation(
                        "at",
                        "at-eda",
                        "eda",
                        "eda"
                ),
                PermissionProcessStatus.ACCEPTED
        );
        var csm = connectionStatusMessageRepository.save(new ConnectionStatusMessageModel(payload));
        connectionStatusMessageRepository.save(new ConnectionStatusMessageModel(otherPayload));

        // When
        var res = connectionStatusMessageRepository.findAll(spec);

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(csm);
    }

    @Test
    void specificationForNestedJsonPath_returnsCorrectConnectionStatusMessage() {
        // Given
        var spec = new JsonPathSpecification<ConnectionStatusMessageModel>(List.of("dataSourceInformation",
                                                                                   "countryCode"), "be");
        var payload = new ConnectionStatusMessage(
                "cid",
                "pid",
                "dnid",
                new TestDataSourceInformation(
                        "be",
                        "be-fluvius",
                        "fluvius",
                        "fluvius"
                ),
                PermissionProcessStatus.ACCEPTED
        );
        var otherPayload = new ConnectionStatusMessage(
                "cid",
                "other-pid",
                "dnid",
                new TestDataSourceInformation(
                        "at",
                        "at-eda",
                        "eda",
                        "eda"
                ),
                PermissionProcessStatus.ACCEPTED
        );
        var csm = connectionStatusMessageRepository.save(new ConnectionStatusMessageModel(payload));
        connectionStatusMessageRepository.save(new ConnectionStatusMessageModel(otherPayload));

        // When
        var res = connectionStatusMessageRepository.findAll(spec);

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(csm);
    }

    @Test
    void specificationFromTo_returnsCorrectConnectionStatusMessage() {
        // Given
        var payload = new ConnectionStatusMessage(
                "cid",
                "pid",
                "dnid",
                new TestDataSourceInformation(
                        "be",
                        "be-fluvius",
                        "fluvius",
                        "fluvius"
                ),
                PermissionProcessStatus.ACCEPTED
        );
        connectionStatusMessageRepository.save(new ConnectionStatusMessageModel(payload));
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var from = now.minusMinutes(1);
        var to = now.plusMinutes(1);
        var spec = InsertionTimeSpecification.<ConnectionStatusMessageModel>insertedAfterEquals(from)
                                             .and(InsertionTimeSpecification.insertedBeforeEquals(to));

        // When
        var res = connectionStatusMessageRepository.findAll(spec);

        // Then
        assertThat(res).hasSize(1);
    }

    @Test
    void specificationFromTo_returnsNoConnectionStatusMessage() {
        // Given
        var payload = new ConnectionStatusMessage(
                "cid",
                "pid",
                "dnid",
                new TestDataSourceInformation(
                        "be",
                        "be-fluvius",
                        "fluvius",
                        "fluvius"
                ),
                PermissionProcessStatus.ACCEPTED
        );
        connectionStatusMessageRepository.save(new ConnectionStatusMessageModel(payload));
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var from = now.minusDays(1);
        var to = now.minusHours(1);
        var spec = InsertionTimeSpecification.<ConnectionStatusMessageModel>insertedAfterEquals(from)
                                             .and(InsertionTimeSpecification.insertedBeforeEquals(to));

        // When
        var res = connectionStatusMessageRepository.findAll(spec);

        // Then
        assertThat(res).isEmpty();
    }
}