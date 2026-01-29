// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger.client;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messenger.PontonTokenProvider;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.FindMessages;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.Messages;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class FindMessagesClient {
    private final WebClient webClient;
    private final PontonTokenProvider tokenProvider;
    private final PontonXPAdapterConfiguration config;

    public FindMessagesClient(
            WebClient webClient, PontonTokenProvider tokenProvider,
            PontonXPAdapterConfiguration config
    ) {
        this.webClient = webClient;
        this.tokenProvider = tokenProvider;
        this.config = config;
    }

    public Mono<Messages> findMessages(FindMessages body) {
        return tokenProvider.getToken()
                            .flatMap(token -> findMessages(body, token));
    }

    private Mono<Messages> findMessages(FindMessages body, String token) {
        return webClient.post()
                        .uri(config.apiEndpoint() + "/messagemonitor/findmessages")
                        .header("Authorization", "Bearer " + token)
                        // Ignore keep alive limit to get latest data
                        .header("Connection", "close")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Messages.class);
    }
}
