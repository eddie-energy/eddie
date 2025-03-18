package energy.eddie.aiida.services;

import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.MqttAction;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import energy.eddie.aiida.models.datasource.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.repositories.MqttDataSourceRepository;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttAuthServiceTest {
    private static final MqttDataSource DATA_SOURCE = new OesterreichsEnergieDataSource(
            new DataSourceDto(UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606"),
                              DataSourceType.Identifiers.SMART_METER_ADAPTER,
                              AiidaAsset.SUBMETER.asset(),
                              "sma",
                              true,
                              null,
                              null,
                              null),
            UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606"),
            new DataSourceMqttDto("tcp://localhost:1883",
                                  "tcp://localhost:1883",
                                  "aiida/test",
                                  "user",
                                  "password")
    );

    @Mock
    private MqttConfiguration mqttConfiguration;

    @Mock
    private MqttDataSourceRepository mqttDataSourceRepository;

    private MqttAuthService mqttAuthService;

    @BeforeEach
    void setUp() {
        lenient().when(mqttConfiguration.adminUsername()).thenReturn("admin");
        lenient().when(mqttConfiguration.adminPassword()).thenReturn("admin");

        lenient().when(mqttDataSourceRepository.findByMqttUsernameAndMqttPassword(any(), any())).thenReturn(null);

        mqttAuthService = new MqttAuthService(mqttConfiguration, mqttDataSourceRepository);
    }

    @Test
    void authenticate_whenAdminUser_shouldReturnTrue() {
        assertTrue(mqttAuthService.authenticate("admin", "admin"));
    }

    @Test
    void authenticate_whenNormalUser_shouldReturnTrue() {
        when(mqttDataSourceRepository.findByMqttUsernameAndMqttPassword("user", "password")).thenReturn(DATA_SOURCE);

        assertTrue(mqttAuthService.authenticate("user", "password"));
    }

    @Test
    void authenticate_whenInvalidUser_shouldReturnFalse() {
        assertFalse(mqttAuthService.authenticate("invalid", "invalid"));
    }

    @Test
    void isAdmin_whenAdminUser_shouldReturnTrue() {
        assertTrue(mqttAuthService.isAdmin("admin", "admin"));
    }

    @Test
    void isAdmin_whenNormalUser_shouldReturnFalse() {
        assertFalse(mqttAuthService.isAdmin("user", "password"));
    }

    @Test
    void isAuthorized_whenAdminUser() {
        assertTrue(mqttAuthService.isAuthorized("admin", "admin", MqttAction.SUBSCRIBE, "$SYS/brokers/connected"));
        assertTrue(mqttAuthService.isAuthorized("admin", "admin", MqttAction.PUBLISH, "$SYS/brokers/connected"));
        assertTrue(mqttAuthService.isAuthorized("admin", "admin", MqttAction.SUBSCRIBE, "aiida/test"));
        assertTrue(mqttAuthService.isAuthorized("admin", "admin", MqttAction.PUBLISH, "aiida/test"));
        assertTrue(mqttAuthService.isAuthorized("admin", "admin", MqttAction.PUBLISH, "some/random/topic"));
    }

    @Test
    void isAuthorized_whenNormalUser() {
        when(mqttDataSourceRepository.findByMqttUsernameAndMqttPassword("user", "password")).thenReturn(DATA_SOURCE);

        assertTrue(mqttAuthService.isAuthorized("user", "password", MqttAction.SUBSCRIBE, "$SYS/brokers/connected"));
        assertTrue(mqttAuthService.isAuthorized("user", "password", MqttAction.PUBLISH, "$SYS/brokers/connected"));
        assertTrue(mqttAuthService.isAuthorized("user", "password", MqttAction.SUBSCRIBE, "aiida/test"));
        assertTrue(mqttAuthService.isAuthorized("user", "password", MqttAction.PUBLISH, "aiida/test"));
        assertFalse(mqttAuthService.isAuthorized("user", "password", MqttAction.SUBSCRIBE, "some/random/topic"));
    }

    @Test
    void isAuthorized_whenInvalidUser() {
        assertFalse(mqttAuthService.isAuthorized("invalid", "invalid", MqttAction.SUBSCRIBE, "$SYS/brokers/connected"));
        assertFalse(mqttAuthService.isAuthorized("invalid", "invalid", MqttAction.PUBLISH, "$SYS/brokers/connected"));
        assertFalse(mqttAuthService.isAuthorized("invalid", "invalid", MqttAction.SUBSCRIBE, "aiida/test"));
        assertFalse(mqttAuthService.isAuthorized("invalid", "invalid", MqttAction.PUBLISH, "aiida/test"));
        assertFalse(mqttAuthService.isAuthorized("invalid", "invalid", MqttAction.SUBSCRIBE, "some/random/topic"));
    }
}
