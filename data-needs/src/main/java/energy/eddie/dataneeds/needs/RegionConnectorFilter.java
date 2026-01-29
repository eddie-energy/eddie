// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "region_connector_filter", schema = "data_needs")
@Schema(description = "Can be used to restrict which region connectors can process a data need. The list can be used as a blocklist or allowlist.")
public final class RegionConnectorFilter {
    @Id
    @Column(name = "data_need_id")
    @JsonIgnore
    @SuppressWarnings("NullAway.Init")
    private String dataNeedId;

    @JsonProperty(required = true, value = "type")
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "If set to 'blocklist', the region connectors in the list will not be able to process the data need. If set to 'allowlist', only the region connectors in the list will be able to process the data need.")
    private Type type;

    @JsonProperty(required = true, value = "regionConnectorIds")
    @Column(name = "rc_id", nullable = false)
    @CollectionTable(name = "region_connector_filter_ids", joinColumns = @JoinColumn(name = "data_need_id"), schema = "data_needs")
    @ElementCollection
    @Schema(description = "List of region connector IDs that should be blocked or allowed. IDs are unique and can be found in the region connector's metadata. Typically, the region connector ID is the country code + '-' + a handle for the data provider, e.g. 'at-eda', 'fr-enedis', 'es-datadis', 'dk-energinet', 'fi-finngrid', 'us-green-button',...")
    private List<String> regionConnectorIds = new ArrayList<>();

    public RegionConnectorFilter(
            Type type, List<String> regionConnectorIds
    ) {
        this.type = type;
        this.regionConnectorIds = regionConnectorIds;
    }

    @SuppressWarnings("NullAway.Init")
    public RegionConnectorFilter() {

    }

    public Type type() {return type;}

    public List<String> regionConnectorIds() {return regionConnectorIds;}

    /**
     * Returns the ID of the data need with which this {@link RegionConnectorFilter} is associated.
     */
    public String dataNeedId() {
        return dataNeedId;
    }

    /**
     * Sets the id of the data need with which this {@link RegionConnectorFilter} should be associated.
     */
    public void setDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
    }

    public enum Type {
        @JsonProperty("blocklist")
        BLOCKLIST,
        @JsonProperty("allowlist")
        ALLOWLIST
    }
}
