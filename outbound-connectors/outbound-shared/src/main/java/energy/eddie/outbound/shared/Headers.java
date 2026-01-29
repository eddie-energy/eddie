// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.shared;

/**
 * Headers that can be used by outbound-connectors.
 * For example, as HTTP headers or AMQP 1.0 properties.
 */
public class Headers {
    /**
     * The ID of the permission request that is related to the message.
     */
    public static final String PERMISSION_ID = "permission-id";
    /**
     * The connection ID of the permission request that is related to the message.
     */
    public static final String CONNECTION_ID = "connection-id";
    /**
     * The data need ID of the data need that is related to the message.
     */
    public static final String DATA_NEED_ID = "data-need-id";

    private Headers() {
        // Utility Class
    }
}
