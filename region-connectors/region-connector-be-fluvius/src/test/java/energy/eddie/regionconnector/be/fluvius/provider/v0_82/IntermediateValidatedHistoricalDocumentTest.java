// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntermediateValidatedHistoricalDocumentTest {

    private final ClassLoader classLoader = this.getClass().getClassLoader();
    private final ObjectMapper mapper = new ObjectMapper();
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME,
            "fallback"
    );

    @Mock
    private DataNeedsService dataNeedsService;

    @Mock
    private FluviusOAuthConfiguration fluviusConfig;

    @Test
    @SuppressWarnings("java:S5961")
    void testGetGasVHD_hourly_differentUnits() {
        // Given
        InputStream inputStream = classLoader.getResourceAsStream("gas_data_measurement_hourly_different_units.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .dataNeedId("dnid")
                                                       .granularity(Granularity.PT1H)
                                                       .build();

        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.NATURAL_GAS,
                        Granularity.PT1H,
                        Granularity.PT1H
                ));

        // When
        var res = new IntermediateValidatedHistoricalDocument(fluviusConfig,
                                                              new IdentifiableMeteringData(pr, json),
                                                              dataNeedsService).toVHD();

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
                () -> assertEquals(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME,
                                   vhd.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("Fluvius", vhd.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(2, vhd.getTimeSeriesList().getTimeSeries().size())
        );

        var firstTimeseries = vhd.getTimeSeriesList().getTimeSeries().getFirst();
        assertAll(
                () -> assertEquals(BusinessTypeList.CONSUMPTION, firstTimeseries.getBusinessType()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_ENERGY, firstTimeseries.getProduct()),
                () -> assertEquals(CommodityKind.NATURALGAS,
                                   firstTimeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME,
                                   firstTimeseries.getMarketEvaluationPointMRID().getCodingScheme()),
                () -> assertEquals("0001", firstTimeseries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(DirectionTypeList.DOWN, firstTimeseries.getFlowDirectionDirection()),
                () -> assertEquals(UnitOfMeasureTypeList.CUBIC_METRE, firstTimeseries.getEnergyMeasurementUnitName()),
                () -> assertEquals(1, firstTimeseries.getSeriesPeriodList().getSeriesPeriods().size())
        );

        var firstSeriesPeriod = firstTimeseries.getSeriesPeriodList().getSeriesPeriods().getFirst();
        assertAll(
                () -> assertEquals("2024-12-01T00:00Z", firstSeriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals("2024-12-01T01:00Z", firstSeriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("PT1H", firstSeriesPeriod.getResolution()),
                () -> assertEquals(1, firstSeriesPeriod.getPointList().getPoints().size())
        );

        var firstSeriesPeriodPoint = firstSeriesPeriod.getPointList().getPoints().getFirst();
        assertAll(
                () -> assertEquals(new BigDecimal("0.008"), firstSeriesPeriodPoint.getEnergyQuantityQuantity()),
                () -> assertEquals("1", firstSeriesPeriodPoint.getPosition()),
                () -> assertEquals(QualityTypeList.ESTIMATED, firstSeriesPeriodPoint.getEnergyQuantityQuality())
        );

        var secondTimeseries = vhd.getTimeSeriesList().getTimeSeries().get(1);
        assertAll(
                () -> assertEquals(BusinessTypeList.CONSUMPTION, secondTimeseries.getBusinessType()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_ENERGY, secondTimeseries.getProduct()),
                () -> assertEquals(CommodityKind.NATURALGAS,
                                   secondTimeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME,
                                   secondTimeseries.getMarketEvaluationPointMRID().getCodingScheme()),
                () -> assertEquals("0001", secondTimeseries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(DirectionTypeList.DOWN, secondTimeseries.getFlowDirectionDirection()),
                () -> assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR,
                                   secondTimeseries.getEnergyMeasurementUnitName()),
                () -> assertEquals(1, secondTimeseries.getSeriesPeriodList().getSeriesPeriods().size())
        );

        var secondSeriesPeriod = secondTimeseries.getSeriesPeriodList().getSeriesPeriods().getFirst();
        assertAll(
                () -> assertEquals("2024-12-01T00:00Z", secondSeriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals("2024-12-01T01:00Z", secondSeriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("PT1H", secondSeriesPeriod.getResolution()),
                () -> assertEquals(1, secondSeriesPeriod.getPointList().getPoints().size())
        );

        var secondSeriesPeriodPoint = secondSeriesPeriod.getPointList().getPoints().getFirst();
        assertAll(
                () -> assertEquals(new BigDecimal("0.093"), secondSeriesPeriodPoint.getEnergyQuantityQuantity()),
                () -> assertEquals("2", secondSeriesPeriodPoint.getPosition()),
                () -> assertEquals(QualityTypeList.ESTIMATED, secondSeriesPeriodPoint.getEnergyQuantityQuality())
        );
    }

    @Test
    void testGetGasVHD_daily_sameUnit() {

        // Given
        InputStream inputStream = classLoader.getResourceAsStream("gas_data_measurement_daily_same_unit.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        when(dataNeedsService.getById("did"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.NATURAL_GAS,
                        Granularity.P1D,
                        Granularity.P1D
                ));

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .granularity(Granularity.P1D)
                                                       .build();

        // When
        var res = new IntermediateValidatedHistoricalDocument(fluviusConfig,
                                                              new IdentifiableMeteringData(pr, json),
                                                              dataNeedsService).toVHD();

        // Then
        assertEquals(1, res.size());

        var vhd = res.getFirst().getValidatedHistoricalDataMarketDocument();
        assertEquals(1, vhd.getTimeSeriesList().getTimeSeries().size());

        var timeseries = vhd.getTimeSeriesList().getTimeSeries().getFirst();
        assertAll(
                () -> assertEquals(CommodityKind.NATURALGAS,
                                   timeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR, timeseries.getEnergyMeasurementUnitName()),
                () -> assertEquals(1, timeseries.getSeriesPeriodList().getSeriesPeriods().size())
        );

        var seriesPeriod = timeseries.getSeriesPeriodList().getSeriesPeriods().getFirst();
        assertAll(
                () -> assertEquals("2024-12-01T00:00Z", seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals("2024-12-02T00:00Z", seriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("P1D", seriesPeriod.getResolution()),
                () -> assertEquals(2, seriesPeriod.getPointList().getPoints().size())
        );
    }

    @Test
    void testGetGasVHD_daily_empty() {

        // Given
        InputStream inputStream = classLoader.getResourceAsStream("gas_data_measurement_hourly_different_units.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        when(dataNeedsService.getById("did"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.NATURAL_GAS,
                        Granularity.P1D,
                        Granularity.P1D
                ));

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .granularity(Granularity.P1D)
                                                       .build();

        // When
        var res = new IntermediateValidatedHistoricalDocument(fluviusConfig,
                                                              new IdentifiableMeteringData(pr, json),
                                                              dataNeedsService).toVHD();

        // Then
        assertEquals(1, res.size());

        var vhd = res.getFirst().getValidatedHistoricalDataMarketDocument();
        assertEquals(0, vhd.getTimeSeriesList().getTimeSeries().size());
    }

    @Test
    void testGetElectricityVHD_quarterHourly() {

        // Given
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_quarter_hourly.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        when(dataNeedsService.getById("did"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT15M,
                        Granularity.PT15M
                ));

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .build();

        // When
        var res = new IntermediateValidatedHistoricalDocument(fluviusConfig,
                                                              new IdentifiableMeteringData(pr, json),
                                                              dataNeedsService).toVHD();

        // Then
        assertEquals(1, res.size());

        var vhd = res.getFirst().getValidatedHistoricalDataMarketDocument();
        assertEquals(2, vhd.getTimeSeriesList().getTimeSeries().size());

        var timeseries = vhd.getTimeSeriesList().getTimeSeries().getFirst();
        assertAll(
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED,
                                   timeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR, timeseries.getEnergyMeasurementUnitName()),
                () -> assertEquals(1, timeseries.getSeriesPeriodList().getSeriesPeriods().size())
        );

        var seriesPeriod = timeseries.getSeriesPeriodList().getSeriesPeriods().getFirst();
        assertAll(
                () -> assertEquals("2025-04-17T00:00Z", seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals("2025-04-17T00:15Z", seriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("PT15M", seriesPeriod.getResolution()),
                () -> assertEquals(1, seriesPeriod.getPointList().getPoints().size())
        );
    }

    @Test
    void testGetElectricityVHD_daily_empty() {

        // Given
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_quarter_hourly.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        when(dataNeedsService.getById("did"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.ELECTRICITY,
                        Granularity.P1D,
                        Granularity.P1D
                ));

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .granularity(Granularity.P1D)
                                                       .build();

        // When
        var res = new IntermediateValidatedHistoricalDocument(fluviusConfig,
                                                              new IdentifiableMeteringData(pr, json),
                                                              dataNeedsService).toVHD();

        // Then
        assertEquals(1, res.size());

        var vhd = res.getFirst().getValidatedHistoricalDataMarketDocument();
        assertEquals(0, vhd.getTimeSeriesList().getTimeSeries().size());
    }

    @Test
    void testGetElectricityVHD_daily() {

        // Given
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_daily.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        when(dataNeedsService.getById("did"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.ELECTRICITY,
                        Granularity.P1D,
                        Granularity.P1D
                ));

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .granularity(Granularity.P1D)
                                                       .build();

        // When
        var res = new IntermediateValidatedHistoricalDocument(fluviusConfig,
                                                              new IdentifiableMeteringData(pr, json),
                                                              dataNeedsService).toVHD();

        // Then
        assertEquals(1, res.size());

        var vhd = res.getFirst().getValidatedHistoricalDataMarketDocument();
        assertEquals(1, vhd.getTimeSeriesList().getTimeSeries().size());

        var timeseries = vhd.getTimeSeriesList().getTimeSeries().getFirst();
        assertAll(
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED,
                                   timeseries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR, timeseries.getEnergyMeasurementUnitName()),
                () -> assertEquals(1, timeseries.getSeriesPeriodList().getSeriesPeriods().size())
        );

        var seriesPeriod = timeseries.getSeriesPeriodList().getSeriesPeriods().getFirst();
        assertAll(
                () -> assertEquals("2025-04-17T00:00Z", seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals("2025-04-18T00:00Z", seriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("P1D", seriesPeriod.getResolution()),
                () -> assertEquals(1, seriesPeriod.getPointList().getPoints().size())
        );
    }
}
