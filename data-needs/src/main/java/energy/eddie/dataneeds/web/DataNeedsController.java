// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import energy.eddie.dataneeds.services.DataNeedsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Data needs public API", description = "Used by the frontend to get information about data needs.")
public class DataNeedsController {
    public static final String EXAMPLE_RESPONSE_JSON = "{\"type\":\"validated\",\"id\":\"d73f44e1-239c-4610-b1e4-212ae35a792e\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":\"purpose\",\"policyLink\":\"https://example.com/toc\",\"createdAt\":\"2024-03-14T06:16:49.983259Z\",\"duration\":{\"type\":\"relativeDuration\",\"start\":\"P-1Y\",\"end\":\"P12M\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT15M\"}";

    private final DataNeedsService dataNeedsService;

    public DataNeedsController(DataNeedsService dataNeedsService) {
        this.dataNeedsService = dataNeedsService;
    }

    @Operation(summary = "Get a list of IDs and names of all data needs")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DataNeedsNameAndIdProjection.class),
                    examples = @ExampleObject(
                            value = "[{\"name\":\"My awesome data need\",\"id\":\"207defb8-3aad-4281-a6ef-d77ba1601217\"},{\"name\":\"Another data need\",\"id\":\"my-id\"}]"
                    )
            )
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DataNeedsNameAndIdProjection>> getDataNeedIdsAndNames() {
        return ResponseEntity.ok(dataNeedsService.getDataNeedIdsAndNames());
    }

    @Operation(summary = "Get details of an existing data need")
    @Parameter(in = ParameterIn.PATH, name = "id", example = "7f57cf16-5121-42a6-919e-7f7335826e64", schema = @Schema(type = "String"))
    @ApiResponse(responseCode = "200",
            content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(oneOf = {AccountingPointDataNeed.class, ValidatedHistoricalDataDataNeed.class, AiidaDataNeed.class}),
                            examples = {
                                    @ExampleObject(
                                            description = "Full data need object",
                                            value = DataNeedsController.EXAMPLE_RESPONSE_JSON
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
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataNeed> getDataNeed(@PathVariable String id) throws DataNeedNotFoundException {
        return dataNeedsService.findById(id)
                               .map(ResponseEntity::ok)
                               .orElseThrow(() -> new DataNeedNotFoundException(id, false));
    }
}
