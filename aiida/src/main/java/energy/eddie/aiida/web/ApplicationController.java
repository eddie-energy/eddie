package energy.eddie.aiida.web;

import energy.eddie.aiida.application.information.ApplicationInformation;
import energy.eddie.aiida.services.ApplicationInformationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApplicationController {
    private final ApplicationInformation applicationInformation;

    public ApplicationController(ApplicationInformationService applicationInformationService) {
        this.applicationInformation = applicationInformationService.applicationInformation();
    }

    @Operation(summary = "Get application information", description = "Get information about the application.")
    @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApplicationInformation.class))})
    @GetMapping(value = "/application-information", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationInformation> applicationInformation() {
        return ResponseEntity.ok(applicationInformation);
    }
}
