// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.application.information.ApplicationInformation;
import energy.eddie.aiida.application.information.persistence.ApplicationInformationRepository;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationInformationServiceTest {
    private final UUID aiidaId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private final LogCaptor logCaptor = LogCaptor.forClass(ApplicationInformationService.class);
    @Mock
    private ApplicationInformationRepository applicationInformationRepository;

    @Test
    void testApplicationInformationService_applicationInformationDoesExist() {
        // Given
        when(applicationInformationRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(new ApplicationInformation(
                aiidaId,
                Instant.now())));
        var applicationInformationService = new ApplicationInformationService(applicationInformationRepository);

        // When
        var applicationInformation = applicationInformationService.applicationInformation();

        // Then
        assertEquals(aiidaId, applicationInformation.aiidaId());
    }

    @Test
    void testApplicationInformationService_applicationInformationDoesNotExist() {
        // Given
        var message = "Creating new ApplicationInformation";
        var applicationInformationService = new ApplicationInformationService(applicationInformationRepository);

        // When
        applicationInformationService.applicationInformation();

        // Then
        assertTrue(logCaptor.getInfoLogs().contains(message));
    }
}
