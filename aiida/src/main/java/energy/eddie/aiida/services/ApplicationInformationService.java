package energy.eddie.aiida.services;

import energy.eddie.aiida.application.information.ApplicationInformation;
import energy.eddie.aiida.application.information.persistence.ApplicationInformationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
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
