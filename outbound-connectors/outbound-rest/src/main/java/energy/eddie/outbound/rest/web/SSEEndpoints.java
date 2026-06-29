// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web;

import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@Component
public class SSEEndpoints {
    public static final String EVENT_STREAM_XML_VALUE = "application/vnd.eddie.energy.sse+xml";
    public static final MediaType EVENT_STREAM_XML = MediaType.parseMediaType(EVENT_STREAM_XML_VALUE);
    public static final String X_ACCEL_BUFFERING = "X-Accel-Buffering";
    private static final Logger LOGGER = LoggerFactory.getLogger(SSEEndpoints.class);
    private final XmlMessageSerde serde;

    public SSEEndpoints() throws SerdeInitializationException {
        this.serde = new XmlMessageSerde();
    }

    /**
     * Converts a flux of any type to a stream of XML documents, suitable for SSE endpoints.
     * It changes the <code>Content-Type</code>-Header to <code>text/event-stream</code>, since otherwise clients are not able to receive any events.
     * Drops objects that cannot be serialized to an XML document.
     *
     * @param flux the source for the events
     * @param <T>  the type of the source of the events
     * @return a stream of XML documents
     */
    public <T> ResponseEntity<Flux<String>> xml(Flux<T> flux) {
        return ResponseEntity.ok()
                             // Tell reverse proxies like Nginx not to buffer the response
                             .header(X_ACCEL_BUFFERING, "no")
                             .header("Content-Type", TEXT_EVENT_STREAM_VALUE)
                             .body(flux.mapNotNull(this::toXml));
    }

    public <T> ResponseEntity<Flux<T>> json(Flux<T> flux) {
        return ResponseEntity.ok()
                             // Tell reverse proxies like Nginx not to buffer the response
                             .header(X_ACCEL_BUFFERING, "no")
                             .body(flux);
    }

    @Nullable
    private String toXml(Object o) {
        try {
            var bytes = this.serde.serialize(o);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (SerializationException e) {
            LOGGER.warn("Error converting to XML: {}", o, e);
        }
        return null;
    }
}
