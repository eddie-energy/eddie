// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

public class MissingInboundMessageFormatException extends Exception {
    public MissingInboundMessageFormatException() {
        super("inboundMessageFormat must not be null when operation is UPDATE_INBOUND_MESSAGE_FORMAT.");
    }
}
