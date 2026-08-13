// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.Name;

import java.time.Duration;

/**
 * This record defines all information needed for a PontonXPAdapter to establish a connection to a Ponton XP Messenger.
 *
 * @param adapterId      ID of the adapter that will be used by the Ponton XP Messenger.
 *                       The value used for this should be configured as the default adapter in the messenger
 * @param adapterVersion Version of the adapter.
 * @param hostname       Address of the Ponton XP Messenger
 * @param port           Port of the Ponton XP Messenger adapter interface (default: 2600)
 * @param apiEndpoint    API endpoint of the Ponton XP Messenger
 * @param workFolder     Path to the folder where the Ponton XP Adapter will store its files like id.dat which is used by the messenger to identify the adapter.
 * @param username       Username for the Ponton XP Messenger to use REST API endpoints that require authentication.
 *                       Needs to be a user without 2FA activated.
 * @param password       Password for the given username.
 * @param inboundConnections Expected amount of inbound WebSocket connections to the Ponton XP Messenger.
 * @param outboundConnections Expected amount of outbound WebSocket connections to the Ponton XP Messenger.
 * @param archiveConnections Expected amount of archive WebSocket connections to the Ponton XP Messenger.
 * @param connectionWatchdogInterval How often the Ponton XP Messenger adapter WebSocket connection is checked.
 * @param connectionStatusTimeout Maximum time after start or reconnect until a connection-status callback must be received.
 */
@ConfigurationProperties("region-connector.at.eda.ponton.messenger")
public record PontonXPAdapterConfiguration(
        @Name("adapter.id") String adapterId,
        @Name("adapter.version") String adapterVersion,
        @Name("hostname") String hostname,
        @Name("port") int port,
        @Name("api.endpoint") String apiEndpoint,
        @Name("folder") String workFolder,
        @Name("username") String username,
        @Name("password") String password,
        @Name("inbound-connections") int inboundConnections,
        @Name("outbound-connections") int outboundConnections,
        @Name("archive-connections") int archiveConnections,
        @Name("connection-watchdog.interval") Duration connectionWatchdogInterval,
        @Name("connection-watchdog.status-timeout") Duration connectionStatusTimeout
) {
    private static final int DEFAULT_INBOUND_CONNECTIONS = 1;
    private static final int DEFAULT_OUTBOUND_CONNECTIONS = 1;
    private static final int DEFAULT_ARCHIVE_CONNECTIONS = 0;
    private static final Duration DEFAULT_CONNECTION_WATCHDOG_INTERVAL = Duration.ofMinutes(1);
    private static final Duration DEFAULT_CONNECTION_STATUS_TIMEOUT = Duration.ofMinutes(2);

    @ConstructorBinding
    public PontonXPAdapterConfiguration {
        inboundConnections = inboundConnections <= 0 ? DEFAULT_INBOUND_CONNECTIONS : inboundConnections;
        outboundConnections = outboundConnections <= 0 ? DEFAULT_OUTBOUND_CONNECTIONS : outboundConnections;
        archiveConnections = Math.max(archiveConnections, DEFAULT_ARCHIVE_CONNECTIONS);
        connectionWatchdogInterval = connectionWatchdogInterval == null
                ? DEFAULT_CONNECTION_WATCHDOG_INTERVAL
                : connectionWatchdogInterval;
        connectionStatusTimeout = connectionStatusTimeout == null
                ? DEFAULT_CONNECTION_STATUS_TIMEOUT
                : connectionStatusTimeout;
    }

    public PontonXPAdapterConfiguration(
            String adapterId,
            String adapterVersion,
            String hostname,
            int port,
            String apiEndpoint,
            String workFolder,
            String username,
            String password
    ) {
        this(
                adapterId,
                adapterVersion,
                hostname,
                port,
                apiEndpoint,
                workFolder,
                username,
                password,
                DEFAULT_INBOUND_CONNECTIONS,
                DEFAULT_OUTBOUND_CONNECTIONS,
                DEFAULT_ARCHIVE_CONNECTIONS,
                DEFAULT_CONNECTION_WATCHDOG_INTERVAL,
                DEFAULT_CONNECTION_STATUS_TIMEOUT
        );
    }
}
