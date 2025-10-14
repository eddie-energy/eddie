package energy.eddie.aiida.publisher;

import energy.eddie.aiida.dtos.events.AiidaEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class AiidaEventPublisher implements EventPublisher<AiidaEvent> {
    private final ApplicationEventPublisher publisher;

    protected AiidaEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishEvent(AiidaEvent event) {
        publisher.publishEvent(event);
    }
}
