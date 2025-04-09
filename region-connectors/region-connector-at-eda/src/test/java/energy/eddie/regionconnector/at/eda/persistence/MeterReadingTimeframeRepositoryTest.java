package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.permission.request.events.DataReceivedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.projections.MeterReadingTimeframe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MeterReadingTimeframeRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private EdaPermissionEventRepository permissionEventRepository;
    @Autowired
    private MeterReadingTimeframeRepository timeframeRepository;

    @Test
    void testFindAllByPermissionId_withMultiplePermissionRequests_returnsCorrectTimeframes() {
        // Given
        var event1 = createEvent("pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10));
        var event2 = createEvent("other", LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 10));
        permissionEventRepository.saveAllAndFlush(List.of(event1, event2));

        // When
        var res = timeframeRepository.findAllByPermissionId("pid");

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(
                        new MeterReadingTimeframe(1L, "pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10))
                );
    }

    @Test
    void testFindAllByPermissionId_withMissingTimeframeInBetween_returnsCorrectTimeframes() {
        // Given
        var event1 = createEvent("pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10));
        var event2 = createEvent("pid", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 20));
        permissionEventRepository.saveAllAndFlush(List.of(event1, event2));

        // When
        var res = timeframeRepository.findAllByPermissionId("pid");

        // Then
        assertThat(res)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        new MeterReadingTimeframe(1L, "pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10)),
                        new MeterReadingTimeframe(1L, "pid", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 20))
                );
    }

    @Test
    void testFindAllByPermissionId_withConsecutiveDays_returnsCorrectTimeframes() {
        // Given
        var event1 = createEvent("pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10));
        var event2 = createEvent("pid", LocalDate.of(2024, 1, 11), LocalDate.of(2024, 1, 20));
        permissionEventRepository.saveAllAndFlush(List.of(event1, event2));

        // When
        var res = timeframeRepository.findAllByPermissionId("pid");

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(
                        new MeterReadingTimeframe(1L, "pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 20))
                );
    }

    @Test
    void testFindAllByPermissionId_whereOneEventIncludesTheOther_returnsCorrectTimeframes() {
        // Given
        var event1 = createEvent("pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10));
        var event2 = createEvent("pid", LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 9));
        permissionEventRepository.saveAllAndFlush(List.of(event1, event2));

        // When
        var res = timeframeRepository.findAllByPermissionId("pid");

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(
                        new MeterReadingTimeframe(1L, "pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10))
                );
    }

    @Test
    void testFindAllByPermissionId_withFrontOverlap_returnsCorrectTimeframes() {
        // Given
        var event1 = createEvent("pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10));
        var event2 = createEvent("pid", LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 15));
        permissionEventRepository.saveAllAndFlush(List.of(event1, event2));

        // When
        var res = timeframeRepository.findAllByPermissionId("pid");

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(
                        new MeterReadingTimeframe(1L, "pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15))
                );
    }

    @Test
    void testFindAllByPermissionId_withConsecutiveOverlaps_returnsCorrectTimeframes() {
        // Given
        var event1 = createEvent("pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10));
        var event2 = createEvent("pid", LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 15));
        var event3 = createEvent("pid", LocalDate.of(2024, 1, 12), LocalDate.of(2024, 1, 20));
        permissionEventRepository.saveAllAndFlush(List.of(event1, event2, event3));

        // When
        var res = timeframeRepository.findAllByPermissionId("pid");

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(
                        new MeterReadingTimeframe(1L, "pid", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 20))
                );
    }

    private static DataReceivedEvent createEvent(String permissionId, LocalDate start, LocalDate end) {
        return new DataReceivedEvent(permissionId,
                                     PermissionProcessStatus.ACCEPTED,
                                     start,
                                     end);
    }
}