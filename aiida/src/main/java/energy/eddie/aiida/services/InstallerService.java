package energy.eddie.aiida.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.InstallerConfiguration;
import energy.eddie.aiida.dtos.installer.InstallerResponseDto;
import energy.eddie.aiida.dtos.installer.InstallerSetupDto;
import energy.eddie.aiida.dtos.installer.LoginDto;
import energy.eddie.aiida.dtos.installer.VersionInfoDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.installer.InstallerException;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class InstallerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstallerService.class);

    private static final String HEALTH_PATH = "/health";
    private static final String LOGIN_PATH = "/login";
    private static final String AIIDA_PATH = "/auth/aiida";
    private static final String SERVICE_PATH = "/auth/services/user";

    private static final String AUTH_HEADER_NAME = "Authorization";
    private static final String AUTH_HEADER_VALUE_PREFIX = "Bearer ";

    private final InstallerConfiguration installerConfiguration;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private @Nullable String authToken;

    @Autowired
    public InstallerService(
            InstallerConfiguration installerConfiguration,
            AuthService authService,
            ObjectMapper objectMapper
    ) {
        this(installerConfiguration, authService, objectMapper, HttpClient.newHttpClient());
    }

    public InstallerService(
            InstallerConfiguration installerConfiguration,
            AuthService authService,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.installerConfiguration = installerConfiguration;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public void health() throws InstallerException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(installerConfiguration.host() + HEALTH_PATH))
                                             .GET()
                                             .build();

            var httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            processInstallerResponse(httpResponse, Void.class);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to get health status", e);
            Thread.currentThread().interrupt();
            throw new InstallerException(HttpStatus.GATEWAY_TIMEOUT, "Failed to get health status");
        }
    }

    @Nullable
    public VersionInfoDto aiidaVersion() throws InstallerException, InvalidUserException {
        return sendAuthenticatedRequest(AIIDA_PATH, HttpMethod.GET, VersionInfoDto.class);
    }

    @Nullable
    public VersionInfoDto installOrUpgradeAiida() throws InstallerException, InvalidUserException {
        return sendAuthenticatedRequest(AIIDA_PATH, HttpMethod.POST, VersionInfoDto.class);
    }

    @Nullable
    public List<VersionInfoDto> servicesVersions() throws InstallerException, InvalidUserException {
        var response = sendAuthenticatedRequest(SERVICE_PATH, HttpMethod.GET, VersionInfoDto[].class);
        return response != null ? Arrays.asList(response) : Collections.emptyList();
    }

    @Nullable
    public VersionInfoDto installNewService(
            String chartName,
            @Nullable InstallerSetupDto installerSetupDto
    ) throws InstallerException, InvalidUserException {
        try {
            var bodyPublisher = installerSetupDto != null
                    ? HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(installerSetupDto))
                    : HttpRequest.BodyPublishers.noBody();

            return sendAuthenticatedRequest(chartServicePath(chartName),
                                            HttpMethod.POST,
                                            VersionInfoDto.class,
                                            bodyPublisher);
        } catch (JsonProcessingException e) {
            throw new InstallerException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Nullable
    public VersionInfoDto serviceVersion(
            String chartName,
            String releaseName
    ) throws InstallerException, InvalidUserException {
        return sendAuthenticatedRequest(releaseServicePath(chartName, releaseName),
                                        HttpMethod.GET,
                                        VersionInfoDto.class);
    }

    @Nullable
    public VersionInfoDto installOrUpgradeService(
            String chartName,
            String releaseName
    ) throws InstallerException, InvalidUserException {
        return sendAuthenticatedRequest(releaseServicePath(chartName, releaseName),
                                        HttpMethod.POST,
                                        VersionInfoDto.class);
    }

    public void deleteService(String chartName, String releaseName) throws InstallerException, InvalidUserException {
        sendAuthenticatedRequest(releaseServicePath(chartName, releaseName), HttpMethod.DELETE, Void.class);
    }

    private String chartServicePath(String chartName) {
        return SERVICE_PATH + "/" + chartName;
    }

    private String releaseServicePath(String chartName, String releaseName) {
        return chartServicePath(chartName) + "/" + releaseName;
    }

    @Nullable
    private <T> T sendAuthenticatedRequest(
            String path,
            HttpMethod httpMethod,
            Class<T> installerResponseDataType
    ) throws InstallerException, InvalidUserException {
        return sendAuthenticatedRequest(path,
                                        httpMethod,
                                        installerResponseDataType,
                                        HttpRequest.BodyPublishers.noBody());
    }

    @Nullable
    private <T> T sendAuthenticatedRequest(
            String path,
            HttpMethod httpMethod,
            Class<T> installerResponseDataType,
            HttpRequest.BodyPublisher bodyPublisher
    ) throws InstallerException, InvalidUserException {
        LOGGER.info("Sending {} request to installer service", httpMethod);

        if (authToken == null) {
            authenticate();
        }

        try {
            var httpResponse = sendAuthenticatedHttpRequest(path, httpMethod, bodyPublisher);

            if (httpResponse.statusCode() == HttpStatus.UNAUTHORIZED.value()) {
                LOGGER.warn("Token possibly expired. Re-authenticating...");
                authenticate();
                httpResponse = sendAuthenticatedHttpRequest(path, httpMethod, bodyPublisher);
            }

            return processInstallerResponse(httpResponse, installerResponseDataType);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to process request: {}", path, e);
            Thread.currentThread().interrupt();
            throw new InstallerException(HttpStatus.GATEWAY_TIMEOUT, "Failed to process request: " + path);
        }
    }

    private HttpResponse<String> sendAuthenticatedHttpRequest(
            String path,
            HttpMethod httpMethod,
            HttpRequest.BodyPublisher bodyPublisher
    ) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                                        .uri(URI.create(installerConfiguration.host() + path))
                                                        .method(httpMethod.name(), bodyPublisher)
                                                        .header(AUTH_HEADER_NAME, AUTH_HEADER_VALUE_PREFIX + authToken);

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void authenticate() throws InstallerException, InvalidUserException {
        LOGGER.info("Authenticating with installer service...");

        var currentUserId = authService.getCurrentUserId();
        var loginDto = new LoginDto(currentUserId, installerConfiguration.token());

        try {
            String requestBody = objectMapper.writeValueAsString(loginDto);
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(installerConfiguration.host() + LOGIN_PATH))
                                             .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                             .build();

            var httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            authToken = processInstallerResponse(httpResponse, String.class);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to authenticate with installer service", e);
            Thread.currentThread().interrupt();
            throw new InstallerException(HttpStatus.GATEWAY_TIMEOUT, "Failed to authenticate with installer service");
        }
    }

    @Nullable
    private <T> T processInstallerResponse(
            HttpResponse<String> httpResponse,
            Class<T> installerResponseDataType
    ) throws IOException, InstallerException {
        var type = objectMapper.getTypeFactory()
                               .constructParametricType(InstallerResponseDto.class, installerResponseDataType);

        InstallerResponseDto<T> installerResponse = objectMapper.readValue(httpResponse.body(), type);

        if (installerResponse.success()) {
            LOGGER.info("Installer success with status code {}", httpResponse.statusCode());

            if (installerResponseDataType == Void.class) {
                return null;
            }
            return Optional.ofNullable(installerResponse.data())
                           .orElseThrow(() -> new InstallerException(HttpStatus.NOT_FOUND, "No data in response"));
        } else {
            LOGGER.error("Installer error with status code {}: {}",
                         httpResponse.statusCode(),
                         installerResponse.error());

            var httpStatus = httpResponse.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR.value() ? HttpStatus.BAD_GATEWAY : HttpStatus.valueOf(
                    httpResponse.statusCode());
            throw new InstallerException(httpStatus, installerResponse.error());
        }
    }
}
