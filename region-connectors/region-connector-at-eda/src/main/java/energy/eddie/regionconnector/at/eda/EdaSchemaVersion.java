// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda;

public enum EdaSchemaVersion {
    CM_REQUEST_01_10("01.10");

    private final String value;

    EdaSchemaVersion(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
