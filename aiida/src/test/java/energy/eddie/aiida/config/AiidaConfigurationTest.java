// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiidaConfigurationTest {

    @Test
    void localhostRewriteFilter_rewritesLocalhostHost() {
        var filter = AiidaConfiguration.localhostReplacementFilter("example.org");
        var next = mock(ExchangeFunction.class);

        when(next.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK).build()));

        var request = ClientRequest.create(
                HttpMethod.GET,
                URI.create("http://localhost:8080/handshake/test?foo=bar")
        ).build();

        filter.filter(request, next).block();

        var captor = ArgumentCaptor.forClass(ClientRequest.class);
        verify(next).exchange(captor.capture());

        assertEquals("http://example.org:8080/handshake/test?foo=bar", captor.getValue().url().toString());
    }
}