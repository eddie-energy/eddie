package energy.eddie.regionconnector.at.eda.ponton.messages;

import energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord.EdaConsumptionRecordInboundMessageFactory;
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

    private EdaConsumptionRecordInboundMessageFactory activeConsumptionRecordFactory;

    public InboundMessageFactoryCollection(
            List<EdaConsumptionRecordInboundMessageFactory> inboundConsumptionRecordFactories
    ) {
        this.inboundConsumptionRecordFactories = inboundConsumptionRecordFactories;
        activeConsumptionRecordFactory = findActiveConsumptionRecordFactory()
                .orElseThrow(() -> new IllegalStateException("No active EdaConsumptionRecordInboundMessageFactory found"));
    }

    private Optional<EdaConsumptionRecordInboundMessageFactory> findActiveConsumptionRecordFactory() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return inboundConsumptionRecordFactories.stream()
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


    public EdaConsumptionRecordInboundMessageFactory activeConsumptionRecordFactory() {
        return activeConsumptionRecordFactory;
    }
}
