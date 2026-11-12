// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_12.StandardBusinessTypeList;
import energy.eddie.cim.v1_12.StandardDirectionTypeList;
import energy.eddie.cim.v1_12.StandardEnergyProductTypeList;
import energy.eddie.cim.v1_12.StandardQualityTypeList;
import energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList;
import energy.eddie.cim.v1_12.vhd.CommodityKind;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private static final ZonedDateTime T0 = ZonedDateTime.of(2026, 4, 30, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final String METERING_POINT_ID = "DE0123456789012345678901234567890";
    private static final String ELIGIBLE_PARTY_ID = "test-ep-id";

    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME,
            "unused-fallback"
    );
    private final DeEtaPlusConfiguration deConfiguration = new DeEtaPlusConfiguration(
            ELIGIBLE_PARTY_ID,
            "http://api.url", "client-id", "client-secret",
            "/meters/historical", "/meters/accounting-point", 30,
            3, 2, true, false,
            null, null
    );
    private final XmlMessageSerde serde = new XmlMessageSerde();

    IntermediateValidatedHistoricalDataMarketDocumentTest() throws SerdeInitializationException {}

    @Test
    void toVhd_emptyReadings_returnsEmptyList() {
        var result = build(EnergyType.ELECTRICITY, List.of()).toVhd();
        assertThat(result).isEmpty();
    }

    @Test
    void toVhd_electricityConsumption_setsBusinessTypeConsumptionAndProductActiveEnergy() {
        var readings = List.of(
                reading(T0, 12.345, "kWh", "VALIDATED", "Consumption"),
                reading(T0.plusHours(1), 13.5, "kWh", "VALIDATED", "Consumption")
        );

        var envelope = build(EnergyType.ELECTRICITY, readings).toVhd().getFirst();
        var ts = envelope.getVHDMarketDocument().getTimeSeries().getFirst();

        assertThat(ts.getBusinessType()).isEqualTo(StandardBusinessTypeList.CONSUMPTION.value());
        assertThat(ts.getFlowDirectionDirection()).isEqualTo(StandardDirectionTypeList.DOWN.value());
        assertThat(ts.getProduct()).isEqualTo(StandardEnergyProductTypeList.ACTIVE_ENERGY.value());
        assertThat(ts.getEnergyMeasurementUnitName()).isEqualTo(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value());
        assertThat(ts.getAccountingPointMeterReadingsReadingsReadingTypeCommodity())
                .isEqualTo(CommodityKind._2);
        assertThat(ts.getAccountingPointMRID().getValue()).isEqualTo(METERING_POINT_ID);
        var period = ts.getSeriesPeriods().getFirst();
        assertThat(period.getPoints()).hasSize(2);
        assertThat(period.getPoints().get(0).getEnergyQuantityQuality())
                .isEqualTo(StandardQualityTypeList.AS_PROVIDED.value());
    }

    @Test
    void toVhd_electricityGeneration_setsBusinessTypeProductionAndDirectionUp() {
        var readings = List.of(reading(T0, 4.812, "kWh", "VALIDATED", "Generation"));

        var ts = build(EnergyType.ELECTRICITY, readings).toVhd().getFirst()
                .getVHDMarketDocument().getTimeSeries().getFirst();

        assertThat(ts.getBusinessType()).isEqualTo(StandardBusinessTypeList.PRODUCTION.value());
        assertThat(ts.getFlowDirectionDirection()).isEqualTo(StandardDirectionTypeList.UP.value());
    }

    @Test
    void toVhd_naturalGas_omitsProductAndUsesCubicMetreUnit() {
        var readings = List.of(reading(T0, 14.728, "m³", "VALIDATED", "Consumption"));

        var ts = build(EnergyType.NATURAL_GAS, readings).toVhd().getFirst()
                .getVHDMarketDocument().getTimeSeries().getFirst();

        assertThat(ts.getProduct()).isNull();
        assertThat(ts.getEnergyMeasurementUnitName()).isEqualTo(StandardUnitOfMeasureTypeList.CUBIC_METRE.value());
        assertThat(ts.getAccountingPointMeterReadingsReadingsReadingTypeCommodity())
                .isEqualTo(CommodityKind._7);
    }

    @Test
    void toVhd_unsupportedUnit_returnsEmptyList() {
        var readings = List.of(reading(T0, 1.0, "Wh", "VALIDATED", "Consumption"));

        assertThat(build(EnergyType.ELECTRICITY, readings).toVhd()).isEmpty();
    }

    @Test
    void toVhd_unknownStatus_omitsQualityOnPoint() {
        var readings = List.of(reading(T0, 1.0, "kWh", "ESTIMATED", "Consumption"));

        var point = build(EnergyType.ELECTRICITY, readings).toVhd().getFirst()
                .getVHDMarketDocument().getTimeSeries().getFirst()
                .getSeriesPeriods().getFirst().getPoints().getFirst();

        assertThat(point.getEnergyQuantityQuality()).isNull();
    }

    @Test
    void toVhd_readingsOutOfOrder_sortsByTimestampAndAssignsPositionsInOrder() {
        var readings = List.of(
                reading(T0.plusHours(2), 3.0, "kWh", "VALIDATED", "Consumption"),
                reading(T0, 1.0, "kWh", "VALIDATED", "Consumption"),
                reading(T0.plusHours(1), 2.0, "kWh", "VALIDATED", "Consumption")
        );

        var period = build(EnergyType.ELECTRICITY, readings).toVhd().getFirst()
                .getVHDMarketDocument().getTimeSeries().getFirst()
                .getSeriesPeriods().getFirst();

        assertThat(period.getPoints()).extracting(p -> p.getEnergyQuantityQuantity().doubleValue())
                .containsExactly(1.0, 2.0, 3.0);
        assertThat(period.getTimeInterval().getStart()).isEqualTo(T0.toString());
        assertThat(period.getTimeInterval().getEnd()).isEqualTo(T0.plusHours(2).toString());
    }

    @Test
    void toVhd_producesXsdValidV112MarketDocument() throws SerializationException {
        var readings = List.of(
                reading(T0, 12.345, "kWh", "VALIDATED", "Consumption"),
                reading(T0.plusHours(1), 13.5, "kWh", "VALIDATED", "Consumption")
        );

        var envelope = build(EnergyType.ELECTRICITY, readings).toVhd().getFirst();
        var xml = serde.serialize(envelope);

        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                "Produced XML failed XSD validation:\n" + new String(xml, StandardCharsets.UTF_8));
    }

    @Test
    void toVhd_setsReceiverFromConnectorConfigAndSenderFromDataSourceInformation() {
        var readings = List.of(reading(T0, 1.0, "kWh", "VALIDATED", "Consumption"));
        var doc = build(EnergyType.ELECTRICITY, readings).toVhd().getFirst().getVHDMarketDocument();

        assertThat(doc.getReceiverMarketParticipantMRID().getValue()).isEqualTo(ELIGIBLE_PARTY_ID);
        assertThat(doc.getReceiverMarketParticipantMRID().getCodingScheme()).isEqualTo("NDE");
        assertThat(doc.getSenderMarketParticipantMRID().getValue()).isEqualTo("eta-plus");
        assertThat(doc.getSenderMarketParticipantMRID().getCodingScheme()).isEqualTo("NDE");
    }

    private IntermediateValidatedHistoricalDataMarketDocument build(
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
        return new IntermediateValidatedHistoricalDataMarketDocument(
                cimConfig, deConfiguration, new IdentifiableValidatedHistoricalData(pr, payload));
    }

    private static EtaPlusMeteredData.MeterReading reading(
            ZonedDateTime ts, double value, String unit, String quality, String direction) {
        return new EtaPlusMeteredData.MeterReading(ts, value, unit, quality, direction);
    }
}
