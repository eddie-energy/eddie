package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.AiidaAssetDto;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceTypeDto;
import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.services.DataSourceService;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/datasources")
@OpenAPIDefinition(info = @Info(title = "Datasources API", version = "1.0", description = "Manage datasources"))
public class DataSourceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceController.class);
    private final DataSourceService service;

    @Autowired
    public DataSourceController(DataSourceService service) {
        this.service = service;
    }

    @Operation(summary = "Get all data source types", description = "Retrieve all available data source types.",
            operationId = "getDataSourceTypes", tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSourceType.class))))
    })
    @GetMapping("/types")
    public List<DataSourceTypeDto> getDataSourceTypes() {
        return Arrays.stream(DataSourceType.values())
                     .map(dataSourceType -> new DataSourceTypeDto(dataSourceType.identifier(),
                                                                  dataSourceType.dataSourceName()))
                     .toList();
    }

    @Operation(summary = "Get all assets", description = "Retrieve all assets.",
            operationId = "getAssets", tags = {"asset"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AiidaAsset.class))))
    })
    @GetMapping("/assets")
    public List<AiidaAssetDto> getAssets() {
        return Arrays.stream(AiidaAsset.values())
                     .map(aiidaAsset -> new AiidaAssetDto(aiidaAsset.asset()))
                     .toList();
    }

    @Operation(summary = "Get all datasources", description = "Retrieve all datasources.",
            operationId = "getAllDatasources", tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSource.class))))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DataSourceDto>> getAllDataSources() throws InvalidUserException {
        var dataSources = service.getDataSources();

        return ResponseEntity.ok(
                dataSources.stream()
                           .map(DataSource::toDto)
                           .toList()
        );
    }

    @Operation(summary = "Add a new datasource", description = "Add a new datasource.",
            operationId = "addDatasource", tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Datasource created successfully",
                    content = @Content(schema = @Schema(implementation = DataSource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addDataSource(@RequestBody DataSourceDto dataSource) throws InvalidUserException {
        LOGGER.info("Adding new datasource");

        if (dataSource.name() == null || dataSource.name().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        service.addDataSource(dataSource);

        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Delete a datasource", description = "Delete a datasource by ID.",
            operationId = "deleteDatasource", tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datasource deleted successfully",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataSource(@PathVariable("id") UUID dataSourceId) {
        LOGGER.info("Deleting datasource with ID: {}", dataSourceId);

        service.deleteDataSource(dataSourceId);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update a datasource", description = "Update the details of a datasource by ID.",
            operationId = "updateDatasource", tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datasource updated successfully",
                    content = @Content(schema = @Schema(implementation = DataSource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PatchMapping(value = "{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateDataSource(
            @PathVariable("id") UUID dataSourceId,
            @RequestBody DataSourceDto dataSource
    ) throws InvalidUserException {
        LOGGER.info("Updating datasource with ID: {} - {}", dataSourceId, dataSource);

        service.updateDataSource(dataSource);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get datasource by ID", description = "Retrieve a datasource by its ID.",
            operationId = "getDatasourceById", tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(schema = @Schema(implementation = DataSource.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DataSourceDto> getDataSourceById(@PathVariable("id") UUID dataSourceId) {
        LOGGER.info("Fetching datasource with ID: {}", dataSourceId);

        return service.getDataSourceById(dataSourceId)
                      .map(dataSource -> ResponseEntity.ok(dataSource.toDto()))
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update enabled state of a datasource",
            description = "Toggle the enabled state of a datasource by ID.",
            operationId = "updateDatasourceEnabledState",
            tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datasource enabled state updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PatchMapping(value = "{id}/enabled", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateEnabledState(
            @PathVariable("id") UUID dataSourceId,
            @RequestBody Boolean enabled
    ) {
        LOGGER.info("Updating enabled state of datasource with ID: {} - {}", dataSourceId, enabled);

        service.updateEnabledState(dataSourceId, enabled);

        return ResponseEntity.ok().build();
    }
}
