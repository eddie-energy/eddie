// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Accepted event for ValidatedHistoricalDataNeeds
 */
@Entity
@SuppressWarnings({"NullAway", "unused"})
public class EsAcceptedEventForVHD extends PersistablePermissionEvent {
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final DistributorCode distributorCode;
    private final Integer supplyPointType;
    private final boolean productionSupport;

    public EsAcceptedEventForVHD(
            String permissionId,
            DistributorCode distributorCode,
            Integer supplyPointType,
            boolean productionSupport
    ) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.distributorCode = distributorCode;
        this.supplyPointType = supplyPointType;
        this.productionSupport = productionSupport;
    }

    protected EsAcceptedEventForVHD() {
        super();
        distributorCode = null;
        supplyPointType = null;
        productionSupport = false;
    }

    public DistributorCode distributorCode() {
        return distributorCode;
    }

    public Integer supplyPointType() {
        return supplyPointType;
    }

    public boolean isProductionSupport() {
        return productionSupport;
    }
}
