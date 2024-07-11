package energy.eddie.outbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.cim.v0_82.pmd.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.api.CommonInformationModelVersions.V0_82;
import static org.junit.jupiter.api.Assertions.*;

class CustomSerializerTest {

    private CustomSerializer customSerializer;

    @BeforeEach
    void setup() {
        customSerializer = new CustomSerializer();
    }

    @AfterEach
    void tearDown() {
        customSerializer.close();
    }

    @Test
    void testSerialize_StatusMessageData() {
        String topic = "test";
        ZonedDateTime now = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ConnectionStatusMessage data = new ConnectionStatusMessage("connectionId",
                                                                   "permissionId",
                                                                   "dataNeedId",
                                                                   new TestDataSourceInformation("cc",
                                                                                                 "rc",
                                                                                                 "pa",
                                                                                                 "mda"),
                                                                   now,
                                                                   PermissionProcessStatus.ACCEPTED,
                                                                   "Granted",
                                                                   new ObjectMapper().createObjectNode()
                                                                                     .put("test", "value"));
        byte[] expected = "{\"connectionId\":\"connectionId\",\"permissionId\":\"permissionId\",\"dataNeedId\":\"dataNeedId\",\"dataSourceInformation\":{\"countryCode\":\"cc\",\"meteredDataAdministratorId\":\"mda\",\"permissionAdministratorId\":\"pa\",\"regionConnectorId\":\"rc\"},\"timestamp\":1672531200.000000000,\"status\":\"ACCEPTED\",\"message\":\"Granted\",\"additionalInformation\":{\"test\":\"value\"}}"
                .getBytes(StandardCharsets.UTF_8);

        byte[] result = customSerializer.serialize(topic, data);
        assertArrayEquals(expected, result);
    }

    @Test
    void testSerialize_ConsumptionRecordData() throws JsonProcessingException {
        String topic = "test";
        ConsumptionRecord data = new ConsumptionRecord();
        byte[] expected = new ObjectMapper().writeValueAsBytes(data);

        byte[] result = customSerializer.serialize(topic, data);

        assertArrayEquals(expected, result);
    }

    @Test
    void testSerialize_NullData() {
        String topic = "test";
        Object data = null;
        byte[] expected = new byte[0];

        byte[] result = customSerializer.serialize(topic, data);

        assertArrayEquals(expected, result);
    }

    @Test
    void testSerialize_UnsupportedDataType() {
        String topic = "test";
        Object data = new Object();

        assertThrows(UnsupportedOperationException.class,
                     () -> customSerializer.serialize(topic, data));
    }

    @Test
    void testSerialize_EddieValidatedHistoricalDataMarketDocument() throws JsonProcessingException {
        String topic = "test";
        EddieValidatedHistoricalDataMarketDocument data = new EddieValidatedHistoricalDataMarketDocument(
                "connectionId",
                "permissionId",
                "dataNeedId",
                new energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocument()
        );
        byte[] expected = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsBytes(data);

        byte[] result = customSerializer.serialize(topic, data);

        assertArrayEquals(expected, result);
    }

