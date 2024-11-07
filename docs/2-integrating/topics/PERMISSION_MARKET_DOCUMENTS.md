# Topic: Permission Market Document

The topic **permission-market-documents**  provides data of the permission request in a CIM-compliant format.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "mrid": {
      "type": "string",
      "description": "The unique identification of the document being exchanged within a business process flow. For EDDIE this is the permission ID."
    },
    "revisionNumber": {
      "type": "string",
      "description": "The identification of the version that distinguishes one evolution of a document from another."
    },
    "type": {
      "type": "string",
      "documentation": "The coded type of a document. The document type describes the principal characteristic of the document."
    },
    "createdDateTime": {
      "type": "string",
      "description": "The date and time of the creation of the document."
    },
    "description": {
      "type": "string",
      "description": "The description describes the purpose of the document. For EDDIE this is the data need ID."
    },
    "senderMarketParticipantMRID": {
      "type": "object",
      "description": "The identification of a party in the energy market.",
      "properties": {
        "codingScheme": {
          "type": "string",
          "description": "Describes the origin of the market participant."
        },
        "value": {
          "type": "string",
          "description": "Describes the sender of the permission request. Most likely the eligible party."
        }
      },
      "required": [
        "codingScheme",
        "value"
      ]
    },
    "senderMarketParticipantMarketRoleType": {
      "type": "string",
      "description": "The identification of the role played by a market player."
    },
    "receiverMarketParticipantMRID": {
      "type": "object",
      "description": "The identification of a party in the energy market.",
      "properties": {
        "codingScheme": {
          "type": "string",
          "description": "Describes the origin of the market participant."
        },
        "value": {
          "type": "string",
          "description": "Describes the sender of the permission request. Most likely the authority to which the permission is sent to."
        }
      },
      "required": [
        "codingScheme",
        "value"
      ]
    },
    "receiverMarketParticipantMarketRoleType": {
      "type": "string",
      "description": "The identification of the role played by a market player."
    },
    "processProcessType": {
      "type": "string",
      "description": "Indicates the nature of process that the document addresses. (What type of data will be requested)"
    },
    "periodTimeInterval": {
      "type": "object",
      "description": "The start and end date and time for a given interval.",
      "properties": {
        "start": {
          "type": "string",
          "description": "The start date and time of the interval with a minute resolution. (YYYY-MM-DDThh:mmZ)"
        },
        "end": {
          "type": "string",
          "description": "The end date and time of the interval with a minute resolution. (YYYY-MM-DDThh:mmZ)"
        }
      },
      "required": [
        "start",
        "end"
      ]
    },
    "permissionList": {
      "type": "object",
      "properties": {
        "permissions": {
          "type": "array",
          "description": "An electronic document containing the information necessary to satisfy the requirements of a given business process.",
          "items": [
            {
              "type": "object",
              "properties": {
                "permissionMRID": {
                  "type": "string",
                  "description": "The permission ID. The same as 'mrid'."
                },
                "createdDateTime": {
                  "type": "string",
                  "description": "The date and time of the creation of the permission."
                },
                "transmissionSchedule": {
                  "type": [
                    "string",
                    "null"
                  ],
                  "description": "Duration as 'PnYnMnDTnHnMnS'. This field can be null."
                },
                "marketEvaluationPointMRID": {
                  "type": "object",
                  "properties": {
                    "codingScheme": {
                      "type": "string",
                      "description": "Describes from where the permission was issued."
                    },
                    "value": {
                      "type": "string",
                      "description": "For EDDIE this is the connection ID and represents the current connection associated with a user."
                    }
                  },
                  "required": [
                    "codingScheme",
                    "value"
                  ]
                },
                "reasonList": {
                  "type": [
                    "object",
                    "null"
                  ],
                  "description": "A list of reasons on the change of a permission. This can be null.",
                  "properties": {
                    "reasons": {
                      "type": "array",
                      "description": "An array of reasons why a change to the permission happened.",
                      "items": [
                        {
                          "type": "object",
                          "properties": {
                            "text": {
                              "type": "string",
                              "description": "The textual explanation corresponding to the reason code."
                            },
                            "code": {
                              "description": "The coded motivation of an act."
                            }
                          }
                        }
                      ]
                    }
                  },
                  "required": [
                    "text",
                    "code"
                  ]
                },
                "mktActivityRecordList": {
                  "type": "object",
                  "properties": {
                    "mktActivityRecords": {
                      "type": "array",
                      "description": "An array of status changes of the current permission.",
                      "items": [
                        {
                          "type": "object",
                          "properties": {
                            "mrid": {
                              "type": "string",
                              "description": "The identification of the mktActivityRecord."
                            },
                            "createdDateTime": {
                              "type": "string",
                              "description": "Date and time this activity record has been created. (YYYY-MM-DDThh:mm:ss.sssZ)"
                            },
                            "description": {
                              "type": "string",
                              "description": "Describes the mktActivityRecord if necessary."
                            },
                            "type": {
                              "type": "string",
                              "description": "The ID of the region connector."
                            },
                            "reason": {
                              "type": [
                                "string",
                                "null"
                              ],
                              "description": "Description of the mktActivityRecord if necessary. This attribute can be null."
                            },
                            "name": {
                              "type": [
                                "string",
                                "null"
                              ],
                              "description": "This attribute can be null."
                            },
                            "status": {
                              "type": "string",
                              "description": "Information on the permission's status."
                            }
                          },
                          "required": [
                            "mrid",
                            "createdDateTime",
                            "description",
                            "type",
                            "reason",
                            "name",
                            "status"
                          ]
                        }
                      ]
                    }
                  },
                  "required": [
                    "mktActivityRecords"
                  ]
                },
                "timeSeriesList": {
                  "type": [
                    "object",
                    "null"
                  ],
                  "description": "A list of time series identifiers. This can be null.",
                  "properties": {
                    "timeSeries": {
                      "type": "array",
                      "description": "An array of time series identifiers associated with the consent market document.",
                      "items": [
                        {
                          "type": "object",
                          "properties": {
                            "mrid": {
                              "type": "string",
                              "description": "The identification of the timeSeries."
                            }
                          },
                          "required": [
                            "mrid"
                          ]
                        }
                      ]
                    }
                  }
                }
              },
              "required": [
                "permissionMRID",
                "createdDateTime",
                "transmissionSchedule",
                "marketEvaluationPointMRID",
                "reasonList",
                "mktActivityRecordList",
                "timeSeriesList"
              ]
            }
          ]
        }
      },
      "required": [
        "permissions"
      ]
    }
  },
  "required": [
    "mrid",
    "revisionNumber",
    "type",
    "createdDateTime",
    "description",
    "senderMarketParticipantMRID",
    "senderMarketParticipantMarketRoleType",
    "receiverMarketParticipantMRID",
    "receiverMarketParticipantMarketRoleType",
    "processProcessType",
    "periodTimeInterval",
    "permissionList"
  ]
}
```

## Possible values for CMD `type`

`type` is the coded type of document. The message type describes the principal characteristic of a document.

| Code | Description                                                         |
|------|---------------------------------------------------------------------|
| A01  | Balance responsible schedule                                        |
| A02  | Allocated capacity schedule                                         |
| A03  | Balance area schedule                                               |
| A04  | System Operator area schedule                                       |
| A05  | Control block area schedule                                         |
| A06  | Coordination center area schedule                                   |
| A07  | Intermediate confirmation report                                    |
| A08  | Final confirmation report                                           |
| A09  | Finalised schedule                                                  |
| A10  | Regulation data report                                              |
| A11  | Aggregated energy data report                                       |
| A12  | Imbalance report                                                    |
| A13  | Interconnection Capacity                                            |
| A14  | Resource Provider Resource Schedule                                 |
| A15  | Acquiring System Operator Reserve Schedule                          |
| A16  | Anomaly Report                                                      |
| A17  | Acknowledgement Document                                            |
| A18  | Confirmation report                                                 |
| A19  | Capacity for Resale                                                 |
| A20  | Approved Capacity Transfer                                          |
| A21  | Capacity transfer notification                                      |
| A22  | Transmission rights portfolio                                       |
| A23  | Allocations                                                         |
| A24  | Bid document                                                        |
| A25  | Allocation result document                                          |
| A26  | Capacity document                                                   |
| A27  | Rights document                                                     |
| A28  | Generation availability schedule                                    |
| A30  | Cross border schedule                                               |
| A31  | Agreed capacity                                                     |
| A32  | Proposed capacity                                                   |
| A33  | System vertical load                                                |
| A34  | Escalation document                                                 |
| A35  | Trouble shooting document                                           |
| A36  | Deactivation document                                               |
| A37  | Reserve tender document                                             |
| A38  | Reserve Allocation Result Document                                  |
| A39  | SATCR activation                                                    |
| A40  | DATCR activation                                                    |
| A41  | Activation response                                                 |
| A42  | Tender reduction                                                    |
| A43  | MOL Document                                                        |
| A44  | Price Document                                                      |
| A45  | Measurement Value Document                                          |
| A46  | SOAM Document                                                       |
| A47  | SOVA Document                                                       |
| A48  | CCVA Document                                                       |
| A49  | Daily settlement document                                           |
| A50  | Weekly settlement document                                          |
| A51  | Capacity Auction Specification Document                             |
| A52  | Market Coupling Results Document                                    |
| A53  | Outage publication Document                                         |
| A54  | Forced generation outage Document                                   |
| A55  | Summarised Market Schedule                                          |
| A56  | Compensation Program Schedule                                       |
| A57  | Load Frequency Control Program Schedule                             |
| A58  | Timeframe Independent Schedule                                      |
| A59  | Status request for a status within a process                        |
| A60  | status request for a position independently from a specific process |
| A61  | Estimated Net Transfer Capacity                                     |
| A62  | Compensation rights                                                 |
| A63  | Redispatch notice                                                   |
| A64  | Tender reduction response                                           |
| A65  | System total load                                                   |
| A66  | Final MOL                                                           |
| A67  | Resource Provider Schedule for production/consumption               |
| A68  | Installed generation per type                                       |
| A69  | Wind and solar forecast                                             |
| A70  | Load forecast margin                                                |
| A71  | Generation forecast                                                 |
| A72  | Reservoir filling information                                       |
| A73  | Actual generation                                                   |
| A74  | Wind and solar generation                                           |
| A75  | Actual generation per type                                          |
| A76  | Load unavailability                                                 |
| A77  | Production unavailability                                           |
| A78  | Transmission unavailability                                         |
| A79  | Offshore grid infrastructure unavailability                         |
| A80  | Generation unavailability                                           |
| A81  | Contracted reserves                                                 |
| A82  | Accepted offers                                                     |
| A83  | Activated balancing quantities                                      |
| A84  | Activated balancing prices                                          |
| A85  | Imbalance prices                                                    |
| A86  | Imbalance volume                                                    |
| A87  | Financial situation                                                 |
| A88  | Cross border balancing                                              |
| A89  | Contracted reserve prices                                           |
| A90  | Interconnection network expansion                                   |
| A91  | Counter trade notice                                                |
| A92  | Congestion costs                                                    |
| A93  | DC link capacity                                                    |
| A94  | Non EU allocations                                                  |
| A95  | Configuration document                                              |
| A96  | Redispatch activation document                                      |
| A97  | Detailed activation history document                                |
| A98  | Aggregated activation history document                              |
| A99  | HVDC Link constraints                                               |
| B01  | HVDC Configuration                                                  |
| B02  | HVDC Schedule                                                       |
| B03  | EIC code request                                                    |
| B04  | EIC code information                                                |
| B05  | EIC code publication                                                |
| B06  | Critical network element determination                              |
| B07  | Critical network element publication                                |
| B08  | Flow based domain                                                   |
| B09  | Flow based domain publication                                       |
| B10  | Flow based domain market impact publication                         |
| B11  | Anonymized flow based parameters publication                        |
| B12  | Critical network element market impact publication                  |
| B13  | Weather document                                                    |
| B14  | Energy prognosis document                                           |
| B15  | Network constraint document                                         |
| B16  | Aggregated netted external market schedule document                 |
| B17  | Aggregated netted external TSO schedule document                    |
| B18  | Reporting status market document                                    |
| B19  | Reporting information market document                               |
| B20  | Status request for a reporting information market document          |
| B21  | Reserve need document                                               |
| B22  | Generation and load shift keys document                             |
| B23  | Offers to be activated                                              |
| B24  | Clearing price                                                      |
| B25  | Security analysis report                                            |
| B26  | Aggregated netted external schedule document                        |
| B27  | External TSO schedule                                               |
| B28  | Move of scheduled production                                        |
| B29  | PS&LC results document                                              |
| B30  | Notification data market document                                   |
| B31  | Additional Constraint document                                      |
| B32  | Operational state document                                          |
| B33  | Published offered capacity                                          |
| B34  | Market result document                                              |
| Z04  | Permission administration document                                  |
| Z01  | Permission termination document                                     |

## Possible values for `codingScheme`

> Can be applied for the following attributes of the schema
> - `senderMarketParticipantMRID`:`codingScheme`
> - `receiverMarketParticipantMRID`:`codingScheme`
> - `permissionList`:`permissions`:`marketEvaluationPointMRID`:`codingScheme`

The value of `codingScheme` is the codification scheme used to identify the coding scheme used for the set of coded
values to identify specific objects.

| code | description                                   |
|------|-----------------------------------------------|
| A01  | EIC                                           |
| A10  | GS1                                           |
| NAD  | Andorra National coding scheme                |
| NAL  | Albania National coding scheme                |
| NAM  | Armenia National coding scheme                |
| NAT  | Austria National coding scheme                |
| NAZ  | Azerbaijan National coding scheme             |
| NBA  | Bosnia and Herzegovina National coding scheme |
| NBE  | Belgium National coding scheme                |
| NBG  | Bulgaria National coding scheme               |
| NCH  | Switzerland National coding scheme            |
| NCS  | Serbia and Montenegro National coding scheme  |
| NCZ  | Czech Republic National coding scheme         |
| NDE  | Germany National coding scheme                |
| NDK  | Denmark National coding scheme                |
| NEE  | Estonia National coding scheme                |
| NES  | Spain National coding scheme                  |
| NFI  | Finland National coding scheme                |
| NFR  | France National coding scheme                 |
| NGB  | United Kingdom National coding scheme         |
| NGE  | Georgia National coding scheme                |
| NGI  | Gibraltar National coding scheme              |
| NGR  | Greece National coding scheme                 |
| NHR  | Croatia National coding scheme                |
| NHU  | Hungary National coding scheme                |
| NIE  | Ireland National coding scheme                |
| NIT  | Italy National coding scheme                  |
| NKG  | Kyrgyzstan National coding scheme             |
| NKZ  | Kazakhstan National coding scheme             |
| NLI  | Liechtenstein National coding scheme          |
| NLT  | Lithuania National coding scheme              |
| NLU  | Luxembourg National coding scheme             |
| NLV  | Latvia National coding scheme                 |
| NMA  | Morocco National coding scheme                |
| NMD  | Moldavia National coding scheme               |
| NMK  | Macedonia National coding scheme              |
| NNL  | Netherlands National coding scheme            |
| NNN  | Nordic Regional coding scheme                 |
| NNO  | Norway National coding scheme                 |
| NPL  | Poland National coding scheme                 |
| NPT  | Portugal National coding scheme               |
| NRO  | Romania National coding scheme                |
| NRU  | Russian Federation National coding scheme     |
| NSE  | Sweden National coding scheme                 |
| NSI  | Slovenia National coding scheme               |
| NSK  | Slovakia National coding scheme               |
| NTR  | Turkey National coding scheme                 |
| NUA  | Ukraine National coding scheme                |
| A02  | CGM                                           |

## Possible values for `senderMarketParticipantMarketRoleType` & `receiverMarketParticipantMarketRoleType`

The values `senderMarketParticipantMarketRoleType` and `receiverMarketParticipantMarketRoleType` represent the
identification of the role played by a party.

| code | description                              |
|------|------------------------------------------|
| A01  | Trade responsible party                  |
| A02  | Consumption responsible party            |
| A03  | Combined power exchange (not to be used) |
| A04  | System operator                          |
| A05  | Imbalance settlement responsible         |
| A06  | Production responsible party             |
| A07  | Transmission capacity allocator          |
| A08  | Balance responsible party                |
| A09  | Metered data aggregator                  |
| A10  | Billing agent                            |
| A11  | Market operator                          |
| A12  | Balance supplier                         |
| A13  | Consumer                                 |
| A14  | Control area operator                    |
| A15  | Control block operator                   |
| A16  | Coordination center operator             |
| A17  | Grid access provider                     |
| A18  | Grid operator                            |
| A19  | Meter administrator                      |
| A20  | Party connected to grid                  |
| A21  | Producer                                 |
| A22  | Profile maintenance party                |
| A23  | Meter operator                           |
| A24  | Metered data collector                   |
| A25  | Metered data responsible                 |
| A26  | Metering point administrator             |
| A27  | Resource Provider                        |
| A28  | Scheduling coordinator                   |
| A29  | Capacity Trader                          |
| A30  | Interconnection Trade Responsible        |
| A31  | Nomination Validator                     |
| A32  | Market information aggregator            |
| A33  | Information receiver                     |
| A34  | Reserve Allocator                        |
| A35  | MOL Responsible                          |
| A36  | Capacity Coordinator                     |
| A37  | Reconciliation Accountable               |
| A38  | Reconciliation Responsible               |
| A39  | Data provider                            |
| A40  | Local Issuing Office (LIO)               |
| A41  | Central Issuing Office (CIO)             |
| A42  | EIC Participant                          |
| A43  | Weather analyser                         |
| A44  | Regional Security Coordinator (RSC)      |
| A45  | Energy Service Company (ESCO)            |
| A46  | Balancing Service Provider               |
| Axx  | Competent authority                      |
| Axx  | Identity service provider                |
| A50  | Permission administrator                 |

## Possible values for `processProcessType`

The value `processProcessType` indicates the nature of process that the document addresses.

| code | description                               |
|------|-------------------------------------------|
| A01  | Day ahead                                 |
| A02  | Intra day incremental                     |
| A03  | Inter-area transit                        |
| A04  | System operation closure                  |
| A05  | Metered data aggregation                  |
| A06  | Imbalance settlement                      |
| A07  | Capacity allocation                       |
| A08  | Central reconciliation                    |
| A09  | Released capacity allocation              |
| A10  | Proposed capacity allocation              |
| A11  | Agreed capacity allocation                |
| A12  | Long term                                 |
| A13  | Post scheduling adjustment                |
| A14  | Forecast                                  |
| A15  | Capacity determination                    |
| A16  | Realised                                  |
| A17  | Schedule day                              |
| A18  | Intraday total                            |
| A19  | Intraday accumulated                      |
| A20  | SOMA process                              |
| A21  | SOVM process                              |
| A22  | RGCE accounting process                   |
| A23  | CCSR RGCE Settlement                      |
| A24  | CBSR Settlement                           |
| A25  | CASR Settlement                           |
| A26  | Outage information                        |
| A27  | Reserve resource process                  |
| A28  | Primary reserve process                   |
| A29  | Secondary reserve process                 |
| A30  | Tertiary reserve process                  |
| A31  | Week ahead                                |
| A32  | Month ahead                               |
| A33  | Year ahead                                |
| A34  | Contracted                                |
| A35  | Network information                       |
| A36  | Creation                                  |
| A37  | Modification                              |
| A38  | Deactivation process                      |
| A39  | Synchronisation process                   |
| A40  | Intraday process                          |
| A41  | Redispatch process                        |
| A42  | Activation history process                |
| A43  | Flow based domain constraint day-ahead    |
| A44  | Flow based domain constraint intraday     |
| A45  | Two days ahead                            |
| A46  | Replacement reserve                       |
| A47  | Manual frequency restoration reserve      |
| A48  | Day-ahead capacity determination          |
| A49  | Intraday capacity determination           |
| A50  | Long term capacity determination          |
| A51  | Automatic frequency restoration reserve   |
| A52  | Frequency containment reserve             |
| A53  | Common Grid Model (CGM) merging process   |
| A54  | Coordinated operational security analysis |
| A55  | Access to metered data                    |

## Possible values for `status`

The attribute `status` describes the current status of a permission request.
For EDDIE, this will most likely only range from **Code A100-A112** which are further explained in
the [permission state](../PERMISSION_STATES.md) documentation.

| code | description                                      |
|------|--------------------------------------------------|
| A01  | Intermediate                                     |
| A02  | Final                                            |
| A03  | Deactivated                                      |
| A04  | Reactivated                                      |
| A05  | Active                                           |
| A06  | Available                                        |
| A07  | Activated                                        |
| A08  | In process                                       |
| A09  | Cancelled                                        |
| A10  | Ordered                                          |
| A11  | No longer available                              |
| A12  | RGCE agreed                                      |
| A13  | Withdrawn                                        |
| A14  | Creation                                         |
| A15  | Update                                           |
| A16  | Deactivation                                     |
| A17  | Reactivation                                     |
| A18  | Preventive                                       |
| A19  | Curative                                         |
| A20  | Automatic                                        |
| A21  | Open                                             |
| A22  | Close                                            |
| A23  | Stop                                             |
| A24  | Start                                            |
| A25  | Relative                                         |
| A26  | Absolute                                         |
| A27  | Curative or preventive                           |
| A28  | Unshared bid                                     |
| A29  | Pre Processed                                    |
| A30  | Substituted                                      |
| A31  | Modified                                         |
| A32  | Result                                           |
| A33  | Not satisfied                                    |
| A34  | Rejected                                         |
| A35  | Preliminary                                      |
| A36  | Planned                                          |
| A37  | Confirmed                                        |
| A38  | Shall Be Used                                    |
| A39  | Could Be Used                                    |
| A40  | Proposed                                         |
| A41  | Individual Network Data                          |
| A42  | Common Network Data                              |
| A43  | Setpoint schedule                                |
| A44  | Proportional external signal                     |
| A45  | AC emulation                                     |
| A46  | Importing element                                |
| A47  | Exporting element                                |
| A48  | To be optimized                                  |
| A49  | To be monitored                                  |
| A50  | To be included in capacity calculation           |
| A51  | Relative to previous point in time               |
| A52  | For flow optimization                            |
| A53  | For voltage optimization                         |
| A54  | Presolved                                        |
| A100 | VALIDATED                                        |
| A101 | MALFORMED                                        |
| A102 | UNABLE_TO_SEND                                   |
| A103 | RECEIVED_PERMISSION_ADMINISTRATOR_RESPONSE       |
| A104 | PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT |
| A105 | SENT_TO_PERMISSION_ADMINISTRATOR                 |
| A106 | TIMED_OUT                                        |
| A107 | ACCEPTED                                         |
| A108 | REVOKED                                          |
| A109 | TERMINATED                                       |
| A110 | FULFILLED                                        |
| A111 | INVALID                                          |
| A112 | CREATED                                          |

## Possible values for `code`

> This refers to `permissionList`:`permissions`:`reasonList`:`reasons`:`code`
>

The attribute `code` describes coded reason why a status of a permission changed.
For EDDIE, this will most likely only range from **Code Z01-Z03** which are further explained in
the [permission state](../PERMISSION_STATES.md) documentation.

| Code | Description                                                                                                   |
|------|---------------------------------------------------------------------------------------------------------------|
| 999  | Errors not specifically identified                                                                            |
| A01  | Message fully accepted                                                                                        |
| A02  | Message fully rejected                                                                                        |
| A03  | Message contains errors at the time series level                                                              |
| A04  | Time interval incorrect                                                                                       |
| A05  | Sender without valid contract                                                                                 |
| A06  | Schedule accepted                                                                                             |
| A07  | Schedule partially accepted                                                                                   |
| A08  | Schedule rejected                                                                                             |
| A09  | Time series not matching                                                                                      |
| A10  | Credit limit exceeded                                                                                         |
| A20  | Time series fully rejected                                                                                    |
| A21  | Time series accepted with specific time interval errors                                                       |
| A22  | In party/Out party invalid                                                                                    |
| A23  | Area invalid                                                                                                  |
| A24  | A24 not applicable                                                                                            |
| A25  | A25 not applicable                                                                                            |
| A26  | Default time series applied                                                                                   |
| A27  | Cross border capacity exceeded                                                                                |
| A28  | Counterpart time series missing                                                                               |
| A29  | Counterpart time series quantity differences                                                                  |
| A30  | Imposed Time series from nominated party's time series (party identified in reason text)                      |
| A41  | Resolution inconsistency                                                                                      |
| A42  | Quantity inconsistency                                                                                        |
| A43  | Quantity increased                                                                                            |
| A44  | Quantity decreased                                                                                            |
| A45  | Default quantity applied                                                                                      |
| A46  | Quantities must not be signed values                                                                          |
| A47  | A47 not applicable                                                                                            |
| A48  | Modification reason                                                                                           |
| A49  | Position inconsistency                                                                                        |
| A50  | Senders time series version conflict                                                                          |
| A51  | Message identification or version conflict                                                                    |
| A52  | Time series missing from new version of message                                                               |
| A53  | Receiving party incorrect                                                                                     |
| A54  | Global position not in balance                                                                                |
| A55  | Time series identification conflict                                                                           |
| A56  | Corresponding Time series not netted                                                                          |
| A57  | Deadline limit exceeded/Gate not open                                                                         |
| A58  | One to one nomination inconsistency                                                                           |
| A59  | Not compliant to local market rules                                                                           |
| A60  | Inter-area transit schedule exceeds nominated schedule                                                        |
| A61  | Currency invalid                                                                                              |
| A62  | Invalid business type                                                                                         |
| A63  | Time Series modified                                                                                          |
| A64  | Resource Object Invalid                                                                                       |
| A65  | Reserve object Technical limits exceeded                                                                      |
| A66  | Planned reserves do not correspond with contractual data                                                      |
| A67  | Limit Data is not available                                                                                   |
| A68  | Reserve Object not qualified for reserve type                                                                 |
| A69  | Mandatory attributes missing                                                                                  |
| A70  | Curtailment                                                                                                   |
| A71  | Linked bid rejected due to associated bid unsuccessful                                                        |
| A72  | Original bid divided to permit acceptance                                                                     |
| A73  | Bid accepted                                                                                                  |
| A74  | Auction Status                                                                                                |
| A75  | Right status information                                                                                      |
| A76  | Agreement identification inconsistency                                                                        |
| A77  | Dependency matrix not respected                                                                               |
| A78  | Sender identification and/or role invalid                                                                     |
| A79  | Process type invalid                                                                                          |
| A80  | Domain invalid                                                                                                |
| A81  | Matching period invalid                                                                                       |
| A82  | In/Out area inconsistent with domain                                                                          |
| A83  | Disagree with matching results                                                                                |
| A84  | Confirmation ignored due to higher version already received                                                   |
| A85  | Confirmation without adjustment (time series have been matched without change)                                |
| A86  | Confirmation with adjustment (time series have been modified)                                                 |
| A87  | For action (only in intermediate confirmation - time series need mutual agreement and action)                 |
| A88  | Time series matched                                                                                           |
| A89  | Time series ignored (note: this can only apply to time series that are set to zero - see matching principles) |
| A90  | Modification proposal (intermediate confirmation)                                                             |
| A91  | Expected document not received                                                                                |
| A92  | Not possible to send document on time, but estimated delivery time is provided                                |
| A93  | Not possible to send document on time, and furthermore no expected time of return to normal situation         |
| A94  | Document cannot be processed by receiving system                                                              |
| A95  | Complementary information                                                                                     |
| A96  | Technical constraint                                                                                          |
| A97  | Force majeure curtailment                                                                                     |
| A98  | Network security curtailment                                                                                  |
| A99  | Auction cancelled                                                                                             |
| B01  | Incomplete document                                                                                           |
| B02  | Accounting Point (tie-line) Time Series missing                                                               |
| B03  | Meter data Time series missing                                                                                |
| B04  | Estimated values not allowed in first transmission                                                            |
| B05  | No quantity values allowed for a quality that is not available                                                |
| B06  | Time series accepted                                                                                          |
| B07  | Auction without bids being entered                                                                            |
| B08  | Data not yet available                                                                                        |
| B09  | Bid not accepted                                                                                              |
| B10  | Initiator area problem                                                                                        |
| B11  | Cooperating area problem                                                                                      |
| B12  | Communication status currently active                                                                         |
| B13  | Communication status currently inactive                                                                       |
| B14  | Communication status currently restricted                                                                     |
| B15  | Problem associated with both areas                                                                            |
| B16  | Tender unavailable in MOL list                                                                                |
| B17  | Price based on preliminary exchange rate                                                                      |
| B18  | Failure                                                                                                       |
| B19  | Foreseen maintenance                                                                                          |
| B20  | Shutdown                                                                                                      |
| B21  | Official exchange rate approved                                                                               |
| B22  | System regulation                                                                                             |
| B23  | Frequency regulation                                                                                          |
| B24  | Load flow overload                                                                                            |
| B25  | Voltage level adjustment                                                                                      |
| B26  | Emergency situation curtailment                                                                               |
| B27  | Calculation process failed                                                                                    |
| B28  | No capacity constraint impact on the market                                                                   |
| B29  | Special Condition                                                                                             |
| B30  | Unverified                                                                                                    |
| B31  | Verified                                                                                                      |
| B32  | CGM inconsistency                                                                                             |
| B33  | Network dictionary inconsistency                                                                              |
| B34  | Capacity reduced by TSO                                                                                       |
| B35  | Overload                                                                                                      |
| B36  | GLSK limitation                                                                                               |
| B37  | Voltage constraint                                                                                            |
| B38  | Angle constraint                                                                                              |
| B39  | Stability                                                                                                     |
| B40  | Loadflow divergence                                                                                           |
| B41  | Exclusion for SoS reasons                                                                                     |
| B42  | Constraint by the market                                                                                      |
| B43  | Ordinary                                                                                                      |
| B44  | Exceptional                                                                                                   |
| B45  | Out of range                                                                                                  |
| B46  | Internal congestion                                                                                           |
| B47  | Operational security constrainsts                                                                             |
| Z01  | FULFILLED                                                                                                     |
| Z02  | CANCELLED_USER                                                                                                |
| Z03  | CANCELLED_EP                                                                                                  |
| Z0$  | Other                                                                                                         |
