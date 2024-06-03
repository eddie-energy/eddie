package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.PermissionDetailsDto;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.api.agnostic.aiida.MqttDto;
import energy.eddie.api.agnostic.aiida.PermissionUpdateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static energy.eddie.api.agnostic.aiida.PermissionUpdateOperation.*;

@Component
public class HandshakeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeService.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private final WebClient webClient;

    public HandshakeService(WebClient webClient) {this.webClient = webClient;}

    /**
     * Fetches the details about the permission and the associated data need from the respective EDDIE framework. This
     * call is blocking.
     */
    public Mono<PermissionDetailsDto> fetchDetailsForPermission(Permission permission) {
        LOGGER.info("Fetching details for {}", permission.permissionId());

        return webClient.get()
                        .uri(permission.handshakeUrl())
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + permission.accessToken())
                        .retrieve()
                        .bodyToMono(PermissionDetailsDto.class);
    }

    /**
     * Makes a blocking request to the EDDIE framework to update the permission to indicate that this AIIDA instance
     * cannot fulfill this permission request. Any occurring errors are just logged.
     */
    public void sendUnfulfillableOrRejected(Permission permission, PermissionStatus status) {
        var otherStatus = switch (status) {
            case PermissionStatus.UNFULFILLABLE -> UNFULFILLABLE;
            case PermissionStatus.REJECTED -> REJECT;
            default -> throw new IllegalArgumentException("Need to pass either UNFULFILLABLE or REJECTED");
        };

        LOGGER.info("Sending {} for permission {}", status, permission.permissionId());

        var operation = new PermissionUpdateDto(otherStatus);

        webClient.patch()
                 .uri(permission.handshakeUrl())
                 .contentType(MediaType.APPLICATION_JSON)
                 .bodyValue(operation)
                 .accept(MediaType.APPLICATION_JSON)
                 .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + permission.accessToken())
                 .retrieve()
                 .bodyToMono(MqttDto.class)
                 .doOnError(error -> LOGGER.atError()
                                           .addArgument(permission.permissionId())
                                           .addArgument(status)
                                           .setCause(error)
                                           .log("Error while updating permission {} to new status {} at the EDDIE framework"))
                 .block();
    }

    /**
     * Accepts the permission and sends the appropriate message to the respective EDDIE framework. Will return the MQTT
     * credentials and topic names that are to be used by the associated permission.
     */
    public Mono<MqttDto> fetchMqttDetails(Permission permission) {
        LOGGER.info("Fetching mqtt details for permission {}", permission.permissionId());

        var operation = new PermissionUpdateDto(ACCEPT);

        return webClient.patch()
                        .uri(permission.handshakeUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(operation)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + permission.accessToken())
                        .retrieve()
                        .bodyToMono(MqttDto.class);
    }
}
