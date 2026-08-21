// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface InboundDataSourceRepository extends JpaRepository<InboundDataSource, UUID> {

    /**
     * Finds inbound data sources using one of the requested provisioning types. The provisioning connection, user, and
     * access-control entry are loaded eagerly so their MQTT publishers can be restored after application startup.
     *
     * @param provisioningTypes Provisioning types to include.
     * @return Matching inbound data sources with their provisioning configuration loaded.
     */
    @EntityGraph(attributePaths = {
            "inboundProvisioningMqttConfig.connection",
            "inboundProvisioningMqttConfig.connection.user",
            "inboundProvisioningMqttConfig.accessControlEntry"
    })
    List<InboundDataSource> findByProvisioningTypeIn(
            Set<InboundProvisioningType> provisioningTypes
    );
}
