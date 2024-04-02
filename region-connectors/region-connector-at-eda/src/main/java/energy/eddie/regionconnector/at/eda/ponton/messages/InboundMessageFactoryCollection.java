package energy.eddie.regionconnector.at.eda.ponton.messages;

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
    private EdaMasterDataInboundMessageFactory activeMasterDataFactory;
    private EdaConsumptionRecordInboundMessageFactory activeConsumptionRecordFactory;

    public InboundMessageFactoryCollection(
            List<EdaConsumptionRecordInboundMessageFactory> inboundConsumptionRecordFactories,
            List<EdaMasterDataInboundMessageFactory> inboundMasterDataFactories
    ) {
        this.inboundConsumptionRecordFactories = inboundConsumptionRecordFactories;
        this.inboundMasterDataFactories = inboundMasterDataFactories;
        activeConsumptionRecordFactory = findActiveConsumptionRecordFactory()
                .orElseThrow(() -> new IllegalStateException("No active EdaConsumptionRecordInboundMessageFactory found"));
        activeMasterDataFactory = findActiveMasterDataFactory()
                .orElseThrow(() -> new IllegalStateException("No active EdaMasterDataInboundMessageFactory found"));
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


    public EdaMasterDataInboundMessageFactory activeMasterDataFactory() {
        return activeMasterDataFactory;
    }


    public EdaConsumptionRecordInboundMessageFactory activeConsumptionRecordFactory() {
        return activeConsumptionRecordFactory;
    }
}
