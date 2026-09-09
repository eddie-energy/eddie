// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EventStream {
    private static final String X_ACCEL_BUFFERING = "X-Accel-Buffering";

    /**
     * Wraps a flux for an SSE endpoint
     * {@code produces = TEXT_EVENT_STREAM_VALUE}.
     *
     * @param stream the source for the events
     * @param <T>     the type of the source of the events
     * @return the SSE response
     */
    public <T> ResponseEntity<Flux<T>> toJson(Flux<T> stream) {
        return ResponseEntity.ok()
                             .header(X_ACCEL_BUFFERING, "no")
                             .body(stream);
    }
}
