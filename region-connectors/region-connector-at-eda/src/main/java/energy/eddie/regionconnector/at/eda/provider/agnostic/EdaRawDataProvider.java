package energy.eddie.regionconnector.at.eda.provider.agnostic;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

@Component
@ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
public class EdaRawDataProvider implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRawDataProvider.class);
    private final Jaxb2Marshaller marshaller;
    private final Flux<RawDataMessage> rawDataStream;

    public EdaRawDataProvider(
            Jaxb2Marshaller marshaller,
            Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordFlux
    ) {
        requireNonNull(marshaller);
        requireNonNull(identifiableConsumptionRecordFlux);
        this.marshaller = marshaller;

        this.rawDataStream = identifiableConsumptionRecordFlux
                .flatMap(this::mapToRawDataMessage); // the mapping method is called for each element for each subscriber if we at some point have multiple subscribers, consider using publish().refCount()
    }

    private Flux<RawDataMessage> mapToRawDataMessage(IdentifiableConsumptionRecord identifiableConsumptionRecord) {

        var writer = new StringWriter();
        try {
            marshaller.marshal(identifiableConsumptionRecord.consumptionRecord()
                                                            .originalConsumptionRecord(),
                               new StreamResult(writer));
        } catch (XmlMappingException e) {
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
    public Flux<RawDataMessage> getRawDataStream() {
        return rawDataStream;
    }

    @Override
    public void close() {
        // Nothing to clean up, flux is closed when the underlying flux is closed
    }
}
