package energy.eddie.aiida.services;

import energy.eddie.aiida.application.information.ApplicationInformation;
import energy.eddie.aiida.application.information.persistence.ApplicationInformationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInformationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInformationService.class);
    private ApplicationInformation applicationInformation;

    public ApplicationInformationService(ApplicationInformationRepository repository) {
        createApplicationInformationIfExists(repository);
    }

    public ApplicationInformation applicationInformation() {
        return this.applicationInformation;
    }

    private void createApplicationInformationIfExists(ApplicationInformationRepository repository) {
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
}
