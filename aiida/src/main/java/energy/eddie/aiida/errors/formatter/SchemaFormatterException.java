// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.formatter;

public abstract class SchemaFormatterException extends Exception {
    protected SchemaFormatterException(Exception exception) {
        super(exception);
    }
}
