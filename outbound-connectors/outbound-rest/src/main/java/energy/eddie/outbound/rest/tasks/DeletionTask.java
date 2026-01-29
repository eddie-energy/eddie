// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.tasks;

import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.persistence.specifications.InsertionTimeSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DeletionTask<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletionTask.class);
    private final JpaSpecificationExecutor<T> repository;
    private final RestOutboundConnectorConfiguration restConfig;

    public DeletionTask(
            JpaSpecificationExecutor<T> repository,
            RestOutboundConnectorConfiguration restConfig
    ) {
        this.repository = repository;
        this.restConfig = restConfig;
    }

    @Scheduled(cron = "${outbound-connector.rest.retention-removal:0 0 * * * *}")
    public void delete() {
        var timestamp = ZonedDateTime.now(ZoneOffset.UTC).minus(restConfig.retentionTime());
        LOGGER.debug("Deleting all records inserted before {}", timestamp);
        var where = InsertionTimeSpecification.<T>insertedBeforeEquals(timestamp);
        repository.delete(where);
    }
}
