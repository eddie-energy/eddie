// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.fr.transformer;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MicroTeleinfoV3ModeNotSupportedException extends Exception {
    private final String payload;

    public MicroTeleinfoV3ModeNotSupportedException(byte[] payload, List<MicroTeleinfoV3Mode> mode) {
        super("Mode %s not applicable for the given payload.".formatted(mode));
        this.payload = new String(payload, StandardCharsets.UTF_8);
    }

    public String payload() {
        return payload;
    }
}
