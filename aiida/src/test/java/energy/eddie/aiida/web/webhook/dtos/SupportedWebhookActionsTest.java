// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web.webhook.dtos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupportedWebhookActionsTest {
    @Test
    void testFromValue_ValidValues() {
        assertEquals(SupportedWebhookActions.CLIENT_DISCONNECTED,
                     SupportedWebhookActions.fromValue("client_disconnected"));
        assertEquals(SupportedWebhookActions.CLIENT_CONNACK, SupportedWebhookActions.fromValue("client_connack"));
    }

    @Test
    void testFromValue_InvalidValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            SupportedWebhookActions.fromValue("invalid_action");
        });
        assertEquals("Invalid action value: invalid_action", exception.getMessage());
    }

    @Test
    void testGetAction() {
        assertEquals("client_disconnected", SupportedWebhookActions.CLIENT_DISCONNECTED.getAction());
        assertEquals("client_connack", SupportedWebhookActions.CLIENT_CONNACK.getAction());
    }
}
