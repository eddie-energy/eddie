// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.image;

import energy.eddie.aiida.models.datasource.DataSource;

public class ImageNotFoundException extends Exception {
    public ImageNotFoundException(DataSource dataSource) {
        super("No image found for data source with ID %s".formatted(dataSource.id()));
    }
}
