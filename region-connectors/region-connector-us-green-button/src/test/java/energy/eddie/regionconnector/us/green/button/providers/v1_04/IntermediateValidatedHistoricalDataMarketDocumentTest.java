package energy.eddie.regionconnector.us.green.button.providers.v1_04;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.providers.UnsupportedUnitException;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "id"
    );
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "http://localhost",
            Map.of("company", "client-id"),
            Map.of("company", "client-secret"),
            Map.of("company", "token"),
            "http://localhost",
            "secret"
    );

    @Test
    @SuppressWarnings("java:S5961")
    void testToVhd_returnsValidatedHistoricalDataMarketDocument() throws FeedException, UnsupportedUnitException {
        // Given
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var permissionRequest = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                                         .setConnectionId("cid")
                                                                         .setDataNeedId("dnid")
                                                                         .setCountryCode("US")
                                                                         .setCompanyId("company")
                                                                         .build();
        var intermediateVhd = new IntermediateValidatedHistoricalDataMarketDocument(
                new IdentifiableSyndFeed(permissionRequest, feed),
                marshaller,
                cimConfig,
                config
        );

        // When
        var res = intermediateVhd.toVhd();

        // Then
        assertEquals(1, res.size());
        var vhd = res.getFirst().getMarketDocument();
        assertAll(
                () -> assertNotNull(vhd.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V1_04.version(), vhd.getRevisionNumber()),
                () -> assertEquals(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value(), vhd.getType()),
                () -> assertEquals(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value(),
                                   vhd.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(StandardRoleTypeList.CONSUMER.value(), vhd.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(CodingSchemeTypeList.CGM.value(), vhd.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("company", vhd.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value(),
                                   vhd.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("client-id", vhd.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals("2024-09-03T00:00Z", vhd.getPeriodTimeInterval().getStart()),
                () -> assertEquals("2024-09-04T00:00Z", vhd.getPeriodTimeInterval().getEnd()),
                () -> assertFalse(vhd.getTimeSeries().isEmpty()),
                () -> {
                    var timeSeries = vhd.getTimeSeries().getFirst();
                    assertAll(
                            () -> assertNotNull(timeSeries.getMRID()),
                            () -> assertEquals(StandardBusinessTypeList.PRODUCTION.value(), timeSeries.getBusinessType()),
                            () -> assertEquals(StandardEnergyProductTypeList.ACTIVE_POWER.value(), timeSeries.getProduct()),
                            () -> assertEquals(StandardDirectionTypeList.UP.value(), timeSeries.getFlowDirectionDirection()),
                            () -> assertEquals(AggregateKind.SUM,
                                               timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate()),
                            () -> assertEquals(CommodityKind.ELECTRICITYSECONDARYMETERED,
                                               timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                            () -> assertEquals(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value(),
                                               timeSeries.getEnergyMeasurementUnitName()),
                            () -> assertEquals(CodingSchemeTypeList.CGM.value(),
                                               timeSeries.getMarketEvaluationPointMRID().getCodingScheme()),
                            () -> assertEquals("1669851", timeSeries.getMarketEvaluationPointMRID().getValue()),
                            () -> assertEquals(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value(),
                                               timeSeries.getReasonCode()),
                            () -> {
                                var seriesPeriod = timeSeries.getPeriods().getFirst();
                                assertAll(
                                        () -> assertEquals("2024-09-03T00:00Z",
                                                           seriesPeriod.getTimeInterval().getStart()),
                                        () -> assertEquals("2024-09-03T00:15Z",
                                                           seriesPeriod.getTimeInterval().getEnd()),
                                        () -> assertEquals(1725321600,
                                                           seriesPeriod.getPoints()
                                                                       .getFirst()
                                                                       .getPosition()),
                                        () -> assertEquals(BigDecimal.valueOf(10000).scaleByPowerOfTen(-8),
                                                           seriesPeriod.getPoints()
                                                                       .getFirst()
                                                                       .getEnergyQuantityQuantity()),
                                        () -> assertEquals(StandardMessageTypeList.SYSTEM_OPERATOR_AREA_SCHEDULE.value(),
                                                           seriesPeriod.getPoints()
                                                                       .getFirst()
                                                                       .getEnergyQuantityQuality())
                                );
                            }
                    );
                }
        );
    }
}