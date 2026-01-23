// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.core.application.information.ApplicationInformation;
import energy.eddie.core.application.information.persistence.ApplicationInformationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RegionConnectorExtension
public class ApplicationInformationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInformationService.class);
    private final ApplicationInformationRepository repository;

    public ApplicationInformationService(ApplicationInformationRepository repository) {
        this.repository = repository;
    }

    public ApplicationInformation applicationInformation() {
        return repository.findFirstByOrderByCreatedAtDesc().orElseGet(() -> {
            LOGGER.info("Creating new ApplicationInformation");
            return repository.save(new ApplicationInformation());
        });
    }
}
