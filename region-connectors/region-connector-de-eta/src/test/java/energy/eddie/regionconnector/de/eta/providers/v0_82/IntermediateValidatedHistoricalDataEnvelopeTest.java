package energy.eddie.regionconnector.de.eta.providers.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.BusinessTypeList;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v0_82.vhd.CommodityKind;
import energy.eddie.cim.v0_82.vhd.DirectionTypeList;
import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.cim.v0_82.vhd.QualityTypeList;
import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntermediateValidatedHistoricalDataEnvelopeTest {

    private static final ZonedDateTime T0 = ZonedDateTime.of(2026, 4, 30, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final String METERING_POINT_ID = "DE0123456789012345678901234567890";
    private static final String ELIGIBLE_PARTY_ID = "test-eligible-party-id";

    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME,
            "unused-fallback"
    );
    private final DeEtaPlusConfiguration deConfiguration = new DeEtaPlusConfiguration(
            ELIGIBLE_PARTY_ID,
            "http://api.url", "client-id", "client-secret",
            "/meters/historical", "/v1/permissions/{id}", 30,
            3, 2, true, false,
            null, null
    );

    @Test
    void toVhd_emptyReadings_returnsEmptyList() {
        assertThat(build(EnergyType.ELECTRICITY, List.of()).toVhd()).isEmpty();
    }

    @Test
    void toVhd_electricityConsumption_setsV082EnumsAndKwhUnit() {
        var readings = List.of(reading(T0, 12.345, "kWh", "VALIDATED", "Consumption"));

        var ts = build(EnergyType.ELECTRICITY, readings).toVhd().getFirst()
                .getValidatedHistoricalDataMarketDocument().getTimeSeriesList().getTimeSeries().getFirst();

        assertThat(ts.getBusinessType()).isEqualTo(BusinessTypeList.CONSUMPTION);
        assertThat(ts.getFlowDirectionDirection()).isEqualTo(DirectionTypeList.DOWN);
        assertThat(ts.getProduct()).isEqualTo(EnergyProductTypeList.ACTIVE_ENERGY);
        assertThat(ts.getEnergyMeasurementUnitName()).isEqualTo(UnitOfMeasureTypeList.KILOWATT_HOUR);
        assertThat(ts.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity())
                .isEqualTo(CommodityKind.ELECTRICITYPRIMARYMETERED);
        var period = ts.getSeriesPeriodList().getSeriesPeriods().getFirst();
        assertThat(period.getPointList().getPoints()).hasSize(1);
        assertThat(period.getPointList().getPoints().getFirst().getEnergyQuantityQuality())
                .isEqualTo(QualityTypeList.AS_PROVIDED);
    }

    @Test
    void toVhd_electricityGeneration_setsBusinessTypeProductionAndDirectionUp() {
        var readings = List.of(reading(T0, 4.812, "kWh", "VALIDATED", "Generation"));

        var ts = build(EnergyType.ELECTRICITY, readings).toVhd().getFirst()
                .getValidatedHistoricalDataMarketDocument().getTimeSeriesList().getTimeSeries().getFirst();

        assertThat(ts.getBusinessType()).isEqualTo(BusinessTypeList.PRODUCTION);
        assertThat(ts.getFlowDirectionDirection()).isEqualTo(DirectionTypeList.UP);
    }

    @Test
    void toVhd_naturalGas_omitsProductAndUsesCubicMetre() {
        var readings = List.of(reading(T0, 14.728, "m³", "VALIDATED", "Consumption"));

        var ts = build(EnergyType.NATURAL_GAS, readings).toVhd().getFirst()
                .getValidatedHistoricalDataMarketDocument().getTimeSeriesList().getTimeSeries().getFirst();

        assertThat(ts.getProduct()).isNull();
        assertThat(ts.getEnergyMeasurementUnitName()).isEqualTo(UnitOfMeasureTypeList.CUBIC_METRE);
        assertThat(ts.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity())
                .isEqualTo(CommodityKind.NATURALGAS);
    }

    @Test
    void toVhd_unsupportedUnit_returnsEmptyList() {
        var readings = List.of(reading(T0, 1.0, "Wh", "VALIDATED", "Consumption"));

        assertThat(build(EnergyType.ELECTRICITY, readings).toVhd()).isEmpty();
    }

    @Test
    void toVhd_setsReceiverFromConnectorConfigAndSenderFromDataSourceInformation() {
        var readings = List.of(reading(T0, 1.0, "kWh", "VALIDATED", "Consumption"));
        var doc = build(EnergyType.ELECTRICITY, readings).toVhd().getFirst()
                .getValidatedHistoricalDataMarketDocument();

        assertThat(doc.getReceiverMarketParticipantMRID().getValue()).isEqualTo(ELIGIBLE_PARTY_ID);
        assertThat(doc.getReceiverMarketParticipantMRID().getCodingScheme())
                .isEqualTo(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME);
        assertThat(doc.getSenderMarketParticipantMRID().getValue()).isEqualTo("eta-plus");
        assertThat(doc.getSenderMarketParticipantMRID().getCodingScheme())
                .isEqualTo(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME);
    }

    private IntermediateValidatedHistoricalDataEnvelope build(
            EnergyType energyType,
            List<EtaPlusMeteredData.MeterReading> readings
    ) {
        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .connectionId("cid")
                .meteringPointId(METERING_POINT_ID)
                .start(LocalDate.of(2026, 4, 30))
                .end(LocalDate.of(2026, 5, 1))
                .granularity(Granularity.PT15M)
                .energyType(energyType)
                .status(PermissionProcessStatus.ACCEPTED)
                .created(T0)
                .dataNeedId("dnid")
                .build();
        var payload = new EtaPlusMeteredData(METERING_POINT_ID, pr.start(), pr.end(), readings);
        return new IntermediateValidatedHistoricalDataEnvelope(
                cimConfig, deConfiguration, new IdentifiableValidatedHistoricalData(pr, payload));
    }

    private static EtaPlusMeteredData.MeterReading reading(
            ZonedDateTime ts, double value, String unit, String quality, String direction) {
        return new EtaPlusMeteredData.MeterReading(ts, value, unit, quality, direction);
    }
}