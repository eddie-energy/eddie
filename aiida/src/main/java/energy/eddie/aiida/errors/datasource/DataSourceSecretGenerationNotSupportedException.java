// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource;

import energy.eddie.aiida.models.datasource.DataSourceType;

public class DataSourceSecretGenerationNotSupportedException extends Exception {
    public DataSourceSecretGenerationNotSupportedException(DataSourceType dataSourceType) {
        super("Regenerating secrets is not supported for this data source. (%s)".formatted(dataSourceType));
    }
}
