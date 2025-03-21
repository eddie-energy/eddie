package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.config.InstallerConfiguration;
import energy.eddie.aiida.dtos.installer.InstallerSetupDto;
import energy.eddie.aiida.dtos.installer.VersionInfoDto;
import energy.eddie.aiida.errors.InstallerException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class InstallerServiceTest {
    private static final String INSTALLER_HOST = "http://aiida-installer:8010";
    private static final String SAMPLE_CHART = "my-chart";
    private static final String SAMPLE_RELEASE = "my-release";
    private static final String VERSION_INFO_JSON = """
            {
                "releaseName": "aiida",
                "releaseInfo": {
                    "firstDeployed": "2025-02-24T11:36:08.086379418+01:00",
                    "lastDeployed": "2025-02-24T13:28:03.555518598+01:00",
                    "deleted": "2025-02-24T13:28:03.555518598+01:00",
                    "description": "Upgrade complete",
                    "status": "deployed"
                },
                "installedChart": {
                    "name": "aiida",
                    "version": "0.1.0",
                    "description": "AIIDA Core (Administrative Interface for In-House Data Access)",
                    "appVersion": "0.0.1"
                },
                "latestChart": {
                    "name": "aiida",
                    "version": "0.1.0",
                    "description": "AIIDA Core (Administrative Interface for In-House Data Access)",
                    "appVersion": "0.0.1"
                }
            }
            """;
    private static final String VERSION_INFO_RESPONSE_JSON = "{\"success\": true, \"data\":" + VERSION_INFO_JSON + "}";
    private static final String LOGIN_SUCCESS_RESPONSE_JSON = """
            {
                "success": true,
                "data": "jwt-token"
            }
            """;

    private final ObjectMapper objectMapper = new AiidaConfiguration().customObjectMapper().build();

    @Mock
    private InstallerConfiguration installerConfiguration;

    @Mock
    private AuthService authService;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponseLogin;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private InstallerService installerService;

    @BeforeEach
    void setUp() throws Exception {
        when(installerConfiguration.host()).thenReturn(INSTALLER_HOST);
        installerService = new InstallerService(installerConfiguration, authService, objectMapper, httpClient);

        lenient().when(installerConfiguration.token()).thenReturn("Test123!");
        lenient().when(authService.getCurrentUserId()).thenReturn(UUID.randomUUID());


        lenient().when(httpResponseLogin.statusCode()).thenReturn(HttpStatus.OK.value());
        lenient().when(httpResponseLogin.body()).thenReturn(LOGIN_SUCCESS_RESPONSE_JSON);

        lenient().when(httpResponse.statusCode()).thenReturn(HttpStatus.OK.value());
    }

    @Test
    void health_success() throws Exception {
        // Given
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.body()).thenReturn("{\"success\": true}");

        // When, Then
        assertDoesNotThrow(() -> installerService.health());
    }

    @Test
    void health_requestFailure() throws Exception {
        // Given
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(IOException.class);

        // When, Then
        var exception = assertThrows(InstallerException.class, () -> installerService.health());
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.httpStatus());
    }

    @Test
    void authenticate_requestFailure() throws Exception {
        // Given
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            if (request.uri().getPath().equals("/login")) {
                throw new IOException();
            }
            return httpResponse;
        });

        // When, Then
        var exception = assertThrows(InstallerException.class, () -> installerService.aiidaVersion());
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.httpStatus());
    }

    @Test
    void aiidaVersion_success() throws Exception {
        // Given
        when(httpResponse.body()).thenReturn(VERSION_INFO_RESPONSE_JSON);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When
        VersionInfoDto result = installerService.aiidaVersion();

        // Then
        assertNotNull(result);
        assertEquals("0.1.0", result.installedChart().version());
    }

    @Test
    void aiidaVersion_loginFailure() throws Exception {
        // Given
        when(httpResponseLogin.statusCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(httpResponseLogin.body()).thenReturn("{\"success\": false, \"error\": \"Unauthorized\"}");
        when(httpClient.send(any(HttpRequest.class),
                             any(HttpResponse.BodyHandler.class))).thenReturn(httpResponseLogin);

        // When, Then
        var exception = assertThrows(InstallerException.class, () -> installerService.aiidaVersion());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.httpStatus());
        assertEquals("Unauthorized", exception.getMessage());
    }

    @Test
    void aiidaVersion_tokenExpired() throws Exception {
        // Given
        when(httpResponse.statusCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(httpResponse.body()).thenReturn("{\"success\": false, \"error\": \"Unauthorized\"}");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When, Then
        var exception = assertThrows(InstallerException.class, () -> installerService.aiidaVersion());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.httpStatus());
        assertEquals("Unauthorized", exception.getMessage());
        verify(httpClient, times(4)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void aiidaVersion_installerFailure() throws Exception {
        // Given
        when(httpResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(httpResponse.body()).thenReturn("{\"success\": false, \"error\": \"Server Error\"}");


        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When, Then
        var exception = assertThrows(InstallerException.class, () -> installerService.aiidaVersion());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.httpStatus());
        assertEquals("Server Error", exception.getMessage());
    }

    @Test
    void aiidaVersion_requestFailure() throws Exception {
        // Given
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            if (request.uri().getPath().equals("/login")) {
                return httpResponseLogin;
            }
            throw new IOException();
        });

        // When, Then
        var exception = assertThrows(InstallerException.class, () -> installerService.aiidaVersion());
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.httpStatus());
    }

    @Test
    void installOrUpgradeAiida_success() throws Exception {
        // Given
        when(httpResponse.body()).thenReturn(VERSION_INFO_RESPONSE_JSON);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When
        VersionInfoDto result = installerService.installOrUpgradeAiida();

        // Then
        assertNotNull(result);
        assertEquals("0.1.0", result.installedChart().version());
    }

    @Test
    void servicesVersions_success() throws Exception {
        // Given
        when(httpResponse.body()).thenReturn("{\"success\": true, \"data\": [" + VERSION_INFO_JSON + "]}");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When
        List<VersionInfoDto> result = installerService.servicesVersions();

        // Then
        assertNotNull(result);
        assertEquals("0.1.0", result.getFirst().installedChart().version());
    }

    @Test
    void installNewService_withoutConfig_success() throws Exception {
        // Given
        when(httpResponse.body()).thenReturn(VERSION_INFO_RESPONSE_JSON);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When
        VersionInfoDto result = installerService.installNewService(SAMPLE_CHART, null);

        // Then
        assertNotNull(result);
        assertEquals("0.1.0", result.installedChart().version());
        verify(httpClient).send(
                argThat(request -> request.bodyPublisher().isPresent()
                                   && request.bodyPublisher().get().contentLength() == 0),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void installNewService_withConfig_success() throws Exception {
        // Given
        when(httpResponse.body()).thenReturn(VERSION_INFO_RESPONSE_JSON);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When
        var installerSetupDto = new InstallerSetupDto(List.of("core.port=1234", "db.name=aiida"));
        VersionInfoDto result = installerService.installNewService(SAMPLE_CHART, installerSetupDto);

        // Then
        assertNotNull(result);
        assertEquals("0.1.0", result.installedChart().version());
        verify(httpClient, times(2)).send(
                argThat(request -> request.bodyPublisher().isPresent()
                                   && request.bodyPublisher().get().contentLength() > 0),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void serviceVersion_success() throws Exception {
        // Given
        when(httpResponse.body()).thenReturn(VERSION_INFO_RESPONSE_JSON);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When
        VersionInfoDto result = installerService.serviceVersion(SAMPLE_CHART, SAMPLE_RELEASE);

        // Then
        assertNotNull(result);
        assertEquals("0.1.0", result.installedChart().version());
    }

    @Test
    void installOrUpgradeService_success() throws Exception {
        // Given
        when(httpResponse.body()).thenReturn(VERSION_INFO_RESPONSE_JSON);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When
        VersionInfoDto result = installerService.installOrUpgradeService(SAMPLE_CHART, SAMPLE_RELEASE);

        // Then
        assertNotNull(result);
        assertEquals("0.1.0", result.installedChart().version());
    }

    @Test
    void deleteService_success() throws Exception {
        // Given
        when(httpResponse.body()).thenReturn(VERSION_INFO_RESPONSE_JSON);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(
                getHttpResponseAnswer());

        // When
        assertDoesNotThrow(() -> installerService.deleteService(SAMPLE_CHART, SAMPLE_RELEASE));
    }

    private @NotNull Answer<Object> getHttpResponseAnswer() {
        return invocation -> {
            HttpRequest request = invocation.getArgument(0);
            if (request.uri().getPath().equals("/login")) {
                return httpResponseLogin;
            }
            return httpResponse;
        };
    }
}
