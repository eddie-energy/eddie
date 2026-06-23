// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.tasks;

import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v1_12.recmmoe.MessageDocumentHeader;
import energy.eddie.cim.v1_12.recmmoe.MetaInformation;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.model.OpaqueEnvelopeModel;
import energy.eddie.outbound.rest.model.cim.v1_12.MinMaxEnvelopeMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.OpaqueEnvelopeRepository;
import energy.eddie.outbound.rest.persistence.cim.v1_12.MinMaxEnvelopeMarketDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InsertionTaskTest {
    @Mock
    private ConnectionStatusMessageRepository csmRepository;
    @Mock
    private OpaqueEnvelopeRepository opaqueEnvelopeRepository;
    @Mock
    private MinMaxEnvelopeMarketDocumentRepository minMaxRepository;

    @Test
    void insertsConnectionStatusMessages() {
        // Given
        var csm = new ConnectionStatusMessage("cid", "pid", "dnid", null, PermissionProcessStatus.CREATED);

        // When
        new InsertionTask<>(Flux.just(csm), csmRepository, ConnectionStatusMessageModel::new);

        // Then
        verify(csmRepository).save(new ConnectionStatusMessageModel(csm));
    }

    @Test
    void insertsOpaqueEnvelopes() {
        // Given
        var envelope = new OpaqueEnvelope("rid",
                                          "pid",
                                          "cid",
                                          "dnid",
                                          "mid",
                                          ZonedDateTime.now(ZoneOffset.UTC),
                                          "{}");

        // When
        new InsertionTask<>(Flux.just(envelope), opaqueEnvelopeRepository, OpaqueEnvelopeModel::new);

        // Then
        verify(opaqueEnvelopeRepository).save(new OpaqueEnvelopeModel(envelope));
    }

    @Test
    void insertsMinMaxEnvelopes() {
        // Given
        var envelope = new RECMMOEEnvelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeader()
                                .withMetaInformation(
                                        new MetaInformation()
                                                .withRequestPermissionId("pid")
                                                .withConnectionId("cid")
                                                .withDataNeedId("dnid")
                                )
                );

        // When
        new InsertionTask<>(Flux.just(envelope), minMaxRepository, MinMaxEnvelopeMarketDocumentModel::new);

        // Then
        verify(minMaxRepository).save(new MinMaxEnvelopeMarketDocumentModel(envelope));
    }
}