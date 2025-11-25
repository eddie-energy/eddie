package energy.eddie.regionconnector.aiida.mqtt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MqttUserRepository extends JpaRepository<MqttUser, String> {
    boolean existsByUsername(String permissionId);
}
