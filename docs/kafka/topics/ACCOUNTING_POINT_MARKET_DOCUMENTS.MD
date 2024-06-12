# Topic: Accounting Point Market Documents

The topic **accounting-point-market-documents** delivers account point data in a CIM-compliant format.
For possible values see
the [AccountingPointMarketDocument.xsd](../../../api/src/main/schemas/cim/xsd/v0_82/ap/AccountingPoint_MarketDocument_2024-04-26T17.01.04.xsd)
file.

```json
{
  "$schema": "https://json-schema.org/draft-04/schema#",
  "id": "http://www.eddie.energy/AP/EDD04/20240422#",
  "definitions": {
    "PartyID_StringComplexType": {
      "type": "object",
      "required": [
        "codingScheme",
        "value"
      ],
      "properties": {
        "codingScheme": {
          "$ref": "#/definitions/CodingSchemeTypeList"
        },
        "value": {
          "type": "string"
        }
      }
    },
    "MeasurementPointID_StringComplexType": {
      "type": "object",
      "required": [
        "codingScheme",
        "value"
      ],
      "properties": {
        "codingScheme": {
          "$ref": "#/definitions/CodingSchemeTypeList"
        },
        "value": {
          "type": "string"
        }
      }
    },
    "AddressComplexType": {
      "type": "object",
      "required": [
        "addressRole",
        "cityName",
        "streetName"
      ],
      "properties": {
        "addressRole": {
          "$ref": "#/definitions/addressRoleType"
        },
        "postalCode": {
          "type": "string"
        },
        "cityName": {
          "type": "string"
        },
        "streetName": {
          "type": "string"
        },
        "buildingNumber": {
          "type": "string"
        },
        "staircaseNumber": {
          "type": "string"
        },
        "floorNumber": {
          "type": "string"
        },
        "doorNumber": {
          "type": "string"
        },
        "addressSuffix": {
          "type": "string"
        }
      }
    },
    "AccountingPointComplexType.AddressList": {
      "type": "object",
      "properties": {
        "Address": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/AddressComplexType"
          },
          "minItems": 0
        }
      }
    },
    "BillingDataComplexType": {
      "type": "object",
      "properties": {
        "referenceNumber": {
          "type": "string"
        },
        "gridAgreementTypeDescription": {
          "type": "string"
        },
        "budgetBillingCycle": {
          "type": "string"
        },
        "consumptionBillingCycle": {
          "type": "string"
        },
        "meterReadingMonth": {
          "type": "integer"
        },
        "consumptionBillingMonth": {
          "type": "integer"
        },
        "yearMonthOfNextBill": {
          "type": "string"
        }
      }
    },
    "ContractPartyComplexType": {
      "type": "object",
      "required": [
        "contractPartyRole"
      ],
      "properties": {
        "contractPartyRole": {
          "$ref": "#/definitions/contractPartyRoleType"
        },
        "salutation": {
          "type": "string"
        },
        "surName": {
          "type": "string"
        },
        "firstName": {
          "type": "string"
        },
        "companyName": {
          "type": "string"
        },
        "Identification": {
          "type": "string"
        },
        "dateOfBirth": {
          "type": "string"
        },
        "dateOfDeath": {
          "type": "string"
        },
        "companyRegisterNumber": {
          "type": "string"
        },
        "VATnumber": {
          "type": "string"
        },
        "email": {
          "type": "string"
        }
      }
    },
    "AccountingPointComplexType.ContractPartyList": {
      "type": "object",
      "properties": {
        "ContractParty": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/ContractPartyComplexType"
          },
          "minItems": 0
        }
      }
    },
    "AccountingPointComplexType": {
      "type": "object",
      "required": [
        "mRID",
        "AddressList",
        "ContractPartyList"
      ],
      "properties": {
        "settlementMethod": {
          "type": "string"
        },
        "mRID": {
          "$ref": "#/definitions/MeasurementPointID_StringComplexType"
        },
        "meterReadingResolution": {
          "type": "string"
        },
        "gridAgreementType": {
          "type": "string"
        },
        "name": {
          "type": "string"
        },
        "administrativeStatus": {
          "type": "string"
        },
        "flexibilityContract": {
          "type": "boolean"
        },
        "resolution": {
          "type": "string"
        },
        "commodity": {
          "type": "string",
          "enum": [
            "0",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "10",
            "11",
            "12",
            "13",
            "14",
            "15",
            "16",
            "17",
            "18",
            "19",
            "20",
            "21",
            "22",
            "23",
            "24",
            "25"
          ]
        },
        "energyCommunity": {
          "type": "string"
        },
        "direction": {
          "$ref": "#/definitions/DirectionTypeList"
        },
        "generationType": {
          "type": "string"
        },
        "loadProfileType": {
          "type": "string"
        },
        "supplyStatus": {
          "type": "string"
        },
        "tariffClassDSO": {
          "type": "string"
        },
        "AddressList": {
          "$ref": "#/definitions/AccountingPointComplexType.AddressList"
        },
        "BillingData": {
          "$ref": "#/definitions/BillingDataComplexType"
        },
        "ContractPartyList": {
          "$ref": "#/definitions/AccountingPointComplexType.ContractPartyList"
        }
      }
    },
    "AccountingPoint_MarketDocumentComplexType.AccountingPointList": {
      "type": "object",
      "properties": {
        "AccountingPoint": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/AccountingPointComplexType"
          },
          "minItems": 0
        }
      }
    },
    "AccountingPoint_MarketDocumentComplexType": {
      "type": "object",
      "required": [
        "mRID",
        "revisionNumber",
        "type",
        "createdDateTime",
        "sender_MarketParticipantmRID",
        "sender_MarketParticipantmarketRoletype",
        "receiver_MarketParticipantmRID",
        "receiver_MarketParticipantmarketRoletype",
        "AccountingPointList"
      ],
      "properties": {
        "mRID": {
          "type": "string"
        },
        "revisionNumber": {
          "type": "string"
        },
        "type": {
          "$ref": "#/definitions/MessageTypeList"
        },
        "createdDateTime": {
          "type": "string"
        },
        "description": {
          "type": "string"
        },
        "sender_MarketParticipantmRID": {
          "$ref": "#/definitions/PartyID_StringComplexType"
        },
        "sender_MarketParticipantmarketRoletype": {
          "$ref": "#/definitions/RoleTypeList"
        },
        "receiver_MarketParticipantmRID": {
          "$ref": "#/definitions/PartyID_StringComplexType"
        },
        "receiver_MarketParticipantmarketRoletype": {
          "$ref": "#/definitions/RoleTypeList"
        },
        "AccountingPointList": {
          "$ref": "#/definitions/AccountingPoint_MarketDocumentComplexType.AccountingPointList"
        }
      }
    },
    "MessageTypeList": {
      "type": "string",
      "enum": [
        "A01",
        "A02",
        "A03",
        "A04",
        "A05",
        "A06",
        "A07",
        "A08",
        "A09",
        "A10",
        "A11",
        "A12",
        "A13",
        "A14",
        "A15",
        "A16",
        "A17",
        "A18",
        "A19",
        "A20",
        "A21",
        "A22",
        "A23",
        "A24",
        "A25",
        "A26",
        "A27",
        "A28",
        "A30",
        "A31",
        "A32",
        "A33",
        "A34",
        "A35",
        "A36",
        "A37",
        "A38",
        "A39",
        "A40",
        "A41",
        "A42",
        "A43",
        "A44",
        "A45",
        "A46",
        "A47",
        "A48",
        "A49",
        "A50",
        "A51",
        "A52",
        "A53",
        "A54",
        "A55",
        "A56",
        "A57",
        "A58",
        "A59",
        "A60",
        "A61",
        "A62",
        "A63",
        "A64",
        "A65",
        "A66",
        "A67",
        "A68",
        "A69",
        "A70",
        "A71",
        "A72",
        "A73",
        "A74",
        "A75",
        "A76",
        "A77",
        "A78",
        "A79",
        "A80",
        "A81",
        "A82",
        "A83",
        "A84",
        "A85",
        "A86",
        "A87",
        "A88",
        "A89",
        "A90",
        "A91",
        "A92",
        "A93",
        "A94",
        "A95",
        "A96",
        "A97",
        "A98",
        "A99",
        "B01",
        "B02",
        "B03",
        "B04",
        "B05",
        "B06",
        "B07",
        "B08",
        "B09",
        "B10",
        "B11",
        "B12",
        "B13",
        "B14",
        "B15",
        "B16",
        "B17",
        "B18",
        "B19",
        "B20",
        "B21",
        "B22",
        "B23",
        "B24",
        "B25",
        "B26",
        "B27",
        "B28",
        "B29",
        "B30",
        "B31",
        "B32",
        "B33",
        "B34",
        "B99"
      ]
    },
    "CodingSchemeTypeList": {
      "type": "string",
      "enum": [
        "A01",
        "A10",
        "NAD",
        "NAL",
        "NAM",
        "NAT",
        "NAZ",
        "NBA",
        "NBE",
        "NBG",
        "NCH",
        "NCS",
        "NCZ",
        "NDE",
        "NDK",
        "NEE",
        "NES",
        "NFI",
        "NFR",
        "NGB",
        "NGE",
        "NGI",
        "NGR",
        "NHR",
        "NHU",
        "NIE",
        "NIT",
        "NKG",
        "NKZ",
        "NLI",
        "NLT",
        "NLU",
        "NLV",
        "NMA",
        "NMD",
        "NMK",
        "NNL",
        "NNN",
        "NNO",
        "NPL",
        "NPT",
        "NRO",
        "NRU",
        "NSE",
        "NSI",
        "NSK",
        "NTR",
        "NUA",
        "A02"
      ]
    },
    "RoleTypeList": {
      "type": "string",
      "enum": [
        "A01",
        "A02",
        "A03",
        "A04",
        "A05",
        "A06",
        "A07",
        "A08",
        "A09",
        "A10",
        "A11",
        "A12",
        "A13",
        "A14",
        "A15",
        "A16",
        "A17",
        "A18",
        "A19",
        "A20",
        "A21",
        "A22",
        "A23",
        "A24",
        "A25",
        "A26",
        "A27",
        "A28",
        "A29",
        "A30",
        "A31",
        "A32",
        "A33",
        "A34",
        "A35",
        "A36",
        "A37",
        "A38",
        "A39",
        "A40",
        "A41",
        "A42",
        "A43",
        "A44",
        "A45",
        "A46",
        " Axx",
        "A57"
      ]
    },
    "DirectionTypeList": {
      "type": "string",
      "enum": [
        "A01",
        "A02",
        "A03",
        "A04"
      ]
    },
    "addressRoleType": {
      "type": "string",
      "enum": [
        "delivery",
        "invoice"
      ]
    },
    "contractPartyRoleType": {
      "type": "string",
      "enum": [
        "contractPartner",
        "invoice"
      ]
    }
  },
  "properties": {
    "AccountingPoint_MarketDocument": {
      "$ref": "#/definitions/AccountingPoint_MarketDocumentComplexType"
    }
  }
}
```
