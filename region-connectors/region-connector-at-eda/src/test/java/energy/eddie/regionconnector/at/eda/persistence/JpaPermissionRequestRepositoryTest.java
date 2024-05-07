package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaPermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private EdaPermissionEventRepository permissionEventRepository;
    @Autowired
    private JpaPermissionRequestRepository permissionRequestRepository;

    @Test
    void findByPermissionId_returnsEmptyOptional_forNonExistentId() {
        // Given
        PermissionEvent event = new SimpleEvent("pid", PermissionProcessStatus.CREATED);
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByPermissionId("otherId");

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void findByPermissionId_returnsPresentOptional_forExistingId() {
        // Given
        PermissionEvent event = new SimpleEvent("pid", PermissionProcessStatus.CREATED);
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByPermissionId("pid");

        // Then
        assertThat(res).isPresent();
    }

    @Test
    void findByConversationIdOrCMRequestId_returnsPresentOptional_forConversationIdAndNullRequestId() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String conversationId = "convId";
        PermissionEvent event = new CreatedEvent("pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"), start,
                                                 end, "mid", AllowedGranularity.PT15M, "cmRequestId",
                                                 conversationId);
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByConversationIdOrCMRequestId(conversationId, null);

        // Then
        assertThat(res).isPresent();
    }

    @Test
    void findByConversationIdOrCMRequestId_returnsPresentOptional_forConversationIdAndNonExistingRequestId() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String conversationId = "convId";
        PermissionEvent event = new CreatedEvent("pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"), start,
                                                 end, "mid", AllowedGranularity.PT15M, "cmRequestId",
                                                 conversationId);
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByConversationIdOrCMRequestId(conversationId, "otherId");

        // Then
        assertThat(res).isPresent();
    }

    @Test
    void findByConversationIdOrCMRequestId_returnsEmptyOptional_forNonExistingConversationIdAndNullRequestId() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String conversationId = "convId";
        PermissionEvent event = new CreatedEvent("pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"), start,
                                                 end, "mid", AllowedGranularity.PT15M, "cmRequestId",
                                                 "otherId");
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByConversationIdOrCMRequestId(conversationId, null);

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void findByConversationIdOrCMRequestId_returnsEmptyOptional_forNonExistingConversationIdAndNonExistingRequestId() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String conversationId = "convId";
        PermissionEvent event = new CreatedEvent("pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"), start,
                                                 end, "mid", AllowedGranularity.PT15M, "cmRequestId",
                                                 conversationId);
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByConversationIdOrCMRequestId("otherId", "otherId");

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void findByConversationIdOrCMRequestId_returnsEmptyOptional_forNonExistingConversationIdAndExistingRequestId() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String cmRequestId = "cmRequestId";
        PermissionEvent event = new CreatedEvent("pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"), start,
                                                 end, "mid", AllowedGranularity.PT15M, cmRequestId,
                                                 "convId");
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByConversationIdOrCMRequestId("otherId", cmRequestId);

        // Then
        assertThat(res).isPresent();
    }

    @Test
    void findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate_returnsEmptyList_forNonExistingPermissionRequest() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String cmRequestId = "cmRequestId";
        PermissionEvent event = new CreatedEvent("pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"), start,
                                                 end, "mid", AllowedGranularity.PT15M, cmRequestId,
                                                 "convId");
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate("mid",
                                                                                                          start.plusDays(
                                                                                                                  1));

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate_returnsPermissionRequest_forNonExistingPermissionRequest() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String cmRequestId = "cmRequestId";
        PermissionEvent event1 = new CreatedEvent("pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"), start,
                                                  end, "mid", AllowedGranularity.PT15M, cmRequestId,
                                                  "convId");
        permissionEventRepository.saveAndFlush(event1);
        PermissionEvent event2 = new SimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        permissionEventRepository.saveAndFlush(event2);
        // When
        var res = permissionRequestRepository.findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate("mid",
                                                                                                          start.plusDays(
                                                                                                                  1));

        // Then
        assertThat(res).hasSize(1);
    }
}
