package energy.eddie.core.services;

import energy.eddie.core.application.information.ApplicationInformation;
import energy.eddie.core.application.information.persistence.ApplicationInformationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationInformationServiceTest {
    private final UUID eddieId = UUID.fromString("a69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private final Instant createdAt = Instant.MIN;

    @Mock
    private ApplicationInformationRepository repository;

    @Test
    void testApplicationInformationService() {
        // Given
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(new ApplicationInformation(eddieId,
                                                                                                             createdAt)));
        ApplicationInformationService service = new ApplicationInformationService(repository);

        // When
        var applicationInformation = service.applicationInformation();

        // Then
        assertEquals(eddieId, applicationInformation.eddieId());
    }
}
