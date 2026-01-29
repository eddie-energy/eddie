// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.formatter;

public abstract class FormatterException extends RuntimeException {
    protected FormatterException(Exception exception) {
        super(exception);
    }
}
