// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.exceptions;

public class CredentialsAlreadyExistException extends Exception {
    public CredentialsAlreadyExistException(String permissionId) {
        super("MQTT credentials for permission '%s' have already been created and fetched, and this is only permitted once".formatted(
                permissionId));
    }
}
