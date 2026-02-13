// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.formatter;

public class CimSchemaFormatterException extends SchemaFormatterException {
    public CimSchemaFormatterException(Exception exception) {
        super(exception);
    }
}
