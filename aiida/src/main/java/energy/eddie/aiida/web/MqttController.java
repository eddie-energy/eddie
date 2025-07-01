package energy.eddie.aiida.web;

import energy.eddie.aiida.errors.MqttTlsCertificateNotFoundException;
import energy.eddie.aiida.services.MqttService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/mqtt")
@RestController
public class MqttController {
    private static final String TLS_CERTIFICATE_FILENAME = "cert.pem";
    private final MqttService mqttService;


    public MqttController(MqttService mqttService) {this.mqttService = mqttService;}

    @Operation(summary = "Get MQTT TLS certificate")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "TLS certificate retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "TLS certificate not found"
            ),
    })
    @GetMapping(value = "/download/tls-certificate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ByteArrayResource> tlsCertificate() throws MqttTlsCertificateNotFoundException {
        var certificate = mqttService.tlsCertificate();
        var contentDispositionHeader = String.format("attachment; filename=\"%s\"", TLS_CERTIFICATE_FILENAME);

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionHeader)
                             .body(certificate);
    }
}
