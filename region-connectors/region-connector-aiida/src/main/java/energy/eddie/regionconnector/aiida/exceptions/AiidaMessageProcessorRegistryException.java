// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.exceptions;

public class AiidaMessageProcessorRegistryException extends Exception {
    public AiidaMessageProcessorRegistryException(String topic) {
        super("No AiidaMessageProcessor found for topic " + topic);
    }
}
