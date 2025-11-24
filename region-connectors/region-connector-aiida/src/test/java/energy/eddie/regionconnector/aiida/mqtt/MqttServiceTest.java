package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.api.agnostic.aiida.mqtt.MqttAclType;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAction;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.shared.utils.PasswordGenerator;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttServiceTest {
    @Mock
    private MqttUserRepository mockUserRepository;
    @Mock
    private MqttAclRepository mockAclRepository;
    @Mock
    private PasswordGenerator mockPasswordGenerator;
    @Mock
    private BCryptPasswordEncoder mockEncoder;
    @Mock
    private MqttAsyncClient mockAsyncClient;
    @Mock
    private AiidaPermissionRequest mockRequest;
    @Mock
    private AiidaConfiguration mockConfiguration;
    @Mock
    private AiidaDataNeed mockDataNeed;
    @Captor
    private ArgumentCaptor<MqttUser> mqttUserCaptor;
    @Captor
    private ArgumentCaptor<Iterable<MqttAcl>> mqttAclCaptor;
    private MqttService mqttService;

    @Mock
    private MqttMessageCallback mqttMessageCallback;

    @BeforeEach
    void setUp() {
        mqttService = new MqttService(mockUserRepository,
                                      mockAclRepository,
                                      mockPasswordGenerator,
                                      mockEncoder,
                                      mockAsyncClient,
                                      mockConfiguration,
                                      mqttMessageCallback
        );
    }

    @Test
    void givenAlreadyExistingCredentials_throwsException() {
        // Given
        when(mockUserRepository.existsByPermissionId(anyString())).thenReturn(true);

        // When, Then
        assertThrows(CredentialsAlreadyExistException.class,
                     () -> mqttService.createCredentialsAndAclForPermission("foo", false));
    }

    @Test
    void givenPermissionId_createsAndSavesUserAndAcls() throws CredentialsAlreadyExistException {
        // Given
        String permissionId = "testId";
        String password = "MySuperSafePassword";
        String hash = "myHash";
        String serverUri = "tcp://localhost:1883";
        when(mockUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mockPasswordGenerator.generatePassword(anyInt())).thenReturn(password);
        when(mockEncoder.encode(anyString())).thenReturn(hash);
        when(mockConfiguration.mqttServerUri()).thenReturn(serverUri);

        // When
        MqttDto dto = mqttService.createCredentialsAndAclForPermission(permissionId, false);

        // Then
        verify(mockUserRepository).save(mqttUserCaptor.capture());
        assertEquals(permissionId, mqttUserCaptor.getValue().permissionId());
        // verify permissionId is used as username
        assertEquals(permissionId, mqttUserCaptor.getValue().username());
        assertFalse(mqttUserCaptor.getValue().isSuperuser());
        // ensure password hash is stored and not plaintext password
        assertEquals(hash, mqttUserCaptor.getValue().passwordHash());

        // verify ACLs
        verify(mockAclRepository).saveAll(mqttAclCaptor.capture());
        List<MqttAcl> acls = StreamSupport.stream(mqttAclCaptor.getValue().spliterator(), false).toList();

        assertEquals("aiida/v1/testId/data/outbound/+", acls.getFirst().topic());
        assertEquals(MqttAction.PUBLISH, acls.getFirst().action());
        assertEquals(MqttAclType.ALLOW, acls.getFirst().aclType());
        assertEquals(permissionId, acls.getFirst().username());

        assertEquals("aiida/v1/testId/status", acls.get(1).topic());
        assertEquals(MqttAction.PUBLISH, acls.get(1).action());
        assertEquals(MqttAclType.ALLOW, acls.get(1).aclType());
        assertEquals(permissionId, acls.get(1).username());

        assertEquals("aiida/v1/testId/termination", acls.get(2).topic());
        assertEquals(MqttAction.SUBSCRIBE, acls.get(2).action());
        assertEquals(MqttAclType.ALLOW, acls.get(2).aclType());
        assertEquals(permissionId, acls.get(2).username());


        assertEquals(serverUri, dto.serverUri());
        assertEquals(permissionId, dto.username());
        assertEquals(password, dto.password());
        assertEquals("aiida/v1/testId/data/outbound", dto.dataTopic());
        assertEquals("aiida/v1/testId/status", dto.statusTopic());
        assertEquals("aiida/v1/testId/termination", dto.terminationTopic());
    }

    @Test
    void givenPermissionIdAndInbound_createsAndSavesUserAndAcls() throws CredentialsAlreadyExistException {
        // Given
        String permissionId = "testId";
        String password = "MySuperSafePassword";
        String hash = "myHash";
        String serverUri = "tcp://localhost:1883";
        when(mockUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mockPasswordGenerator.generatePassword(anyInt())).thenReturn(password);
        when(mockEncoder.encode(anyString())).thenReturn(hash);
        when(mockConfiguration.mqttServerUri()).thenReturn(serverUri);

        // When
        MqttDto dto = mqttService.createCredentialsAndAclForPermission(permissionId, true);

        verify(mockAclRepository).saveAll(mqttAclCaptor.capture());
        List<MqttAcl> acls = StreamSupport.stream(mqttAclCaptor.getValue().spliterator(), false).toList();

        assertEquals("aiida/v1/testId/data/inbound/+", acls.getFirst().topic());
        assertEquals(MqttAction.SUBSCRIBE, acls.getFirst().action());
        assertEquals(MqttAclType.ALLOW, acls.getFirst().aclType());
        assertEquals(permissionId, acls.getFirst().username());

        assertEquals("aiida/v1/testId/data/inbound/+", dto.dataTopic());
    }

    @Test
    void deleteAclsForPermission_deletesAclsFromRepository() {
        // When
        mqttService.deleteAclsForPermission("myPermissionId");

        // Then
        verify(mockAclRepository).deleteByUsername("myPermissionId");
    }

    @Test
    void subscribeToOutboundDataTopic_subscribesViaMqttClient() throws MqttException {
        // Given
        var permissionId = "test";
        var expected = "aiida/v1/test/data/outbound/+";

        // When
        mqttService.subscribeToOutboundDataTopic(permissionId);

        // Then
        verify(mockAsyncClient).subscribe(expected, 1);
    }

    @Test
    void subscribeToStatusTopic_subscribesViaMqttClient() throws MqttException {
        // Given
        var permissionId = "test";
        var expected = "aiida/v1/test/status";

        // When
        mqttService.subscribeToStatusTopic(permissionId);

        // Then
        verify(mockAsyncClient).subscribe(expected, 1);
    }

    @Test
    void sendTerminationRequest_sendsViaMqttClient() throws MqttException {
        // Given
        var permissionId = "testMyId";
        when(mockRequest.permissionId()).thenReturn(permissionId);
        when(mockRequest.terminationTopic()).thenReturn("MyTopic");

        // When
        mqttService.sendTerminationRequest(mockRequest);

        // Then
        verify(mockAsyncClient).publish("MyTopic", permissionId.getBytes(StandardCharsets.UTF_8), 1, true);
    }

    @Test
    void close_disconnectsAndCloses() throws MqttException {
        // When
        mqttService.close();

        // Then
        verify(mockAsyncClient).disconnect(3000);
        verify(mockAsyncClient).close(true);
    }

    @Test
    void subscribeToStatusTopic() throws MqttException {
        // Given
        var permissionId = "test";
        var expected = "aiida/v1/" + permissionId + "/status";

        // When
        mqttService.subscribeToStatusTopic(permissionId);

        // Then
        verify(mockAsyncClient).subscribe(expected, 1);
    }
}
