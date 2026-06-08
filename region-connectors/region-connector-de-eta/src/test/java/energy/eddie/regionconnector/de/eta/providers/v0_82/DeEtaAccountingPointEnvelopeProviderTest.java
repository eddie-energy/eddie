// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.AccountingPointDataStream;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusAccountingPointData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableAccountingPointData;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeEtaAccountingPointEnvelopeProviderTest {

    @Test
    void getAccountingPointMarketDocumentsStream_emitsOneEnvelopePerInputPayload() {
        var stream = mock(AccountingPointDataStream.class);
        when(stream.accountingPointData()).thenReturn(Flux.just(samplePayload()));

        var provider = new DeEtaAccountingPointEnvelopeProvider(
                stream,
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME, "fallback"),
                new DeEtaPlusConfiguration(
                        "ep-id", "http://api.url", "client-id", "client-secret",
                        "/meters/historical", "/meters/accounting-point", 30,
                        3, 2, true, false, null, null)
        );

        StepVerifier.create(provider.getAccountingPointMarketDocumentsStream())
                .assertNext(envelope -> {
                    assertThat(envelope.getAccountingPointMarketDocument()).isNotNull();
                    assertThat(envelope.getAccountingPointMarketDocument()
                                       .getAccountingPointList()
                                       .getAccountingPoints()).hasSize(1);
                })
                .verifyComplete();
    }

    private static IdentifiableAccountingPointData samplePayload() {
        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid").connectionId("cid").meteringPointId("mp")
                .start(LocalDate.of(2026, 4, 30)).end(LocalDate.of(2026, 5, 1))
                .dataNeedId("dnid")
                .build();
        EtaPlusAccountingPointData payload = new EtaPlusAccountingPointData(
                "mp", "cust", "ELECTRICITY", "Consumption", null, null);
        return new IdentifiableAccountingPointData(pr, payload);
    }
}
