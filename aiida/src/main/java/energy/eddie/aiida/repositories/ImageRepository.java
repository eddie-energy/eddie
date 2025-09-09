package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.image.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {
}