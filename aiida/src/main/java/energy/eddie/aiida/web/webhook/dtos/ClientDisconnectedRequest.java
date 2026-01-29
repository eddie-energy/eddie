// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web.webhook.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("NullAway")
public class ClientDisconnectedRequest extends WebhookRequest {
    @JsonProperty
    private String reason;

    public String reason() {
        return reason;
    }
}
