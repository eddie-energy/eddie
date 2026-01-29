// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.datasource.mqtt.it;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;

@SuppressWarnings({"NullAway.Init"})
public class SinapsiAlfaDataSourceDto extends DataSourceDto {
    @JsonProperty
    protected String activationKey;

    public String activationKey() {
        return activationKey;
    }
}
