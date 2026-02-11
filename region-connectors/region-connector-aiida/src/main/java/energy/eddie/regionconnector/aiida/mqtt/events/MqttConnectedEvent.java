// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.events;

public class MqttConnectedEvent implements MqttEvent {
    private final String serverUri;

    public MqttConnectedEvent(String serverUri) {
        this.serverUri = serverUri;
    }

    public String getServerUri() {
        return serverUri;
    }
}
