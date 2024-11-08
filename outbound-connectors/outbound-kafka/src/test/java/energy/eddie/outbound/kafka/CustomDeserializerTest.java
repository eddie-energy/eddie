package energy.eddie.outbound.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.pmd.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CustomDeserializerTest {
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @Test
    void testDeserialize_withInvalidJson() {
        // Given
        var json = """
                {
                    "key": "value"
                }
                """.getBytes(StandardCharsets.UTF_8);
        var deserializer = new CustomDeserializer(objectMapper);

        // When
        var res = deserializer.deserialize("anyTopic", json);

        // Then
        assertNull(res);

        // Clean-Up
        deserializer.close();
    }

    @Test
    @SuppressWarnings("java:S5961")
        // Mapping logic is sometimes this long
    void testDeserialize_withValidPermissionMarketDocument() {
        // Given
        // language=JSON
        var json = """
                   {
                     "messageDocumentHeader": null,
                     "permissionMarketDocument": {
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
                             "reasonList": null,
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
                             "timeSeriesList": null
                           }
                         ]
                       }
                    }
                  }
                """;
        var deserializer = new CustomDeserializer(objectMapper);

        // When
        PermissionEnvelope res = deserializer.deserialize("anyTopic", json.getBytes(StandardCharsets.UTF_8));

        // Then
        assertNotNull(res);
        var pmd = res.getPermissionMarketDocument();
        assertAll(
                () -> assertEquals("permissionId", pmd.getMRID()),
                () -> assertEquals("0.82", pmd.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.PERMISSION_ADMINISTRATION_DOCUMENT, pmd.getType()),
                () -> assertEquals("2024-01-25T09:09Z", pmd.getCreatedDateTime()),
                () -> assertEquals("9bd0668f-cc19-40a8-99db-dc2cb2802b17", pmd.getDescription()),
                () -> assertEquals(RoleTypeList.PARTY_CONNECTED_TO_GRID,
                                   pmd.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.PERMISSION_ADMINISTRATOR,
                                   pmd.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.ACCESS_TO_METERED_DATA, pmd.getProcessProcessType()),
                () -> assertEquals(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME,
                                   pmd.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("epId", pmd.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME,
                                   pmd.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("Energinet", pmd.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals("2023-10-27T00:00Z", pmd.getPeriodTimeInterval().getStart()),
                () -> assertEquals("2024-01-24T00:00Z", pmd.getPeriodTimeInterval().getEnd()),
                () -> assertEquals("permissionId",
                                   pmd.getPermissionList().getPermissions().getFirst().getPermissionMRID()),
                () -> assertEquals("2024-01-25T10:09Z",
                                   pmd.getPermissionList().getPermissions().getFirst().getCreatedDateTime()),
                () -> assertNull(pmd.getPermissionList().getPermissions().getFirst().getTransmissionSchedule()),
                () -> assertEquals(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME,
                                   pmd.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                      .getCodingScheme()),
                () -> assertEquals("cid",
                                   pmd.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                      .getValue()),
                () -> assertNull(pmd.getPermissionList().getPermissions().getFirst().getReasonList()),
                () -> assertNotNull(pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                       .getMktActivityRecords().getFirst().getMRID()),
                () -> assertNotNull(pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                       .getMktActivityRecords().getFirst().getCreatedDateTime()),
                () -> assertEquals("", pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                          .getMktActivityRecords().getFirst().getDescription()),
                () -> assertEquals("dk-energinet",
                                   pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                      .getMktActivityRecords().getFirst().getType()),
                () -> assertEquals(StatusTypeList.A112,
                                   pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                      .getMktActivityRecords().getFirst().getStatus())
        );


        // Clean-Up
        deserializer.close();
    }

    @Test
    @SuppressWarnings("java:S5961")
        // Mapping logic is sometimes this long
    void testDeserialize_withValidPermissionMarketDocument_forXml() {
        // Given
        // language=XML
        var xml = """
                <PermissionEnvelope>
                  <messageDocumentHeader>
                    <creationDateTime>2024-11-07T00:00:00.000+00:00</creationDateTime>
                    <messageDocumentHeaderMetaInformation>
                      <connectionid>1</connectionid>
                      <permissionid>af54581f-c577-40b0-8402-4e60daa2b0f0</permissionid>
                      <dataNeedid>9bd0668f-cc19-40a8-99db-dc2cb2802b17</dataNeedid>
                      <dataType>permission-market-document</dataType>
                      <messageDocumentHeaderRegion>
                        <connector>us-green-button</connector>
                        <country>NUS</country>
                      </messageDocumentHeaderRegion>
                    </messageDocumentHeaderMetaInformation>
                  </messageDocumentHeader>
                  <permissionMarketDocument>
                    <mrid>permissionId</mrid>
                    <revisionNumber>0.82</revisionNumber>
                    <type>Z04</type>
                    <createdDateTime>2024-01-25T09:09Z</createdDateTime>
                    <description>9bd0668f-cc19-40a8-99db-dc2cb2802b17</description>
                    <senderMarketParticipantMRID>
                      <codingScheme>NUS</codingScheme>
                      <value>REPLACE_ME</value>
                    </senderMarketParticipantMRID>
                    <senderMarketParticipantMarketRoleType>A20</senderMarketParticipantMarketRoleType>
                    <receiverMarketParticipantMRID>
                      <codingScheme>NUS</codingScheme>
                      <value>REPLACE_ME</value>
                    </receiverMarketParticipantMRID>
                    <receiverMarketParticipantMarketRoleType>A50</receiverMarketParticipantMarketRoleType>
                    <processProcessType>A55</processProcessType>
                    <periodTimeInterval>
                      <start>2023-10-27T00:00Z</start>
                      <end>2024-01-24T00:00Z</end>
                    </periodTimeInterval>
                    <permissionList>
                      <permissions>
                        <permissions>
                          <permissionMRID>permissionId</permissionMRID>
                          <createdDateTime>2024-01-25T10:09Z</createdDateTime>
                          <transmissionSchedule />
                          <marketEvaluationPointMRID>
                            <codingScheme>NAT</codingScheme>
                            <value>cid</value>
                          </marketEvaluationPointMRID>
                          <timeSeriesList />
                          <mktActivityRecordList>
                            <mktActivityRecords>
                              <mktActivityRecords>
                                <mrid>24e5d685-5e9e-4ca9-85c5-ad5d32de876e</mrid>
                                <createdDateTime>2024-11-07T11:06:14Z</createdDateTime>
                                <description>CREATED</description>
                                <type>us-green-button</type>
                                <reason />
                                <name />
                                <status>Creation</status>
                              </mktActivityRecords>
                            </mktActivityRecords>
                          </mktActivityRecordList>
                          <reasonList />
                        </permissions>
                      </permissions>
                    </permissionList>
                  </permissionMarketDocument>
                </PermissionEnvelope>
                """;
        var deserializer = new CustomDeserializer(new ObjectMapperConfig().xmlMapper());

        // When
        var res = deserializer.deserialize("anyTopic", xml.getBytes(StandardCharsets.UTF_8));

        // Then
        assertNotNull(res);
        var pmd = res.getPermissionMarketDocument();
        assertAll(
                () -> assertEquals("permissionId", pmd.getMRID()),
                () -> assertEquals("0.82", pmd.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.PERMISSION_ADMINISTRATION_DOCUMENT, pmd.getType()),
                () -> assertEquals("2024-01-25T09:09Z", pmd.getCreatedDateTime()),
                () -> assertEquals("9bd0668f-cc19-40a8-99db-dc2cb2802b17", pmd.getDescription()),
                () -> assertEquals(RoleTypeList.PARTY_CONNECTED_TO_GRID,
                                   pmd.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.PERMISSION_ADMINISTRATOR,
                                   pmd.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.ACCESS_TO_METERED_DATA, pmd.getProcessProcessType()),
                () -> assertEquals(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME,
                                   pmd.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("REPLACE_ME", pmd.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME,
                                   pmd.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("REPLACE_ME", pmd.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals("2023-10-27T00:00Z", pmd.getPeriodTimeInterval().getStart()),
                () -> assertEquals("2024-01-24T00:00Z", pmd.getPeriodTimeInterval().getEnd()),
                () -> assertEquals("permissionId",
                                   pmd.getPermissionList().getPermissions().getFirst().getPermissionMRID()),
                () -> assertEquals("2024-01-25T10:09Z",
                                   pmd.getPermissionList().getPermissions().getFirst().getCreatedDateTime()),
                () -> assertEquals("",
                                   pmd.getPermissionList().getPermissions().getFirst().getTransmissionSchedule(),
                                   "transmissionSchedule should be empty"),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   pmd.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                      .getCodingScheme()),
                () -> assertEquals("cid",
                                   pmd.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                      .getValue()),
                () -> assertTrue(pmd.getPermissionList()
                                    .getPermissions()
                                    .getFirst()
                                    .getReasonList()
                                    .getReasons()
                                    .isEmpty()),
                () -> assertNotNull(pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                       .getMktActivityRecords().getFirst().getMRID()),
                () -> assertNotNull(pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                       .getMktActivityRecords().getFirst().getCreatedDateTime()),
                () -> assertEquals("CREATED",
                                   pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                      .getMktActivityRecords().getFirst().getDescription()),
                () -> assertEquals("us-green-button",
                                   pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                      .getMktActivityRecords().getFirst().getType()),
                () -> assertEquals(StatusTypeList.A14,
                                   pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                      .getMktActivityRecords().getFirst().getStatus())
        );


        // Clean-Up
        deserializer.close();
    }
}
