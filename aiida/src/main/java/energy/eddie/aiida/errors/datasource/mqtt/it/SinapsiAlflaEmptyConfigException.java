// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource.mqtt.it;

public class SinapsiAlflaEmptyConfigException extends Exception {
    public SinapsiAlflaEmptyConfigException() {
        super("Sinapsi Alfa credentials are not configured - cannot proceed without them.”");
    }
}
