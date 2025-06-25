package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "fallback"
    );
    private final JsonResourceObjectMapper<List<MijnAansluitingResponse>> mapper = new JsonResourceObjectMapper<>(new TypeReference<>() {});
    private final MijnAansluitingConfiguration config = new MijnAansluitingConfiguration(
            "",
            "",
            new ClientID("client-id"),
            new Scope(),
            URI.create("http://localhost"), "jwt", null
    );
    private final MijnAansluitingPermissionRequest pr = new MijnAansluitingPermissionRequest("pid",
                                                                                             "cid",
                                                                                             "dnid",
                                                                                             PermissionProcessStatus.ACCEPTED,
                                                                                             "",
                                                                                             "",
                                                                                             null,
                                                                                             null,
                                                                                             null,
                                                                                             null, "11", "999AB");

    @Test
    @SuppressWarnings({"java:S5961"})
        // The CIM requires too many asserts
    void testToEddieValidatedHistoricalDataMarketDocuments_withValidObisCode() throws IOException {
        // Given
        var json = mapper.loadTestJson("single_consumption_data_multiple_meters.json");
        var identifiableMeteredData = new IdentifiableMeteredData(pr, json);
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, config, identifiableMeteredData);

        // When
        var res = doc.toEddieValidatedHistoricalDataMarketDocuments();

        // Then
        assertEquals(1, res.size());
        var eddieVHD = res.getFirst();
        var header = eddieVHD.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        assertAll(
                () -> assertEquals("cid", header.getConnectionid()),
                () -> assertEquals("pid", header.getPermissionid()),
                () -> assertEquals("dnid", header.getDataNeedid())
        );
        var vhd = eddieVHD.getValidatedHistoricalDataMarketDocument();
        assertAll(
                () -> assertNotNull(vhd.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), vhd.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT, vhd.getType()),
                () -> assertNotNull(vhd.getCreatedDateTime()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   vhd.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER, vhd.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.REALISED, vhd.getProcessProcessType()),
                () -> assertEquals(cimConfig.eligiblePartyNationalCodingScheme(),
                                   vhd.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(config.continuousClientId().getValue(),
                                   vhd.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME,
                                   vhd.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("EDSN", vhd.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(1, vhd.getTimeSeriesList().getTimeSeries().size())
        );
        var timeseries = vhd.getTimeSeriesList().getTimeSeries().getFirst();
        assertAll(
                () -> assertEquals("E0003000007083514", timeseries.getRegisteredResource().getMRID()),
                () -> assertEquals(BusinessTypeList.CONSUMPTION, timeseries.getBusinessType()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_POWER, timeseries.getProduct()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED,
                                   timeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME,
                                   timeseries.getMarketEvaluationPointMRID().getCodingScheme()),
                () -> assertEquals("871690930000909597", timeseries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(DirectionTypeList.DOWN, timeseries.getFlowDirectionDirection()),
                () -> assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR, timeseries.getEnergyMeasurementUnitName()),
                () -> assertEquals(1, timeseries.getSeriesPeriodList().getSeriesPeriods().size())
        );
        var seriesPeriod = timeseries.getSeriesPeriodList().getSeriesPeriods().getFirst();
        assertAll(
                () -> assertEquals("2023-04-30T22:00Z", seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals("2023-05-02T22:00Z", seriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("P1D", seriesPeriod.getResolution()),
                () -> assertEquals(3, seriesPeriod.getPointList().getPoints().size())
        );
        var points = seriesPeriod.getPointList().getPoints();
        var first = points.getFirst();
        assertAll(
                () -> assertEquals(BigDecimal.valueOf(100).setScale(2, RoundingMode.CEILING), first.getEnergyQuantityQuantity()),
                () -> assertEquals("2023-04-30T22:00Z", first.getPosition()),
                () -> assertEquals(QualityTypeList.AS_PROVIDED, first.getEnergyQuantityQuality())
        );
        var second = points.get(1);
        assertAll(
                () -> assertEquals(BigDecimal.valueOf(210.00).setScale(2, RoundingMode.CEILING), second.getEnergyQuantityQuantity()),
                () -> assertEquals("2023-05-01T22:00Z", second.getPosition()),
                () -> assertEquals(QualityTypeList.AS_PROVIDED, second.getEnergyQuantityQuality())
        );
        var third = points.get(2);
        assertAll(
                () -> assertEquals(BigDecimal.valueOf(110.00).setScale(2, RoundingMode.CEILING), third.getEnergyQuantityQuantity()),
                () -> assertEquals("2023-05-02T22:00Z", third.getPosition()),
                () -> assertEquals(QualityTypeList.AS_PROVIDED, third.getEnergyQuantityQuality())
        );
    }

    @Test
    void testToEddieValidatedHistoricalDataMarketDocuments_withGasConsumptionData() throws IOException {
        // Given
        var json = mapper.loadTestJson("gas_consumption_data.json");
        var identifiableMeteredData = new IdentifiableMeteredData(pr, json);
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, config, identifiableMeteredData);

        // When
        var res = doc.toEddieValidatedHistoricalDataMarketDocuments();

        // Then
        assertEquals(1, res.size());
        var vhd = res.getFirst().getValidatedHistoricalDataMarketDocument();
        assertEquals(1, vhd.getTimeSeriesList().getTimeSeries().size());
        var timeseries = vhd.getTimeSeriesList().getTimeSeries().getFirst();
        assertAll(
                () -> assertEquals("G0003000007083514", timeseries.getRegisteredResource().getMRID()),
                () -> assertEquals(BusinessTypeList.PRODUCTION, timeseries.getBusinessType()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_POWER, timeseries.getProduct()),
                () -> assertEquals(CommodityKind.NATURALGAS,
                                   timeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals("871690930000909597", timeseries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(DirectionTypeList.UP, timeseries.getFlowDirectionDirection())
        );
    }


    @Test
    void testToEddieValidatedHistoricalDataMarketDocuments_withInvalidObisCode() throws IOException {
        // Given
        var json = mapper.loadTestJson("invalid_obis_code_consumption_data.json");
        var identifiableMeteredData = new IdentifiableMeteredData(pr, json);
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, config, identifiableMeteredData);

        // When
        var res = doc.toEddieValidatedHistoricalDataMarketDocuments();

        // Then
        assertEquals(0, res.size());
    }
}