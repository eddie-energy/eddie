// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.auth;

public class InvalidUserException extends Exception {
    public InvalidUserException() {
        super("Could not get UUID from current User!");
    }
}
