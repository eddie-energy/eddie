// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.tasks;

import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.model.OpaqueEnvelopeModel;
import energy.eddie.outbound.rest.model.cim.v1_12.MinMaxEnvelopeMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.OpaqueEnvelopeRepository;
import energy.eddie.outbound.rest.persistence.cim.v1_12.MinMaxEnvelopeMarketDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.PredicateSpecification;

import java.time.Duration;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeletionTaskTest {
    private final RestOutboundConnectorConfiguration config = new RestOutboundConnectorConfiguration(Duration.ZERO);
    @Mock
    private ConnectionStatusMessageRepository csmRepository;
    @Mock
    private OpaqueEnvelopeRepository opaqueRepository;
    @Mock
    private MinMaxEnvelopeMarketDocumentRepository minMaxRepository;

    @Test
    void deleteConnectionStatusMessages_deletes() {
        // Given
        var task = new DeletionTask<>(csmRepository, config);

        // When
        task.delete();

        // Then
        verify(csmRepository).delete(ArgumentMatchers.<PredicateSpecification<ConnectionStatusMessageModel>>any());
    }

    @Test
    void deleteOpaqueEnvelopes_deletes() {
        // Given
        var task = new DeletionTask<>(opaqueRepository, config);

        // When
        task.delete();

        // Then
        verify(opaqueRepository).delete(ArgumentMatchers.<PredicateSpecification<OpaqueEnvelopeModel>>any());
    }

    @Test
    void deleteMinMaxEnvelopes_deletes() {
        // Given
        var task = new DeletionTask<>(minMaxRepository, config);

        // When
        task.delete();

        // Then
        verify(minMaxRepository).delete(ArgumentMatchers.<PredicateSpecification<MinMaxEnvelopeMarketDocumentModel>>any());
    }
}