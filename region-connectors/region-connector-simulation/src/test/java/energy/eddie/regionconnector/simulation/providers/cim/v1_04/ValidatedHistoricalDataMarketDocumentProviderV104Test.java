// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_04;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
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
            StepVerifier.create(Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(), provider.getValidatedHistoricalDataV112Stream()))
                    .then(() -> streams.publish(reading))
                    .assertNext(pair -> {
                        var source = pair.getT1();
                        var target = pair.getT2();
                        assertEquals("pid", target.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
                        assertEquals("dnid", target.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
                        assertEquals(source.getMarketDocument().getMRID(), target.getVHDMarketDocument().getMRID());
                        var left = source.getMarketDocument().getTimeSeries().getFirst();
                        var right = target.getVHDMarketDocument().getTimeSeries().getFirst();
                        assertEquals(left.getMRID(), right.getMRID());
                        assertDoesNotThrow(() -> assertTrue(energy.eddie.cim.testing.XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(
                                new energy.eddie.cim.serde.XmlMessageSerde().serialize(target))));
                        assertEquals(left.getEnergyMeasurementUnitName(), right.getEnergyMeasurementUnitName());
                        assertEquals(left.getPeriods().getFirst().getTimeInterval().getStart(), right.getSeriesPeriods().getFirst().getTimeInterval().getStart());
                        assertEquals(left.getPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuantity(), right.getSeriesPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuantity());
                        assertEquals(left.getPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuality(), right.getSeriesPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuality());
                    }).thenCancel().verify();
        }
    }
}
