package energy.eddie.exampleappbackend.kafka;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.exampleappbackend.service.ValidatedHistoricalDataService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@AllArgsConstructor
public class ValidatedHistoricalDataConsumer {
    private final ValidatedHistoricalDataService validatedHistoricalDataService;

    @Transactional
    @KafkaListener(topics = "ep.eddie-demo-hardening.cim_0_82.validated-historical-data-md", containerFactory = "validatedHistoricalDataEnvelopeListenerContainerFactory")
    public void listen(ConsumerRecord<String, ValidatedHistoricalDataEnvelope> consumerRecord) {
        log.info("Received a new Validated Historical Data Message! Processing ...");
        if (consumerRecord.value() == null) {
            log.warn("Validated Historical Data Envelope is empty! Ignoring Message!");
            return;
        }
        validatedHistoricalDataService.handleValidatedHistoricalDataEnvelope(consumerRecord.value());
    }

}
