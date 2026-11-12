// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_12.StandardDirectionTypeList;
import energy.eddie.cim.v1_12.StandardQualityTypeList;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();
    private final PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "epID"
    );

    IntermediateValidatedHistoricalDataMarketDocumentTest() throws SerdeInitializationException {}

    @Test
    void toVhd_mapsConsumptionRecord() {
        // Given
        var consumptionRecord = EdaValidatedHistoricalDataMarketDocumentProviderV112Test.consumptionRecord(
                List.of(new SimplePermissionRequest("pid", "cid", "dnid"))
        );
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord);

        // When
        var res = doc.toVhd();

        // Then
        assertEquals(1, res.size());
        var envelope = res.getFirst();
        var meta = envelope.getMessageDocumentHeader().getMetaInformation();
        var vhd = envelope.getVHDMarketDocument();
        var consumption = vhd.getTimeSeries().getFirst();
        var production = vhd.getTimeSeries().get(1);
        assertAll(
                () -> assertEquals("pid", meta.getRequestPermissionId()),
                () -> assertEquals("AT", meta.getRegionCountry()),
                () -> assertEquals("messageId", vhd.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V1_12.cimify(), vhd.getRevisionNumber()),
                () -> assertEquals(StandardDirectionTypeList.DOWN.value(), consumption.getFlowDirectionDirection()),
                () -> assertEquals(StandardDirectionTypeList.UP.value(), production.getFlowDirectionDirection()),
                () -> assertEquals("AT0030000000000000000000000123456", consumption.getAccountingPointMRID().getValue()),
                () -> assertEquals(3, consumption.getSeriesPeriods().getFirst().getPoints().size()),
                () -> assertEquals(StandardQualityTypeList.AS_PROVIDED.value(),
                                   consumption.getSeriesPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuality()),
                () -> assertNull(consumption.getSeriesPeriods().getFirst().getPoints().get(2).getEnergyQuantityQuality()),
                () -> assertNull(production.getEnergyMeasurementUnitName())
        );
    }

    @Test
    void toVhd_producesValidXml() throws SerializationException {
        // Given
        var consumptionRecord = EdaValidatedHistoricalDataMarketDocumentProviderV112Test.consumptionRecord(
                List.of(new SimplePermissionRequest("pid", "cid", "dnid"))
        );
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord);
        // Only use the first time series, since the second one contains an unknown unit on purpose
        var envelope = doc.toVhd().getFirst();
        envelope.getVHDMarketDocument().getTimeSeries().removeLast();
        envelope.getVHDMarketDocument()
                .getTimeSeries()
                .getFirst()
                .getSeriesPeriods()
                .getFirst()
                .getPoints()
                .removeLast();

        // When
        var xml = serde.serialize(envelope);

        // Then
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                   "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
    }
}
