package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.AiidaLocalDataNeed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiidaLocalDataNeedRepository extends JpaRepository<AiidaLocalDataNeed, UUID> {
}
