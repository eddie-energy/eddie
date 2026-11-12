// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EnergyPosition;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEnergy;
import energy.eddie.regionconnector.at.eda.dto.SimpleEnergyData;
import energy.eddie.regionconnector.at.eda.provider.HistoricalDataInput;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdaValidatedHistoricalDataMarketDocumentProviderTest {
    @Mock
    private IdentifiableStreams streams;
    @Mock
    private EdaAdapter adapter;
    @Mock
    private CommonInformationModelConfiguration legacyConfig;
    @Mock
    private ObjectProvider<UnaryOperator<IdentifiableConsumptionRecord>> filters;

    @Test
    void mapsPopulatedRawFixtureWithoutLegacyProvider() {
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "ep-id");
        var consumptionRecord = rawRecord();
        Sinks.Many<IdentifiableConsumptionRecord> records = Sinks.many().unicast().onBackpressureBuffer();
        when(streams.historicalDataInputs()).thenReturn(records.asFlux().map(HistoricalDataInput::new).share());

        var providerV112 = new EdaValidatedHistoricalDataMarketDocumentProvider(cimConfig, streams);

        StepVerifier.create(providerV112.getValidatedHistoricalDataV112Stream())
                    .then(() -> assertEquals(Sinks.EmitResult.OK, records.tryEmitNext(consumptionRecord)))
                    .assertNext(EdaValidatedHistoricalDataMarketDocumentProviderTest::assertPopulatedDocument)
                    .then(() -> assertEquals(Sinks.EmitResult.OK, records.tryEmitComplete()))
                    .verifyComplete();
    }

    @Test
    void sourcePreparationRunsOnceAcrossVersionsMappingFailureAndCancellation() {
        var subscriptions = new AtomicInteger();
        var filtered = new AtomicInteger();
        var completed = new AtomicInteger();
        Sinks.Many<IdentifiableConsumptionRecord> records = Sinks.many().unicast().onBackpressureBuffer();
        when(adapter.getConsumptionRecordStream()).thenReturn(records.asFlux().doOnSubscribe(ignored -> subscriptions.incrementAndGet()));
        when(filters.getIfAvailable(any())).thenReturn(record -> {
            filtered.incrementAndGet();
            return record;
        });
        var inputs = new IdentifiableStreams(filters, adapter);
        var config = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "ep-id");
        when(legacyConfig.eligiblePartyNationalCodingScheme())
                .thenReturn(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                .thenThrow(new IllegalArgumentException("invalid legacy configuration"))
                .thenReturn(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME);
        var legacy = new energy.eddie.regionconnector.at.eda.provider.v1_04.EdaValidatedHistoricalDataMarketDocumentProvider(legacyConfig, inputs);
        var direct = new EdaValidatedHistoricalDataMarketDocumentProvider(config, inputs);
        var source = new ArrayList<VHDEnvelope>();
        var target = new ArrayList<Envelope>();
        var sourceSubscription = legacy.getValidatedHistoricalDataMarketDocumentsStream().subscribe(source::add);
        var targetSubscription = direct.getValidatedHistoricalDataV112Stream().subscribe(target::add,
                error -> fail(error), completed::incrementAndGet);
        try {
            assertEquals(Sinks.EmitResult.OK, records.tryEmitNext(rawRecord()));
            assertEquals(1, subscriptions.get());
            assertEquals(1, filtered.get());
            assertEquals(1, source.size());
            assertEquals(1, target.size());
            assertEquals(source.getFirst().getMarketDocument().getMRID(), target.getFirst().getVHDMarketDocument().getMRID());
            assertEquals(Sinks.EmitResult.OK, records.tryEmitNext(rawRecord()));
            assertEquals(1, subscriptions.get());
            assertEquals(2, filtered.get());
            assertEquals(1, source.size());
            assertEquals(2, target.size());
            assertEquals(Sinks.EmitResult.OK, records.tryEmitNext(rawRecord()));
            assertEquals(1, subscriptions.get());
            assertEquals(3, filtered.get());
            assertEquals(2, source.size());
            assertEquals(3, target.size());
            sourceSubscription.dispose();
            assertEquals(Sinks.EmitResult.OK, records.tryEmitNext(rawRecord()));
            assertEquals(1, subscriptions.get());
            assertEquals(4, filtered.get());
            assertEquals(2, source.size());
            assertEquals(4, target.size());
            assertEquals(Sinks.EmitResult.OK, records.tryEmitComplete());
            assertEquals(1, completed.get());
        } finally {
            sourceSubscription.dispose();
            targetSubscription.dispose();
        }
    }

    private static IdentifiableConsumptionRecord rawRecord() {
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 3);
        var created = ZonedDateTime.of(2025, 1, 4, 10, 0, 0, 0, AT_ZONE_ID);
        var processDate = created.plusDays(1);
        var meterCode = "1-1:1.9.0 P.01";
        var meteringPoint = "AT0030000000000000000000000123456";
        var consumptionRecord = new IdentifiableConsumptionRecord(
                new SimpleEdaConsumptionRecord()
                        .setMessageId("message-id")
                        .setConversationId("conversation-id")
                        .setMeteringPoint(meteringPoint)
                        .setStartDate(start)
                        .setEndDate(end)
                        .setSenderMessageAddress("eda")
                        .setReceiverMessageAddress("eddie")
                        .setDocumentCreationDateTime(created)
                        .setProcessDate(processDate)
                        .setSchemaVersion("01.41")
                        .setEnergy(List.of(
                                new SimpleEnergy()
                                        .setGranularity(Granularity.P1D)
                                        .setMeterReadingStart(start.atStartOfDay(AT_ZONE_ID))
                                        .setMeterReadingEnd(end.atStartOfDay(AT_ZONE_ID))
                                        .setMeteringReason("01")
                                        .setEnergyData(List.of(
                                                new SimpleEnergyData()
                                                        .setMeterCode(meterCode)
                                                        .setBillingUnit("KWH")
                                                        .setEnergyPositions(List.of(
                                                                new EnergyPosition(new BigDecimal("12.5"), "L1"),
                                                                new EnergyPosition(new BigDecimal("7.25"), "L3")
                                                        ))
                                        ))
                        )),
                List.of(new SimplePermissionRequest("pid", "cid", "dnid")),
                start,
                end
        );
        return consumptionRecord;
    }

    private static void assertPopulatedDocument(Envelope envelope) {
        assertEquals("pid", envelope.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
        assertEquals("dnid", envelope.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
        var document = envelope.getVHDMarketDocument();
        assertNotNull(document);
        assertFalse(document.getTimeSeries().isEmpty());
        var pointCount = 0;
        for (var series : document.getTimeSeries()) {
            assertNotNull(series.getDateAndOrTimeDateTime());
            assertFalse(series.getSeriesPeriods().isEmpty());
            for (var period : series.getSeriesPeriods()) {
                assertNotNull(period.getResolution());
                assertNotNull(period.getTimeInterval());
                assertFalse(period.getPoints().isEmpty());
                for (var point : period.getPoints()) {
                    assertTrue(point.getPosition() > 0);
                    assertTrue(point.getEnergyQuantityQuantity() != null || point.getEnergyQualityQuantityQuantity() != null);
                    pointCount++;
                }
            }
        }
        assertTrue(pointCount > 0);
    }
}
