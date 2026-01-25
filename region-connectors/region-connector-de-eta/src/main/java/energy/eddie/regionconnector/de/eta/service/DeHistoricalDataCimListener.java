package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.de.eta.providers.cim.EtaToCimMapper;
import energy.eddie.regionconnector.de.eta.providers.cim.v104.DeValidatedHistoricalDataMarketDocumentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Listens to the ValidatedHistoricalDataStream and converts incoming data
 * into CIM v1.04 Market Documents for distribution.
 * * This bridges the gap between the internal data stream and the external market provider.
 */
@Service
public class DeHistoricalDataCimListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeHistoricalDataCimListener.class);

    private final EtaToCimMapper mapper;
    private final DeValidatedHistoricalDataMarketDocumentProvider publisher;

    public DeHistoricalDataCimListener(
            ValidatedHistoricalDataStream stream,
            EtaToCimMapper mapper,
            DeValidatedHistoricalDataMarketDocumentProvider publisher
    ) {
        this.mapper = mapper;
        this.publisher = publisher;

        // Automatically subscribe to the stream on startup
        stream.validatedHistoricalData()
                .subscribe(this::processData);
    }

    private void processData(IdentifiableValidatedHistoricalData data) {
        try {
            // Map the internal data format to the CIM Envelope
            Optional<VHDEnvelope> envelope = mapper.mapToEnvelope(
                    data.permissionRequest(),
                    data.payload().readings()
            );

            // Emit the document to the market provider
            if (envelope.isPresent()) {
                publisher.emitDocument(envelope.get());
                LOGGER.debug("Dispatched CIM document for permission {}", data.permissionRequest().permissionId());
            } else {
                LOGGER.warn("Failed to map data for permission {}", data.permissionRequest().permissionId());
            }

        } catch (Exception e) {
            LOGGER.error("Error processing CIM generation for permission {}", data.permissionRequest().permissionId(), e);
        }
    }
}