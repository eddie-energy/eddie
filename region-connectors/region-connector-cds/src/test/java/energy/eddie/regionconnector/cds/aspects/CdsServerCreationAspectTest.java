// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.aspects;

import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.client.MetadataCollection;
import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.health.HealthIndicatorCreator;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.client.creation.CdsClientCreationService;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {AspectConfig.class, CdsClientCreationService.class})
class CdsServerCreationAspectTest {
    @MockitoBean
    private HealthIndicatorCreator creator;
    @Autowired
    private CdsClientCreationService creationService;
    @MockitoBean
    private CdsServerRepository repository;
    @SuppressWarnings("unused")
    @MockitoBean
    private CdsServerClientFactory factory;
    @MockitoBean
    private MetadataCollection collection;
    @SuppressWarnings("unused")
    @MockitoBean
    private OAuthService oAuthService;
    @SuppressWarnings("unused")
    @MockitoBean
    private CdsConfiguration config;

    @Test
    void testAspect_callsHealthIndicatorCreator() throws MalformedURLException {
        // Given
        var cdsServer = new CdsServerBuilder().build();
        when(repository.findByBaseUri(any()))
                .thenReturn(Optional.of(cdsServer));

        // When
        creationService.createOAuthClients(URI.create("http://localhost").toURL());

        // Then
        verify(creator).register(cdsServer);
    }
    @Test
    void testAspect_doesntCallHealthIndicatorCreator_onFailedCdsServerRegistration() throws MalformedURLException {
        // Given
        when(repository.findByBaseUri(any()))
                .thenReturn(Optional.empty());
        when(collection.metadata(any()))
                .thenReturn(Mono.error(OAuthNotSupportedException::new));

        // When
        creationService.createOAuthClients(URI.create("http://localhost").toURL());

        // Then
        verify(creator, never()).register(any());
    }
}