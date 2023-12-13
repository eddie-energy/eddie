package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.client.*;
import energy.eddie.regionconnector.es.datadis.config.ConfigDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.permission.request.InMemoryPermissionRequestRepository;
import org.eclipse.microprofile.config.Config;
import reactor.netty.http.client.HttpClient;

import static java.util.Objects.requireNonNull;

public class DatadisRegionConnectorFactory implements RegionConnectorFactory {

    @Override
    // sonarcloud complains about not using try-with-resources, but using it would close the adapter upon returning, which is not what we want
    @SuppressWarnings("java:S2095")
    public RegionConnector create(Config config) {
        requireNonNull(config);

        DatadisEndpoints endpoints = new DatadisEndpoints();
        DatadisConfig datadisConfig = new ConfigDatadisConfiguration(config);
        DatadisTokenProvider tokenProvider = new NettyDatadisTokenProvider(datadisConfig, HttpClient.create(), endpoints);
        DataApi dataApi = new NettyDataApiClient(HttpClient.create(), tokenProvider, endpoints);
        AuthorizationApi authorizationApi = new NettyAuthorizationApiClient(HttpClient.create(), tokenProvider, endpoints);

        return new DatadisRegionConnector(dataApi, authorizationApi, new InMemoryPermissionRequestRepository());
    }
}
