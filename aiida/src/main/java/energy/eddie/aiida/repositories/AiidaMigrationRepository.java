package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.migration.Migration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiidaMigrationRepository extends JpaRepository<Migration, Integer> {
    Optional<Migration> findMigrationByMigrationKey(String migrationKey);
}
