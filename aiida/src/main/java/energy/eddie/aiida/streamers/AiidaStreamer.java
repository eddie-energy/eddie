// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.agnostic.PermissionCommand;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Streams the records of a permission to the streaming target (EP) and receives {@link PermissionCommand}s from it.
 * Implementations should initialize and prepare any necessary resources on creation, but only open connections and
 * stream data after {@link #connect()} was called.
 */
public interface AiidaStreamer extends AutoCloseable {
    /**
     * Open required connections to the streaming target (EP) in this method, not beforehand. Subscribe to the Fluxes in
     * this method, to receive records and status messages that shall be sent. Start listening for termination requests
     * from the EP.
     */
    void connect();

    /**
     * Returns whether this streamer is currently connected to the streaming target (EP).
     */
    boolean isConnected();

    /**
     * Unsubscribe from any Flux and free any used resources in this method. May flush all queued messages beforehand.
     */
    @Override
    void close();

    /**
     * Sends a {@link AiidaConnectionStatusMessageDto} with the passed {@code status} before closing this streamer for
     * good. May block until the passed {@code statusMessage} is transmitted.
     *
     * @see AiidaStreamer#close()
     */
    void closeTerminally(AiidaConnectionStatusMessageDto statusMessage);

    /**
     * Enables or disables data transmission for this streamer. While disabled, records are not published to the EP.
     */
    void setTransmissionEnabled(boolean enabled);

    /**
     * Replaces the record flux this streamer is subscribed to and resubscribes. Used to apply a new transmission
     * schedule, which re-aggregates the upstream records on a different cadence.
     */
    void updateRecordFlux(Flux<AiidaRecord> recordFlux);

    /**
     * Publishes a payload of the provided schema to the permission of the provided id.
     */
    void publishSchemaPayload(UUID permissionId, AiidaSchema schema, String payload);
}
