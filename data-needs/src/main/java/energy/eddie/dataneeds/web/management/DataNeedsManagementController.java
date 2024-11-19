package energy.eddie.dataneeds.web.management;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.SmartMeterAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsDbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping(path = DataNeedsManagementController.BASE_PATH_KEY, produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
@Tag(name = "Data needs management controller", description = "Only available when data needs are loaded from the database!")
public class DataNeedsManagementController {
    public static final String BASE_PATH_KEY = "${eddie.management.server.urlprefix}";
    public final String basePath;
    private final DataNeedsDbService service;

    public DataNeedsManagementController(DataNeedsDbService service, @Value(BASE_PATH_KEY) String basePath) {
        this.service = service;
        this.basePath = basePath;
    }

    @Operation(summary = "Create a new data need")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(
                    examples = {
                            @ExampleObject(name = "Validated historical consumption data with open start/end",
                                    description = "Create a new data need for validated historical consumption data with the earliest possible start date and latest possible end date for electricity and accept data with a granularity between quarter-hourly and hourly.",
                                    value = "{\"type\":\"validated\",\"policyLink\":\"https://example.com/toc\",\"name\":\"My awesome data need\",\"description\":\"# This is a description\",\"purpose\":\"A text explaining the purpose of this data need.\",\"duration\":{\"type\":\"relativeDuration\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"P1D\",\"regionConnectorFilter\":{\"type\":\"blocklist\",\"regionConnectorIds\":[\"aiida\"]}}"
                            ),
                            @ExampleObject(name = "Generic AIIDA data need for the next 10 days",
                                    description = "Create a new data need to get the generic AIIDA data tags '1.7.0' and '1.8.0' in a two second interval for the next ten days including today",
                                    value = "{\"type\":\"genericAiida\",\"name\":\"Generic AIIDA data need\",\"description\":\"Please describe the data need.\",\"purpose\":\"And also its purpose.\",\"policyLink\":\"https://example.com/toc\",\"transmissionSchedule\":\"*/2 * * * * *\",\"asset\":\"Connection Agreement Point\",\"duration\":{\"type\":\"relativeDuration\",\"start\":\"P0D\",\"end\":\"P10D\"},\"dataTags\":[\"1.8.0\",\"1.7.0\"],\"regionConnectorFilter\":{\"type\":\"allowlist\",\"regionConnectorIds\":[\"aiida\"]}}"
                            )
                    }
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the data need",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(oneOf = {AccountingPointDataNeed.class, ValidatedHistoricalDataDataNeed.class, SmartMeterAiidaDataNeed.class, GenericAiidaDataNeed.class}), examples = {
                                    @ExampleObject(
                                            description = "Full data need object",
                                            value = "{\"type\":\"VALIDATED_HISTORICAL_CONSUMPTION_DATA\",\"id\":\"7f57cf16-5121-42a6-919e-7f7335826e64\",\"name\":\"My awesome data need\",\"description\":\"Some description.\",\"purpose\":\"My purpose.\",\"policyLink\":\"https://example.com/toc\",\"duration\":{\"type\":\"relative\",\"durationStart\":-12,\"durationEnd\":12,\"durationType\":\"MONTH\"},\"createdAt\":\"2024-03-04T12:55:13.014024Z\",\"energyType\":\"ELECTRICITY\",\"granularity\":\"PT15M\",\"regionConnectorFilter\":{\"type\":\"blocklist\",\"regionConnectorIds\":[\"aiida\"]}}"
                                    )
                            }
                            )
                    },
                    headers = {@Header(name = "Location", description = "Relative URL of the created data need", schema = @Schema(type = "string", example = "/dataNeed/7f57cf16-5121-42a6-919e-7f7335826e64"))}
            ),
            @ApiResponse(responseCode = "400", description = "Bad request body supplied or validation of values failed.",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = EddieApiError.class)),
                            examples = @ExampleObject("{\"errors\":[{\"message\":\"description: must not be blank\"},{\"message\":\"purpose: must not be blank\"}]}")
                    ))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataNeed> createNewDataNeed(@Validated @RequestBody DataNeed newDataNeed) {
        DataNeed savedDataNeed = service.saveNewDataNeed(newDataNeed);

        return ResponseEntity
                .created(URI.create(basePath + "/" + newDataNeed.id()))
                .body(savedDataNeed);
    }

    @Operation(summary = "Get details of all existing data needs")
    @GetMapping
    // Cannot use Page<DataNeed> because then the type information is not serialized; see https://github.com/FasterXML/jackson-databind/issues/2710#issuecomment-624839647
    public ResponseEntity<List<DataNeed>> getAllDataNeeds() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Get details of an existing data need")
    @Parameter(in = ParameterIn.PATH, name = "id", example = "7f57cf16-5121-42a6-919e-7f7335826e64", schema = @Schema(type = "String"))
    @ApiResponse(responseCode = "200",
            description = "OK",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(oneOf = {AccountingPointDataNeed.class, ValidatedHistoricalDataDataNeed.class, SmartMeterAiidaDataNeed.class, GenericAiidaDataNeed.class}),
                    examples = {
                            @ExampleObject(
                                    value = "{\"type\":\"validated\",\"id\":\"5dc53107-144b-406f-a689-74fb50729271\",\"name\":\"My awesome data need\",\"description\":\"Some description.\",\"purpose\":\"My purpose.\",\"policyLink\":\"https://example.com/toc\",\"createdAt\":\"2024-03-18T06:33:45.205489Z\",\"duration\":{\"type\":\"relativeDuration\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT15M\",\"regionConnectorFilter\":{\"type\":\"blocklist\",\"regionConnectorIds\":[\"aiida\"]}}"
                            )
                    }
            ))
    @ApiResponse(responseCode = "404",
            description = "No data need with the supplied id was found",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = EddieApiError.class)),
                    examples = @ExampleObject("{\"errors\":[{\"message\":\"No data need with ID '7f57cf16-5121-42a6-919e-7f7335826e64' found.\"}]}")
            ))
    @GetMapping("/{id}")
    public ResponseEntity<DataNeed> getDataNeed(@PathVariable String id) throws DataNeedNotFoundException {
        return service.findById(id)
                      .map(ResponseEntity::ok)
                      .orElseThrow(() -> new DataNeedNotFoundException(id, false));
    }

    @Operation(summary = "Delete an existing data need. Please note that deleting a data need will negatively influence all permission requests for that data need that are not yet in a terminal state, possibly rendering them useless.")
    @Parameter(in = ParameterIn.PATH, name = "id", example = "7f57cf16-5121-42a6-919e-7f7335826e64", schema = @Schema(type = "String"))
    @ApiResponse(responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema))
    @ApiResponse(responseCode = "404",
            description = "No data need with the supplied id was found",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = EddieApiError.class)),
                    examples = @ExampleObject("{\"errors\":[{\"message\":\"No data need with ID '7f57cf16-5121-42a6-919e-7f7335826e64' found.\"}]}")
            ))
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDataNeed(@PathVariable String id) throws DataNeedNotFoundException {
        if (!service.existsById(id)) {
            throw new DataNeedNotFoundException(id, false);
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Enable or disable an existing data need. Note that disabling data needs will not influence already created permission requests.")
    @Parameter(in = ParameterIn.PATH, name = "id", example = "7f57cf16-5121-42a6-919e-7f7335826e64", schema = @Schema(type = "String"))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = EnableDisableBody.class),
                    examples = @ExampleObject("{\"isEnabled\": true }")
            )
    )
    @ApiResponse(responseCode = "204",
            description = "OK",
            content = @Content(schema = @Schema))
    @ApiResponse(responseCode = "404",
            description = "No data need with the supplied id was found",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = EddieApiError.class)),
                    examples = @ExampleObject("{\"errors\":[{\"message\":\"No data need with ID '7f57cf16-5121-42a6-919e-7f7335826e64' found.\"}]}")
            ))
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> enableOrDisableDataNeed(
            @PathVariable String id,
            @RequestBody EnableDisableBody enableDisableBody
    ) throws DataNeedNotFoundException {
        if (!service.existsById(id)) {
            throw new DataNeedNotFoundException(id, false);
        }
        service.enableOrDisableDataNeed(id, enableDisableBody.isEnabled());
        return ResponseEntity.noContent().build();
    }
}
