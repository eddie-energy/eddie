// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.client;

import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdsServerClientFactoryTest {
    @Mock
    private CdsServerRepository repository;
    @InjectMocks
    private CdsServerClientFactory factory;

    @Test
    void testGetAll_returnsAllCdsServers() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L).build();
        when(repository.findAll()).thenReturn(List.of(cdsServer));

        // When
        var res = factory.getAll();

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testGetById_forUnknownCdsServer_returnsEmpty() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.empty());

        // When
        var res = factory.get(1L);

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void testGetById_forKnownCdsServer_returnsClient() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(cdsServer));

        // When
        var res = factory.get(1L);

        // Then
        assertThat(res).isPresent();
    }

    @Test
    void testGetById_forCachedCdsServer_returnsClient() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(cdsServer));

        // When
        factory.get(1L);
        var res = factory.get(1L);

        // Then
        assertThat(res).isPresent();
    }

    @Test
    void testGetByCdsServer_returnsClient() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L).build();

        // When
        var res = factory.get(cdsServer);

        // Then
        assertThat(res).isNotNull();
    }

    @Test
    void testGetByPermissionRequest_returnsClient() {
        // Given
        var pr = new CdsPermissionRequestBuilder().setCdsServer(1L).build();
        var cdsServer = new CdsServerBuilder().setId(1L).build();
        when(repository.getReferenceById(1L)).thenReturn(cdsServer);

        // When
        var res = factory.get(pr);

        // Then
        assertThat(res).isNotNull();
    }

    @Test
    void testGetTemporary_returnsClient() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L).build();

        // When
        var res = factory.getTemporary(cdsServer);

        // Then
        verify(repository, never()).getReferenceById(anyLong());
        assertThat(res).isNotNull();
    }
}