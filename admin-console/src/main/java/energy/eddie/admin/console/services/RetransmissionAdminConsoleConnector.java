package energy.eddie.admin.console.services;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequestRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
public class RetransmissionAdminConsoleConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetransmissionAdminConsoleConnector.class);
    private final RetransmissionRequestRouter retransmissionRequestRouter;

    public RetransmissionAdminConsoleConnector(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") RetransmissionRequestRouter retransmissionRequestRouter) {
        requireNonNull(retransmissionRequestRouter);
        this.retransmissionRequestRouter = retransmissionRequestRouter;
    }


    public void retransmit(String regionConnectorId, RetransmissionRequest retransmissionRequest) {
        LOGGER.info("Requesting retransmission of {} for region connector with id {}",
                    retransmissionRequest,
                    regionConnectorId);
        retransmissionRequestRouter
                .routeRetransmissionRequest(regionConnectorId, retransmissionRequest)
                .doOnError(e -> LOGGER.atError()
                                      .addArgument(retransmissionRequest)
                                      .addArgument(regionConnectorId)
                                      .setCause(e)
                                      .log("Error while requesting retransmission of {} for region connector with id {}")
                )
                .onErrorComplete()
                .subscribe(result -> {
                    // Update this with GH-1388
                    LOGGER.info(
                            "Retransmission request for {} for region connector with id {} completed with result {}",
                            retransmissionRequest,
                            regionConnectorId,
                            result);
                });
    }
}
