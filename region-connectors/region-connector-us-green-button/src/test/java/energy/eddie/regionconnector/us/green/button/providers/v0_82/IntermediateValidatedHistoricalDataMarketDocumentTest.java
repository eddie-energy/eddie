package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
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
        var vhd = res.getFirst().getValidatedHistoricalDataMarketDocument();
        assertAll(
                () -> assertNotNull(vhd.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), vhd.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT, vhd.getType()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   vhd.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER, vhd.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(CodingSchemeTypeList.CGM, vhd.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("company", vhd.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   vhd.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("client-id", vhd.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals("2024-09-03T00:00Z", vhd.getPeriodTimeInterval().getStart()),
                () -> assertEquals("2024-09-04T00:00Z", vhd.getPeriodTimeInterval().getEnd()),
                () -> assertFalse(vhd.getTimeSeriesList().getTimeSeries().isEmpty()),
                () -> {
                    var timeSeries = vhd.getTimeSeriesList().getTimeSeries().getFirst();
                    assertAll(
                            () -> assertNotNull(timeSeries.getMRID()),
                            () -> assertEquals(BusinessTypeList.PRODUCTION, timeSeries.getBusinessType()),
                            () -> assertEquals(EnergyProductTypeList.ACTIVE_POWER, timeSeries.getProduct()),
                            () -> assertEquals(DirectionTypeList.UP, timeSeries.getFlowDirectionDirection()),
                            () -> assertEquals(AggregateKind.SUM,
                                               timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                            () -> assertEquals(CommodityKind.ELECTRICITYSECONDARYMETERED,
                                               timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                            () -> assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR,
                                               timeSeries.getEnergyMeasurementUnitName()),
                            () -> assertEquals(CodingSchemeTypeList.CGM,
                                               timeSeries.getMarketEvaluationPointMRID().getCodingScheme()),
                            () -> assertEquals("1669851", timeSeries.getMarketEvaluationPointMRID().getValue()),
                            () -> assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED,
                                               timeSeries.getReasonList().getReasons().getFirst().getCode()),
                            () -> {
                                var seriesPeriod = timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst();
                                assertAll(
                                        () -> assertEquals("PT15M", seriesPeriod.getResolution()),
                                        () -> assertEquals("2024-09-03T00:00Z",
                                                           seriesPeriod.getTimeInterval().getStart()),
                                        () -> assertEquals("2024-09-03T00:15Z",
                                                           seriesPeriod.getTimeInterval().getEnd()),
                                        () -> assertEquals(String.valueOf(1725321600),
                                                           seriesPeriod.getPointList()
                                                                       .getPoints()
                                                                       .getFirst()
                                                                       .getPosition()),
                                        () -> assertEquals(BigDecimal.valueOf(10000).scaleByPowerOfTen(-8),
                                                           seriesPeriod.getPointList()
                                                                       .getPoints()
                                                                       .getFirst()
                                                                       .getEnergyQuantityQuantity()),
                                        () -> assertEquals(QualityTypeList.AS_PROVIDED,
                                                           seriesPeriod.getPointList()
                                                                       .getPoints()
                                                                       .getFirst()
                                                                       .getEnergyQuantityQuality())
                                );
                            }
                    );
                }
        );
    }
}