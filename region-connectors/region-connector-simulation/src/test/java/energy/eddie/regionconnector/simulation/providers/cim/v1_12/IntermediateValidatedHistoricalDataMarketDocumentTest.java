// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_12;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();
    private final PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "EP-ID");

    IntermediateValidatedHistoricalDataMarketDocumentTest() throws SerdeInitializationException {}

    @Test
    void givenSimulatedMeterReading_whenValue_returnsValidEnvelope() throws SerializationException {
        // Given
        var intermediate = new IntermediateValidatedHistoricalDataMarketDocument(simulatedMeterReading(), cimConfig);

        // When
        var res = intermediate.value();

        // Then
        var vhd = res.getVHDMarketDocument();
        var timeSeries = vhd.getTimeSeries().getFirst();
        assertAll(
                () -> assertEquals("pid", res.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId()),
                () -> assertEquals(CommonInformationModelVersions.V1_12.cimify(), vhd.getRevisionNumber()),
                () -> assertEquals("mid", timeSeries.getAccountingPointMRID().getValue()),
                () -> assertEquals(vhd.getCreatedDateTime(), timeSeries.getDateAndOrTimeDateTime()),
                () -> assertEquals(2, timeSeries.getSeriesPeriods().getFirst().getPoints().size())
        );
        var bytes = serde.serialize(res);
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(bytes),
                   "Failed to validate XML, see:\n" + new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void givenSimulatedMeterReading_whenValue_isEquivalentToV104() {
        // Given
        var reading = simulatedMeterReading();
        var v104 = new energy.eddie.regionconnector.simulation.providers.cim.v1_04.IntermediateValidatedHistoricalDataMarketDocument(
                reading, cimConfig
        ).value();

        // When
        var v112 = new IntermediateValidatedHistoricalDataMarketDocument(reading, cimConfig).value();

        // Then
        // The creation time is taken from the clock at mapping time, so it differs between versions
        assertEquals(List.of(),
                     ValidatedHistoricalDataVersionComparator.differencesIgnoring(v104, v112, "createdDateTime"));
    }

    private static SimulatedMeterReading simulatedMeterReading() {
        var now = ZonedDateTime.of(2025, 11, 6, 0, 0, 0, 0, ZoneOffset.UTC);
        return new SimulatedMeterReading("cid",
                                         "dnid",
                                         "pid",
                                         "mid",
                                         now,
                                         PT15M.toString(),
                                         List.of(new Measurement(10.0, Measurement.MeasurementType.MEASURED),
                                                 new Measurement(5.0, Measurement.MeasurementType.EXTRAPOLATED)));
    }
}
