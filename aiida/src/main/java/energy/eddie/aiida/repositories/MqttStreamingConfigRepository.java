// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MqttStreamingConfigRepository extends JpaRepository<MqttStreamingConfig, UUID> {
}
