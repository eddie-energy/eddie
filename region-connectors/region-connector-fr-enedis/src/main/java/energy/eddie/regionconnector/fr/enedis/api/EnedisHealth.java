// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.api;

import org.springframework.boot.health.contributor.Health;

import java.util.Map;

public interface EnedisHealth {
    Map<String, Health> health();
}
