package energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

public class ConsentMarketDocumentExtension<T extends TimeframedPermissionRequest> implements Extension<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentMarketDocumentExtension.class);
    private final Sinks.Many<ConsentMarketDocument> cmdSink;
    private final String customerIdentifier;
    private final TransmissionScheduleProvider<T> transmissionScheduleProvider;
    private final String countryCode;

    public ConsentMarketDocumentExtension(Sinks.Many<ConsentMarketDocument> cmdSink,
                                          TransmissionScheduleProvider<T> transmissionScheduleProvider,
                                          String customerIdentifier,
                                          String countryCode) {
        this.cmdSink = cmdSink;
        this.customerIdentifier = customerIdentifier;
        this.transmissionScheduleProvider = transmissionScheduleProvider;
        this.countryCode = countryCode;
    }

    public ConsentMarketDocumentExtension(Sinks.Many<ConsentMarketDocument> cmdSink, String customerIdentifier, String countryCode) {
        this(cmdSink, ignored -> null, customerIdentifier, countryCode);
    }

    @Override
    public void accept(T permissionRequest) {
        try {
            cmdSink.tryEmitNext(
                    new IntermediateConsentMarketDocument<>(
                            permissionRequest,
                            customerIdentifier,
                            transmissionScheduleProvider,
                            countryCode
                    ).toConsentMarketDocument()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Error while trying to emit ConsentMarketDocument.", exception);
            cmdSink.tryEmitError(exception);
        }

    }
}
