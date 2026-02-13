// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.datasource.mqtt.inbound;

import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceIcon;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.api.agnostic.aiida.AiidaAsset;

import java.util.UUID;

public class InboundDataSourceDto extends DataSourceDto {
    public InboundDataSourceDto(
            AiidaAsset asset,
            UUID permissionId
    ) {
        this.type = DataSourceType.INBOUND;
        this.name = permissionId.toString();
        this.asset = asset;
        this.enabled = true;
        this.countryCode = "AT";
        this.icon = DataSourceIcon.ELECTRICITY;
    }
}
