package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class StreamerFactory {
    private StreamerFactory() {
    }

    /**
     * Creates a new {@link AiidaStreamer} applying the specified streamingConfig.
     *
     * @param permission             Permission for which to create the AiidaStreamer.
     * @param recordFlux             Flux on which the records that should be sent are published.
     * @param statusMessageFlux      Flux on which status messages that should be sent are published.
     * @param terminationRequestSink Sink, to which the permissionId will be published, when the EP requests a
     *                               termination.
     * @param mapper                 {@link ObjectMapper} that should be used to convert the records to JSON.
     */
    protected static AiidaStreamer getAiidaStreamer(
            Permission permission,
            Flux<AiidaRecord> recordFlux,
            Flux<ConnectionStatusMessage> statusMessageFlux,
            Sinks.One<String> terminationRequestSink,
            ObjectMapper mapper
    ) {
        return new AiidaStreamer(recordFlux, statusMessageFlux, terminationRequestSink) {
            @Override
            public void connect() {
                // dummy
            }

            @Override
            public void close() {
                terminationRequestSink.tryEmitEmpty();
            }
        };
    }
}
