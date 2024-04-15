package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.KafkaStreamingConfig;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.aiida.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.internals.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.UUID;

@Component
public class PermissionCreationValidationSendingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCreationValidationSendingService.class);
    private final Outbox outbox;
    private final DataNeedsService dataNeedsService;
    private final Clock clock;
    private final AiidaConfiguration configuration;

    public PermissionCreationValidationSendingService(
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // defined in core
            DataNeedsService dataNeedsService,
            Clock clock,
            AiidaConfiguration configuration
    ) {
        this.outbox = outbox;
        this.dataNeedsService = dataNeedsService;
        this.clock = clock;
        this.configuration = configuration;
    }

    public PermissionDto createValidateAndSendPermissionRequest(
            PermissionRequestForCreation forCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionId = UUID.randomUUID().toString();
        LOGGER.info("Creating new permission request with ID {}", permissionId);

        LocalDate today = LocalDate.now(clock);

        DataNeedWrapper wrapper = dataNeedsService.findDataNeedAndCalculateStartAndEnd(
                forCreation.dataNeedId(),
                today,
                // in case of open end/start, fixed values are used
                Period.ZERO,
                Period.ZERO);

        if (!(wrapper.timeframedDataNeed() instanceof GenericAiidaDataNeed genericAiidaDataNeed))
            throw new UnsupportedDataNeedException(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   forCreation.dataNeedId(),
                                                   "Only GenericDataNeeds are currently supported.");

        // use nice looking fixed values if data need uses open start/end --> only possible for AIIDA because it has no real limits how long data can be accessed
        LocalDate startDate = getAppropriateStartDate(wrapper);
        LocalDate endDate = getAppropriateEndDate(wrapper);

        String terminationTopic = terminationTopicForPermissionId(permissionId);
        var createdEvent = new CreatedEvent(permissionId,
                                            forCreation.connectionId(),
                                            forCreation.dataNeedId(),
                                            startDate,
                                            endDate,
                                            terminationTopic);

        outbox.commit(createdEvent);
        // no validation for AIIDA requests necessary
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.VALIDATED));
        // we consider displaying the QR code to the user as SENT_TO_PERMISSION_ADMINISTRATOR for AIIDA
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));

        var kafkaConfig = new KafkaStreamingConfig(
                configuration.kafkaBoostrapServers(),
                configuration.kafkaDataTopic(),
                configuration.kafkaStatusMessagesTopic(),
                terminationTopic
        );

        return new PermissionDto(
                permissionId,
                genericAiidaDataNeed.name(),
                genericAiidaDataNeed.id(),
                ZonedDateTime.of(startDate, LocalTime.MIN, ZoneOffset.UTC).toInstant(),
                ZonedDateTime.of(endDate, LocalTime.MAX.withNano(0), ZoneOffset.UTC).toInstant(),
                createdEvent.connectionId(),
                genericAiidaDataNeed.dataTags(),
                kafkaConfig
        );
    }

    /**
     * Returns the calculated start date from the wrapper or the fixed start date if the data need is using open start.
     *
     * @param wrapper Wrapper as returned from
     *                {@link DataNeedsService#findDataNeedAndCalculateStartAndEnd(String, LocalDate, Period, Period)}
     * @return LocalDate to use as start date for the permission request.
     */
    private static LocalDate getAppropriateStartDate(DataNeedWrapper wrapper) {
        if (wrapper.timeframedDataNeed()
                   .duration() instanceof RelativeDuration relativeDuration && relativeDuration.start().isEmpty())
            return LocalDate.of(2000, 1, 1);

        return wrapper.calculatedStart();
    }

    /**
     * Returns the calculated end date from the wrapper or the fixed end date if the data need is using open start.
     *
     * @param wrapper Wrapper as returned from
     *                {@link DataNeedsService#findDataNeedAndCalculateStartAndEnd(String, LocalDate, Period, Period)}
     * @return LocalDate to use as end date for the permission request.
     */
    private static LocalDate getAppropriateEndDate(DataNeedWrapper wrapper) {
        if (wrapper.timeframedDataNeed()
                   .duration() instanceof RelativeDuration relativeDuration && relativeDuration.end().isEmpty())
            return LocalDate.of(9999, 12, 31);

        return wrapper.calculatedEnd();
    }

    /**
     * Creates the termination topic name by concatenating the termination topic prefix and the permissionId with an
     * underscore.
     *
     * @param permissionId Id of permission request
     * @return Kafka topic name
     * @throws InvalidTopicException If the resulting topic name is invalid. This indicates that either the permissionId
     *                               is not a valid UUID-4, or the prefix is invalid.
     */
    private String terminationTopicForPermissionId(String permissionId) throws InvalidTopicException {
        var topic = configuration.kafkaTerminationTopicPrefix() + "_" + permissionId;
        Topic.validate(topic);
        return topic;
    }
}
