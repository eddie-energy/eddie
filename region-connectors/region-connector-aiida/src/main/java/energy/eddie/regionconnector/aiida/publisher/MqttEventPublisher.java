// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.publisher;

import energy.eddie.regionconnector.aiida.mqtt.events.MqttEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MqttEventPublisher implements EventPublisher<MqttEvent> {
    private final ApplicationEventPublisher publisher;

    protected MqttEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishEvent(MqttEvent event) {
        publisher.publishEvent(event);
    }
}
