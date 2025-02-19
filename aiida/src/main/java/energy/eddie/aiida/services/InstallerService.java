package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.dtos.installer.LoginDto;
import energy.eddie.aiida.dtos.installer.InstallerResponseDto;
import energy.eddie.aiida.dtos.installer.VersionInfoDto;
import energy.eddie.aiida.errors.InstallerException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;

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
    /**
     * TODO: define custom values for services (values.yaml)
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallerService.class);
    private static final String INSTALLER_HOST = "http://localhost:8010"; // TODO: Replace with actual host

    private static final String HEALTH_PATH = "/health";
    private static final String LOGIN_PATH = "/login";
    private static final String AIIDA_PATH = "/auth/aiida";
    private static final String SERVICE_PATH = "/auth/services/user";

    private static final String AUTH_HEADER_NAME = "Authorization";
    private static final String AUTH_HEADER_VALUE_PREFIX = "Bearer ";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private @Nullable String authToken;

    public InstallerService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    public void getHealth() throws InstallerException {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(INSTALLER_HOST + HEALTH_PATH)).GET().build();

            var httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            processInstallerResponse(httpResponse, Void.class);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to get health status", e);
            throw new InstallerException(HttpStatus.GATEWAY_TIMEOUT, "Failed to get health status");
        }
    }

    @Nullable
    public VersionInfoDto getVersionInfoAiida() throws InstallerException {
        return sendAuthenticatedRequest(AIIDA_PATH, "GET", VersionInfoDto.class);
    }

    @Nullable
    public VersionInfoDto installOrUpgradeAiida() throws InstallerException {
        return sendAuthenticatedRequest(AIIDA_PATH, "POST", VersionInfoDto.class);
    }

    @Nullable
    public List<VersionInfoDto> getVersionInfosServices() throws InstallerException {
        var response = sendAuthenticatedRequest(SERVICE_PATH, "GET", VersionInfoDto[].class);
        return response != null ? Arrays.asList(response) : Collections.emptyList();
    }
    
    @Nullable
    public VersionInfoDto getVersionInfoService(String chartName, String releaseName) throws InstallerException {
        return sendAuthenticatedRequest(getReleaseServicePath(chartName, releaseName), "GET", VersionInfoDto.class);
    }

    @Nullable
    public VersionInfoDto installNewService(String chartName) throws InstallerException {
        return sendAuthenticatedRequest(getChartServicePath(chartName), "POST", VersionInfoDto.class);
    }

    @Nullable
    public VersionInfoDto installOrUpgradeService(String chartName, String releaseName) throws InstallerException {
        return sendAuthenticatedRequest(getReleaseServicePath(chartName, releaseName), "POST", VersionInfoDto.class);
    }

    public void deleteService(String chartName, String releaseName) throws InstallerException {
        sendAuthenticatedRequest(getReleaseServicePath(chartName, releaseName), "DELETE", Void.class);
    }

    private String getChartServicePath(String chartName) {
        return SERVICE_PATH + "/" + chartName;
    }

    private String getReleaseServicePath(String chartName, String releaseName) {
        return getChartServicePath(chartName) + "/" + releaseName;
    }

    @Nullable
    private <T> T sendAuthenticatedRequest(
            String path,
            String method,
            Class<T> installerResponseDataType
    ) throws InstallerException {
        LOGGER.info("Sending {} request to {}", method, path);

        if (authToken == null) {
            authenticate();
        }

        try {
            var httpResponse = sendAuthenticatedHttpRequest(path, method);

            if (httpResponse.statusCode() == HttpStatus.UNAUTHORIZED.value()) {
                LOGGER.warn("Token possibly expired. Re-authenticating...");
                authenticate();
                httpResponse = sendAuthenticatedHttpRequest(path, method);
            }

            return processInstallerResponse(httpResponse, installerResponseDataType);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to process request: {}", path, e);
            throw new InstallerException(HttpStatus.GATEWAY_TIMEOUT, "Failed to process request: " + path);
        }
    }

    private HttpResponse<String> sendAuthenticatedHttpRequest(
            String path,
            String method
    ) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                                        .uri(URI.create(INSTALLER_HOST + path))
                                                        .method(method, HttpRequest.BodyPublishers.noBody())
                                                        .header(AUTH_HEADER_NAME, AUTH_HEADER_VALUE_PREFIX + authToken);

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void authenticate() throws InstallerException {
        LOGGER.info("Authenticating with installer service...");

        // TODO: Replace with actual credentials
        var loginDto = new LoginDto("test123", "Test");

        try {
            String requestBody = objectMapper.writeValueAsString(loginDto);
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(INSTALLER_HOST + LOGIN_PATH))
                                             .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                             .build();

            var httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            authToken = processInstallerResponse(httpResponse, String.class);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to authenticate with installer service", e);
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
            LOGGER.info("Installer response with status code {}: {}",
                        httpResponse.statusCode(),
                        installerResponse.data());

            if (installerResponseDataType == Void.class) {
                return null;
            }
            return Optional.ofNullable(installerResponse.data())
                           .orElseThrow(() -> new InstallerException(HttpStatus.NOT_FOUND, "No data in response"));
        } else {
            LOGGER.error("Installer response with status code {}: {}",
                         httpResponse.statusCode(),
                         installerResponse.error());

            var httpStatus = httpResponse.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR.value() ? HttpStatus.BAD_GATEWAY : HttpStatus.valueOf(
                    httpResponse.statusCode());
            throw new InstallerException(httpStatus, installerResponse.error());
        }
    }
}
