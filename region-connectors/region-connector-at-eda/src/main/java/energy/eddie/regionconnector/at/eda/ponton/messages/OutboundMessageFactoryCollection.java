// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages;

import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke.CMRevokeOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cprequest.CPRequestOutboundMessageFactory;
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
    private final List<CPRequestOutboundMessageFactory> outboundCPRequestFactories;

    private CMRequestOutboundMessageFactory activeCmRequestFactory;
    private CMRevokeOutboundMessageFactory activeCmRevokeFactory;
    private CPRequestOutboundMessageFactory activeCPRequestFactory;

    public OutboundMessageFactoryCollection(
            List<CMRequestOutboundMessageFactory> outboundCMRequestFactories,
            List<CMRevokeOutboundMessageFactory> outboundCMRevokeFactories,
            List<CPRequestOutboundMessageFactory> outboundCPRequestFactories
    ) {
        this.outboundCMRequestFactories = outboundCMRequestFactories;
        this.outboundCMRevokeFactories = outboundCMRevokeFactories;
        this.outboundCPRequestFactories = outboundCPRequestFactories;

        activeCmRequestFactory = findActiveCMRequestFactory()
                .orElseThrow(() -> new IllegalStateException("No active CMRequestOutboundMessageFactory found"));
        activeCmRevokeFactory = findActiveCMRevokeFactory()
                .orElseThrow(() -> new IllegalStateException("No active CMRevokeOutboundMessageFactory found"));
        activeCPRequestFactory = findActiveCPrequestFactory()
                .orElseThrow(() -> new IllegalStateException("No active CPRequestOutboundMessageFactory found"));
    }

    /**
     * EDA supports multiple versions of the same schema, but they have certain validity periods.
     * We can add support for multiple schema version of the same type, but only one of them can be active at a time.
     * This cron job goes through all the available factories and sets the active ones based on the current date.
     * This allows us to switch between different versions of the same message type without restarting the application.
     */
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

        findActiveCPrequestFactory().ifPresentOrElse(
                this::updateActiveCPRequestOutboundMessageFactory,
                () -> LOGGER.error("No active CPRequestOutboundMessageFactory found")
        );
    }

    public CMRequestOutboundMessageFactory activeCmRequestFactory() {
        return activeCmRequestFactory;
    }

    public CMRevokeOutboundMessageFactory activeCmRevokeFactory() {
        return activeCmRevokeFactory;
    }

    public CPRequestOutboundMessageFactory activeCPRequestFactory() {
        return activeCPRequestFactory;
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

    private Optional<CPRequestOutboundMessageFactory> findActiveCPrequestFactory() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return outboundCPRequestFactories.stream()
                                         .filter(factory -> factory.isActive(now))
                                         .findFirst();
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

    private void updateActiveCPRequestOutboundMessageFactory(CPRequestOutboundMessageFactory factory) {
        if (activeCPRequestFactory != factory) {
            LOGGER.atInfo()
                  .addArgument(() -> activeCPRequestFactory.getClass().getSimpleName())
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Switching active CPRequestOutboundMessageFactory from {} to {}");

            activeCPRequestFactory = factory;
        } else {
            LOGGER.atInfo()
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Active CPRequestOutboundMessageFactory is still {}");
        }
    }
}
