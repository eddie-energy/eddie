// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission.dataneed;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;

/**
 * Real local data needs for tests that need a serializable data need or a direction specific permission.
 */
public final class LocalDataNeedTestData {
    private static final String INBOUND_DATA_NEED_JSON = """
            {
              "type": "inbound-aiida",
              "id": "f7698978-b9fe-40c8-aebe-c997f7f58f2f",
              "name": "FUTURE_MIN_MAX_ENVELOPE_INBOUND",
              "description": "Inbound min-max envelopes",
              "purpose": "purpose",
              "policyLink": "https://example.com/toc",
              "enabled": true,
              "duration": {"type": "relativeDuration", "start": "P0D", "end": "P10D"},
              "transmissionSchedule": "*/5 * * * * *",
              "acknowledgementRequired": true,
              "allowedPermissionCommands": ["UPDATE_LIMIT_DEFAULTS"],
              "asset": "CONNECTION-AGREEMENT-POINT",
              "schemas": ["OPAQUE", "MIN-MAX-ENVELOPE-CIM-V1-12"]
            }
            """;
    private static final String OUTBOUND_DATA_NEED_JSON = """
            {
              "type": "outbound-aiida",
              "id": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
              "name": "FUTURE_NEAR_REALTIME_DATA_OUTBOUND",
              "description": "Outbound near real-time data",
              "purpose": "purpose",
              "policyLink": "https://example.com/toc",
              "enabled": true,
              "duration": {"type": "relativeDuration", "start": "P0D", "end": "P10D"},
              "transmissionSchedule": "*/5 * * * * *",
              "acknowledgementRequired": false,
              "allowedPermissionCommands": [],
              "asset": "CONNECTION-AGREEMENT-POINT",
              "schemas": ["SMART-METER-P1-RAW"],
              "dataTags": ["1-0:1.8.0"]
            }
            """;

    private LocalDataNeedTestData() {}

    public static InboundAiidaLocalDataNeed inboundDataNeed() {
        return (InboundAiidaLocalDataNeed) AiidaLocalDataNeedFactory.create(parse(INBOUND_DATA_NEED_JSON));
    }

    public static OutboundAiidaLocalDataNeed outboundDataNeed() {
        return (OutboundAiidaLocalDataNeed) AiidaLocalDataNeedFactory.create(parse(OUTBOUND_DATA_NEED_JSON));
    }

    private static AiidaDataNeed parse(String json) {
        return ObjectMapperCreatorUtil.mapper().readValue(json, AiidaDataNeed.class);
    }
}
