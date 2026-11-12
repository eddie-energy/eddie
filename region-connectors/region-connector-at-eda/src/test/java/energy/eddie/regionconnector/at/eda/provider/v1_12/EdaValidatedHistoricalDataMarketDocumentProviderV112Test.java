// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import energy.eddie.regionconnector.at.eda.provider.v1_04.EdaValidatedHistoricalDataMarketDocumentProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdaValidatedHistoricalDataMarketDocumentProviderV112Test {
    private final PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "ep-id"
    );
    @Mock
    private IdentifiableStreams streams;

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_returnsDocuments() {
        // Given
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
        var provider = new EdaValidatedHistoricalDataMarketDocumentProviderV112(cimConfig, streams);

        // When
        var res = provider.getValidatedHistoricalDataMarketDocumentsStream();

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_isEquivalentToV104() {
        // Given
        var consumptionRecord = consumptionRecord(List.of(
                new SimplePermissionRequest("pid", "cid", "dnid"),
                new SimplePermissionRequest("pid2", "cid2", "dnid2")
        ));
        when(streams.consumptionRecordStream()).thenReturn(Flux.just(consumptionRecord));
        var v104Provider = new EdaValidatedHistoricalDataMarketDocumentProvider(cimConfig, streams);
        var v112Provider = new EdaValidatedHistoricalDataMarketDocumentProviderV112(cimConfig, streams);

        // When
        List<VHDEnvelope> v104 = v104Provider.getValidatedHistoricalDataMarketDocumentsStream().collectList().block();
        List<Envelope> v112 = v112Provider.getValidatedHistoricalDataMarketDocumentsStream().collectList().block();

        // Then
        assertNotNull(v104);
        assertNotNull(v112);
        assertEquals(2, v104.size());
        assertEquals(2, v112.size());
        for (int i = 0; i < v104.size(); i++) {
            assertEquals(List.of(), ValidatedHistoricalDataVersionComparator.differences(v104.get(i), v112.get(i)));
        }
        // State isolation: every envelope has its own market document
        assertNotSame(v112.get(0).getVHDMarketDocument(), v112.get(1).getVHDMarketDocument());
    }

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_withoutEnergy_isEquivalentToV104() {
        // Given
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);
        var consumptionRecord = new IdentifiableConsumptionRecord(
                new SimpleEdaConsumptionRecord()
                        .setMessageId("messageId")
                        .setStartDate(start)
                        .setEndDate(end)
                        .setDocumentCreationDateTime(ZonedDateTime.of(2025, 1, 3, 0, 0, 0, 0, AT_ZONE_ID))
                        .setEnergy(List.of()),
                List.of(new SimplePermissionRequest("pid", "cid", "dnid")),
                start,
                end
        );
        when(streams.consumptionRecordStream()).thenReturn(Flux.just(consumptionRecord));
        var v104 = new EdaValidatedHistoricalDataMarketDocumentProvider(cimConfig, streams)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .blockFirst();
        var v112 = new EdaValidatedHistoricalDataMarketDocumentProviderV112(cimConfig, streams)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .blockFirst();

        // When
        var res = ValidatedHistoricalDataVersionComparator.differences(v104, v112);

        // Then
        assertEquals(List.of(), res);
    }

    static IdentifiableConsumptionRecord consumptionRecord(List<AtPermissionRequest> permissionRequests) {
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);
        var created = ZonedDateTime.of(2025, 1, 3, 0, 0, 0, 0, AT_ZONE_ID);
        var simpleRecord = new SimpleEdaConsumptionRecord()
                .setMessageId("messageId")
                .setConversationId("conversationId")
                .setMeteringPoint("AT0030000000000000000000000123456")
                .setStartDate(start)
                .setEndDate(end)
                .setSenderMessageAddress("eda")
                .setReceiverMessageAddress("eddie")
                .setDocumentCreationDateTime(created)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(Granularity.PT15M)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setEnergyPositions(List.of(
                                                        new EnergyPosition(BigDecimal.ONE, "L1"),
                                                        new EnergyPosition(BigDecimal.TEN, "L2"),
                                                        new EnergyPosition(BigDecimal.TWO, "unknown")
                                                ))
                                                .setMeterCode("1-1:1.9.0 P.01")
                                                .setBillingUnit("KWH"),
                                        new SimpleEnergyData()
                                                .setEnergyPositions(List.of(
                                                        new EnergyPosition(BigDecimal.ONE, "L3")
                                                ))
                                                .setMeterCode("1-1:2.9.0 P.01")
                                                .setBillingUnit("unknown-unit")
                                ))
                                .setMeterReadingStart(start.atStartOfDay(AT_ZONE_ID))
                                .setMeterReadingEnd(endOfDay(end, AT_ZONE_ID))
                                .setMeteringReason("reason")
                ))
                .setSchemaVersion("version")
                .setProcessDate(created);
        return new IdentifiableConsumptionRecord(simpleRecord, List.copyOf(permissionRequests), start, end);
    }
}
