package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.installer.InstallerSetupDto;
import energy.eddie.aiida.dtos.installer.VersionInfoDto;
import energy.eddie.aiida.errors.InstallerException;
import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.services.InstallerService;
import energy.eddie.api.agnostic.EddieApiError;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/installer")
@OpenAPIDefinition(info = @Info(title = "Installer API", version = "1.0", description = "Manage versions of AIIDA and services"))
public class InstallerController {
    private static final String AIIDA_PATH = "/aiida";
    private static final String SERVICE_PATH = "/services/user";
    private static final String CHART_SERVICE_PATH = SERVICE_PATH + "/{chartName}";
    private static final String RELEASE_SERVICE_PATH = CHART_SERVICE_PATH + "/{releaseName}";
    private static final String VERSION_INFO_EXAMPLE_RETURN_JSON = """
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

    private final InstallerService installerService;

    @Autowired
    public InstallerController(InstallerService service) {
        this.installerService = service;
    }

    @Operation(summary = "Get health status", description = "Get the health status of the installer service.", operationId = "getHealth", tags = {"installer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Installer is running and reachable"),
            @ApiResponse(responseCode = "504", description = "Installer is unavailable or unreachable ", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
    })
    @GetMapping(path = "/health")
    public ResponseEntity<Void> health() throws InstallerException {
        installerService.health();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get version info of AIIDA", operationId = "getAiidaVersion", tags = {"installer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = VersionInfoDto.class), examples = @ExampleObject(value = VERSION_INFO_EXAMPLE_RETURN_JSON))),
            @ApiResponse(responseCode = "502", description = "Installer has an internal error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "504", description = "Installer is unavailable or unreachable ", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
    })
    @Nullable
    @GetMapping(path = AIIDA_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public VersionInfoDto aiidaVersion() throws InstallerException, InvalidUserException {
        return installerService.aiidaVersion();
    }

    @Operation(summary = "Install or upgrade AIIDA", operationId = "installOrUpgradeAiida", tags = {"installer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully installed or upgraded AIIDA", content = @Content(schema = @Schema(implementation = VersionInfoDto.class), examples = @ExampleObject(value = VERSION_INFO_EXAMPLE_RETURN_JSON))),
            @ApiResponse(responseCode = "502", description = "Installer has an internal error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "504", description = "Installer is unavailable or unreachable ", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
    })
    @Nullable
    @PostMapping(path = AIIDA_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public VersionInfoDto installOrUpgradeAiida() throws InstallerException, InvalidUserException {
        return installerService.installOrUpgradeAiida();
    }

    @Operation(summary = "Get version info of all services per user", operationId = "getServicesVersionsUser", tags = {"installer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(array = @ArraySchema(schema = @Schema(implementation = VersionInfoDto.class)), examples = @ExampleObject(value = "[" + VERSION_INFO_EXAMPLE_RETURN_JSON + "]"))),
            @ApiResponse(responseCode = "502", description = "Installer has an internal error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "504", description = "Installer is unavailable or unreachable ", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))})
    })
    @Nullable
    @GetMapping(path = SERVICE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<VersionInfoDto> servicesVersions() throws InstallerException, InvalidUserException {
        return installerService.servicesVersions();
    }

    @Operation(summary = "Install new service", operationId = "installNewService", tags = {"installer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully installed new service", content = @Content(schema = @Schema(implementation = VersionInfoDto.class), examples = @ExampleObject(value = VERSION_INFO_EXAMPLE_RETURN_JSON))),
            @ApiResponse(responseCode = "502", description = "Installer has an internal error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "504", description = "Installer is unavailable or unreachable ", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))})
    })
    @Nullable
    @PostMapping(path = CHART_SERVICE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public VersionInfoDto installNewService(
            @PathVariable String chartName,
            @RequestBody(required = false) InstallerSetupDto installerSetupDto
    ) throws InstallerException, InvalidUserException {
        return installerService.installNewService(chartName, installerSetupDto);
    }

    @Operation(summary = "Get version info a service", operationId = "getServiceVersion", tags = {"installer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = VersionInfoDto.class), examples = @ExampleObject(value = VERSION_INFO_EXAMPLE_RETURN_JSON))),
            @ApiResponse(responseCode = "502", description = "Installer has an internal error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "504", description = "Installer is unavailable or unreachable ", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))})
    })
    @Nullable
    @GetMapping(path = RELEASE_SERVICE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public VersionInfoDto serviceVersion(
            @PathVariable String chartName,
            @PathVariable String releaseName
    ) throws InstallerException, InvalidUserException {
        return installerService.serviceVersion(chartName, releaseName);
    }

    @Operation(summary = "Install or upgrade a service", operationId = "installOrUpgradeService", tags = {"installer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully installed or upgraded the service", content = @Content(schema = @Schema(implementation = VersionInfoDto.class), examples = @ExampleObject(value = VERSION_INFO_EXAMPLE_RETURN_JSON))),
            @ApiResponse(responseCode = "502", description = "Installer has an internal error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "504", description = "Installer is unavailable or unreachable ", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
    })
    @Nullable
    @PostMapping(path = RELEASE_SERVICE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public VersionInfoDto installOrUpgradeService(
            @PathVariable String chartName,
            @PathVariable String releaseName
    ) throws InstallerException, InvalidUserException {
        return installerService.installOrUpgradeService(chartName, releaseName);
    }

    @Operation(summary = "Delete a service", operationId = "deleteService", tags = {"installer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted the service"),
            @ApiResponse(responseCode = "502", description = "Installer has an internal error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "504", description = "Installer is unavailable or unreachable ", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
    })
    @DeleteMapping(path = RELEASE_SERVICE_PATH)
    public ResponseEntity<Void> deleteService(
            @PathVariable String chartName,
            @PathVariable String releaseName
    ) throws InstallerException, InvalidUserException {
        installerService.deleteService(chartName, releaseName);
        return ResponseEntity.ok().build();
    }
}
