// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

public class MissingDisplayNameException extends Exception {
    public MissingDisplayNameException() {
        super("displayName must not be blank when operation is UPDATE_DISPLAY_NAME.");
    }
}
