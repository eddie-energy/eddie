// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.PermissionDetailsDto;
import energy.eddie.aiida.models.permission.HandshakeTarget;
import energy.eddie.api.agnostic.aiida.AiidaPermissionAcceptDto;
import energy.eddie.api.agnostic.aiida.AiidaPermissionDeclineDto;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Performs the handshake with the EDDIE framework that created a permission request: fetching its details and
 * reporting whether it was accepted, rejected, or cannot be fulfilled.
 */
@Service
public class HandshakeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeService.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCEPT_PATH = "/accept";
    private static final String REJECT_PATH = "/reject";
    private static final String UNFULFILLABLE_PATH = "/unfulfillable";
    private final UUID aiidaId;
    private final WebClient webClient;

    public HandshakeService(
            ApplicationInformationService applicationInformationService,
            WebClient webClient
    ) {
        this.aiidaId = applicationInformationService.applicationInformation().aiidaId();
        this.webClient = webClient;
    }

    /**
     * Fetches the details about the permission and the associated data need from the respective EDDIE framework.
     */
    public Mono<PermissionDetailsDto> fetchDetails(HandshakeTarget target) {
        LOGGER.info("Fetching details from {}", target.handshakeUrl());

        return webClient.get()
                        .uri(target.handshakeUrl())
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + target.accessToken())
                        .retrieve()
                        .bodyToMono(PermissionDetailsDto.class);
    }

    /**
     * Accepts the permission request at the respective EDDIE framework. Returns the MQTT credentials and topic names
     * that are to be used by the associated permission.
     */
    public Mono<MqttDto> accept(HandshakeTarget target) {
        LOGGER.info("Accepting permission request {}", target.handshakeUrl());

        return webClient.post()
                        .uri(target.handshakeUrl() + ACCEPT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new AiidaPermissionAcceptDto(aiidaId))
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + target.accessToken())
                        .retrieve()
                        .bodyToMono(MqttDto.class);
    }

    /**
     * Makes a blocking request to the EDDIE framework to report that the user rejected the permission request.
     * Occurring errors are logged and rethrown.
     *
     * @param reason Optional human-readable reason that is forwarded to the eligible party.
     */
    public void reject(HandshakeTarget target, @Nullable String reason) {
        decline(target, REJECT_PATH, reason);
    }

    /**
     * Makes a blocking request to the EDDIE framework to report that this AIIDA instance cannot fulfill the permission
     * request. Occurring errors are logged and rethrown.
     *
     * @param reason Optional human-readable reason that is forwarded to the eligible party.
     */
    public void denoteUnfulfillable(HandshakeTarget target, @Nullable String reason) {
        decline(target, UNFULFILLABLE_PATH, reason);
    }

    private void decline(HandshakeTarget target, String path, @Nullable String reason) {
        var url = target.handshakeUrl() + path;
        LOGGER.info("Declining permission request via {}", url);

        webClient.post()
                 .uri(url)
                 .contentType(MediaType.APPLICATION_JSON)
                 .bodyValue(new AiidaPermissionDeclineDto(aiidaId, reason))
                 .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + target.accessToken())
                 .retrieve()
                 .toBodilessEntity()
                 .doOnError(error -> LOGGER.atError()
                                           .addArgument(url)
                                           .setCause(error)
                                           .log("Error while declining permission request via {}"))
                 .block();
    }
}
