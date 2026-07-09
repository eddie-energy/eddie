// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.secrets;

import java.util.Locale;

public enum SecretType {
    PASSWORD,
    API_KEY;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
