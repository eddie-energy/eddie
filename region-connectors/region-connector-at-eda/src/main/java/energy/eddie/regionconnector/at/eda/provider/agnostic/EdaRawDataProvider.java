package energy.eddie.regionconnector.at.eda.provider.agnostic;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

@Component
@ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
public class EdaRawDataProvider implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRawDataProvider.class);
    private final Marshaller marshaller;
    private final Flux<RawDataMessage> rawDataStream;

    public EdaRawDataProvider(Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordFlux) throws JAXBException {
        requireNonNull(identifiableConsumptionRecordFlux);

        this.marshaller = JAXBContext.newInstance(ConsumptionRecord.class).createMarshaller();
        this.rawDataStream = identifiableConsumptionRecordFlux
                .flatMap(this::mapToRawDataMessage); // the mapping method is called for each element for each subscriber if we at some point have multiple subscribers, consider using publish().refCount()
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return rawDataStream;
    }

    private Flux<RawDataMessage> mapToRawDataMessage(IdentifiableConsumptionRecord identifiableConsumptionRecord) {

        var writer = new StringWriter();
        try {
            marshaller.marshal(identifiableConsumptionRecord.consumptionRecord(), writer);
        } catch (JAXBException e) {
            LOGGER.error("Error while marshalling ConsumptionRecord back into XML for raw data output", e);
            return Flux.empty();
        }
        String rawXml = writer.toString();
        return Flux.fromIterable(identifiableConsumptionRecord.permissionRequests())
                .map(permissionRequest -> new RawDataMessage(
                        permissionRequest.permissionId(),
                        permissionRequest.connectionId(),
                        permissionRequest.dataNeedId(),
                        permissionRequest.dataSourceInformation(),
                        ZonedDateTime.now(ZoneId.of("UTC")),
                        rawXml
                ));
    }

    @Override
    public void close() {
        // Nothing to clean up, flux is closed when the underlying flux is closed
    }
}