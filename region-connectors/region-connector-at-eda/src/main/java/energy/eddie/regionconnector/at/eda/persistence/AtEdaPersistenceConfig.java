// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackageClasses = JpaPermissionRequestRepository.class)
@EntityScan(basePackageClasses = {AtEdaPersistenceConfig.class, EdaPermissionRequest.class})
@Configuration
public class AtEdaPersistenceConfig {
}
