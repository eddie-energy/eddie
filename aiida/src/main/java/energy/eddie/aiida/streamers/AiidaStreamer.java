// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.schemas.rtd.SchemaFormatterRegistry;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.cim.agnostic.PermissionCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public abstract class AiidaStreamer implements AutoCloseable {
    protected final Sinks.Many<PermissionCommand> commandSink;
    protected final SchemaFormatterRegistry schemaFormatterRegistry;
    protected Flux<AiidaRecord> recordFlux;

    /**
     * Create a new AiidaStreamer and sets the Flux for records and status messages that should be sent. The constructor
     * should initialize and prepare any necessary resources but only after {@link #connect()} was called, connections
     * should be opened and data streamed.
     *
     * @param recordFlux  Flux, where records that should be sent are available.
     * @param commandSink Sink, to which a {@link PermissionCommand} is published when the EP sends a control command.
     */
    protected AiidaStreamer(
            Flux<AiidaRecord> recordFlux,
            SchemaFormatterRegistry schemaFormatterRegistry,
            Sinks.Many<PermissionCommand> commandSink
    ) {
        this.recordFlux = recordFlux;
        this.schemaFormatterRegistry = schemaFormatterRegistry;
        this.commandSink = commandSink;
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

    /**
     * Enables or disables data transmission for this streamer. While disabled, records are not published to the EP.
     */
    public abstract void setTransmissionEnabled(boolean enabled);

    /**
     * Replaces the record flux this streamer is subscribed to and resubscribes. Used to apply a new transmission
     * schedule, which re-aggregates the upstream records on a different cadence.
     */
    public abstract void updateRecordFlux(Flux<AiidaRecord> recordFlux);
}