    @Test
    void givenRawDataMessage_serializes_asExpected() {
        // Given
        var expectedString = "{\"permissionId\":\"foo\",\"connectionId\":\"bar\",\"dataNeedId\":\"id1\",\"dataSourceInformation\":{\"countryCode\":\"TEST\",\"meteredDataAdministratorId\":\"tEsT\",\"permissionAdministratorId\":\"TeSt\",\"regionConnectorId\":\"test\"},\"timestamp\":\"2024-01-16T12:00:00Z\",\"rawPayload\":\"rawPayload with <xml> and <html> stuff and special Ϸ ϲ ℻ characters\"}";
        var topic = "myTest";
        var dataSourceInformation = new TestDataSourceInformation("TEST", "test", "TeSt", "tEsT");
        var message = new RawDataMessage("foo",
                                         "bar",
                                         "id1",
                                         dataSourceInformation,
                                         ZonedDateTime.parse("2024-01-16T12:00:00Z"),
                                         "rawPayload with <xml> and <html> stuff and special Ϸ ϲ ℻ characters");

        // When
        var result = customSerializer.serialize(topic, message);

        // Then
        assertEquals(expectedString, new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void givenPermissionMarketDocument_serializes_asExpected() {
        // Given
        // language=JSON
        var json = """
                {
                    "messageDocumentHeader": null,
                    "permissionMarketDocument":
                    {
                      "mrid": "permissionId",
                      "revisionNumber": "0.82",
                      "type": "Z04",
                      "createdDateTime": "2024-01-25T09:09Z",
                      "description": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                      "senderMarketParticipantMRID": {
                        "codingScheme": "NDK",
                        "value": "epId"
                      },
                      "senderMarketParticipantMarketRoleType": "A20",
                      "receiverMarketParticipantMRID": {
                        "codingScheme": "NDK",
                        "value": "Energinet"
                      },
                      "receiverMarketParticipantMarketRoleType": "A50",
                      "processProcessType": "A55",
                      "periodTimeInterval": {
                        "start": "2023-10-27T00:00Z",
                        "end": "2024-01-24T00:00Z"
                      },
                      "permissionList": {
                        "permissions": [
                          {
                            "permissionMRID": "permissionId",
                            "createdDateTime": "2024-01-25T10:09Z",
                            "transmissionSchedule": null,
                            "marketEvaluationPointMRID": {
                              "codingScheme": "NDK",
                              "value": "cid"
                            },
                            "timeSeriesList": null,
                            "mktActivityRecordList": {
                              "mktActivityRecords": [
                                {
                                  "mrid": "uniqueId",
                                  "createdDateTime": "2024-01-25T09:09Z",
                                  "description": "",
                                  "type": "dk-energinet",
                                  "reason": null,
                                  "name": null,
                                  "status": "CREATED"
                                }
                              ]
                            },
                            "reasonList": null
                          }
                        ]
                      }
                    }
                }
                """.replace("\n", "")
                   .replace(" ", "");
        var pmd = new PermissionEnveloppe()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("permissionId")
                                .withRevisionNumber(V0_82.version())
                                .withType(MessageTypeList.PERMISSION_ADMINISTRATION_DOCUMENT)
                                .withCreatedDateTime("2024-01-25T09:09Z")
                                .withDescription("9bd0668f-cc19-40a8-99db-dc2cb2802b17")
                                .withSenderMarketParticipantMarketRoleType(RoleTypeList.PARTY_CONNECTED_TO_GRID)
                                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.PERMISSION_ADMINISTRATOR)
                                .withProcessProcessType(ProcessTypeList.ACCESS_TO_METERED_DATA)
                                .withSenderMarketParticipantMRID(
                                        new PartyIDStringComplexType()
                                                .withCodingScheme(
                                                        CodingSchemeTypeList.fromValue("NDK")
                                                )
                                                .withValue("epId")
                                )
                                .withReceiverMarketParticipantMRID(
                                        new PartyIDStringComplexType()
                                                .withCodingScheme(
                                                        CodingSchemeTypeList.fromValue("NDK")
                                                )
                                                .withValue("Energinet")
                                )
                                .withPeriodTimeInterval(
                                        new ESMPDateTimeIntervalComplexType()
                                                .withStart("2023-10-27T00:00Z")
                                                .withEnd("2024-01-24T00:00Z")
                                )
                                .withPermissionList(
                                        new PermissionMarketDocumentComplexType.PermissionList()
                                                .withPermissions(
                                                        new PermissionComplexType()
                                                                .withPermissionMRID("permissionId")
                                                                .withCreatedDateTime("2024-01-25T10:09Z")
                                                                .withTransmissionSchedule(null)
                                                                .withMarketEvaluationPointMRID(
                                                                        new MeasurementPointIDStringComplexType()
                                                                                .withCodingScheme(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME)
                                                                                .withValue("cid")
                                                                )
                                                                .withMktActivityRecordList(
                                                                        new PermissionComplexType.MktActivityRecordList()
                                                                                .withMktActivityRecords(
                                                                                        new MktActivityRecordComplexType()
                                                                                                .withMRID("uniqueId")
                                                                                                .withCreatedDateTime(
                                                                                                        "2024-01-25T09:09Z")
                                                                                                .withDescription("")
                                                                                                .withType("dk-energinet")
                                                                                                .withStatus(
                                                                                                        StatusTypeList.A112)
                                                                                )
                                                                )
                                                )
                                )
                );
        var serializer = new CustomSerializer();

        // When
        var res = serializer.serialize("anyTopic", pmd);

        // Then
        assertEquals(json, new String(res, StandardCharsets.UTF_8));

        // Clean-Up
        serializer.close();
    }

    private record TestDataSourceInformation(String countryCode,
                                             String regionConnectorId,
                                             String permissionAdministratorId,
                                             String meteredDataAdministratorId) implements DataSourceInformation {
    }
}
