package energy.eddie.outbound.shared.serde;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.cim.v0_82.ap.CommodityKind;
import energy.eddie.cim.v0_82.ap.DirectionTypeList;
import energy.eddie.cim.v0_82.pmd.*;
import energy.eddie.cim.v0_82.pmd.ESMPDateTimeIntervalComplexType;
import energy.eddie.cim.v0_82.pmd.ProcessTypeList;
import energy.eddie.cim.v0_82.pmd.TimeSeriesComplexType;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.cim.v0_82.vhd.AccumulationKind;
import energy.eddie.cim.v0_82.vhd.AggregateKind;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v0_82.vhd.MeasurementPointIDStringComplexType;
import energy.eddie.cim.v0_91_08.*;
import energy.eddie.outbound.shared.testing.XmlValidator;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class XmlMessageSerdeTest {

    @Test
    void testSerialize_producesCIMCompliantPermissionMarketDocument() throws SerdeInitializationException, SerializationException {
        // Given
        var dateTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var document = new PermissionEnvelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderComplexType()
                                .withCreationDateTime(dateTime)
                                .withMessageDocumentHeaderMetaInformation(
                                        new energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid("pid")
                                                .withConnectionid("cid")
                                                .withDataNeedid("dnid")
                                                .withDataType("permission-market-document")
                                                .withMessageDocumentHeaderRegion(
                                                        new energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderRegionComplexType()
                                                                .withConnector("at-ed")
                                                                .withCountry(energy.eddie.cim.v0_82.pmd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                )
                                )
                )
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("mrid")
                                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                                .withCreatedDateTime("2024-01-01T00:00:00Z")
                                .withType(energy.eddie.cim.v0_82.pmd.MessageTypeList.PERMISSION_ADMINISTRATION_DOCUMENT)
                                .withDescription("bla")
                                .withSenderMarketParticipantMarketRoleType(energy.eddie.cim.v0_82.pmd.RoleTypeList.PERMISSION_ADMINISTRATOR)
                                .withSenderMarketParticipantMRID(
                                        new energy.eddie.cim.v0_82.pmd.PartyIDStringComplexType()
                                                .withCodingScheme(energy.eddie.cim.v0_82.pmd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                .withValue("EDA")
                                )
                                .withReceiverMarketParticipantMarketRoleType(energy.eddie.cim.v0_82.pmd.RoleTypeList.PARTY_CONNECTED_TO_GRID)
                                .withReceiverMarketParticipantMRID(
                                        new energy.eddie.cim.v0_82.pmd.PartyIDStringComplexType()
                                                .withCodingScheme(energy.eddie.cim.v0_82.pmd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                .withValue("eligible-party")
                                )
                                .withProcessProcessType(ProcessTypeList.REALISED)
                                .withPeriodTimeInterval(
                                        new ESMPDateTimeIntervalComplexType()
                                                .withStart("2024-01-01T00:00Z")
                                                .withEnd("2024-01-01T00:00Z")
                                )
                                .withPermissionList(
                                        new PermissionMarketDocumentComplexType.PermissionList()
                                                .withPermissions(
                                                        new PermissionComplexType()
                                                                .withPermissionMRID("pmRID")
                                                                .withCreatedDateTime("2024-01-01T00:00:00Z")
                                                                .withMarketEvaluationPointMRID(
                                                                        new energy.eddie.cim.v0_82.pmd.MeasurementPointIDStringComplexType()
                                                                                .withCodingScheme(energy.eddie.cim.v0_82.pmd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                                                .withValue("AT00")
                                                                )
                                                                .withTransmissionSchedule("PT1D")
                                                                .withTimeSeriesList(
                                                                        new PermissionComplexType.TimeSeriesList()
                                                                                .withTimeSeries(
                                                                                        new TimeSeriesComplexType()
                                                                                                .withMRID("tmRID")
                                                                                )
                                                                )
                                                                .withMktActivityRecordList(
                                                                        new PermissionComplexType.MktActivityRecordList()
                                                                                .withMktActivityRecords(
                                                                                        new MktActivityRecordComplexType()
                                                                                                .withMRID("mrID")
                                                                                                .withDescription(
                                                                                                        "ACCEPTED")
                                                                                                .withType("at-eda")
                                                                                                .withCreatedDateTime(
                                                                                                        "2024-01-01T00:00:00Z")
                                                                                                .withStatus(
                                                                                                        StatusTypeList.A07)
                                                                                )
                                                                )
                                                                .withReasonList(new PermissionComplexType.ReasonList())
                                                )
                                )
                );
        var serde = new XmlMessageSerde();

        // When
        var res = serde.serialize(document);
        var valid = XmlValidator.validatePermissionMarketDocument(new String(res, StandardCharsets.UTF_8));

        // Then
        assertTrue(valid);
    }

    @Test
    void testSerialize_producesCIMCompliantAccountingPointMarketDocument() throws SerdeInitializationException, SerializationException {
        // Given
        var dateTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var document = new AccountingPointEnvelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v0_82.ap.MessageDocumentHeaderComplexType()
                                .withCreationDateTime(dateTime)
                                .withMessageDocumentHeaderMetaInformation(
                                        new energy.eddie.cim.v0_82.ap.MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid("pid")
                                                .withConnectionid("cid")
                                                .withDataNeedid("dnid")
                                                .withDataType("permission-market-document")
                                                .withMessageDocumentHeaderRegion(
                                                        new energy.eddie.cim.v0_82.ap.MessageDocumentHeaderRegionComplexType()
                                                                .withConnector("at-ed")
                                                                .withCountry(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                )
                                )
                )
                .withAccountingPointMarketDocument(
                        new AccountingPointMarketDocumentComplexType()
                                .withMRID("mrid")
                                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                                .withCreatedDateTime("2024-01-01T00:00:00Z")
                                .withType(energy.eddie.cim.v0_82.ap.MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                                .withDescription("bla")
                                .withSenderMarketParticipantMarketRoleType(energy.eddie.cim.v0_82.ap.RoleTypeList.METER_ADMINISTRATOR)
                                .withSenderMarketParticipantMRID(
                                        new energy.eddie.cim.v0_82.ap.PartyIDStringComplexType()
                                                .withCodingScheme(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                .withValue("EDA")
                                )
                                .withReceiverMarketParticipantMarketRoleType(energy.eddie.cim.v0_82.ap.RoleTypeList.PARTY_CONNECTED_TO_GRID)
                                .withReceiverMarketParticipantMRID(
                                        new energy.eddie.cim.v0_82.ap.PartyIDStringComplexType()
                                                .withCodingScheme(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                .withValue("eligible-party")
                                )
                                .withAccountingPointList(
                                        new AccountingPointMarketDocumentComplexType.AccountingPointList()
                                                .withAccountingPoints(
                                                        new AccountingPointComplexType()
                                                                .withMRID(
                                                                        new energy.eddie.cim.v0_82.ap.MeasurementPointIDStringComplexType()
                                                                                .withCodingScheme(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                                                .withValue("EDA")
                                                                )
                                                                .withMeterReadingResolution("PT15M")
                                                                .withGridAgreementType("contract")
                                                                .withName("John Doe")
                                                                .withAdministrativeStatus("allowed")
                                                                .withFlexibilityContract(false)
                                                                .withResolution("PT15M")
                                                                .withCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                                                                .withEnergyCommunity("none")
                                                                .withDirection(DirectionTypeList.UP)
                                                                .withGenerationType("electricity")
                                                                .withLoadProfileType("none")
                                                                .withSupplyStatus("available")
                                                                .withTariffClassDSO("tariff")
                                                                .withContractPartyList(
                                                                        new AccountingPointComplexType.ContractPartyList()
                                                                                .withContractParties(
                                                                                        new ContractPartyComplexType()
                                                                                                .withContractPartyRole(
                                                                                                        ContractPartyRoleType.INVOICE)
                                                                                                .withEmail(
                                                                                                        "john@doe.com")
                                                                                )
                                                                )
                                                                .withAddressList(
                                                                        new AccountingPointComplexType.AddressList()
                                                                                .withAddresses(
                                                                                        new AddressComplexType()
                                                                                                .withAddressRole(
                                                                                                        AddressRoleType.INVOICE)
                                                                                                .withCityName("Vienna")
                                                                                                .withPostalCode("1010")
                                                                                                .withStreetName(
                                                                                                        "Stephansplatz")
                                                                                                .withBuildingNumber("1")
                                                                                )
                                                                )
                                                )
                                )
                );
        var serde = new XmlMessageSerde();

        // When
        var res = serde.serialize(document);
        var valid = XmlValidator.validateAccountingPointMarketDocument(new String(res, StandardCharsets.UTF_8));

        // Then
        assertTrue(valid);
    }

    @Test
    void testSerialize_producesCIMCompliantValidatedHistoricalDataMarketDocument() throws SerdeInitializationException, SerializationException {
        // Given
        var dateTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var document = new ValidatedHistoricalDataEnvelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderComplexType()
                                .withCreationDateTime(dateTime)
                                .withMessageDocumentHeaderMetaInformation(
                                        new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid("pid")
                                                .withConnectionid("cid")
                                                .withDataNeedid("dnid")
                                                .withDataType("permission-market-document")
                                                .withMessageDocumentHeaderRegion(
                                                        new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderRegionComplexType()
                                                                .withConnector("at-ed")
                                                                .withCountry(energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                )
                                )
                )
                .withValidatedHistoricalDataMarketDocument(
                        new ValidatedHistoricalDataMarketDocumentComplexType()
                                .withMRID("MRID")
                                .withCreatedDateTime("2024-01-01T00:00:00Z")
                                .withDescription("")
                                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                                .withType(energy.eddie.cim.v0_82.vhd.MessageTypeList.MARKET_RESULT_DOCUMENT)
                                .withDescription("bla")
                                .withSenderMarketParticipantMarketRoleType(energy.eddie.cim.v0_82.vhd.RoleTypeList.METERING_POINT_ADMINISTRATOR)
                                .withSenderMarketParticipantMRID(
                                        new energy.eddie.cim.v0_82.vhd.PartyIDStringComplexType()
                                                .withCodingScheme(energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                .withValue("EDA")
                                )
                                .withReceiverMarketParticipantMarketRoleType(energy.eddie.cim.v0_82.vhd.RoleTypeList.PARTY_CONNECTED_TO_GRID)
                                .withReceiverMarketParticipantMRID(
                                        new energy.eddie.cim.v0_82.vhd.PartyIDStringComplexType()
                                                .withCodingScheme(energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                .withValue("eligible-party")
                                )
                                .withProcessProcessType(energy.eddie.cim.v0_82.vhd.ProcessTypeList.REALISED)
                                .withPeriodTimeInterval(
                                        new energy.eddie.cim.v0_82.vhd.ESMPDateTimeIntervalComplexType()
                                                .withStart("2024-01-01T00:00Z")
                                                .withEnd("2024-01-01T00:00Z")
                                )
                                .withTimeSeriesList(
                                        new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                                .withTimeSeries(
                                                        new energy.eddie.cim.v0_82.vhd.TimeSeriesComplexType()
                                                                .withMRID("MRID")
                                                                .withProduct(EnergyProductTypeList.ACTIVE_ENERGY)
                                                                .withBusinessType(BusinessTypeList.MAXIMUM_VALUE_OF_DC_FLOW)
                                                                .withVersion(CommonInformationModelVersions.V0_82.version())
                                                                .withFlowDirectionDirection(energy.eddie.cim.v0_82.vhd.DirectionTypeList.UP)
                                                                .withMarketEvaluationPointMRID(
                                                                        new MeasurementPointIDStringComplexType()
                                                                                .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                                                .withValue("ID")
                                                                )
                                                                .withMarketEvaluationPointMeterReadingsMRID(
                                                                        new ResourceIDStringComplexType()
                                                                                .withCodingScheme("AT")
                                                                                .withValue("ID")
                                                                )
                                                                .withMarketEvaluationPointMeterReadingsReadingsMRID(
                                                                        new ResourceIDStringComplexType()
                                                                                .withCodingScheme("AT")
                                                                                .withValue("ID")
                                                                )
                                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulate(
                                                                        AccumulationKind.DELTADATA
                                                                )
                                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(
                                                                        AggregateKind.AVERAGE
                                                                )
                                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(
                                                                        energy.eddie.cim.v0_82.vhd.CommodityKind.ELECTRICITYPRIMARYMETERED
                                                                )
                                                                .withMarketEvaluationPointUsagePointLocationGeoInfoReference(
                                                                        "ref")
                                                                .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT_HOUR)
                                                                .withEnergyQualityMeasurementUnitName(
                                                                        UnitOfMeasureTypeList.KILOWATT_HOUR)
                                                                .withRegistrationDateAndOrTimeDateTime(
                                                                        new DateAndOrTimeComplexType()
                                                                                .withDateTime(dateTime)
                                                                )
                                                                .withRegisteredResource(
                                                                        new RegisteredResourceComplexType()
                                                                                .withMRID("MRID")
                                                                                .withName("name")
                                                                                .withDescription("description")
                                                                                .withFuelFuel(FuelTypeList.FOSSIL_GASEOUS_UNSPECIFIED)
                                                                                .withLocationMRID("mrid")
                                                                                .withLocationName("name")
                                                                                .withLocationPositionPointsSequenceNumber(
                                                                                        BigInteger.ZERO
                                                                                )
                                                                                .withLocationPositionPointsXPosition("x")
                                                                                .withLocationPositionPointsYPosition("y")
                                                                                .withLocationPositionPointsZPosition("z")
                                                                                .withLocationCoordinateSystemCrsUrn(
                                                                                        "URN")
                                                                                .withPSRTypePsrType("psr")
                                                                                .withResourceCapacityList(
                                                                                        new RegisteredResourceComplexType.ResourceCapacityList()
                                                                                )
                                                                )
                                                                .withInDomainMRID(
                                                                        new AreaIDStringComplexType()
                                                                                .withCodingScheme(energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                                                .withValue("id")
                                                                )
                                                                .withOutDomainMRID(
                                                                        new AreaIDStringComplexType()
                                                                                .withCodingScheme(energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                                                .withValue("id")
                                                                )
                                                                .withSeriesPeriodList(
                                                                        new energy.eddie.cim.v0_82.vhd.TimeSeriesComplexType.SeriesPeriodList()
                                                                )
                                                                .withReasonList(
                                                                        new energy.eddie.cim.v0_82.vhd.TimeSeriesComplexType.ReasonList()
                                                                                .withReasons(
                                                                                        new energy.eddie.cim.v0_82.vhd.ReasonComplexType()
                                                                                                .withCode(energy.eddie.cim.v0_82.vhd.ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
                                                                                                .withText("")
                                                                                )
                                                                )

                                                )
                                )
                );
        var serde = new XmlMessageSerde();

        // When
        var res = serde.serialize(document);
        var valid = XmlValidator.validateValidatedHistoricalMarketDocument(new String(res, StandardCharsets.UTF_8));

        // Then
        assertTrue(valid);
    }

    @Test
    void testSerialize_emptyObject_throws() throws SerdeInitializationException {
        // Given
        var serde = new XmlMessageSerde();
        var message = new Object();

        // When & Then
        assertThrows(SerializationException.class, () -> serde.serialize(message));
    }

    @Test
    void testSerialize_unknownObject_serializesWithFallback() throws SerdeInitializationException, SerializationException {
        // Given
        var serde = new XmlMessageSerde();
        var message = Map.of("key", "value");
        //language=XML
        var expected = """
                <Map1><key>value</key></Map1>
                """.trim();

        // When
        var res = serde.serialize(message);

        // Then
        assertEquals(expected, new String(res, StandardCharsets.UTF_8));
    }

    @Test
    void testDeserialize_deserializesCIMType() throws SerdeInitializationException, DeserializationException {
        // Given
        var serde = new XmlMessageSerde();
        //language=XML
        var pmd = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Permission_Envelope xmlns="http://www.eddie.energy/Consent/EDD02/20240125" xmlns:ns2="htthttp://www.eddie.energy/AP/EDD04/20240422" xmlns:ns3="http://www.eddie.energy/VHD/EDD01/20240614">
                  <MessageDocumentHeader>
                    <creationDateTime>2024-01-01T00:00:00.000Z</creationDateTime>
                    <MessageDocumentHeader_MetaInformation>
                      <connectionid>cid</connectionid>
                      <permissionid>pid</permissionid>
                      <dataNeedid>dnid</dataNeedid>
                      <dataType>permission-market-document</dataType>
                      <MessageDocumentHeader_Region>
                        <connector>at-ed</connector>
                        <country>NAT</country>
                      </MessageDocumentHeader_Region>
                    </MessageDocumentHeader_MetaInformation>
                  </MessageDocumentHeader>
                  <Permission_MarketDocument>
                    <mRID>mrid</mRID>
                    <revisionNumber>0.82</revisionNumber>
                    <type>Z04</type>
                    <createdDateTime>2024-01-01T00:00:00Z</createdDateTime>
                    <description>bla</description>
                    <sender_MarketParticipant.mRID>
                      <codingScheme>NAT</codingScheme>
                      <value>EDA</value>
                    </sender_MarketParticipant.mRID>
                    <sender_MarketParticipant.marketRole.type>A50</sender_MarketParticipant.marketRole.type>
                    <receiver_MarketParticipant.mRID>
                      <codingScheme>NAT</codingScheme>
                      <value>eligible-party</value>
                    </receiver_MarketParticipant.mRID>
                    <receiver_MarketParticipant.marketRole.type>A20</receiver_MarketParticipant.marketRole.type>
                    <process.processType>A16</process.processType>
                    <period.timeInterval>
                      <start>2024-01-01T00:00Z</start>
                      <end>2024-01-01T00:00Z</end>
                    </period.timeInterval>
                    <PermissionList>
                      <Permission>
                        <permission.mRID>pmRID</permission.mRID>
                        <createdDateTime>2024-01-01T00:00:00Z</createdDateTime>
                        <transmissionSchedule>PT1D</transmissionSchedule>
                        <marketEvaluationPoint.mRID>
                          <codingScheme>NAT</codingScheme>
                          <value>AT00</value>
                        </marketEvaluationPoint.mRID>
                        <TimeSeriesList>
                          <TimeSeries>
                            <mRID>tmRID</mRID>
                          </TimeSeries>
                        </TimeSeriesList>
                        <MktActivityRecordList>
                          <MktActivityRecord>
                            <mRID>mrID</mRID>
                            <createdDateTime>2024-01-01T00:00:00Z</createdDateTime>
                            <description>ACCEPTED</description>
                            <type>at-eda</type>
                            <status>Activated</status>
                          </MktActivityRecord>
                        </MktActivityRecordList>
                        <ReasonList/>
                      </Permission>
                    </PermissionList>
                  </Permission_MarketDocument>
                </Permission_Envelope>
                """;

        // When
        var res = serde.deserialize(pmd.getBytes(StandardCharsets.UTF_8), PermissionEnvelope.class);

        // Then
        assertNotNull(res);
    }

    @Test
    void testSerialize_producesCIMCompliantRTREnvelope() throws SerdeInitializationException, SerializationException {
        // Given
        var dateTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var document = new RTREnvelope()
                .withMarketDocumentMRID("mrid")
                .withMessageDocumentHeaderCreationDateTime(dateTime)
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMessageDocumentHeaderMetaInformationRegionConnector("rc-id")
                .withMarketDocumentPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart("2024-01-01T00:00Z")
                                .withEnd("2024-01-01T00:00Z")
                );
        var serde = new XmlMessageSerde();

        // When
        var res = serde.serialize(document);
        var valid = XmlValidator.validateRtrEnvelope(new String(res, StandardCharsets.UTF_8));

        // Then
        assertTrue(valid);
    }

    @Test
    void testDeserialize_toNonCimType() throws SerdeInitializationException, DeserializationException {
        // Given
        var serde = new XmlMessageSerde();
        //language=XML
        var message = "<HashMap><key>value</key></HashMap>";

        // When
        var res = serde.deserialize(message.getBytes(StandardCharsets.UTF_8), HashMap.class);

        // Then
        assertEquals(Map.of("key", "value"), res);
    }

    @Test
    void testDeserialize_throwsOnInvalidXML() throws SerdeInitializationException {
        // Given
        var serde = new XmlMessageSerde();
        //language=JSON
        var message = "{\"key\": \"value\" }";

        // When & Then
        assertThrows(DeserializationException.class,
                     () -> serde.deserialize(message.getBytes(StandardCharsets.UTF_8), HashMap.class));
    }

    @Test
    void testSerialize_producesCIM_v0_91_08CompliantValidatedHistoricalDataMarketDocument() throws SerdeInitializationException, SerializationException {
        // Given
        var dateTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var document = new VHDEnvelope()
                .withMessageDocumentHeaderCreationDateTime(dateTime)
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMessageDocumentHeaderMetaInformationConnectionId("cid")
                .withMessageDocumentHeaderMetaInformationDataNeedId("dnid")
                .withMessageDocumentHeaderMetaInformationDocumentType("validated-historical-data-market-document")
                .withMessageDocumentHeaderMetaInformationRegionConnector("at-eda")
                .withMessageDocumentHeaderMetaInformationRegionCountry("at")
                .withMarketDocument(
                        new VHDMarketDocument()
                                .withMRID("MRID")
                                .withCreatedDateTime(dateTime)
                                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METER_ADMINISTRATOR.value())
                                .withDescription("validated-historical-data-market-document")
                                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                                .withRevisionNumber(CommonInformationModelVersions.V0_91_08.version())
                                .withSenderMarketParticipantMRID(
                                        new PartyIDString()
                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                .withValue("EDA")
                                )
                                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                                .withReceiverMarketParticipantMRID(
                                        new PartyIDString()
                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                .withValue("eligible-party")
                                )
                                .withProcessProcessType(StandardProcessTypeList.ACCESS_TO_METERED_DATA.value())
                                .withPeriodTimeInterval(
                                        new ESMPDateTimeInterval()
                                                .withStart("2024-01-01T00:00Z")
                                                .withEnd("2024-01-01T00:00Z")
                                )
                                .withTimeSeries(
                                        new TimeSeries()
                                                .withMRID(UUID.randomUUID().toString())
                                                .withVersion(CommonInformationModelVersions.V0_91_08.version())
                                                .withFlowDirectionDirection(StandardDirectionTypeList.DOWN.value())
                                                .withProduct(StandardEnergyProductTypeList.ACTIVE_ENERGY.value())
                                                .withMarketEvaluationPointMRID(
                                                        new MeasurementPointIDString()
                                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                                .withValue("ID")
                                                )
                                                .withMarketEvaluationPointMeterReadingsMRID(
                                                        new ResourceIDString()
                                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                                .withValue("ID")
                                                )
                                                .withMarketEvaluationPointMeterReadingsReadingsMRID(
                                                        new ResourceIDString()
                                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                                .withValue("ID")
                                                )
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulation(
                                                        energy.eddie.cim.v0_91_08.AccumulationKind.DELTADATA
                                                )
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(
                                                        energy.eddie.cim.v0_91_08.AggregateKind.AVERAGE
                                                )
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(
                                                        energy.eddie.cim.v0_91_08.CommodityKind.ELECTRICITYPRIMARYMETERED
                                                )
                                                .withMarketEvaluationPointUsagePointLocationGeoInfoReference("ref")
                                                .withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                                                .withEnergyQualityMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                                                .withDateAndOrTimeDateTime(dateTime)
                                                .withRegisteredResourceMRID(
                                                        new ResourceIDString()
                                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                                .withValue("MRID")
                                                )
                                                .withRegisteredResourceFuelFuel(StandardFuelTypeList.FOSSIL_GASEOUS_COALDERIVED_GAS.value())
                                                .withRegisteredResourceLocationMRID("mrid")
                                                .withRegisteredResourceName("name")
                                                .withRegisteredResourceDescription("description")
                                                .withRegisteredResourceLocationPositionPointsSequenceNumber(BigInteger.ZERO)
                                                .withRegisteredResourceLocationPositionPointsXPosition("x")
                                                .withRegisteredResourceLocationPositionPointsYPosition("y")
                                                .withRegisteredResourceLocationPositionPointsZPosition("z")
                                                .withRegisteredResourceLocationCoordinateSystemCrsUrn(
                                                        StandardCoordinateSystemTypeList.OSGB36.value())
                                                .withRegisteredResourcePSRTypePsrType(StandardAssetTypeList.AC_LINK.value())
                                                .withReasonCode(StandardReasonCodeTypeList.ACCOUNTING_POINT_TIELINE_TIME_SERIES_MISSING.value())
                                )
                );
        var serde = new XmlMessageSerde();

        // When
        var res = serde.serialize(document);
        var valid = XmlValidator.validateV09108ValidatedHistoricalDataMarketDocument(new String(res,
                                                                                                StandardCharsets.UTF_8));

        // Then
        assertTrue(valid);
    }
}