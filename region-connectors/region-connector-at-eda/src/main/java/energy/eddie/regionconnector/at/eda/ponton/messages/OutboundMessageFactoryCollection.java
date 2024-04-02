package energy.eddie.regionconnector.at.eda.ponton.messages;

import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke.CMRevokeOutboundMessageFactory;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Service
public class OutboundMessageFactoryCollection {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OutboundMessageFactoryCollection.class);
    private final List<CMRequestOutboundMessageFactory> outboundCMRequestFactories;
    private final List<CMRevokeOutboundMessageFactory> outboundCMRevokeFactories;

    private CMRequestOutboundMessageFactory activeCmRequestFactory;
    private CMRevokeOutboundMessageFactory activeCmRevokeFactory;

    public OutboundMessageFactoryCollection(
            List<CMRequestOutboundMessageFactory> outboundCMRequestFactories,
            List<CMRevokeOutboundMessageFactory> outboundCMRevokeFactories
    ) {
        this.outboundCMRequestFactories = outboundCMRequestFactories;
        this.outboundCMRevokeFactories = outboundCMRevokeFactories;

        activeCmRequestFactory = findActiveCMRequestFactory()
                .orElseThrow(() -> new IllegalStateException("No active CMRequestOutboundMessageFactory found"));
        activeCmRevokeFactory = findActiveCMRevokeFactory()
                .orElseThrow(() -> new IllegalStateException("No active CMRevokeOutboundMessageFactory found"));
    }

    private Optional<CMRequestOutboundMessageFactory> findActiveCMRequestFactory() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return outboundCMRequestFactories.stream()
                                         .filter(factory -> factory.isActive(now))
                                         .findFirst();
    }

    private Optional<CMRevokeOutboundMessageFactory> findActiveCMRevokeFactory() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return outboundCMRevokeFactories.stream()
                                        .filter(factory -> factory.isActive(now))
                                        .findFirst();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Europe/Vienna")
    public void updateActiveFactories() {
        LOGGER.info("Checking for active Factories");
        findActiveCMRequestFactory()
                .ifPresentOrElse(
                        this::updateActiveCMRequestOutboundMessageFactory,
                        () -> LOGGER.error("No active CMRequestOutboundMessageFactory found")
                );

        findActiveCMRevokeFactory().ifPresentOrElse(
                this::updateActiveCMRevokeOutboundMessageFactory,
                () -> LOGGER.error("No active CMRevokeOutboundMessageFactory found")
        );
    }

    private void updateActiveCMRequestOutboundMessageFactory(CMRequestOutboundMessageFactory factory) {
        if (activeCmRequestFactory != factory) {
            LOGGER.atInfo()
                  .addArgument(() -> activeCmRequestFactory.getClass().getSimpleName())
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Switching active CMRequestOutboundMessageFactory from {} to {}");

            activeCmRequestFactory = factory;
        } else {
            LOGGER.atInfo()
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Active CMRequestOutboundMessageFactory is still {}");
        }
    }

    private void updateActiveCMRevokeOutboundMessageFactory(CMRevokeOutboundMessageFactory factory) {
        if (activeCmRevokeFactory != factory) {
            LOGGER.atInfo()
                  .addArgument(() -> activeCmRevokeFactory.getClass().getSimpleName())
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Switching active CMRevokeOutboundMessageFactory from {} to {}");

            activeCmRevokeFactory = factory;
        } else {
            LOGGER.atInfo()
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Active CMRevokeOutboundMessageFactory is still {}");
        }
    }

    public CMRequestOutboundMessageFactory activeCmRequestFactory() {
        return activeCmRequestFactory;
    }

    public CMRevokeOutboundMessageFactory activeCmRevokeFactory() {
        return activeCmRevokeFactory;
    }
}
