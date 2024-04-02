package energy.eddie.regionconnector.at.eda.ponton.messages;

import energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification.EdaCMNotificationInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord.EdaConsumptionRecordInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata.EdaMasterDataInboundMessageFactory;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Service
public class InboundMessageFactoryCollection {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InboundMessageFactoryCollection.class);
    private final List<EdaConsumptionRecordInboundMessageFactory> inboundConsumptionRecordFactories;
    private final List<EdaMasterDataInboundMessageFactory> inboundMasterDataFactories;
    private final List<EdaCMNotificationInboundMessageFactory> inboundCMNotificationFactories;
    private EdaMasterDataInboundMessageFactory activeMasterDataFactory;
    private EdaConsumptionRecordInboundMessageFactory activeConsumptionRecordFactory;
    private EdaCMNotificationInboundMessageFactory activeCMNotificationFactory;

    public InboundMessageFactoryCollection(
            List<EdaConsumptionRecordInboundMessageFactory> inboundConsumptionRecordFactories,
            List<EdaMasterDataInboundMessageFactory> inboundMasterDataFactories,
            List<EdaCMNotificationInboundMessageFactory> inboundCMNotificationFactories
    ) {
        this.inboundConsumptionRecordFactories = inboundConsumptionRecordFactories;
        this.inboundMasterDataFactories = inboundMasterDataFactories;
        this.inboundCMNotificationFactories = inboundCMNotificationFactories;
        activeConsumptionRecordFactory = findActiveConsumptionRecordFactory()
                .orElseThrow(() -> new IllegalStateException("No active EdaConsumptionRecordInboundMessageFactory found"));
        activeMasterDataFactory = findActiveMasterDataFactory()
                .orElseThrow(() -> new IllegalStateException("No active EdaMasterDataInboundMessageFactory found"));
        activeCMNotificationFactory = findActiveCMNotificationFactory()
                .orElseThrow(() -> new IllegalStateException("No active EdaCMNotificationInboundMessageFactory found"));
    }

    private Optional<EdaConsumptionRecordInboundMessageFactory> findActiveConsumptionRecordFactory() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return inboundConsumptionRecordFactories.stream()
                                                .filter(factory -> factory.isActive(now))
                                                .findFirst();
    }

    private Optional<EdaMasterDataInboundMessageFactory> findActiveMasterDataFactory() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return inboundMasterDataFactories.stream()
                                         .filter(factory -> factory.isActive(now))
                                         .findFirst();
    }

    private Optional<EdaCMNotificationInboundMessageFactory> findActiveCMNotificationFactory() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return inboundCMNotificationFactories.stream()
                                             .filter(factory -> factory.isActive(now))
                                             .findFirst();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Europe/Vienna")
    public void updateActiveFactories() {
        LOGGER.info("Checking for active Factories");
        findActiveConsumptionRecordFactory()
                .ifPresentOrElse(
                        this::updateActiveConsumptionRecordFactory,
                        () -> LOGGER.error("No active EdaConsumptionRecordInboundMessageFactory found")
                );

        findActiveMasterDataFactory()
                .ifPresentOrElse(
                        this::updateActiveMasterDataFactory,
                        () -> LOGGER.error("No active EdaMasterDataInboundMessageFactory found")
                );

        findActiveCMNotificationFactory()
                .ifPresentOrElse(
                        this::updateActiveCMNotificationFactory,
                        () -> LOGGER.error("No active EdaCMNotificationInboundMessageFactory found")
                );
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


    public EdaMasterDataInboundMessageFactory activeMasterDataFactory() {
        return activeMasterDataFactory;
    }


    public EdaConsumptionRecordInboundMessageFactory activeConsumptionRecordFactory() {
        return activeConsumptionRecordFactory;
    }

    public EdaCMNotificationInboundMessageFactory activeCMNotificationFactory() {
        return activeCMNotificationFactory;
    }
}
