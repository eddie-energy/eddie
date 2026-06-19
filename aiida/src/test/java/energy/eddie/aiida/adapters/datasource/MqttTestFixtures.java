// SPDX-FileCopyrightText: 2026 The EDDIE Developers
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource;

public final class MqttTestFixtures {
    public static final String SHELLY_PLUG_GEN3_SWITCH_PAYLOAD = """
            {
              \"src\": \"shelly1pmg4-8813bfe14804\",
              \"dst\": \"aiida/17a019b1-253b-40de-8800-d577e4e96181/events\",
              \"method\": \"NotifyStatus\",
              \"params\": {
                \"ts\": 1757591520.01,
                \"switch:0\": {
                  \"aenergy\": {
                    \"by_minute\": [
                      0,
                      0,
                      0
                    ],
                    \"minute_ts\": 1757591520,
                    \"total\": 1235.456
                  },
                  \"apower\": 35,
                  \"current\": 77,
                  \"freq\": 50.01,
                  \"id\": 0,
                  \"output\": true,
                  \"ret_aenergy\": {
                    \"by_minute\": [
                      312.345,
                      23.456,
                      356.789
                    ],
                    \"minute_ts\": 1757591520,
                    \"total\": 234.567
                  },
                  \"voltage\": 228.5
                }
              }
            }
            """;

    private MqttTestFixtures() {
    }
}
