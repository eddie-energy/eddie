package energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

import java.time.ZoneId;

public class ConsentMarketDocumentExtension<T extends PermissionRequest> implements Extension<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentMarketDocumentExtension.class);
    private final Sinks.Many<ConsentMarketDocument> cmdSink;
    private final String customerIdentifier;
    private final TransmissionScheduleProvider<T> transmissionScheduleProvider;
    private final String countryCode;
    private final ZoneId zoneId;

    public ConsentMarketDocumentExtension(
            Sinks.Many<ConsentMarketDocument> cmdSink,
            String customerIdentifier,
            String countryCode,
            ZoneId zoneId
    ) {
        this(cmdSink, ignored -> null, customerIdentifier, countryCode, zoneId);
    }

    public ConsentMarketDocumentExtension(
            Sinks.Many<ConsentMarketDocument> cmdSink,
            TransmissionScheduleProvider<T> transmissionScheduleProvider,
            String customerIdentifier,
            String countryCode, ZoneId zoneId
    ) {
        this.cmdSink = cmdSink;
        this.customerIdentifier = customerIdentifier;
        this.transmissionScheduleProvider = transmissionScheduleProvider;
        this.countryCode = countryCode;
        this.zoneId = zoneId;
    }

    @Override
    public void accept(T permissionRequest) {
        try {
            cmdSink.tryEmitNext(
                    new IntermediateConsentMarketDocument<>(
                            permissionRequest,
                            customerIdentifier,
                            transmissionScheduleProvider,
                            countryCode,
                            zoneId
                    ).toConsentMarketDocument()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Error while trying to emit ConsentMarketDocument.", exception);
            cmdSink.tryEmitError(exception);
        }
    }
}
