// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web.webhook.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SupportedWebhookActions {
    CLIENT_DISCONNECTED("client_disconnected"),
    CLIENT_CONNACK("client_connack");

    private final String action;

    SupportedWebhookActions(String action) {
        this.action = action;
    }

    @JsonCreator
    public static SupportedWebhookActions fromValue(String value) {
        for (SupportedWebhookActions action : SupportedWebhookActions.values()) {
            if (action.getAction().equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Invalid action value: " + value);
    }

    @JsonValue
    public String getAction() {
        return action;
    }
}
