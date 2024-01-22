package energy.eddie.regionconnector.at.eda.processing.agnostic;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Flow;

@Component
@ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
public class EdaRawDataProvider implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRawDataProvider.class);
    private final EdaAdapter adapter;
    private final PermissionRequestService permissionRequestService;
    private final Marshaller marshaller;

    public EdaRawDataProvider(EdaAdapter adapter, PermissionRequestService permissionRequestService) throws JAXBException {
        this.adapter = adapter;
        this.permissionRequestService = permissionRequestService;

        marshaller = JAXBContext.newInstance(ConsumptionRecord.class).createMarshaller();
    }

    @Override
    public Flow.Publisher<RawDataMessage> getRawDataStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(adapter.getConsumptionRecordStream()
                .mapNotNull(this::createRawDataMessage));
    }

    private @Nullable RawDataMessage createRawDataMessage(ConsumptionRecord consumptionRecord) {
        String conversationId = consumptionRecord.getProcessDirectory().getConversationId();
        var requestOptional = permissionRequestService.findByConversationIdOrCMRequestId(conversationId, null);

        if (requestOptional.isEmpty()) {
            LOGGER.error("No permission for conversationId {} found in repository.", conversationId);
            return null;
        }

        var request = requestOptional.get();
        var writer = new StringWriter();
        try {
            marshaller.marshal(consumptionRecord, writer);
        } catch (JAXBException e) {
            LOGGER.error("Error while marshalling ConsumptionRecord back into XML for raw data output", e);
            return null;
        }

        return new RawDataMessage(request.permissionId(), request.connectionId(), request.dataNeedId(),
                request.dataSourceInformation(), ZonedDateTime.now(ZoneId.of("UTC")), writer.toString());
    }

    @Override
    public void close() {
        // complete is emitted when adapter#get completes
    }
}
