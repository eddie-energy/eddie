// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages;

import energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification.EdaCMNotificationInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke.EdaCMRevokeInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord.EdaConsumptionRecordInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification.EdaCPNotificationInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist.EdaECMPListInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata.EdaMasterDataInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata.MultiMasterDataInboundMessageFactory;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Service
public class InboundMessageFactoryCollection {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InboundMessageFactoryCollection.class);
    private final List<EdaConsumptionRecordInboundMessageFactory> inboundConsumptionRecordFactories;
    private final List<EdaMasterDataInboundMessageFactory> inboundMasterDataFactories;
    private final List<EdaCMNotificationInboundMessageFactory> inboundCMNotificationFactories;
    private final List<EdaCMRevokeInboundMessageFactory> inboundCMRevokeFactories;
    private final List<EdaCPNotificationInboundMessageFactory> inboundCPNotificationFactories;
    private final List<EdaECMPListInboundMessageFactory> ecmpListFactories;
    private EdaMasterDataInboundMessageFactory activeMasterDataFactory;
    private EdaConsumptionRecordInboundMessageFactory activeConsumptionRecordFactory;
    private EdaCMNotificationInboundMessageFactory activeCMNotificationFactory;
    private EdaCMRevokeInboundMessageFactory activeCMRevokeFactory;
    private EdaCPNotificationInboundMessageFactory activeCPNotificationFactory;
    private EdaECMPListInboundMessageFactory activeECMPListFactory;

    public InboundMessageFactoryCollection(
            List<EdaConsumptionRecordInboundMessageFactory> inboundConsumptionRecordFactories,
            List<EdaMasterDataInboundMessageFactory> inboundMasterDataFactories,
            List<EdaCMNotificationInboundMessageFactory> inboundCMNotificationFactories,
            List<EdaCMRevokeInboundMessageFactory> inboundCMRevokeFactories,
            List<EdaCPNotificationInboundMessageFactory> inboundCPNotificationFactories,
            List<EdaECMPListInboundMessageFactory> ecmpListFactories
    ) {
        this.inboundConsumptionRecordFactories = inboundConsumptionRecordFactories;
        this.inboundMasterDataFactories = inboundMasterDataFactories;
        this.inboundCMNotificationFactories = inboundCMNotificationFactories;
        this.inboundCMRevokeFactories = inboundCMRevokeFactories;
        this.inboundCPNotificationFactories = inboundCPNotificationFactories;
        this.ecmpListFactories = ecmpListFactories;
        activeConsumptionRecordFactory = findActiveFactory(inboundConsumptionRecordFactories)
                .orElseThrow(() -> new IllegalStateException("No active EdaConsumptionRecordInboundMessageFactory found"));
        activeMasterDataFactory = findActiveMasterDataFactory()
                .orElseThrow(() -> new IllegalStateException("No active EdaMasterDataInboundMessageFactory found"));
        activeCMNotificationFactory = findActiveFactory(inboundCMNotificationFactories)
                .orElseThrow(() -> new IllegalStateException("No active EdaCMNotificationInboundMessageFactory found"));
        activeCMRevokeFactory = findActiveFactory(inboundCMRevokeFactories)
                .orElseThrow(() -> new IllegalStateException("No active EdaCMRevokeInboundMessageFactory found"));
        activeCPNotificationFactory = findActiveFactory(inboundCPNotificationFactories)
                .orElseThrow(() -> new IllegalStateException("No active EdaCPNotificationInboundMessageFactory found"));
        activeECMPListFactory = findActiveFactory(ecmpListFactories)
                .orElseThrow(() -> new IllegalStateException("No active EdaECMPListInboundMessageFactory found"));
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
        findActiveFactory(inboundConsumptionRecordFactories)
                .ifPresentOrElse(
                        this::updateActiveConsumptionRecordFactory,
                        () -> LOGGER.error("No active EdaConsumptionRecordInboundMessageFactory found")
                );

        findActiveMasterDataFactory()
                .ifPresentOrElse(
                        this::updateActiveMasterDataFactory,
                        () -> LOGGER.error("No active EdaMasterDataInboundMessageFactory found")
                );

        findActiveFactory(inboundCMNotificationFactories)
                .ifPresentOrElse(
                        this::updateActiveCMNotificationFactory,
                        () -> LOGGER.error("No active EdaCMNotificationInboundMessageFactory found")
                );

        findActiveFactory(inboundCMRevokeFactories)
                .ifPresentOrElse(
                        this::updateActiveCMRevokeFactory,
                        () -> LOGGER.error("No active EdaCMRevokeInboundMessageFactory found")
                );

        findActiveFactory(inboundCPNotificationFactories)
                .ifPresentOrElse(
                        this::updateActiveCPNotificationFactory,
                        () -> LOGGER.error("No active EdaCPNotificationInboundMessageFactory found")
                );

        findActiveFactory(ecmpListFactories)
                .ifPresentOrElse(
                        this::updateActiveECMPListFactory,
                        () -> LOGGER.error("No active EdaECMPListInboundMessageFactory found")
                );
    }

    public EdaMasterDataInboundMessageFactory activeMasterDataFactory() {
        return activeMasterDataFactory;
    }

    public EdaConsumptionRecordInboundMessageFactory activeConsumptionRecordFactory() {
        return activeConsumptionRecordFactory;
    }

    public EdaCMNotificationInboundMessageFactory activeCMNotificationFactory() {
        return activeCMNotificationFactory;
    }

    public EdaCMRevokeInboundMessageFactory activeCMRevokeFactory() {
        return activeCMRevokeFactory;
    }

    public EdaCPNotificationInboundMessageFactory activeCPNotificationFactory() {
        return activeCPNotificationFactory;
    }

    public EdaECMPListInboundMessageFactory activeECMPListFactory() {
        return activeECMPListFactory;
    }

    /**
     * There can be multiple active master data factories, so this method will return a decorator, that delegates between the active factories.
     *
     * @return an EdaMasterDataInboundMessageFactory
     */
    private Optional<EdaMasterDataInboundMessageFactory> findActiveMasterDataFactory() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        var factories = inboundMasterDataFactories.stream()
                                                  .filter(factory -> factory.isActive(now))
                                                  .collect(Collectors.toSet());
        if (factories.isEmpty()) {
            return Optional.empty();
        }
        if (factories.size() != 1) {
            return Optional.of(new MultiMasterDataInboundMessageFactory(factories));
        }
        return factories.stream().findFirst();
    }

    private static <T extends PontonMessageFactory> Optional<T> findActiveFactory(List<T> factories) {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return factories.stream()
                        .filter(factory -> factory.isActive(now))
                        .findFirst();
    }

    private void updateActiveConsumptionRecordFactory(EdaConsumptionRecordInboundMessageFactory factory) {
        if (activeConsumptionRecordFactory != factory) {
            LOGGER.atInfo()
                  .addArgument(() -> activeConsumptionRecordFactory.getClass().getSimpleName())
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Switching active EdaConsumptionRecordInboundMessageFactory from {} to {}");

            activeConsumptionRecordFactory = factory;
        } else {
            LOGGER.atInfo()
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Active EdaConsumptionRecordInboundMessageFactory is still {}");
        }
    }

    private void updateActiveMasterDataFactory(EdaMasterDataInboundMessageFactory factory) {
        if (activeMasterDataFactory != factory) {
            LOGGER.atInfo()
                  .addArgument(() -> activeMasterDataFactory.getClass().getSimpleName())
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Switching active EdaMasterDataInboundMessageFactory from {} to {}");

            activeMasterDataFactory = factory;
        } else {
            LOGGER.atInfo()
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Active EdaMasterDataInboundMessageFactory is still {}");
        }
    }

    private void updateActiveCMNotificationFactory(EdaCMNotificationInboundMessageFactory factory) {
        if (activeCMNotificationFactory != factory) {
            LOGGER.atInfo()
                  .addArgument(() -> activeCMNotificationFactory.getClass().getSimpleName())
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Switching active EdaCMNotificationInboundMessageFactory from {} to {}");

            activeCMNotificationFactory = factory;
        } else {
            LOGGER.atInfo()
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Active EdaCMNotificationInboundMessageFactory is still {}");
        }
    }

    private void updateActiveCMRevokeFactory(EdaCMRevokeInboundMessageFactory factory) {
        if (activeCMRevokeFactory != factory) {
            LOGGER.atInfo()
                  .addArgument(() -> activeCMRevokeFactory.getClass().getSimpleName())
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Switching active EdaCMRevokeInboundMessageFactory from {} to {}");

            activeCMRevokeFactory = factory;
        } else {
            LOGGER.atInfo()
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Active EdaCMRevokeInboundMessageFactory is still {}");
        }
    }

    private void updateActiveCPNotificationFactory(EdaCPNotificationInboundMessageFactory factory) {
        if (activeCPNotificationFactory != factory) {
            LOGGER.atInfo()
                  .addArgument(() -> activeCPNotificationFactory.getClass().getSimpleName())
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Switching active EdaCPNotificationInboundMessageFactory from {} to {}");

            activeCPNotificationFactory = factory;
        } else {
            LOGGER.atInfo()
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Active EdaCPNotificationInboundMessageFactory is still {}");
        }
    }

    private void updateActiveECMPListFactory(EdaECMPListInboundMessageFactory factory) {
        if (activeECMPListFactory != factory) {
            LOGGER.atInfo()
                  .addArgument(() -> activeECMPListFactory.getClass().getSimpleName())
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Switching active EdaECMPListInboundMessageFactory from {} to {}");

            activeECMPListFactory = factory;
        } else {
            LOGGER.atInfo()
                  .addArgument(() -> factory.getClass().getSimpleName())
                  .log("Active EdaECMPListInboundMessageFactory is still {}");
        }
    }

}
