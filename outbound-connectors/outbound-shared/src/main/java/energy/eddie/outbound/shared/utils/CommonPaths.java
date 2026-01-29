// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.shared.utils;

public class CommonPaths {

    private CommonPaths() {}

    public static String getServletPathForOutboundConnector(String outboundConnectorId) {
        return "/outbound-connectors/" + outboundConnectorId;
    }
}
