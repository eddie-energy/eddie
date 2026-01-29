// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource;

public class InvalidDataSourceTypeException extends Exception {
    public InvalidDataSourceTypeException() {
        super("Invalid data source type.");
    }
}
