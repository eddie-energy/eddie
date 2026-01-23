// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.connectors.cim.v0_82;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class CimConnectorTest {
    private final CimConnector connector = new CimConnector();

    @Test
    void setVhd_producesVhds() {
        // Given
        var flux = Flux.just(new ValidatedHistoricalDataEnvelope());

        // When
        connector.setEddieValidatedHistoricalDataMarketDocumentStream(flux);

        // Then
        StepVerifier.create(connector.getHistoricalDataMarketDocumentStream())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void setPmd_producesPmds() {
        // Given
        var flux = Flux.just(new PermissionEnvelope());

        // When
        connector.setPermissionMarketDocumentStream(flux);

        // Then
        StepVerifier.create(connector.getPermissionMarketDocumentStream())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void setAp_producesAps() {
        // Given
        var flux = Flux.just(new AccountingPointEnvelope());

        // When
        connector.setAccountingPointEnvelopeStream(flux);

        // Then
        StepVerifier.create(connector.getAccountingPointDataMarketDocumentStream())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void setPublish_producesTerminationDocument() {
        // Given
        var permissionEnvelope = new PermissionEnvelope();
        // When
        connector.publish(permissionEnvelope);

        // Then
        StepVerifier.create(connector.getTerminationMessages())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}