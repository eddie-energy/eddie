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
    private final ApplicationInformation applicationInformation;

    public ApplicationInformationService(ApplicationInformationRepository repository) {
        var optionalApplicationInformation = repository.findFirstByOrderByCreatedAtAsc();

        if (optionalApplicationInformation.isPresent()) {
            this.applicationInformation = optionalApplicationInformation.get();
        } else {
            var newApplicationInformation = new ApplicationInformation();
            repository.save(newApplicationInformation);
            LOGGER.info("Created ApplicationInformation.");
            this.applicationInformation = newApplicationInformation;
        }
    }

    public ApplicationInformation applicationInformation() {
        return this.applicationInformation;
    }
}
