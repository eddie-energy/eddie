package energy.eddie.regionconnector.aiida.mqtt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MqttAclRepository extends JpaRepository<MqttAcl, String> {}
