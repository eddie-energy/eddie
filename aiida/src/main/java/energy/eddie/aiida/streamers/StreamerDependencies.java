// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.schemas.rtd.SchemaFormatterRegistry;
import energy.eddie.aiida.services.secrets.SecretsService;
import energy.eddie.cim.agnostic.PermissionCommand;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

/**
 * Bundles the dependencies shared by every {@link AiidaStreamer} implementation.
 * These are wired once by {@link StreamerManager} and forwarded to every streamer.
 */
public record StreamerDependencies(
        FailedToSendRepository failedToSendRepository,
        ObjectMapper mapper,
        SchemaFormatterRegistry schemaFormatterRegistry,
        Sinks.Many<PermissionCommand> commandSink,
        PermissionLatestRecordMap permissionLatestRecordMap,
        SecretsService secretsService
) {
}
