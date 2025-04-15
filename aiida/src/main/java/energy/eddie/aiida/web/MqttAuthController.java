package energy.eddie.aiida.web;

import energy.eddie.aiida.errors.MqttUnauthorizedException;
import energy.eddie.aiida.models.datasource.mqtt.MqttAction;
import energy.eddie.aiida.services.MqttAuthService;
import energy.eddie.api.agnostic.EddieApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mqtt-auth")
public class MqttAuthController {
    private final MqttAuthService mqttAuthService;

    public MqttAuthController(MqttAuthService mqttAuthService) {
        this.mqttAuthService = mqttAuthService;
    }

    @Operation(summary = "Authenticate")
    @ApiResponse(responseCode = "200", description = "success")
    @ApiResponse(responseCode = "401", description = "unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class)))
    @PostMapping(value = "/auth", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> auth(
            @RequestParam String username,
            @RequestParam String password
    ) throws MqttUnauthorizedException {
        mqttAuthService.isAuthenticatedOrThrow(username, password);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Authenticate Admin")
    @ApiResponse(responseCode = "200", description = "success")
    @ApiResponse(responseCode = "401", description = "unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class)))
    @PostMapping(value = "/superuser", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> superuser(
            @RequestParam String username,
            @RequestParam String password
    ) throws MqttUnauthorizedException {
        mqttAuthService.isAdminOrThrow(username, password);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Authorize")
    @ApiResponse(responseCode = "200", description = "success")
    @ApiResponse(responseCode = "401", description = "unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class)))
    @PostMapping(value = "/acl", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> acl(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String action,
            @RequestParam String topic
    ) throws MqttUnauthorizedException {
        var mqttAction = MqttAction.forId(Integer.parseInt(action));
        mqttAuthService.isAuthorizedOrThrow(username, password, mqttAction, topic);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
