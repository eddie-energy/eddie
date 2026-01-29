// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource;

import java.util.UUID;

public class DataSourceNotFoundException extends Exception {
    public DataSourceNotFoundException(UUID dataSourceId) {
        super("Data source not found with ID :%s".formatted(dataSourceId));
    }
}
