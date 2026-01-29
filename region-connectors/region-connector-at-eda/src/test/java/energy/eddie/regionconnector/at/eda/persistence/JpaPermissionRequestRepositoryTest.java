// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaPermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");

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
        PermissionEvent event = new ValidatedEvent("pid", start,
                                                   end, AllowedGranularity.PT15M, "cmRequestId",
                                                   conversationId, ValidatedEvent.NeedsToBeSent.YES);
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByConversationIdOrCMRequestId(conversationId, null);

        // Then
        assertThat(res).isNotEmpty();
    }

    @Test
    void findByConversationIdOrCMRequestId_returnsPresentOptional_forConversationIdAndNonExistingRequestId() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String conversationId = "convId";
        PermissionEvent event = new ValidatedEvent("pid", start,
                                                   end, AllowedGranularity.PT15M, "cmRequestId",
                                                   conversationId, ValidatedEvent.NeedsToBeSent.YES);
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByConversationIdOrCMRequestId(conversationId, "otherId");

        // Then
        assertThat(res).isNotEmpty();
    }

    @Test
    void findByConversationIdOrCMRequestId_returnsEmptyOptional_forNonExistingConversationIdAndNullRequestId() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String conversationId = "convId";
        PermissionEvent event = new ValidatedEvent("pid", start,
                                                   end, AllowedGranularity.PT15M, "cmRequestId",
                                                   "otherId", ValidatedEvent.NeedsToBeSent.YES);
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
        PermissionEvent event = new ValidatedEvent("pid", start,
                                                   end, AllowedGranularity.PT15M, "cmRequestId",
                                                   conversationId, ValidatedEvent.NeedsToBeSent.YES);
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
        PermissionEvent event = new ValidatedEvent("pid", start,
                                                   end, AllowedGranularity.PT15M, cmRequestId,
                                                   "convId", ValidatedEvent.NeedsToBeSent.YES);
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByConversationIdOrCMRequestId("otherId", cmRequestId);

        // Then
        assertThat(res).isNotEmpty();
    }

    @Test
    void findByMeteringPointIdAndDate_AndStateAfterAcceptedOrSentToPA_returnsEmptyList_forNonExistingPermissionRequest() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        var end = LocalDate.of(2024, 1, 31);
        String cmRequestId = "cmRequestId";
        PermissionEvent event = new ValidatedEvent("pid", start,
                                                   end, AllowedGranularity.PT15M, cmRequestId,
                                                   "convId", ValidatedEvent.NeedsToBeSent.YES);
        permissionEventRepository.saveAndFlush(event);

        // When
        var res = permissionRequestRepository.findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted("mid",
                                                    start.plusDays(
                                                                                                                  1));

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void findByMeteringPointIdAndDate_AndStateAfterAcceptedOrSentToPA_returnsPermissionRequest_forNonExistingPermissionRequest() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        PermissionEvent event1 = new CreatedEvent("pid", "cid", "did", new EdaDataSourceInformation("asd"), "mid");
        permissionEventRepository.saveAndFlush(event1);
        PermissionEvent event2 = new ValidatedEvent("pid", start,
                                                    null, AllowedGranularity.PT15M, "cmRequestId",
                                                    "convId", ValidatedEvent.NeedsToBeSent.YES);
        permissionEventRepository.saveAndFlush(event2);
        PermissionEvent event3 = new SimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        permissionEventRepository.saveAndFlush(event3);
        // When
        var res = permissionRequestRepository.findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted("mid",
                                                    start.plusDays(
                                                                                                                  1));

        // Then
        assertThat(res).hasSize(1);
    }

    @Test
    void findByMeteringPointIdAndDate_AndStateAfterAcceptedOrSentToPA__returnsPermissionRequest_forExistingPermissionRequest() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        PermissionEvent event1 = new CreatedEvent("pid", "cid", "did", new EdaDataSourceInformation("asd"), "mid");
        permissionEventRepository.saveAndFlush(event1);
        PermissionEvent event2 = new ValidatedEvent("pid", start,
                                                    null, AllowedGranularity.PT15M, "cmRequestId",
                                                    "convId", ValidatedEvent.NeedsToBeSent.YES);
        permissionEventRepository.saveAndFlush(event2);
        PermissionEvent event3 = new SimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        permissionEventRepository.saveAndFlush(event3);

        // When
        var res = permissionRequestRepository.findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted("mid",
                                                    start);

        // Then
        assertThat(res).hasSize(1);
    }

    @Test
    void findByMeteringPointIdAndDate_AndStateAfterAcceptedOrSendToPA__returnsNoPermission_forCreatedEvent() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        PermissionEvent event1 = new CreatedEvent("pid", "cid", "did", new EdaDataSourceInformation("asd"), "mid");
        permissionEventRepository.saveAndFlush(event1);

        // When
        var res = permissionRequestRepository.findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted("mid",
                                                    start);

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void findByMeteringPointIdAndDate_AndStateAfterAcceptedOrSendToPA__parseObjects() {
        // Given
        var start = LocalDate.of(2024, 1, 1);
        PermissionEvent event1 = new CreatedEvent("pid", "cid", "did", new EdaDataSourceInformation("asd"), "mid");
        permissionEventRepository.saveAndFlush(event1);
        PermissionEvent event2 = new ValidatedEvent("pid", start,
                                                    null, AllowedGranularity.PT15M, "cmRequestId",
                                                    "convId", ValidatedEvent.NeedsToBeSent.YES);
        permissionEventRepository.saveAndFlush(event2);
        PermissionEvent event3 = new SimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        permissionEventRepository.saveAndFlush(event3);

        // When
        var res = permissionRequestRepository.findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted("mid",
                                                    start);

        // Then
        var first = res.getFirst();
        assertThat(first.getPermissionId()).isEqualTo("pid");
        assertThat(first.getConnectionId()).isEqualTo("cid");
        assertThat(first.getDataNeedId()).isEqualTo("did");
        assertThat(first.getMeteringPointId()).isEqualTo("mid");
    }

    @Test
    void findByMeteringPointIdAndDate_AndStateAfterAcceptedOrSendToPA__multipleMeteringPoints() {
        // Given
        var start = LocalDate.of(2024, 1, 1);

        // Metering Point 1
        PermissionEvent event1 = new CreatedEvent("pid", "cid", "did", new EdaDataSourceInformation("asd"), "mid");
        permissionEventRepository.saveAndFlush(event1);
        PermissionEvent event2 = new ValidatedEvent("pid", start,
                                                    null, AllowedGranularity.PT15M, "cmRequestId",
                                                    "convId", ValidatedEvent.NeedsToBeSent.YES);
        permissionEventRepository.saveAndFlush(event2);
        PermissionEvent event3 = new SimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        permissionEventRepository.saveAndFlush(event3);

        // Metering Point 2
        PermissionEvent event21 = new CreatedEvent("pid2", "cid2", "did2", new EdaDataSourceInformation("asd2"), "mid2");
        permissionEventRepository.saveAndFlush(event21);
        PermissionEvent event22 = new ValidatedEvent("pid2", start,
                                                    null, AllowedGranularity.PT15M, "cmRequestId2",
                                                    "convId2", ValidatedEvent.NeedsToBeSent.YES);
        permissionEventRepository.saveAndFlush(event22);
        PermissionEvent event23 = new SimpleEvent("pid2", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        permissionEventRepository.saveAndFlush(event23);

        // When
        var res = permissionRequestRepository.findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted("mid",
                                                                                                          start);

        // Then
        assertThat(res).hasSize(1);
    }
}
