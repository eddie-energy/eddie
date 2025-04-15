package energy.eddie.aiida.services;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.errors.MqttUnauthorizedException;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAction;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttAuthServiceTest {
    private static final MqttDataSource DATA_SOURCE = new OesterreichsEnergieDataSource(
            new DataSourceDto(UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606"),
                              DataSourceType.SMART_METER_ADAPTER,
                              AiidaAsset.SUBMETER,
                              "sma",
                              true,
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
    private DataSourceService dataSourceService;

    @Mock DataSourceAdapter<MqttDataSource> dataSourceAdapter;

    private MqttAuthService mqttAuthService;

    @BeforeEach
    void setUp() {
        lenient().when(mqttConfiguration.adminUsername()).thenReturn("admin");
        lenient().when(mqttConfiguration.adminPassword()).thenReturn("admin");

        lenient().when(dataSourceService.findDataSourceAdapter(any(Predicate.class))).thenReturn(Optional.empty());

        lenient().when(dataSourceAdapter.dataSource()).thenReturn(DATA_SOURCE);

        mqttAuthService = new MqttAuthService(mqttConfiguration, dataSourceService);
    }

    @Test
    void authenticate_whenAdminUser_shouldNotThrow() {
        assertDoesNotThrow(() -> mqttAuthService.isAuthenticatedOrThrow("admin", "admin"));
    }

    @Test
    void authenticate_whenNormalUser_shouldNotThrow() {
        when(dataSourceService.findDataSourceAdapter(any(Predicate.class))).thenReturn(Optional.of(dataSourceAdapter));

        assertDoesNotThrow(() -> mqttAuthService.isAuthenticatedOrThrow("user", "password"));
    }

    @Test
    void authenticate_whenInvalidUser_shouldThrow() {
        assertThrows(MqttUnauthorizedException.class, () -> mqttAuthService.isAuthenticatedOrThrow("invalid", "invalid"));
    }

    @Test
    void isAdmin_whenAdminUser_shouldNotThrow() {
        assertDoesNotThrow(() -> mqttAuthService.isAdminOrThrow("admin", "admin"));
    }

    @Test
    void isAdmin_whenNormalUser_shouldThrow() {
        assertThrows(MqttUnauthorizedException.class, () -> mqttAuthService.isAdminOrThrow("user", "password"));
    }

    @Test
    void isAuthorized_whenAdminUser() {
        assertDoesNotThrow(() -> mqttAuthService.isAuthorizedOrThrow("admin", "admin", MqttAction.SUBSCRIBE, "$SYS/brokers/connected"));
        assertDoesNotThrow(() -> mqttAuthService.isAuthorizedOrThrow("admin", "admin", MqttAction.PUBLISH, "$SYS/brokers/connected"));
        assertDoesNotThrow(() -> mqttAuthService.isAuthorizedOrThrow("admin", "admin", MqttAction.SUBSCRIBE, "aiida/test"));
        assertDoesNotThrow(() -> mqttAuthService.isAuthorizedOrThrow("admin", "admin", MqttAction.PUBLISH, "aiida/test"));
        assertDoesNotThrow(() -> mqttAuthService.isAuthorizedOrThrow("admin", "admin", MqttAction.PUBLISH, "some/random/topic"));
    }

    @Test
    void isAuthorized_whenNormalUser() {
        when(dataSourceService.findDataSourceAdapter(any(Predicate.class))).thenReturn(Optional.of(dataSourceAdapter));

        assertDoesNotThrow(() -> mqttAuthService.isAuthorizedOrThrow("user", "password", MqttAction.SUBSCRIBE, "$SYS/brokers/connected"));
        assertDoesNotThrow(() -> mqttAuthService.isAuthorizedOrThrow("user", "password", MqttAction.PUBLISH, "$SYS/brokers/connected"));
        assertDoesNotThrow(() -> mqttAuthService.isAuthorizedOrThrow("user", "password", MqttAction.SUBSCRIBE, "aiida/test"));
        assertDoesNotThrow(() -> mqttAuthService.isAuthorizedOrThrow("user", "password", MqttAction.PUBLISH, "aiida/test"));
        assertThrows(MqttUnauthorizedException.class, () -> mqttAuthService.isAuthorizedOrThrow("user", "password", MqttAction.SUBSCRIBE, "some/random/topic"));
    }

    @Test
    void isAuthorized_whenInvalidUser() {
        assertThrows(MqttUnauthorizedException.class, () -> mqttAuthService.isAuthorizedOrThrow("invalid", "invalid", MqttAction.SUBSCRIBE, "$SYS/brokers/connected"));
        assertThrows(MqttUnauthorizedException.class, () -> mqttAuthService.isAuthorizedOrThrow("invalid", "invalid", MqttAction.PUBLISH, "$SYS/brokers/connected"));
        assertThrows(MqttUnauthorizedException.class, () -> mqttAuthService.isAuthorizedOrThrow("invalid", "invalid", MqttAction.SUBSCRIBE, "aiida/test"));
        assertThrows(MqttUnauthorizedException.class, () -> mqttAuthService.isAuthorizedOrThrow("invalid", "invalid", MqttAction.PUBLISH, "aiida/test"));
        assertThrows(MqttUnauthorizedException.class, () -> mqttAuthService.isAuthorizedOrThrow("invalid", "invalid", MqttAction.SUBSCRIBE, "some/random/topic"));
    }
}
