// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MqttUserRepository extends JpaRepository<MqttUser, String> {
    boolean existsByUsername(String permissionId);
}
