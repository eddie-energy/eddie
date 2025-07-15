package energy.eddie.regionconnector.at.eda.provider.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.outbound.shared.serde.SerdeInitializationException;
import energy.eddie.outbound.shared.serde.SerializationException;
import energy.eddie.outbound.shared.serde.XmlMessageSerde;
import energy.eddie.outbound.shared.testing.XmlValidator;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntermediateValidatedHistoricalDataMarketDocumentTest {

    @ParameterizedTest
    @MethodSource("meterCodeAndMeteringModeSource")
    // CIM mapping requires that amount of asserts
    @SuppressWarnings("java:S5961")
    void toVhd_returnsVHDEnvelopes(
            String meterCode,
            StandardDirectionTypeList expectedDirection,
            String meteringMode,
            StandardQualityTypeList expectedQuality
    ) {
        // Given
        var pr = new SimplePermissionRequest("pid", "cid", "dnid");
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);
        var now = ZonedDateTime.now(AT_ZONE_ID);
        var cal = DatatypeFactory.newDefaultInstance()
                                 .newXMLGregorianCalendar(GregorianCalendar.from(now));
        var codingScheme = StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value();
        var simpleRecord = new SimpleEdaConsumptionRecord()
                .setMessageId("messageId")
                .setConversationId("conversationId")
                .setMeteringPoint(meterCode)
                .setStartDate(start)
                .setEndDate(end)
                .setSenderMessageAddress("eda")
                .setReceiverMessageAddress("eddie")
                .setDocumentCreationDateTime(now)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(Granularity.P1D)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setEnergyPositions(List.of(
                                                        new EnergyPosition(BigDecimal.ONE, meteringMode)
                                                ))
                                                .setMeterCode(meterCode)
                                                .setBillingUnit("KWH")
                                ))
                                .setMeterReadingStart(start.atStartOfDay(AT_ZONE_ID))
                                .setMeterReadingEnd(endOfDay(end, AT_ZONE_ID))
                                .setMeteringReason("reason")
                ))
                .setSchemaVersion("version")
                .setProcessDate(cal);
        var consumptionRecord = new IdentifiableConsumptionRecord(simpleRecord, List.of(pr), start, end);
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "epID");
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord);

        // When
        var res = doc.toVhd();

        // Then
        assertThat(res)
                .singleElement()
                // asserts for VHDEnvelope
                .satisfies(document -> {
                    assertThat(document.getMessageDocumentHeaderCreationDateTime()).isNotNull();
                    assertThat(document.getMessageDocumentHeaderMetaInformationConnectionId()).isEqualTo("cid");
                    assertThat(document.getMessageDocumentHeaderMetaInformationDocumentType()).isEqualTo(
                            "validated-historical-data-market-document");
                    assertThat(document.getMessageDocumentHeaderMetaInformationDataNeedId()).isEqualTo("dnid");
                    assertThat(document.getMessageDocumentHeaderMetaInformationPermissionId()).isEqualTo("pid");
                    assertThat(document.getMessageDocumentHeaderMetaInformationRegionConnector()).isEqualTo("at-eda");
                    assertThat(document.getMessageDocumentHeaderMetaInformationRegionCountry()).isEqualTo("AT");
                })
                // asserts for the market document
                .extracting(VHDEnvelope::getMarketDocument)
                .satisfies(document -> {
                    assertThat(document.getMRID()).isEqualTo("messageId");
                    assertThat(document.getCreatedDateTime()).isEqualTo(now);
                    assertThat(document.getReceiverMarketParticipantMarketRoleType()).isEqualTo(StandardRoleTypeList.CONSUMER.value());
                    assertThat(document.getReceiverMarketParticipantMRID())
                            .satisfies(partyId -> {
                                assertThat(partyId.getCodingScheme()).isEqualTo(codingScheme);
                                assertThat(partyId.getValue()).isEqualTo("eddie");
                            });
                    assertThat(document.getSenderMarketParticipantMarketRoleType()).isEqualTo(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value());
                    assertThat(document.getSenderMarketParticipantMRID())
                            .satisfies(partyId -> {
                                assertThat(partyId.getCodingScheme()).isEqualTo(codingScheme);
                                assertThat(partyId.getValue()).isEqualTo("eda");
                            });
                    assertThat(document.getPeriodTimeInterval())
                            .satisfies(interval -> {
                                assertThat(interval.getStart()).isEqualTo("2024-12-31T23:00Z");
                                assertThat(interval.getEnd()).isEqualTo("2025-01-01T23:00Z");
                            });
                    assertThat(document.getProcessProcessType()).isEqualTo(StandardProcessTypeList.REALISED.value());
                })
                // asserts for time series
                .extracting(VHDMarketDocument::getTimeSeries)
                .asInstanceOf(InstanceOfAssertFactories.list(TimeSeries.class))
                .singleElement()
                .satisfies(document -> {
                    assertThat(document.getVersion()).isEqualTo("1");
                    assertThat(document.getDateAndOrTimeDateTime()).isCloseTo(now, within(1, ChronoUnit.MILLIS));
                    assertThat(document.getMarketEvaluationPointMRID())
                            .satisfies(pointId -> {
                                assertThat(pointId.getCodingScheme()).isEqualTo(codingScheme);
                                assertThat(pointId.getValue()).isEqualTo(meterCode);
                            });
                    assertThat(document.getEnergyMeasurementUnitName()).isEqualTo(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value());
                    assertThat(document.getReasonCode()).isEqualTo(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value());
                    assertThat(document.getReasonText()).isEqualTo("reason");
                    assertThat(document.getRegisteredResourceMRID())
                            .satisfies(resourceId -> {
                                assertThat(resourceId.getCodingScheme()).isEqualTo(codingScheme);
                                assertThat(resourceId.getValue()).isEqualTo(meterCode);
                            });
                    assertThat(document.getFlowDirectionDirection()).isEqualTo(expectedDirection.value());
                })
                // asserts for periods
                .extracting(TimeSeries::getPeriods)
                .asInstanceOf(InstanceOfAssertFactories.list(SeriesPeriod.class))
                .singleElement()
                .satisfies(period -> {
                    var granularity = DatatypeFactory.newDefaultInstance()
                                                     .newDuration(Duration.ofDays(1).toMillis());
                    assertThat(period.getResolution()).isEqualTo(granularity);
                    assertThat(period.getTimeInterval())
                            .satisfies(interval -> {
                                assertThat(interval.getStart()).isEqualTo("2024-12-31T23:00Z");
                                assertThat(interval.getEnd()).isEqualTo("2025-01-02T22:59Z");
                            });
                })
                // asserts for point
                .extracting(SeriesPeriod::getPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(Point.class))
                .singleElement()
                .satisfies(point -> {
                    assertThat(point.getPosition()).isEqualTo(1);
                    assertThat(point.getEnergyQuantityQuantity()).isEqualTo(BigDecimal.ONE);
                    assertThat(point.getEnergyQuantityQuality()).isEqualTo(expectedQuality.value());
                });
    }

    @Test
    void toVhd_producesValidXml() throws SerdeInitializationException, SerializationException {
        // Given
        var pr = new SimplePermissionRequest("pid", "cid", "dnid");
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);
        var created = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, AT_ZONE_ID);
        var cal = DatatypeFactory.newDefaultInstance()
                                 .newXMLGregorianCalendar(GregorianCalendar.from(created));
        var simpleRecord = new SimpleEdaConsumptionRecord()
                .setMessageId("messageId")
                .setConversationId("conversationId")
                .setMeteringPoint("1-1:1.9.0 P.01")
                .setStartDate(start)
                .setEndDate(end)
                .setSenderMessageAddress("eda")
                .setReceiverMessageAddress("eddie")
                .setDocumentCreationDateTime(created)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(Granularity.P1D)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setEnergyPositions(List.of(
                                                        new EnergyPosition(BigDecimal.ONE, "L1")
                                                ))
                                                .setMeterCode("1-1:1.9.0 P.01")
                                                .setBillingUnit("KWH")
                                ))
                                .setMeterReadingStart(start.atStartOfDay(AT_ZONE_ID))
                                .setMeterReadingEnd(endOfDay(end, AT_ZONE_ID))
                                .setMeteringReason("reason")
                ))
                .setSchemaVersion("version")
                .setProcessDate(cal);
        var consumptionRecord = new IdentifiableConsumptionRecord(simpleRecord, List.of(pr), start, end);
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "epID");
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord);

        // When
        var res = doc.toVhd();

        // Then
        var xmlDoc = new String(new XmlMessageSerde().serialize(res.getFirst()), StandardCharsets.UTF_8);
        assertTrue(XmlValidator.validateV104ValidatedHistoricalDataMarketDocument(xmlDoc.getBytes(StandardCharsets.UTF_8)),
                   "Failed to validate XML, see:\n" + xmlDoc);
    }

    private static Stream<Arguments> meterCodeAndMeteringModeSource() {
        return Stream.of(
                Arguments.of("1-1:1.9.0 P.01",
                             StandardDirectionTypeList.DOWN,
                             "L1",
                             StandardQualityTypeList.AS_PROVIDED),
                Arguments.of("1-1:2.9.0 P.01", StandardDirectionTypeList.UP, "L2", StandardQualityTypeList.ADJUSTED),
                Arguments.of("1-1:2.9.0 P.01", StandardDirectionTypeList.UP, "L3", StandardQualityTypeList.ESTIMATED)
        );
    }
}