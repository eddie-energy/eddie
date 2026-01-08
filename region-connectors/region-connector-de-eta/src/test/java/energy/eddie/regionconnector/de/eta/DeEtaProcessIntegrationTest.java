package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class DeEtaProcessIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private Outbox outbox;

    @Autowired
    private DePermissionRequestRepository repository;

    @Test
    void shouldUpdateLatestMeterReadingOnAcceptedEvent() {
        // 1. Setup: Create a permission request
        String permissionId = UUID.randomUUID().toString();

        // Emit CREATED event to initialize the Read Model
        outbox.commit(new CreatedEvent(
                permissionId,
                "data-need-1",
                "conn-1",
                "meter-1"
        ));

        // Emit VALIDATED event to set start/end dates
        outbox.commit(new ValidatedEvent(
                permissionId,
                LocalDate.now(ZoneId.of("UTC")).minusDays(10),
                LocalDate.now(ZoneId.of("UTC")).minusDays(1),
                Granularity.PT15M
        ));

        // Wait for projection
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(repository.findByPermissionId(permissionId)).isPresent()
        );

        // 2. Act: Emit ACCEPTED event
        // This should trigger: AcceptedHandler -> Stream -> MeterReadingUpdateService -> LatestMeterReadingEvent -> DB
        outbox.commit(new AcceptedEvent(permissionId));

        // 3. Assert: Verify the Read Model eventually has the latest reading
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            DePermissionRequest request = repository.findByPermissionId(permissionId).get();

            // Check Status
            assertThat(request.status()).isEqualTo(PermissionProcessStatus.ACCEPTED);

            // Check Latest Reading (This proves the entire new refactor works!)
            assertThat(request.latestReading()).isPresent();

            // Assuming your Stub returns data for 'start' date
            assertThat(request.latestReading().get().toLocalDate())
                    .isEqualTo(LocalDate.now(ZoneId.of("UTC")).minusDays(1));
        });
    }
}