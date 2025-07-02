package energy.eddie.aiida.web;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/inbound")
@Tag(name = "Inbound Controller")
public class InboundController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final InboundService inboundService;

    public InboundController(InboundService inboundService) {
        this.inboundService = inboundService;
    }

    @Operation(summary = "Get latest inbound record for data source")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = InboundRecord.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
    })
    @GetMapping(value = "/latest/{dataSourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InboundRecord> latestRecord(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID dataSourceId
    ) throws UnauthorizedException {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            var accessCode = authorizationHeader.replace(BEARER_PREFIX, "");
            return ResponseEntity.ok(inboundService.latestRecord(accessCode, dataSourceId));
        }

        throw new UnauthorizedException("Authorization header is missing or invalid");
    }
}
