// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_04;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidatedHistoricalDataMarketDocumentProviderV104Test {
    @Test
    void sameSimulatedInputProducesBothVersionsWithSamePermissionAndMeasurements() {
        var config = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "ep");
        try (var streams = new DocumentStreams(config, new ObjectMapper())) {
            var provider = new ValidatedHistoricalDataMarketDocumentProviderV104(streams, config);
            var reading = new SimulatedMeterReading("cid", "dnid", "pid", "mid", ZonedDateTime.parse("2026-01-01T00:00Z"),
                    "PT15M", List.of(new Measurement(10.25, Measurement.MeasurementType.MEASURED)));
            var pairedDocuments = Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(),
                                           provider.getValidatedHistoricalDataV112Stream());
            StepVerifier.create(pairedDocuments)
                    .then(() -> streams.publish(reading))
                    .assertNext(pair -> {
                        var source = pair.getT1();
                        var target = pair.getT2();
                        assertEquals("pid", target.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
                        assertEquals("dnid", target.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
                        assertEquals(source.getMarketDocument().getMRID(), target.getVHDMarketDocument().getMRID());
                        assertEquals(1, source.getMarketDocument().getTimeSeries().size());
                        assertEquals(1, target.getVHDMarketDocument().getTimeSeries().size());
                        var sourceSeries = source.getMarketDocument().getTimeSeries().getFirst();
                        var targetSeries = target.getVHDMarketDocument().getTimeSeries().getFirst();
                        assertEquals(sourceSeries.getMRID(), targetSeries.getMRID());
                        var xml = assertDoesNotThrow(() -> new XmlMessageSerde().serialize(target));
                        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml));
                        assertEquals(1, sourceSeries.getPeriods().size());
                        assertEquals(1, targetSeries.getSeriesPeriods().size());
                        var sourcePeriod = sourceSeries.getPeriods().getFirst();
                        var targetPeriod = targetSeries.getSeriesPeriods().getFirst();
                        assertEquals(1, sourcePeriod.getPoints().size());
                        assertEquals(1, targetPeriod.getPoints().size());
                        var sourcePoint = sourcePeriod.getPoints().getFirst();
                        var targetPoint = targetPeriod.getPoints().getFirst();
                        assertEquals(sourceSeries.getEnergyMeasurementUnitName(), targetSeries.getEnergyMeasurementUnitName());
                        assertEquals(sourcePeriod.getTimeInterval().getStart(), targetPeriod.getTimeInterval().getStart());
                        assertEquals(sourcePoint.getEnergyQuantityQuantity(), targetPoint.getEnergyQuantityQuantity());
                        assertEquals(sourcePoint.getEnergyQuantityQuality(), targetPoint.getEnergyQuantityQuality());
                    })
                    .thenCancel()
                    .verify();
        }
    }
}
