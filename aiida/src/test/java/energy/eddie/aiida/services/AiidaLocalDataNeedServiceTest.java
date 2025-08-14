package energy.eddie.aiida.services;

import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.repositories.AiidaLocalDataNeedRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiidaLocalDataNeedServiceTest {
    private final UUID dataNeedId = UUID.fromString("72831e2c-a01c-41b8-9db6-3f51670df7a5");

    @Mock
    AiidaLocalDataNeedRepository repository;

    @Test
    void testOptionalAiidaLocalDataNeedById() {
        // Given
        var service = new AiidaLocalDataNeedService(repository);
        when(repository.findById(dataNeedId)).thenReturn(Optional.ofNullable(mock(AiidaLocalDataNeed.class)));

        // When
        var optional = service.optionalAiidaLocalDataNeedById(dataNeedId);

        // Then
        assertTrue(optional.isPresent());
    }
}
