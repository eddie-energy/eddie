package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.serde.MessageSerde;
import energy.eddie.cim.serde.SerdeFactory;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.*;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.outbound.shared.testing.MockDataSourceInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.cim.CommonInformationModelVersions.V0_82;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomSerializerTest {
    @Mock
    private MessageSerde mockSerde;

    @Test
    void testSerialize_StatusMessageData() throws SerdeInitializationException {
        var customSerializer = new CustomSerializer(SerdeFactory.getInstance().create("json"));
        String topic = "test";
        ZonedDateTime now = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ConnectionStatusMessage data = new ConnectionStatusMessage("connectionId",
                                                                   "permissionId",
                                                                   "dataNeedId",
                                                                   new MockDataSourceInformation("cc",
                                                                                                 "rc",
                                                                                                 "pa",
                                                                                                 "mda"),
                                                                   now,
                                                                   PermissionProcessStatus.ACCEPTED,
                                                                   "Granted",
                                                                   new ObjectMapper().createObjectNode()
                                                                                     .put("test", "value"));
        byte[] expected = "{\"connectionId\":\"connectionId\",\"permissionId\":\"permissionId\",\"dataNeedId\":\"dataNeedId\",\"dataSourceInformation\":{\"countryCode\":\"cc\",\"meteredDataAdministratorId\":\"mda\",\"permissionAdministratorId\":\"pa\",\"regionConnectorId\":\"rc\"},\"timestamp\":\"2023-01-01T00:00:00Z\",\"status\":\"ACCEPTED\",\"message\":\"Granted\",\"additionalInformation\":{\"test\":\"value\"}}"
                .getBytes(StandardCharsets.UTF_8);

        byte[] result = customSerializer.serialize(topic, data);
        assertArrayEquals(expected, result);

        customSerializer.close();
    }

    @Test
    void testSerialize_NullData() throws SerdeInitializationException {
        var customSerializer = new CustomSerializer(SerdeFactory.getInstance().create("json"));
        String topic = "test";

        byte[] result = customSerializer.serialize(topic, null);

        assertNull(result);

        customSerializer.close();
    }

    @Test
    void testSerialize_UnsupportedDataType() throws SerdeInitializationException {
        var customSerializer = new CustomSerializer(SerdeFactory.getInstance().create("json"));
        String topic = "test";
        Object data = new Object();

        assertNull(customSerializer.serialize(topic, data));

        customSerializer.close();
    }

    @Test
    void testSerialize_EddieValidatedHistoricalDataMarketDocument() throws SerdeInitializationException {
        // Given
        var customSerializer = new CustomSerializer(SerdeFactory.getInstance().create("json"));
        String topic = "test";
        var data = new ValidatedHistoricalDataEnvelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderComplexType()
                                .withMessageDocumentHeaderMetaInformation(
                                        new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderMetaInformationComplexType()
                                                .withConnectionid("connectionId")
                                                .withPermissionid("permissionId")
                                                .withDataNeedid("dataNeedId")
                                )
                )
                .withValidatedHistoricalDataMarketDocument(
                        new ValidatedHistoricalDataMarketDocumentComplexType()
                );

        // When
        byte[] result = customSerializer.serialize(topic, data);

        // Then
        assertNotNull(result);

        // Clean-Up
        customSerializer.close();
    }

    @Test
    void testSerialize_EddieNearRealTimeMarketDocument() throws SerdeInitializationException {
        // Given
        var customSerializer = new CustomSerializer(SerdeFactory.getInstance().create("json"));
        String topic = "test";
        var data = new RTDEnvelope()
                .withMessageDocumentHeaderMetaInformationPermissionId("permissionId")
                .withMessageDocumentHeaderMetaInformationConnectionId("connectionId")
                .withMessageDocumentHeaderMetaInformationDataNeedId("dataNeedId");

        // When
        byte[] result = customSerializer.serialize(topic, data);

        // Then
        assertNotNull(result);

        // Clean-Up
        customSerializer.close();
    }

    @Test
    void givenRawDataMessage_serializes_asExpected() throws SerdeInitializationException {
        // Given
        var customSerializer = new CustomSerializer(SerdeFactory.getInstance().create("json"));
        var expectedString = "{\"permissionId\":\"foo\",\"connectionId\":\"bar\",\"dataNeedId\":\"id1\",\"dataSourceInformation\":{\"countryCode\":\"TEST\",\"meteredDataAdministratorId\":\"tEsT\",\"permissionAdministratorId\":\"TeSt\",\"regionConnectorId\":\"test\"},\"timestamp\":\"2024-01-16T12:00:00Z\",\"rawPayload\":\"rawPayload with <xml> and <html> stuff and special Ϸ ϲ ℻ characters\"}";
        var topic = "myTest";
        var dataSourceInformation = new MockDataSourceInformation("TEST", "test", "TeSt", "tEsT");
        var message = new RawDataMessage("foo",
                                         "bar",
                                         "id1",
                                         dataSourceInformation,
                                         ZonedDateTime.parse("2024-01-16T12:00:00Z"),
                                         "rawPayload with <xml> and <html> stuff and special Ϸ ϲ ℻ characters");

        // When
        var result = customSerializer.serialize(topic, message);

        // Then
        assertThat(result)
                .isNotNull()
                .asString(StandardCharsets.UTF_8)
                .isEqualTo(expectedString);

        // Clean-Up
        customSerializer.close();
    }

    @Test
    void givenPermissionMarketDocument_serializes_asExpected() throws SerdeInitializationException {
        // Given
        var customSerializer = new CustomSerializer(SerdeFactory.getInstance().create("json"));
        // language=JSON
        var json = """
                {
                  "MessageDocumentHeader": null,
                  "Permission_MarketDocument": {
                    "mRID": "permissionId",
                    "revisionNumber": "0.82",
                    "type": "Z04",
                    "createdDateTime": "2024-01-25T09:09Z",
                    "description": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                    "sender_MarketParticipant.mRID": {
                      "codingScheme": "NDK",
                      "value": "epId"
                    },
                    "sender_MarketParticipant.marketRole.type": "A20",
                    "receiver_MarketParticipant.mRID": {
                      "codingScheme": "NDK",
                      "value": "Energinet"
                    },
                    "receiver_MarketParticipant.marketRole.type": "A50",
                    "process.processType": "A55",
                    "period.timeInterval": {
                      "start": "2023-10-27T00:00Z",
                      "end": "2024-01-24T00:00Z"
                    },
                    "PermissionList": {
                      "Permission": [
                        {
                          "permission.mRID": "permissionId",
                          "createdDateTime": "2024-01-25T10:09Z",
                          "transmissionSchedule": null,
                          "marketEvaluationPoint.mRID": {
                            "codingScheme": "NDK",
                            "value": "cid"
                          },
                          "TimeSeriesList": null,
                          "MktActivityRecordList": {
                            "MktActivityRecord": [
                              {
                                "mRID": "uniqueId",
                                "createdDateTime": "2024-01-25T09:09Z",
                                "description": "",
                                "type": "dk-energinet",
                                "reason": null,
                                "name": null,
                                "status": "CREATED"
                              }
                            ]
                          },
                          "ReasonList": null
                        }
                      ]
                    }
                  }
                }
                """.replace("\n", "")
                   .replace(" ", "");
        var pmd = new PermissionEnvelope()
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

        // When
        var res = customSerializer.serialize("anyTopic", pmd);

        // Then
        assertThat(res)
                .isNotNull()
                .asString(StandardCharsets.UTF_8)
                .isEqualTo(json);

        // Clean-Up
        customSerializer.close();
    }

    @Test
    void givenAccountingPointMarketDocument_canSerialize() throws SerdeInitializationException {
        // Given
        var customSerializer = new CustomSerializer(SerdeFactory.getInstance().create("json"));
        var ac = new AccountingPointEnvelope();

        // When
        var res = customSerializer.serialize("any", ac);

        // Then
        assertThat(res).isNotNull();

        // Clean-Up
        customSerializer.close();
    }

    @Test
    void testSerialize_throwsOnSerializationException() throws SerializationException {
        // Given
        var customSerializer = new CustomSerializer(mockSerde);
        when(mockSerde.serialize(any())).thenThrow(new SerializationException(null));

        // When
        var res = customSerializer.serialize("any", new RTREnvelope());

        // Then
        assertNull(res);

        // Clean-Up
        customSerializer.close();
    }
}
