// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.api;

public enum Pages {
    SLURP(true), NO_SLURP(false);
    private final boolean value;

    Pages(boolean value) {
        this.value = value;
    }

    public boolean value() {
        return value;
    }
}
