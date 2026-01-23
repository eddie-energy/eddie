// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_04;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdaValidatedHistoricalDataMarketDocumentProviderTest {
    @Mock
    private IdentifiableStreams streams;

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_returnsDocuments() {
        // Given
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "ep-id");
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);
        when(streams.consumptionRecordStream())
                .thenReturn(Flux.just(
                        new IdentifiableConsumptionRecord(
                                new SimpleEdaConsumptionRecord()
                                        .setStartDate(start)
                                        .setEndDate(end)
                                        .setEnergy(List.of()),
                                List.of(new SimplePermissionRequest("pid", "cid", "dnid")),
                                start,
                                end
                        )
                ));
        var provider = new EdaValidatedHistoricalDataMarketDocumentProvider(cimConfig, streams);

        // When
        var res = provider.getValidatedHistoricalDataMarketDocumentsStream();

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}