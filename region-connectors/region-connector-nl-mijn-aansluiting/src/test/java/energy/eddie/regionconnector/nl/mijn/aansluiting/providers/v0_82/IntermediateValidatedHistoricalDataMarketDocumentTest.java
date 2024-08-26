package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
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
                                                                                         null);
    private final NlPermissionRequest pr = new MijnAansluitingPermissionRequest("pid",
                                                                                "cid",
                                                                                "dnid",
                                                                                PermissionProcessStatus.ACCEPTED,
                                                                                "",
                                                                                "",
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null);

    @Test
    @SuppressWarnings({"java:S5961"})
        // The CIM requires too many asserts
    void testToEddieValidatedHistoricalDataMarketDocuments_withValidObisCode() throws IOException {
        // Given
        var json = mapper.loadTestJson("single_consumption_data.json");
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
                () -> assertEquals("871690930000909597", vhd.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), vhd.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT, vhd.getType()),
                () -> assertNotNull(vhd.getCreatedDateTime()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   vhd.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER, vhd.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.REALISED, vhd.getProcessProcessType()),
                () -> assertEquals(cimConfig.eligiblePartyNationalCodingScheme(),
                                   vhd.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(config.clientId().getValue(), vhd.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME,
                                   vhd.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("Stichting Mijn Aansluiting", vhd.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(1, vhd.getTimeSeriesList().getTimeSeries().size())
        );
        var timeseries = vhd.getTimeSeriesList().getTimeSeries().getFirst();
        assertAll(
                () -> assertEquals("1.8.1", timeseries.getMRID()),
                () -> assertEquals(BusinessTypeList.AGGREGATED_ENERGY_DATA, timeseries.getBusinessType()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_ENERGY, timeseries.getProduct()),
                () -> assertEquals(AccumulationKind.DELTADATA,
                                   timeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulate()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED,
                                   timeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME,
                                   timeseries.getMarketEvaluationPointMRID().getCodingScheme()),
                () -> assertEquals("E0003000007083514", timeseries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED,
                                   timeseries.getReasonList().getReasons().getFirst().getCode()),
                () -> assertEquals(DirectionTypeList.UP, timeseries.getFlowDirectionDirection()),
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
        assertAll(
                () -> assertEquals(BigDecimal.valueOf(9738.65), points.getFirst().getEnergyQuantityQuantity()),
                () -> assertEquals("0", points.getFirst().getPosition()),
                () -> assertEquals(QualityTypeList.ADJUSTED, points.getFirst().getEnergyQuantityQuality())
        );
        assertAll(
                () -> assertEquals(BigDecimal.valueOf(9838.65), points.get(1).getEnergyQuantityQuantity()),
                () -> assertEquals("1", points.get(1).getPosition()),
                () -> assertEquals(QualityTypeList.ADJUSTED, points.get(1).getEnergyQuantityQuality())
        );
        assertAll(
                () -> assertEquals(BigDecimal.valueOf(9948.65), points.get(2).getEnergyQuantityQuantity()),
                () -> assertEquals("2", points.get(2).getPosition()),
                () -> assertEquals(QualityTypeList.ADJUSTED, points.get(2).getEnergyQuantityQuality())
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
                () -> assertEquals("2.8.1", timeseries.getMRID()),
                () -> assertEquals(BusinessTypeList.CONSUMPTION, timeseries.getBusinessType()),
                () -> assertNull(timeseries.getProduct()),
                () -> assertEquals(CommodityKind.NATURALGAS,
                                   timeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals("G0003000007083514", timeseries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(DirectionTypeList.DOWN, timeseries.getFlowDirectionDirection())
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