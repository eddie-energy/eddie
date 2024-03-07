package energy.eddie.outbound.kafka;

import energy.eddie.cim.v0_82.cmd.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CustomDeserializerTest {

    @Test
    void testDeserialize_withInvalidJson() {
        // Given
        var json = """
                {
                    "key": "value"
                }
                """.getBytes(StandardCharsets.UTF_8);
        var deserializer = new CustomDeserializer();

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
    void testDeserialize_withValidConsentMarketDocument() {
        // Given
        var json = """
                {
                  "mrid": "permissionId",
                  "revisionNumber": "0.82",
                  "type": "Z04",
                  "createdDateTime": "2024-01-25T09:09Z",
                  "description": "LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY",
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
                """;
        var deserializer = new CustomDeserializer();

        // When
        ConsentMarketDocument res = deserializer.deserialize("anyTopic", json.getBytes(StandardCharsets.UTF_8));

        // Then
        assertAll(
                () -> assertEquals("permissionId", res.getMRID()),
                () -> assertEquals("0.82", res.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.PERMISSION_ADMINISTRATION_DOCUMENT, res.getType()),
                () -> assertEquals("2024-01-25T09:09Z", res.getCreatedDateTime()),
                () -> assertEquals("LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY", res.getDescription()),
                () -> assertEquals(RoleTypeList.PARTY_CONNECTED_TO_GRID,
                                   res.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.PERMISSION_ADMINISTRATOR,
                                   res.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.ACCESS_TO_METERED_DATA, res.getProcessProcessType()),
                () -> assertEquals(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME,
                                   res.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("epId", res.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME,
                                   res.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("Energinet", res.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals("2023-10-27T00:00Z", res.getPeriodTimeInterval().getStart()),
                () -> assertEquals("2024-01-24T00:00Z", res.getPeriodTimeInterval().getEnd()),
                () -> assertEquals("permissionId",
                                   res.getPermissionList().getPermissions().getFirst().getPermissionMRID()),
                () -> assertEquals("2024-01-25T10:09Z",
                                   res.getPermissionList().getPermissions().getFirst().getCreatedDateTime()),
                () -> assertNull(res.getPermissionList().getPermissions().getFirst().getTransmissionSchedule()),
                () -> assertEquals(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME,
                                   res.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                           .getCodingScheme()),
                () -> assertEquals("cid",
                                   res.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                           .getValue()),
                () -> assertNull(res.getPermissionList().getPermissions().getFirst().getReasonList()),
                () -> assertNull(res.getPermissionList().getPermissions().getFirst().getTransmissionSchedule()),
                () -> assertNotNull(res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                            .getMktActivityRecords().getFirst().getMRID()),
                () -> assertNotNull(res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                            .getMktActivityRecords().getFirst().getCreatedDateTime()),
                () -> assertEquals("", res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                        .getMktActivityRecords().getFirst().getDescription()),
                () -> assertEquals("dk-energinet",
                                   res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                           .getMktActivityRecords().getFirst().getType()),
                () -> assertEquals(StatusTypeList.A112,
                                   res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                           .getMktActivityRecords().getFirst().getStatus())
        );


        // Clean-Up
        deserializer.close();
    }
}