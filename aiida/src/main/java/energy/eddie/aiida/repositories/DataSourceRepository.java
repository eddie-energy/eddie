package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DataSourceRepository extends JpaRepository<DataSource, UUID> {
    List<DataSource> findByUserId(UUID userId);

    List<DataSource> findByUserIdAndAsset(UUID userId, AiidaAsset asset);
}