// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EnergyPosition;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEnergy;
import energy.eddie.regionconnector.at.eda.dto.SimpleEnergyData;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_returnsDocuments_alsoEmitsEquivalentV112() {
        // Given
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "ep-id");
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
        Sinks.Many<IdentifiableConsumptionRecord> records = Sinks.many().unicast().onBackpressureBuffer();
        when(streams.consumptionRecordStream()).thenReturn(records.asFlux());
        var provider = new EdaValidatedHistoricalDataMarketDocumentProvider(cimConfig, streams);
        var pairedDocuments = Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(),
                                       provider.getValidatedHistoricalDataV112Stream());

        // When / Then: both versions subscribe before the single provider record is emitted.
        StepVerifier.create(pairedDocuments)
                    .then(() -> assertEquals(Sinks.EmitResult.OK, records.tryEmitNext(consumptionRecord)))
                    .assertNext(pair -> {
                        var source = pair.getT1();
                        var target = pair.getT2();
                        var metadata = target.getMessageDocumentHeader().getMetaInformation();
                        assertEquals("pid", source.getMessageDocumentHeaderMetaInformationPermissionId());
                        assertEquals("pid", metadata.getRequestPermissionId());
                        assertEquals("dnid", source.getMessageDocumentHeaderMetaInformationDataNeedId());
                        assertEquals("dnid", metadata.getDataNeedId());
                        assertEquals("cid", metadata.getConnectionId());
                        assertEquals(source.getMessageDocumentHeaderCreationDateTime(),
                                     target.getMessageDocumentHeader().getCreationDateTime());
                        assertEquals("message-id", source.getMarketDocument().getMRID());
                        assertEquals(source.getMarketDocument().getMRID(), target.getVHDMarketDocument().getMRID());
                        assertEquals(created, source.getMarketDocument().getCreatedDateTime());
                        assertEquals(created, target.getVHDMarketDocument().getCreatedDateTime());

                        assertEquals(1, source.getMarketDocument().getTimeSeries().size());
                        assertEquals(1, target.getVHDMarketDocument().getTimeSeries().size());
                        var sourceSeries = source.getMarketDocument().getTimeSeries().getFirst();
                        var targetSeries = target.getVHDMarketDocument().getTimeSeries().getFirst();
                        assertEquals(processDate, sourceSeries.getDateAndOrTimeDateTime());
                        assertEquals(processDate, targetSeries.getDateAndOrTimeDateTime());
                        assertEquals("KWH", targetSeries.getEnergyMeasurementUnitName());
                        assertEquals(sourceSeries.getEnergyMeasurementUnitName(), targetSeries.getEnergyMeasurementUnitName());
                        assertEquals("C62", targetSeries.getEnergyQualityMeasurementUnitName());
                        assertEquals(sourceSeries.getEnergyQualityMeasurementUnitName(), targetSeries.getEnergyQualityMeasurementUnitName());
                        assertEquals("A02", targetSeries.getFlowDirectionDirection());
                        assertEquals(sourceSeries.getFlowDirectionDirection(), targetSeries.getFlowDirectionDirection());
                        assertEquals(meterCode, sourceSeries.getRegisteredResourceMRID().getValue());
                        assertEquals(meterCode, targetSeries.getRegisteredResourceMRID().getValue());
                        assertEquals(sourceSeries.getRegisteredResourceMRID().getCodingScheme(),
                                     targetSeries.getRegisteredResourceMRID().getCodingScheme());
                        assertEquals(meteringPoint, sourceSeries.getMarketEvaluationPointMRID().getValue());
                        assertEquals(meteringPoint, targetSeries.getAccountingPointMRID().getValue());
                        assertEquals(sourceSeries.getMarketEvaluationPointMRID().getCodingScheme(),
                                     targetSeries.getAccountingPointMRID().getCodingScheme());

                        assertEquals(1, sourceSeries.getPeriods().size());
                        assertEquals(1, targetSeries.getSeriesPeriods().size());
                        var sourcePeriod = sourceSeries.getPeriods().getFirst();
                        var targetPeriod = targetSeries.getSeriesPeriods().getFirst();
                        assertEquals("P0Y0M1DT0H0M0.000S", targetPeriod.getResolution().toString());
                        assertEquals(sourcePeriod.getResolution(), targetPeriod.getResolution());
                        assertEquals("2024-12-31T23:00Z", targetPeriod.getTimeInterval().getStart());
                        assertEquals("2025-01-02T23:00Z", targetPeriod.getTimeInterval().getEnd());
                        assertEquals(sourcePeriod.getTimeInterval().getStart(), targetPeriod.getTimeInterval().getStart());
                        assertEquals(sourcePeriod.getTimeInterval().getEnd(), targetPeriod.getTimeInterval().getEnd());
                        assertEquals(2, sourcePeriod.getPoints().size());
                        assertEquals(2, targetPeriod.getPoints().size());
                        assertEquals(List.of(1, 2), targetPeriod.getPoints().stream().map(point -> point.getPosition()).toList());
                        assertEquals(List.of(new BigDecimal("12.5"), new BigDecimal("7.25")),
                                     targetPeriod.getPoints().stream().map(point -> point.getEnergyQuantityQuantity()).toList());
                        assertEquals(sourcePeriod.getPoints().stream().map(point -> point.getEnergyQuantityQuantity()).toList(),
                                     targetPeriod.getPoints().stream().map(point -> point.getEnergyQuantityQuantity()).toList());
                        assertEquals(List.of("A04", "A03"),
                                     targetPeriod.getPoints().stream().map(point -> point.getEnergyQuantityQuality()).toList());
                        assertEquals(sourcePeriod.getPoints().stream().map(point -> point.getEnergyQuantityQuality()).toList(),
                                     targetPeriod.getPoints().stream().map(point -> point.getEnergyQuantityQuality()).toList());
                        var xml = assertDoesNotThrow(() -> new XmlMessageSerde().serialize(target));
                        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml));
                    })
                    .then(() -> assertEquals(Sinks.EmitResult.OK, records.tryEmitComplete()))
                    .verifyComplete();
    }
}
