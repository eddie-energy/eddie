package energy.eddie.regionconnector.at.eda.ponton;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

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
        @Name("password") String password
) {}
