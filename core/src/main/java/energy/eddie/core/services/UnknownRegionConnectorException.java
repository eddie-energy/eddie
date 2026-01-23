// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

public class UnknownRegionConnectorException extends Exception {
    public UnknownRegionConnectorException(String regionConnectorId) {
        super("Unknown region connector: " + regionConnectorId);
    }
}
