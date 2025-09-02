package energy.eddie.aiida.web;

import energy.eddie.aiida.errors.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.PermissionNotFoundException;
import energy.eddie.aiida.errors.UnauthorizedException;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.InboundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/inbound")
@Tag(name = "Inbound Controller")
public class InboundController {
    private final InboundService inboundService;

    public InboundController(InboundService inboundService) {
        this.inboundService = inboundService;
    }

    @Operation(summary = "Get latest inbound record for permission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = InboundRecord.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
    })
    @GetMapping(value = "/latest/{permissionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InboundRecord> latestRecord(
            @PathVariable UUID permissionId,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader,
            @RequestParam(name = "apiKey", required = false) String apiKeyQuery
    ) throws UnauthorizedException, PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException {
        String apiKey = (apiKeyHeader != null && !apiKeyHeader.isBlank())
                ? apiKeyHeader
                : apiKeyQuery;

        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("API key missing: provide X-API-Key header or ?apiKey= query param.");
        }

        return ResponseEntity.ok(inboundService.latestRecord(apiKey, permissionId));
    }
}
