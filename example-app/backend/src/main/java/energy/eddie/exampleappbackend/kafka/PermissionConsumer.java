package energy.eddie.exampleappbackend.kafka;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.exampleappbackend.config.ExampleAppConfig;
import energy.eddie.exampleappbackend.service.PermissionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@AllArgsConstructor
public class PermissionConsumer {
    private final PermissionService permissionService;
    private final ExampleAppConfig exampleAppConfig;

    @Transactional
    @KafkaListener(topics = "ep.${example-app.eddie-id}.${example-app.kafka-permission-cim-version}.permission-md", containerFactory = "permissionEnvelopeListenerContainerFactory")
    public void listen(ConsumerRecord<String, PermissionEnvelope> consumerRecord) {
        log.info("Received a new Permission Envelope Message! Processing ...");
        if (consumerRecord.value() == null) {
            log.warn("Permission Envelope is empty! Ignoring Message!");
            return;
        }
        permissionService.handlePermissionEnvelope(consumerRecord.value());
    }

}

