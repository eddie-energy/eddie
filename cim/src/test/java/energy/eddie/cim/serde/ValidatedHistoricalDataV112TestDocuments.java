// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import energy.eddie.cim.v1_12.StandardCodingSchemeTypeList;
import energy.eddie.cim.v1_12.StandardDirectionTypeList;
import energy.eddie.cim.v1_12.StandardQualityTypeList;
import energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList;
import energy.eddie.cim.v1_12.vhd.*;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Builds a fully populated CIM v1.12 validated historical data envelope for serialization tests.
 */
final class ValidatedHistoricalDataV112TestDocuments {
    static final ZonedDateTime DATE_TIME = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private ValidatedHistoricalDataV112TestDocuments() {
        // Utility Class
    }

    static Envelope envelope() {
        return new Envelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeader()
                                .withCreationDateTime(DATE_TIME)
                                .withMetaInformation(
                                        new MetaInformation()
                                                .withConnectionId("cid")
                                                .withRequestPermissionId("pid")
                                                .withDataNeedId("dnid")
                                                .withDocumentType("validated-historical-data-market-document")
                                                .withRegionConnector("at-eda")
                                                .withRegionCountry("AT")
                                )
                )
                .withVHDMarketDocument(
                        new VHDMarketDocument()
                                .withMRID("mrid")
                                .withCreatedDateTime(DATE_TIME)
                                .withRevisionNumber("112")
                                .withProcessProcessType("A16")
                                .withSenderMarketParticipantMRID(
                                        new PartyIDString()
                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                .withValue("AT003000")
                                )
                                .withSenderMarketParticipantMarketRoleType("A26")
                                .withReceiverMarketParticipantMRID(
                                        new PartyIDString()
                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                .withValue("EP100307")
                                )
                                .withReceiverMarketParticipantMarketRoleType("A13")
                                .withTimeSeries(
                                        new TimeSeries()
                                                .withVersion("1")
                                                .withFlowDirectionDirection(StandardDirectionTypeList.DOWN.value())
                                                .withDateAndOrTimeDateTime(DATE_TIME)
                                                .withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                                                .withAccountingPointMRID(
                                                        new MeasurementPointIDString()
                                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                                .withValue("AT0030000000000000000000030107193")
                                                )
                                                .withSeriesPeriods(
                                                        new SeriesPeriod()
                                                                .withResolution(DatatypeFactory.newDefaultInstance().newDuration("PT15M"))
                                                                .withTimeInterval(
                                                                        new ESMPDateTimeInterval()
                                                                                .withStart("2024-01-01T00:00Z")
                                                                                .withEnd("2024-01-01T00:15Z")
                                                                )
                                                                .withPoints(
                                                                        new Point()
                                                                                .withPosition(1)
                                                                                .withEnergyQuantityQuantity(BigDecimal.valueOf(1.5))
                                                                                .withEnergyQuantityQuality(StandardQualityTypeList.AS_PROVIDED.value())
                                                                )
                                                )
                                )
                );
    }
}
