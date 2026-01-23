// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

public class DeserializationException extends Exception {
    public DeserializationException(Exception e) {
        super(e);
    }
}
