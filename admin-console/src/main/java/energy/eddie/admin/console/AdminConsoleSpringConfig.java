package energy.eddie.admin.console;

import energy.eddie.admin.console.data.StatusMessageRepository;
import energy.eddie.admin.console.data.StatusMessageService;
import energy.eddie.api.v0_82.ConsentMarketDocumentServiceInterface;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@Configuration
@EnableWebMvc
@SpringBootApplication
public class AdminConsoleSpringConfig {

    @Bean
    public StatusMessageService statusMessageService(StatusMessageRepository statusMessageRepository,
                                                     ConsentMarketDocumentServiceInterface consentMarketDocumentService) {
        StatusMessageService service = new StatusMessageService(statusMessageRepository, consentMarketDocumentService);
        service.subscribeToFlux();
        return service;
    }
}
