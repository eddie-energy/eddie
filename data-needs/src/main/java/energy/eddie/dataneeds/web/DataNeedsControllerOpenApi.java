package energy.eddie.dataneeds.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
// This interface is only used to contain the OpenAPI definitions so they do not pollute the controller itself
public interface DataNeedsControllerOpenApi {
    String EXAMPLE_RESPONSE_JSON = "{\"type\":\"validated\",\"id\":\"d73f44e1-239c-4610-b1e4-212ae35a792e\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":\"purpose\",\"policyLink\":\"https://example.com/toc\",\"createdAt\":\"2024-03-14T06:16:49.983259Z\",\"duration\":{\"type\":\"relativeDuration\",\"start\":\"P-1Y\",\"end\":\"P12M\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT15M\"}";

    @Operation(summary = "Get a list of IDs and names of all data needs")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DataNeedsNameAndIdProjection.class),
                    examples = @ExampleObject(
                            value = "[{\"name\":\"My awesome data need\",\"id\":\"207defb8-3aad-4281-a6ef-d77ba1601217\"},{\"name\":\"Another data need\",\"id\":\"my-id\"}]"
                    )
            )
    )
    ResponseEntity<List<DataNeedsNameAndIdProjection>> getDataNeedIdsAndNames();

    @Operation(summary = "Get details of an existing data need")
    @Parameter(in = ParameterIn.PATH, name = "id", example = "7f57cf16-5121-42a6-919e-7f7335826e64", schema = @Schema(type = "String"))
    @ApiResponse(responseCode = "200",
            content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(oneOf = {AccountingPointDataNeed.class, ValidatedHistoricalDataDataNeed.class, AiidaDataNeed.class}),
                            examples = {
                                    @ExampleObject(
                                            description = "Full data need object",
                                            value = DataNeedsControllerOpenApi.EXAMPLE_RESPONSE_JSON
                                    )
                            }
                    )
            }
    )
    @ApiResponse(responseCode = "404",
            description = "No data need with the supplied id was found.",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = EddieApiError.class)),
                    examples = @ExampleObject("{\"errors\":[{\"message\":\"No data need with ID '7f57cf16-5121-42a6-919e-7f7335826e64' found.\"}]}")
            )
    )
    ResponseEntity<DataNeed> getDataNeeds(String id) throws DataNeedNotFoundException;

    @Operation(summary = "Get details of a number of data needs")
    @Parameter(in = ParameterIn.QUERY, name = "data-need-id", example = "7f57cf16-5121-42a6-919e-7f7335826e64", schema = @Schema(type = "String"))
    @ApiResponse(responseCode = "200",
            content = {
                    @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(oneOf = {AccountingPointDataNeed.class, ValidatedHistoricalDataDataNeed.class, AiidaDataNeed.class})),
                            examples = {
                                    @ExampleObject(
                                            description = "Full data need object",
                                            value = "[ " + DataNeedsControllerOpenApi.EXAMPLE_RESPONSE_JSON + "]"
                                    )
                            }
                    )
            }
    )
    @ApiResponse(responseCode = "404",
            description = "No data need with the supplied id was found.",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = EddieApiError.class)),
                    examples = @ExampleObject("{\"errors\":[{\"message\":\"No data need with ID '7f57cf16-5121-42a6-919e-7f7335826e64' found.\"}]}")
            )
    )
    ResponseEntity<List<DataNeed>> getDataNeeds(Set<String> ids);
}
