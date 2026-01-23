// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;

public abstract class AiidaStreamer implements AutoCloseable {
    protected final UUID aiidaId;
    protected final Flux<AiidaRecord> recordFlux;
    protected final Sinks.One<UUID> terminationRequestSink;

    /**
     * Create a new AiidaStreamer and sets the Flux for records and status messages that should be sent. The constructor
     * should initialize and prepare any necessary resources but only after {@link #connect()} was called, connections
     * should be opened and data streamed.
     *
     * @param recordFlux             Flux, where records that should be sent are available.
     * @param terminationRequestSink Sink, to which the permissionId will be published when the EP requests a
     *                               termination.
     */
    protected AiidaStreamer(UUID aiidaId, Flux<AiidaRecord> recordFlux, Sinks.One<UUID> terminationRequestSink) {
        this.aiidaId = aiidaId;
        this.recordFlux = recordFlux;
        this.terminationRequestSink = terminationRequestSink;
    }

    /**
     * Open required connections to the streaming target (EP) in this method, not beforehand. Subscribe to the Fluxes in
     * this method, to receive records and status messages that shall be sent. Start listening for termination requests
     * from the EP.
     */
    public abstract void connect();

    /**
     * Unsubscribe from any Flux and free any used resources in this method. May flush all queued messages beforehand.
     */
    @Override
    public abstract void close();

    /**
     * Sends a {@link AiidaConnectionStatusMessageDto} with the passed {@code status} before closing this streamer for good.
     * May block until the passed {@code statusMessage} is transmitted.
     *
     * @see AiidaStreamer#close()
     */
    public abstract void closeTerminally(AiidaConnectionStatusMessageDto statusMessage);
}
