package energy.eddie.aiida.dtos.datasource.mqtt.inbound;

import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceIcon;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;

import java.util.UUID;

public class InboundDataSourceDto extends DataSourceDto {
    public InboundDataSourceDto(
            AiidaAsset asset,
            UUID permissionId
    ) {
        this.dataSourceType = DataSourceType.INBOUND;
        this.name = permissionId.toString();
        this.asset = asset;
        this.enabled = true;
        this.icon = DataSourceIcon.ELECTRICITY;
    }
}
