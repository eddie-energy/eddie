package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.datasource.MqttDataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MqttDataSourceRepository extends JpaRepository<MqttDataSource, UUID> {
    MqttDataSource findByMqttUsernameAndMqttPassword(String mqttUsername, String mqttPassword);
}