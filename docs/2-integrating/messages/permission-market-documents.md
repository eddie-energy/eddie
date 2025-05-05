# Permission Market Documents

Permission market documents are used to signal the status change of one or multiple permission requests.
They contain a lot of information, the following example focuses on the most necessary parts.
The header is described in [CIM envelope](./messages.md#cim-envelope).
The schema can be found [here](https://github.com/eddie-energy/eddie/tree/main/api/src/main/schemas/cim/xsd/v0_82/pmd).

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The following is an example of a permission market document produced by EDDIE.
The comments explain the usage of each element.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Permission_Envelope xmlns="http://www.eddie.energy/Consent/EDD02/20240125"
>
    <MessageDocumentHeader>
        <!-- ... omitted for conciseness -->
    </MessageDocumentHeader>
    <Permission_MarketDocument>
        <!-- the ID of the permission request -->
        <mRID>b9b06543-4f14-4081-8419-4b933e4b7f9d</mRID>
        <!-- version of the CIM schema that is used -->
        <revisionNumber>0.82</revisionNumber>
        <!-- Describes what type of permission market document this is
             'Z04' identifies it as a permission administrator document.
             This is the only permission market document type emitted by EDDIE.
             Another relevant one is 'Z01' permission termination document, described further down the page. -->
        <type>Z04</type>
        <!-- The datetime when this document was created in ISO 8601 -->
        <createdDateTime>2024-12-02T10:04:22Z</createdDateTime>
        <!-- The ID of the data need related to this document and permission request -->
        <description>9bd0668f-cc19-40a8-99db-dc2cb2802b17</description>
        <!-- Sender of the document that resulted in the status change of the permission request -->
        <sender_MarketParticipant.mRID>
            <codingScheme>NUS</codingScheme>
            <value>REPLACE_ME</value>
        </sender_MarketParticipant.mRID>
        <!-- The role of the sender -->
        <sender_MarketParticipant.marketRole.type>A20</sender_MarketParticipant.marketRole.type>
        <!-- receiver of the document that resulted in the status change of the permission request -->
        <receiver_MarketParticipant.mRID>
            <codingScheme>NUS</codingScheme>
            <value>DEMOUTILITY</value>
        </receiver_MarketParticipant.mRID>
        <!-- The role of the receiver -->
        <receiver_MarketParticipant.marketRole.type>A50</receiver_MarketParticipant.marketRole.type>
        <!-- The process type shows the type of data need related to the permission request -->
        <process.processType>A55</process.processType>
        <!-- Contains the start and end time of the permission request if already known -->
        <period.timeInterval>
            <!-- can be empty -->
            <start>2024-09-02T00:00Z</start>
            <!-- can be empty -->
            <end>2024-12-01T00:00Z</end>
        </period.timeInterval>
        <!-- Contains all permission requests related to this document, for EDDIE always only one -->
        <PermissionList>
            <Permission>
                <!-- again the ID of the permission request -->
                <permission.mRID>b9b06543-4f14-4081-8419-4b933e4b7f9d</permission.mRID>
                <!-- created datetime of the permission request -->
                <createdDateTime>2024-12-02T10:04:22Z</createdDateTime>
                <!-- Contains the connectionID plus the coding scheme selected by the eligible party -->
                <marketEvaluationPoint.mRID>
                    <codingScheme>NAT</codingScheme>
                    <!-- connectionID -->
                    <value>1</value>
                </marketEvaluationPoint.mRID>
                <TimeSeriesList/>
                <MktActivityRecordList>
                    <MktActivityRecord>
                        <!-- Random UUID identifying only this market activity record -->
                        <mRID>9f8f770c-fb9d-47f7-9b71-8fb1f31e5729</mRID>
                        <!-- created datetime of this market activity record -->
                        <createdDateTime>2024-12-02T10:04:22Z</createdDateTime>
                        <!-- EDDIE internal status in the permission process model -->
                        <description>CREATED</description>
                        <!-- region-connector ID -->
                        <type>us-green-button</type>
                        <!-- Matching CIM status to the permission process model -->
                        <status>Creation</status>
                    </MktActivityRecord>
                </MktActivityRecordList>
                <ReasonList/>
            </Permission>
        </PermissionList>
    </Permission_MarketDocument>
</Permission_Envelope>
```

## Termination Documents

A special kind of permission market document is the termination document.
It indicates that an already **accepted** permission request should be terminated.
The termination document is sent to EDDIE by the eligible party, it uses the same format as described [above](#permission-market-documents).

> [!Info]
> Only accepted permission requests can be terminated, see [the permission process model](../integrating.md#permission-process-model)

The following is a minimal example of a permission market document.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Permission_Envelope xmlns="http://www.eddie.energy/Consent/EDD02/20240125"
>
    <Permission_MarketDocument>
        <!-- The permission request to be terminated -->
        <mRID>{{permissioId}}</mRID>
        <!-- Identifies the document as a permission termiantion document -->
        <type>Z01</type>
        <PermissionList>
            <Permission>
                <MktActivityRecordList>
                    <MktActivityRecord>
                        <type>{{region-connector-id}}</type>
                    </MktActivityRecord>
                </MktActivityRecordList>
                <ReasonList>
                    <Reason>
                        <!-- Cancelled by EP -->
                        <code>Z03</code>
                    </Reason>
                </ReasonList>
            </Permission>
        </PermissionList>
    </Permission_MarketDocument>
</Permission_Envelope>
```

Here a json example.

```json
{
  "Permission_MarketDocument": {
    "mRID": "REPLACE_ME",
    "type": "Z01",
    "PermissionList": {
      "Permission": [
        {
          "MktActivityRecordList": {
            "MktActivityRecord": [
              {
                "type": "at-eda"
              }
            ]
          },
          "ReasonList": {
            "Reason": [
              {
                "code": "Z03"
              }
            ]
          }
        }
      ]
    }
  }
}
```

